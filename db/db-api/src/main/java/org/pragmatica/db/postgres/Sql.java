package org.pragmatica.db.postgres;

import com.github.pgasync.net.SqlException;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.FieldNames;
import org.pragmatica.lang.type.FieldValues;
import org.pragmatica.lang.type.RecordTemplate;

import java.lang.StringTemplate.Processor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

public sealed interface Sql {
    Object[] EMPTY_VALUES = new Object[0];

    String sql();

    Object[] values();

    record Query(String sql, Object... values) implements Sql {
        public QueryResponseBuilder in(DbEnv env) {
            return () -> env.execute(this);
        }
    }

    interface QueryResponseBuilder {
        Promise<ResultAccessor> get();

        default <T extends Record> Promise<Stream<T>> as(RecordTemplate<T> template) {
            return get().mapResult(resultAccessor -> resultAccessor.as(template));
        }

        default <T extends Record> Promise<T> asSingle(RecordTemplate<T> template) {
            return get().mapResult(resultAccessor -> resultAccessor.asSingle(template));
        }

        default Promise<Unit> asUnit() {
            return get().mapToUnit();
        }
    }

    record Script(String sql) implements Sql {
        @Override
        public Object[] values() {
            return EMPTY_VALUES;
        }

        public Promise<Collection<ResultAccessor>> in(DbEnv env) {
            return env.execute(this);
        }
    }

    // Single query
    Processor<Query, SqlException> QRY = stringTemplate -> {
        if (stringTemplate.values().isEmpty()) {
            return new Query(stringTemplate.fragments().getFirst());
        }

        if (stringTemplate.values().size() != stringTemplate.fragments().size() - 1) {
            throw new SqlException("Number of values does not match number of placeholders");
        }

        var builder = new StringBuilder();
        int index = 1;

        var iterator = stringTemplate.values().iterator();
        var collectedValues = new ArrayList<>();

        for (var fragment : stringTemplate.fragments()) {
            builder.append(fragment);

            if (iterator.hasNext()) {
                var value = iterator.next();

                //TODO: support for collection -> array conversion
                switch (value) {
                    case FieldNames fieldNames -> builder.append(fieldNames.formattedNames());
                    case FieldValues fieldValues -> {
                        index = fieldValues.formatParameters(builder, index);
                        collectedValues.addAll(fieldValues.values());
                    }
                    case null, default -> {
                        builder.append("$").append(index++);
                        collectedValues.add(value);
                    }
                }
            }
        }

        return new Query(builder.toString(), collectedValues.toArray());
    };

    // One or more scripts (i.e. SQL which does not return a result set) separated by semicolons
    Processor<Script, SqlException> DDL = stringTemplate -> {
        if (!stringTemplate.values().isEmpty()) {
            throw new SqlException("DDL does not support parameters");
        }

        return new Script(stringTemplate.fragments().getFirst());
    };
}
