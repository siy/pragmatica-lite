package com.github.pgasync;

import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Connection;

import com.github.pgasync.async.IntermediatePromise;
import java.util.function.Function;
import java.util.function.Supplier;

public class PgDatabase extends PgConnectible {

    public PgDatabase(ConnectibleBuilder.ConnectibleConfiguration properties, Supplier<IntermediatePromise<ProtocolStream>> obtainStream) {
        super(properties, obtainStream);
    }

    @Override
    public IntermediatePromise<Connection> getConnection() {
        return obtainStream.get()
                           .flatMap(stream -> new PgConnection(stream, dataConverter).connect(username, password, database))
                           .flatMap(connection -> {
                               if (validationQuery != null && !validationQuery.isBlank()) {
                                   return connection.completeScript(validationQuery)
                                                    .fold((_, th) -> {
                                                        if (th != null) {
                                                            return connection.close()
                                                                             .flatMap(_ -> IntermediatePromise.<Connection>failed(th));
                                                        } else {
                                                            return IntermediatePromise.successful(connection);
                                                        }
                                                    })
                                                    .flatMap(Function.identity());
                               } else {
                                   return IntermediatePromise.successful(connection);
                               }
                           });
    }

    @Override
    public IntermediatePromise<Void> close() {
        return IntermediatePromise.successful(null);
    }
}
