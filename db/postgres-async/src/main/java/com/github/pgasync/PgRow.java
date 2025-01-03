/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pgasync;

import com.github.pgasync.conversion.DataConverter;
import com.github.pgasync.message.backend.DataRow;
import com.github.pgasync.net.Row;
import com.github.pgasync.net.SqlException;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.KeyToValue;
import org.pragmatica.lang.type.TypeToken;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Map;

import static org.pragmatica.lang.Option.option;

/**
 * Result row, uses {@link DataConverter} for all conversions.
 *
 * @author Antti Laisi
 */
public class PgRow implements Row, KeyToValue {
    private final DataRow data;
    private final DataConverter dataConverter;
    private final Map<String, PgColumn> columnsByName;
    private final PgColumn[] columns;

    PgRow(DataRow data, Map<String, PgColumn> columnsByName, PgColumn[] columns, DataConverter dataConverter) {
        this.data = data;
        this.dataConverter = dataConverter;
        this.columnsByName = columnsByName;
        this.columns = columns;
    }

    @Override
    public String getString(int index) {
        return dataConverter.toString(columns[index].type(), data.getValue(index));
    }

    @Override
    public String getString(String column) {
        return getString(getColumn(column).index());
    }

    @Override
    public Character getChar(int index) {
        return dataConverter.toChar(columns[index].type(), data.getValue(index));
    }

    @Override
    public Character getChar(String column) {
        return getChar(getColumn(column).index());
    }

    @Override
    public Byte getByte(int index) {
        return dataConverter.toByte(columns[index].type(), data.getValue(index));
    }

    @Override
    public Byte getByte(String column) {
        return getByte(getColumn(column).index());
    }

    @Override
    public Short getShort(int index) {
        return dataConverter.toShort(columns[index].type(), data.getValue(index));
    }

    @Override
    public Short getShort(String column) {
        return getShort(getColumn(column).index());
    }

    @Override
    public Integer getInt(int index) {
        return dataConverter.toInteger(columns[index].type(), data.getValue(index));
    }

    @Override
    public Integer getInt(String column) {
        return getInt(getColumn(column).index());
    }

    @Override
    public Long getLong(int index) {
        return dataConverter.toLong(columns[index].type(), data.getValue(index));
    }

    @Override
    public Long getLong(String column) {
        return getLong(getColumn(column).index());
    }

    @Override
    public BigInteger getBigInteger(int index) {
        return dataConverter.toBigInteger(columns[index].type(), data.getValue(index));
    }

    @Override
    public BigInteger getBigInteger(String column) {
        return getBigInteger(getColumn(column).index());
    }

    @Override
    public BigDecimal getBigDecimal(int index) {
        return dataConverter.toBigDecimal(columns[index].type(), data.getValue(index));
    }

    @Override
    public BigDecimal getBigDecimal(String column) {
        return getBigDecimal(getColumn(column).index());
    }

    @Override
    public Double getDouble(int index) {
        return dataConverter.toDouble(columns[index].type(), data.getValue(index));
    }

    @Override
    public Double getDouble(String column) {
        return getDouble(getColumn(column).index());
    }

    @Override
    public LocalDate getLocalDate(int index) {
        return dataConverter.toLocalDate(columns[index].type(), data.getValue(index));
    }

    @Override
    public LocalDate getLocalDate(String column) {
        return getLocalDate(getColumn(column).index());
    }

    @Override
    public LocalTime getLocalTime(int index) {
        return dataConverter.toLocalTime(columns[index].type(), data.getValue(index));
    }

    @Override
    public LocalTime getLocalTime(String column) {
        return getLocalTime(getColumn(column).index());
    }

    @Override
    public LocalDateTime getLocalDateTime(int index) {
        return dataConverter.toLocalDateTime(columns[index].type(), data.getValue(index));
    }

    @Override
    public LocalDateTime getLocalDateTime(String column) {
        return getLocalDateTime(getColumn(column).index());
    }

    @Override
    public Instant getInstant(int index) {
        return dataConverter.toInstant(columns[index].type(), data.getValue(index));
    }

    @Override
    public Instant getInstant(String column) {
        return getInstant(getColumn(column).index());
    }

    @Override
    public byte[] getBytes(int index) {
        return dataConverter.toBytes(columns[index].type(), data.getValue(index));
    }

    @Override
    public byte[] getBytes(String column) {
        return getBytes(getColumn(column).index());
    }

    @Override
    public Boolean getBoolean(int index) {
        return dataConverter.toBoolean(columns[index].type(), data.getValue(index));
    }

    @Override
    public Boolean getBoolean(String column) {
        return getBoolean(getColumn(column).index());
    }

    @Override
    public <TArray> TArray getArray(int index, Class<TArray> arrayType) {
        return dataConverter.toArray(arrayType, columns[index].type(), data.getValue(index));
    }

    @Override
    public <TArray> TArray getArray(String column, Class<TArray> arrayType) {
        return getArray(getColumn(column).index(), arrayType);
    }

    //TODO: does not work for known types, only for custom ones.
    @Override
    public <T> T get(int index, Class<T> type) {
        return dataConverter.toObject(columns[index].type(), data.getValue(index), type);
    }

    @Override
    public <T> T get(String column, Class<T> type) {
        return get(getColumn(column).index(), type);
    }

    public Object get(int index) {
        return dataConverter.toObject(columns[index].type(), data.getValue(index), null);
    }

    public Object get(String column) {
        return get(getColumn(column).index());
    }

    //TODO: arrays are not supported yet
    @SuppressWarnings("unchecked")
    @Override
    public <T> Result<T> get(String prefix, String key, TypeToken<T> typeToken) {
        // Prefix is always empty for mapping of rows to records, so just ignore it
        var column = columnsByName.get(key.toLowerCase(Locale.ROOT));

        if (column == null) {
            return new SqlError.ColumnNotFound("Unknown column '" + key + "'").result();
        }

        if (typeToken.rawType().equals(Option.class)) {
            var value = typeToken.typeArgument(0)
                                 .map(cls -> get(column.index(), cls));
            return Result.success((T) option(value));
        } else {
            return Result.success(get(column.index(), (Class<T>) typeToken.rawType()));
        }
    }

    private PgColumn getColumn(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Column name is required");
        }

        var column = columnsByName.get(name.toLowerCase(Locale.ROOT));

        if (column == null) {
            throw new SqlException("Unknown column '" + name + "'");
        }

        return column;
    }

    @Override
    public String toString() {
        var builder = new StringBuilder("PgRow[");

        int last = builder.length();
        for (int i = 0; i < columns.length; i++) {
            builder.append(columns[i].name());
            builder.append("=");
            builder.append(get(i));
            last = builder.length();
            builder.append(", ");
        }
        builder.setLength(last);
        builder.append("]");

        return builder.toString();
    }
}
