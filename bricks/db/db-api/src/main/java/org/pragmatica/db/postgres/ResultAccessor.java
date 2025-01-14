package org.pragmatica.db.postgres;

import com.github.pgasync.PgResultSet;
import com.github.pgasync.SqlError;
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

    default <T extends Record> Result<T> asSingle(RecordTemplate<T> template) {
        return resultSet().stream(template)
                          .map(Stream::toList)
                          .flatMap(list -> {
                              if (list.isEmpty()) {
                                  return new SqlError.NoResultsReturned("Response contains no records").result();
                              }
                              if (list.size() > 1) {
                                  return new SqlError.TooManyResultsReturned("Response contains more than one record").result();
                              }
                              return Result.success(list.getFirst());
                          });
    }

    //TODO: asCount()

    static ResultAccessor wrap(PgResultSet resultSet) {
        return () -> resultSet;
    }
}
