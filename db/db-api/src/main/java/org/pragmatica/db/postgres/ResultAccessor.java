package org.pragmatica.db.postgres;

import com.github.pgasync.PgResultSet;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.RecordTemplate;

import java.util.stream.Stream;

/**
 * Convenience wrapper around {@code ResultSet} with less tedious and error-prone API.
 */
public interface ResultAccessor {
    PgResultSet resultSet();

    default int affectedRows() {
        return resultSet().affectedRows();
    }

    default int size() {
        return resultSet().size();
    }

    default <T extends Record> Result<Stream<T>> as(RecordTemplate<T> template) {
        return resultSet().stream(template);
    }

    //TODO: asCount()

    static ResultAccessor wrap(PgResultSet resultSet) {
        return () -> resultSet;
    }
}
