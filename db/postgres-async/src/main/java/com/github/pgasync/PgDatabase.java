package com.github.pgasync;

import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Connection;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.function.Supplier;

import static org.pragmatica.lang.Result.unitResult;

public class PgDatabase extends PgConnectible {

    public PgDatabase(ConnectibleBuilder.ConnectibleConfiguration properties, Supplier<Promise<ProtocolStream>> obtainStream) {
        super(properties, obtainStream);
    }

    @Override
    public Promise<Connection> connection() {
        return obtainStream.get()
                           .flatMap(stream -> new PgConnection(stream, dataConverter).connect(username, password, database))
                           .flatMap(connection -> {
                               if (validationQuery != null && !validationQuery.isBlank()) {
                                   return connection.completeScript(validationQuery)
                                                          .map(_ -> connection)
                                                          .onFailureDo(connection::close);
                               } else {
                                   return Promise.successful(connection);
                               }
                           });
    }

    @Override
    public Promise<Unit> close() {
        return Promise.resolved(unitResult());
    }

}
