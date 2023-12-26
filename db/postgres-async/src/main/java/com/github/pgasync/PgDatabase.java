package com.github.pgasync;

import com.github.pgasync.async.ThrowableCause;
import com.github.pgasync.async.ThrowingPromise;
import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Connection;
import org.pragmatica.lang.Unit;

import java.util.function.Supplier;

public class PgDatabase extends PgConnectible {

    public PgDatabase(ConnectibleBuilder.ConnectibleConfiguration properties, Supplier<ThrowingPromise<ProtocolStream>> obtainStream) {
        super(properties, obtainStream);
    }

    @Override
    public ThrowingPromise<Connection> getConnection() {
        return obtainStream.get()
                           .flatMap(stream -> new PgConnection(stream, dataConverter).connect(username, password, database))
                           .flatMap(connection -> {
                               if (validationQuery != null && !validationQuery.isBlank()) {
                                   return connection.completeScript(validationQuery)
                                                    .fold(result -> result.fold(
                                                        cause -> ThrowingPromise.failed(((ThrowableCause) cause).throwable()),
                                                        _ -> ThrowingPromise.successful(connection)
                                                    ));
                               } else {
                                   return ThrowingPromise.successful(connection);
                               }
                           });
    }

    @Override
    public ThrowingPromise<Unit> close() {
        return ThrowingPromise.successful(Unit.aUnit());
    }
}
