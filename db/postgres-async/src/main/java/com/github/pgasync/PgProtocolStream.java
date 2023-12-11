/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pgasync;

import com.github.pgasync.message.ExtendedQueryMessage;
import com.github.pgasync.message.Message;
import com.github.pgasync.message.backend.Authentication;
import com.github.pgasync.message.backend.BIndicators;
import com.github.pgasync.message.backend.BackendKeyData;
import com.github.pgasync.message.backend.CommandComplete;
import com.github.pgasync.message.backend.DataRow;
import com.github.pgasync.message.backend.ErrorResponse;
import com.github.pgasync.message.backend.NoticeResponse;
import com.github.pgasync.message.backend.NotificationResponse;
import com.github.pgasync.message.backend.ParameterStatus;
import com.github.pgasync.message.backend.ReadyForQuery;
import com.github.pgasync.message.backend.RowDescription;
import com.github.pgasync.message.backend.UnknownMessage;
import com.github.pgasync.message.frontend.*;
import com.github.pgasync.net.SqlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Messages stream to Postgres backend.
 *
 * @author Marat Gainullin
 */
public abstract class PgProtocolStream implements ProtocolStream {
    private static final Logger log = LoggerFactory.getLogger(PgProtocolStream.class);

    protected final Executor futuresExecutor;
    protected final Charset encoding;

    private CompletableFuture<? super Message> onResponse;
    private final Map<String, Set<Consumer<String>>> subscriptions = new HashMap<>();

    private Consumer<RowDescription.ColumnDescription[]> onColumns;
    private Consumer<DataRow> onRow;
    private Consumer<CommandComplete> onAffected;

    private boolean seenReadyForQuery;
    private Message readyForQueryPendingMessage;
    private Message lastSentMessage;

    public PgProtocolStream(Charset encoding, Executor futuresExecutor) {
        this.encoding = encoding;
        this.futuresExecutor = futuresExecutor;
    }

    private CompletableFuture<? super Message> consumeOnResponse() {
        CompletableFuture<? super Message> wasOnResponse = onResponse;
        onResponse = null;
        return wasOnResponse;
    }

    @Override
    public CompletableFuture<Message> authenticate(String userName, String password, Authentication authRequired) {
        if (authRequired.saslScramSha256()) {
            var clientNonce = UUID.randomUUID().toString();
            var saslInitialResponse = new SASLInitialResponse(Authentication.SUPPORTED_SASL,
                                                              null, ""/*SaslPrep.asQueryString(userName) - Postgres requires an empty string here*/,
                                                              clientNonce);
            return send(saslInitialResponse)
                    .thenApply(message -> {
                        if (message instanceof Authentication authentication) {
                            var serverFirstMessage = authentication.saslContinueData();
                            if (serverFirstMessage != null) {
                                return send(SASLResponse.of(password, serverFirstMessage, clientNonce, saslInitialResponse.gs2Header(), saslInitialResponse.clientFirstMessageBare()));
                            } else {
                                throw new IllegalStateException("Bad SASL authentication sequence message detected on 'server-first-message' step");
                            }
                        } else {
                            throw new IllegalStateException("Bad SASL authentication sequence detected on 'server-first-message' step");
                        }
                    })
                    .thenCompose(Function.identity());
        } else {
            return send(PasswordMessage.passwordMessage(userName, password, authRequired.md5salt(), encoding));
        }
    }

    protected abstract void write(Message... messages);

    @Override
    public CompletableFuture<Message> send(Message message) {
        return offerRoundTrip(() -> {
            lastSentMessage = message;
            write(message);
            if (message instanceof ExtendedQueryMessage) {
                write(FIndicators.SYNC);
            }
        });
    }

    @Override
    public CompletableFuture<Void> send(Query query, Consumer<RowDescription.ColumnDescription[]> onColumns, Consumer<DataRow> onRow, Consumer<CommandComplete> onAffected) {
        this.onColumns = onColumns;
        this.onRow = onRow;
        this.onAffected = onAffected;
        return send(query).thenAccept(_ -> {});
    }

    @Override
    public CompletableFuture<Integer> send(Bind bind, Describe describe, Consumer<RowDescription.ColumnDescription[]> onColumns, Consumer<DataRow> onRow) {
        this.onColumns = onColumns;
        this.onRow = onRow;
        this.onAffected = null;
        return offerRoundTrip(() -> {
            Execute execute;
            lastSentMessage = execute = new Execute();
            write(bind, describe, execute, FIndicators.SYNC);
        }).thenApply(commandComplete -> ((CommandComplete) commandComplete).affectedRows());
    }

