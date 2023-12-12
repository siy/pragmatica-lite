package com.github.pgasync;

import com.github.pgasync.conversion.DataConverter;
import com.github.pgasync.net.Connectible;
import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Row;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class PgConnectible implements Connectible {
    final String validationQuery;
    final String username;
    final DataConverter dataConverter;
    final Supplier<Promise<ProtocolStream>> obtainStream;

    protected final String password;
    protected final String database;
    protected final Charset encoding;

    PgConnectible(ConnectibleBuilder.ConnectibleConfiguration properties,
                  Supplier<Promise<ProtocolStream>> obtainStream) {
        this.username = properties.username();
        this.password = properties.password();
        this.database = properties.database();
        this.dataConverter = properties.dataConverter();
        this.validationQuery = properties.validationQuery();
        this.encoding = Charset.forName(properties.encoding());
        this.obtainStream = obtainStream;
    }

    @Override
    public Promise<Unit> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                Consumer<Row> onRow,
                                Consumer<Integer> onAffected,
                                String sql) {
        return connection()
            .flatMap(connection ->
                         connection.script(onColumns, onRow, onAffected, sql)
                                   .fold(result ->
                                             connection.close()
                                                       .flatMap(_ -> Promise.resolved(result))));
    }

    @Override
    public Promise<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                  Consumer<Row> onRow,
                                  String sql,
                                  Object... params) {
        return connection()
            .flatMap(connection ->
                         connection.query(onColumns, onRow, sql, params)
                                   .fold(result ->
                                             connection.close()
                                                       .flatMap(_ -> Promise.resolved(result))));
    }
}
