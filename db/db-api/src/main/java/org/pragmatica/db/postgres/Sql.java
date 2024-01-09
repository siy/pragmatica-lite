package org.pragmatica.db.postgres;

import com.github.pgasync.net.SqlException;

import java.lang.StringTemplate.Processor;

public sealed interface Sql {
    Object[] EMPTY_VALUES = new Object[0];

    String sql();
    Object[] values();

    record Query(String sql, Object... values) implements Sql {}
    record Script(String sql) implements Sql {
        public Object[] values() {
            return EMPTY_VALUES;
        }
    }

    // Single query
    Processor<Query, SqlException> QRY = stringTemplate -> {
        if (stringTemplate.values().isEmpty()) {
            return new Query(stringTemplate.fragments().getFirst());
        }

        var builder = new StringBuilder();
        int index = 1;
        int last = 0;

        for(var fragment : stringTemplate.fragments()) {
            builder.append(fragment);
            last = builder.length();
            builder.append("$");
            builder.append(index++);
        }

        builder.setLength(last);

        return new Query(builder.toString(), stringTemplate.values().toArray());
    };

    // One or more scripts (i.e. SQL which does not return a result set) separated by semicolons
    Processor<Script, SqlException> DDL = stringTemplate -> {
        if (!stringTemplate.values().isEmpty()) {
            throw new SqlException("DDL does not support parameters");
        }

        return new Script(stringTemplate.fragments().getFirst());
    };
}
