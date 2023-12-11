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

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Messages stream to Postgres backend.
 *
 * @author Marat Gainullin
 */
public abstract class PgProtocolStream implements ProtocolStream {

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
        if (authRequired.isSaslScramSha256()) {
            String clientNonce = UUID.randomUUID().toString();
            SASLInitialResponse saslInitialResponse = new SASLInitialResponse(Authentication.SUPPORTED_SASL, null, ""/*SaslPrep.asQueryString(userName) - Postgres requires an empty string here*/, clientNonce);
            return send(saslInitialResponse)
                    .thenApply(message -> {
                        if (message instanceof Authentication) {
                            String serverFirstMessage = ((Authentication) message).saslContinueData();
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
            return send(PasswordMessage.passwordMessage(userName, password, authRequired.md5Salt(), encoding));
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
        return send(query).thenAccept(readyForQuery -> {
        });
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
        }).thenApply(commandComplete -> ((CommandComplete) commandComplete).getAffectedRows());
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
        }).thenApply(commandComplete -> ((CommandComplete) commandComplete).getAffectedRows());
    }

    @Override
    public Runnable subscribe(String channel, Consumer<String> onNotification) {
        subscriptions
                .computeIfAbsent(channel, ch -> new HashSet<>())
                .add(onNotification);
        return () -> subscriptions.computeIfPresent(channel, (ch, subscription) -> {
            subscription.remove(onNotification);
            if (subscription.isEmpty()) {
                return null;
            } else {
                return subscription;
            }
        });
    }

    protected void gotException(Throwable th) {
        onColumns = null;
        onRow = null;
        onAffected = null;
        readyForQueryPendingMessage = null;
        lastSentMessage = null;
        if (onResponse != null) {
            CompletableFuture<? super Message> uponResponse = consumeOnResponse();
            futuresExecutor.execute(() -> uponResponse.completeExceptionally(th));
        }
    }

    protected void gotMessage(Message message) {
        if (message instanceof NotificationResponse) {
            publish((NotificationResponse) message);
        } else if (message == BIndicators.BIND_COMPLETE) {
            // op op since bulk message sequence
        } else if (message == BIndicators.PARSE_COMPLETE || message == BIndicators.CLOSE_COMPLETE) {
            readyForQueryPendingMessage = message;
        } else if (message instanceof RowDescription) {
            onColumns.accept(((RowDescription) message).getColumns());
        } else if (message == BIndicators.NO_DATA) {
            onColumns.accept(new RowDescription.ColumnDescription[]{});
        } else if (message instanceof DataRow) {
            onRow.accept((DataRow) message);
        } else if (message instanceof ErrorResponse) {
            if (seenReadyForQuery) {
                readyForQueryPendingMessage = message;
            } else {
                gotException(toSqlException((ErrorResponse) message));
            }
        } else if (message instanceof CommandComplete) {
            if (isSimpleQueryInProgress()) {
                onAffected.accept((CommandComplete) message);
            } else {
                // assert !isSimpleQueryInProgress() :
                // "During simple query message flow, CommandComplete message should be consumed only by dedicated callback,
                // due to possibility of multiple CommandComplete messages, one per sql clause.";
                readyForQueryPendingMessage = message;
            }
        } else if (message instanceof Authentication) {
            Authentication authentication = (Authentication) message;
            if (authentication.authenticationOk() || authentication.isSaslServerFinalResponse()) {
                readyForQueryPendingMessage = message;
            } else {
                consumeOnResponse().completeAsync(() -> message, futuresExecutor);
            }
        } else if (message == ReadyForQuery.INSTANCE) {
            seenReadyForQuery = true;
            if (readyForQueryPendingMessage instanceof ErrorResponse) {
                gotException(toSqlException((ErrorResponse) readyForQueryPendingMessage));
            } else {
                onColumns = null;
                onRow = null;
                onAffected = null;
                Message response = readyForQueryPendingMessage != null ? readyForQueryPendingMessage : message;
                consumeOnResponse().completeAsync(() -> response, futuresExecutor);
            }
            readyForQueryPendingMessage = null;
        } else if (message instanceof ParameterStatus) {
            Logger.getLogger(PgProtocolStream.class.getName()).log(Level.FINE, message.toString());
        } else if (message instanceof BackendKeyData) {
            Logger.getLogger(PgProtocolStream.class.getName()).log(Level.FINE, message.toString());
        } else if (message instanceof NoticeResponse) {
            Logger.getLogger(PgProtocolStream.class.getName()).log(Level.WARNING, message.toString());
        } else if (message instanceof UnknownMessage) {
            Logger.getLogger(PgProtocolStream.class.getName()).log(Level.FINE, message.toString());
        } else {
            consumeOnResponse().completeAsync(() -> message, futuresExecutor);
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
        return new SqlException(error.getLevel(), error.getCode(), error.getMessage());
    }
}
