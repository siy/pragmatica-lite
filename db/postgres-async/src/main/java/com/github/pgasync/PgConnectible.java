package com.github.pgasync;

import com.github.pgasync.async.ThrowableCause;
import com.github.pgasync.async.ThrowingPromise;
import com.github.pgasync.conversion.DataConverter;
import com.github.pgasync.net.Connectible;
import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Connection;
import com.github.pgasync.net.Row;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class PgConnectible implements Connectible {
    final String validationQuery;
    final String username;
    final DataConverter dataConverter;
    final Supplier<ThrowingPromise<ProtocolStream>> obtainStream;

    protected final String password;
    protected final String database;
    protected final Charset encoding;

    PgConnectible(ConnectibleBuilder.ConnectibleConfiguration properties,
                  Supplier<ThrowingPromise<ProtocolStream>> obtainStream) {
        this.username = properties.username();
        this.password = properties.password();
        this.database = properties.database();
        this.dataConverter = properties.dataConverter();
        this.validationQuery = properties.validationQuery();
        this.encoding = Charset.forName(properties.encoding());
        this.obtainStream = obtainStream;
    }

    @Override
    public ThrowingPromise<Unit> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                        Consumer<Row> onRow,
                                        Consumer<Integer> onAffected,
                                        String sql) {
        return getConnection()
            .flatMap(connection ->
                         connection.script(onColumns, onRow, onAffected, sql)
                                   .onResult(result -> closeConnection(result, connection)));
    }

    @Override
    public ThrowingPromise<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                          Consumer<Row> onRow,
                                          String sql,
                                          Object... params) {
        return getConnection()
            .flatMap(connection ->
                         connection.query(onColumns, onRow, sql, params)
                                   .onResult(result -> closeConnection(result, connection)));
    }

    private static <T> void closeConnection(Result<T> result, Connection connection) {
        connection.close()
                  .map(_ -> result.fold(
                      cause -> {throw new RuntimeException(((ThrowableCause) cause).throwable());},
                      value -> value
                  ));
    }
}
