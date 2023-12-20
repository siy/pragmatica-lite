package com.github.pgasync;

import com.github.pgasync.async.IntermediatePromise;
import com.github.pgasync.async.ThrowableCause;
import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Connection;
import org.pragmatica.lang.Unit;

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
                                                    .fold(result -> result.fold(
                                                        cause -> IntermediatePromise.failed(((ThrowableCause) cause).throwable()),
                                                        _ -> IntermediatePromise.successful(connection)
                                                    ));
                               } else {
                                   return IntermediatePromise.successful(connection);
                               }
                           });
    }

    @Override
    public IntermediatePromise<Unit> close() {
        return IntermediatePromise.successful(Unit.aUnit());
    }
}
