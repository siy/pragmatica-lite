package com.github.pgasync;

import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Connection;

import com.github.pgasync.async.IntermediateFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class PgDatabase extends PgConnectible {

    public PgDatabase(ConnectibleBuilder.ConnectibleConfiguration properties, Supplier<IntermediateFuture<ProtocolStream>> obtainStream) {
        super(properties, obtainStream);
    }

    @Override
    public IntermediateFuture<Connection> getConnection() {
        return obtainStream.get()
                           .thenCompose(stream -> new PgConnection(stream, dataConverter).connect(username, password, database))
                           .thenCompose(connection -> {
                               if (validationQuery != null && !validationQuery.isBlank()) {
                                   return connection.completeScript(validationQuery)
                                                    .handle((_, th) -> {
                                                        if (th != null) {
                                                            return connection.close()
                                                                             .thenCompose(_ -> IntermediateFuture.<Connection>failedFuture(th));
                                                        } else {
                                                            return IntermediateFuture.completedFuture(connection);
                                                        }
                                                    })
                                                    .thenCompose(Function.identity());
                               } else {
                                   return IntermediateFuture.completedFuture(connection);
                               }
                           });
    }

    @Override
    public IntermediateFuture<Void> close() {
        return IntermediateFuture.completedFuture(null);
    }
}
