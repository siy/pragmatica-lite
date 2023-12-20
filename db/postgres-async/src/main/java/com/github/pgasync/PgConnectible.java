package com.github.pgasync;

import com.github.pgasync.conversion.DataConverter;
import com.github.pgasync.net.Connectible;
import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Connection;
import com.github.pgasync.net.Row;

import java.nio.charset.Charset;
import java.util.Map;
import com.github.pgasync.async.IntermediatePromise;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class PgConnectible implements Connectible {
    final String validationQuery;
    final String username;
    final DataConverter dataConverter;
    final Supplier<IntermediatePromise<ProtocolStream>> obtainStream;

    protected final String password;
    protected final String database;
    protected final Charset encoding;

    PgConnectible(ConnectibleBuilder.ConnectibleConfiguration properties,
                  Supplier<IntermediatePromise<ProtocolStream>> obtainStream) {
        this.username = properties.username();
        this.password = properties.password();
        this.database = properties.database();
        this.dataConverter = properties.dataConverter();
        this.validationQuery = properties.validationQuery();
        this.encoding = Charset.forName(properties.encoding());
        this.obtainStream = obtainStream;
    }

    @Override
    public IntermediatePromise<Void> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                            Consumer<Row> onRow,
                                            Consumer<Integer> onAffected,
                                            String sql) {
        return getConnection()
            .flatMap(connection ->
                             connection.script(onColumns, onRow, onAffected, sql)
                                       .onResult((message, th) -> closeConnection(connection, message, th)));
    }

    @Override
    public IntermediatePromise<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                              Consumer<Row> onRow,
                                              String sql,
                                              Object... params) {
        return getConnection()
            .flatMap(connection ->
                             connection.query(onColumns, onRow, sql, params)
                                       .onResult((affected, th) -> closeConnection(connection, affected, th)));
    }

    private static <T> void closeConnection(Connection connection, T value, Throwable th) {
        connection.close()
                  .map(_ -> {
                      if (th == null) {
                          return value;
                      } else {
                          throw new RuntimeException(th);
                      }
                  });
    }
}
