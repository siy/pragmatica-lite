package org.pragmatica.db.postgres;

import com.github.pgasync.net.ResultSet;
import com.github.pgasync.net.SqlException;
import org.pragmatica.lang.Promise;

import java.lang.StringTemplate.Processor;
import java.util.Collection;

public sealed interface Sql {
    Object[] EMPTY_VALUES = new Object[0];

    String sql();
    Object[] values();

    record Query(String sql, Object... values) implements Sql {
        public Promise<ResultSet> in(DbEnv env) {
            return env.execute(this);
        }
    }
    record Script(String sql) implements Sql {
        public Object[] values() {
            return EMPTY_VALUES;
        }

        public Promise<Collection<ResultSet>> in(DbEnv env) {
            return env.execute(this);
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
