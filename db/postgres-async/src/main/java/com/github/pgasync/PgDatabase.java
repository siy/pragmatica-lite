package com.github.pgasync;

import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Connection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

public class PgDatabase extends PgConnectible {

    public PgDatabase(ConnectibleBuilder.ConnectibleProperties properties, Supplier<CompletableFuture<ProtocolStream>> obtainStream, Executor futuresExecutor) {
        super(properties, obtainStream, futuresExecutor);
    }

    @Override
    public CompletableFuture<Connection> getConnection() {
        return obtainStream.get()
                .thenApply(stream -> new PgConnection(stream, dataConverter).connect(username, password, database))
                .thenCompose(Function.identity())
                .thenApply(connection -> {
                    if (validationQuery != null && !validationQuery.isBlank()) {
                        return connection.completeScript(validationQuery)
                                .handle((rss, th) -> {
                                    if (th != null) {
                                        return connection.close()
                                                .thenApply(v -> CompletableFuture.<Connection>failedFuture(th))
                                                .thenCompose(Function.identity());
                                    } else {
                                        return CompletableFuture.completedFuture(connection);
                                    }
                                })
                                .thenCompose(Function.identity());
                    } else {
                        return CompletableFuture.completedFuture(connection);
                    }
                })
                .thenCompose(Function.identity());
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.completedFuture(null);
    }

}