    @Override
    public CompletableFuture<Integer> send(Bind bind, Consumer<DataRow> onRow) {
        this.onColumns = null;
        this.onRow = onRow;
        this.onAffected = null;
        return offerRoundTrip(() -> {
            Execute execute;
            lastSentMessage = execute = new Execute();
            write(bind, execute, FIndicators.SYNC);
        }).thenApply(commandComplete -> ((CommandComplete) commandComplete).affectedRows());
    }

    @Override
    public Runnable subscribe(String channel, Consumer<String> onNotification) {
        subscriptions
                .computeIfAbsent(channel, _ -> new HashSet<>())
                .add(onNotification);
        return () -> subscriptions.computeIfPresent(channel, (_, subscription) -> {
            subscription.remove(onNotification);
            return subscription.isEmpty() ? null : subscription;
        });
    }

    protected void gotException(Throwable th) {
        onColumns = null;
        onRow = null;
        onAffected = null;
        readyForQueryPendingMessage = null;
        lastSentMessage = null;
        if (onResponse != null) {
            var uponResponse = consumeOnResponse();
            futuresExecutor.execute(() -> uponResponse.completeExceptionally(th));
        }
    }

    protected void gotMessage(Message message) {
        switch (message) {
            case NotificationResponse notification -> publish(notification);
            case BIndicators.BindComplete _ -> {}                 // op op since bulk message sequence
            case BIndicators.ParseComplete _, BIndicators.CloseComplete _ -> readyForQueryPendingMessage = message;
            case RowDescription rowDescription -> onColumns.accept(rowDescription.getColumns());
            case BIndicators.NoData _ -> onColumns.accept(new RowDescription.ColumnDescription[]{});
            case DataRow dataRow -> onRow.accept(dataRow);
            case ErrorResponse errorResponse -> {
                if (seenReadyForQuery) {
                    readyForQueryPendingMessage = message;
                } else {
                    gotException(toSqlException(errorResponse));
                }
            }
            case CommandComplete commandComplete -> {
                if (isSimpleQueryInProgress()) {
                    onAffected.accept(commandComplete);
                } else {
                    // assert !isSimpleQueryInProgress() :
                    // "During simple query message flow, CommandComplete message should be consumed only by dedicated callback,
                    // due to possibility of multiple CommandComplete messages, one per sql clause.";
                    readyForQueryPendingMessage = message;
                }
            }
            case Authentication authentication -> {
                if (authentication.authenticationOk() || authentication.saslServerFinalResponse()) {
                    readyForQueryPendingMessage = message;
                } else {
                    consumeOnResponse().completeAsync(() -> message, futuresExecutor);
                }
            }
            case ReadyForQuery _ -> {
                seenReadyForQuery = true;
                if (readyForQueryPendingMessage instanceof ErrorResponse errorResponse) {
                    gotException(toSqlException(errorResponse));
                } else {
                    onColumns = null;
                    onRow = null;
                    onAffected = null;
                    var response = readyForQueryPendingMessage != null ? readyForQueryPendingMessage : message;
                    consumeOnResponse().completeAsync(() -> response, futuresExecutor);
                }
                readyForQueryPendingMessage = null;
            }
            case ParameterStatus _, BackendKeyData _, UnknownMessage _ -> log.trace(message.toString());
            case NoticeResponse _ -> log.warn(message.toString());
            case null, default -> consumeOnResponse().completeAsync(() -> message, futuresExecutor);
        }
    }

    private CompletableFuture<Message> offerRoundTrip(Runnable requestAction) {
        return offerRoundTrip(requestAction, true);
    }

    protected CompletableFuture<Message> offerRoundTrip(Runnable requestAction, boolean assumeConnected) {
        CompletableFuture<Message> uponResponse = new CompletableFuture<>();
        if (!assumeConnected || isConnected()) {
            if (onResponse == null) {
                onResponse = uponResponse;
                try {
                    requestAction.run();
                } catch (Throwable th) {
                    gotException(th);
                }
            } else {
                futuresExecutor.execute(() -> uponResponse.completeExceptionally(new IllegalStateException("Postgres messages stream simultaneous use detected")));
            }
        } else {
            futuresExecutor.execute(() -> uponResponse.completeExceptionally(new IllegalStateException("Channel is closed")));
        }
        return uponResponse;
    }

    private void publish(NotificationResponse notification) {
        Set<Consumer<String>> consumers = subscriptions.get(notification.getChannel());
        if (consumers != null) {
            consumers.forEach(c -> c.accept(notification.getPayload()));
        }
    }

    private boolean isSimpleQueryInProgress() {
        return lastSentMessage instanceof Query;
    }

    private boolean isExtendedQueryInProgress() {
        return lastSentMessage instanceof ExtendedQueryMessage;
    }

    private static SqlException toSqlException(ErrorResponse error) {
        return new SqlException(error.level(), error.code(), error.message());
    }
}
