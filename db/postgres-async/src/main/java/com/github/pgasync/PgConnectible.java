package com.github.pgasync;

import com.github.pgasync.conversion.DataConverter;
import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Connectible;
import com.github.pgasync.net.Row;
import org.pragmatica.lang.Promise;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class PgConnectible implements Connectible {

    final String validationQuery;
    final String username;
    final DataConverter dataConverter;
    final Supplier<CompletableFuture<ProtocolStream>> obtainStream;

    protected final String password;
    protected final String database;
    protected final Charset encoding;

    PgConnectible(ConnectibleBuilder.ConnectibleConfiguration properties,
                  Supplier<CompletableFuture<ProtocolStream>> obtainStream) {
        this.username = properties.username();
        this.password = properties.password();
        this.database = properties.database();
        this.dataConverter = properties.dataConverter();
        this.validationQuery = properties.validationQuery();
        this.encoding = Charset.forName(properties.encoding());
        this.obtainStream = obtainStream;
    }

    @Override
    public CompletableFuture<Void> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                          Consumer<Row> onRow,
                                          Consumer<Integer> onAffected,
                                          String sql) {
        return connection()
            .thenApply(connection ->
                           connection.script(onColumns, onRow, onAffected, sql)
                                     .handle((message, th) ->
                                                 connection.close()
                                                           .thenApply(v -> {
                                                               if (th == null) {
                                                                   return message;
                                                               } else {
                                                                   throw new RuntimeException(th);
                                                               }
                                                           })
                                     ).thenCompose(Function.identity())
            ).thenCompose(Function.identity());
    }

    @Override
    public Promise<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                  Consumer<Row> onRow,
                                  String sql,
                                  Object... params) {
        return connection()
            .thenApply(connection ->
                           connection.query(onColumns, onRow, sql, params)
                               .fold(cause -> connection.close().flatMap(_ -> Promise.<Integer>failed(cause)),
                                     affected -> connection.close().map(() -> affected));
//                               .onResultDp(() -> connection.close())
//                                     .flatMap((affected, th) ->
//                                                 connection.close()
//                                                           .thenApply(v -> {
//                                                               if (th == null) {
//                                                                   return affected;
//                                                               } else {
//                                                                   throw new RuntimeException(th);
//                                                               }
//                                                           })
//                                     )
            );
    }
}
