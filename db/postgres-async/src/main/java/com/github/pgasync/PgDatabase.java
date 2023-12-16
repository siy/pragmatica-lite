package com.github.pgasync;

import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Connection;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class PgDatabase extends PgConnectible {

    public PgDatabase(ConnectibleBuilder.ConnectibleConfiguration properties, Supplier<CompletableFuture<ProtocolStream>> obtainStream) {
        super(properties, obtainStream);
    }

    @Override
    public CompletableFuture<Connection> getConnection() {
        return obtainStream.get()
                .thenApply(stream -> new PgConnection(stream, dataConverter).connect(username, password, database))
                .thenCompose(Function.identity())
                .thenApply(connection -> {
                    if (validationQuery != null && !validationQuery.isBlank()) {
                        return connection.completeScript(validationQuery)
                                .handle((_, th) -> {
                                    if (th != null) {
                                        return connection.close()
                                                .thenApply(_ -> CompletableFuture.<Connection>failedFuture(th))
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
