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

import com.github.pgasync.net.Row;
import com.github.pgasync.net.SqlException;
import com.github.pgasync.conversion.DataConverter;
import com.github.pgasync.message.backend.DataRow;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Result row, uses {@link DataConverter} for all conversions.
 *
 * @author Antti Laisi
 */
public class PgRow implements Row {
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
        PgColumn pgColumn = getColumn(column);
        return getString(pgColumn.index());
    }

    @Override
    public Character getChar(int index) {
        return dataConverter.toChar(columns[index].type(), data.getValue(index));
    }

    @Override
    public Character getChar(String column) {
        PgColumn pgColumn = getColumn(column);
        return getChar(pgColumn.index());
    }

    @Override
    public Byte getByte(int index) {
        return dataConverter.toByte(columns[index].type(), data.getValue(index));
    }

    @Override
    public Byte getByte(String column) {
        PgColumn pgColumn = getColumn(column);
        return getByte(pgColumn.index());
    }

    @Override
    public Short getShort(int index) {
        return dataConverter.toShort(columns[index].type(), data.getValue(index));
    }

    @Override
    public Short getShort(String column) {
        PgColumn pgColumn = getColumn(column);
        return getShort(pgColumn.index());
    }

    @Override
    public Integer getInt(int index) {
        return dataConverter.toInteger(columns[index].type(), data.getValue(index));
    }

    @Override
    public Integer getInt(String column) {
        PgColumn pgColumn = getColumn(column);
        return getInt(pgColumn.index());
    }

    @Override
    public Long getLong(int index) {
        return dataConverter.toLong(columns[index].type(), data.getValue(index));
    }

    @Override
    public Long getLong(String column) {
        PgColumn pgColumn = getColumn(column);
        return getLong(pgColumn.index());
    }

    @Override
    public BigInteger getBigInteger(int index) {
        return dataConverter.toBigInteger(columns[index].type(), data.getValue(index));
    }

    @Override
    public BigInteger getBigInteger(String column) {
        PgColumn pgColumn = getColumn(column);
        return getBigInteger(pgColumn.index());
    }

    @Override
    public BigDecimal getBigDecimal(int index) {
        return dataConverter.toBigDecimal(columns[index].type(), data.getValue(index));
    }

    @Override
    public BigDecimal getBigDecimal(String column) {
        PgColumn pgColumn = getColumn(column);
        return getBigDecimal(pgColumn.index());
    }

    @Override
    public Double getDouble(int index) {
        return dataConverter.toDouble(columns[index].type(), data.getValue(index));
    }

    @Override
    public Double getDouble(String column) {
        PgColumn pgColumn = getColumn(column);
        return getDouble(pgColumn.index());
    }

    @Override
    public LocalDate getLocalDate(int index) {
        return dataConverter.toLocalDate(columns[index].type(), data.getValue(index));
    }

    @Override
    public LocalDate getLocalDate(String column) {
        PgColumn pgColumn = getColumn(column);
        return getLocalDate(pgColumn.index());
    }

    @Override
    public Time getTime(int index) {
        return dataConverter.toTime(columns[index].type(), data.getValue(index));
    }

    @Override
    public Time getTime(String column) {
        PgColumn pgColumn = getColumn(column);
        return getTime(pgColumn.index());
    }

    @Override
    public Date getDate(int index) {
        return dataConverter.toDate(columns[index].type(), data.getValue(index));
    }

    @Override
    public Date getDate(String column) {
        PgColumn pgColumn = getColumn(column);
        return getDate(pgColumn.index());
    }

    @Override
    public Timestamp getTimestamp(int index) {
        return dataConverter.toTimestamp(columns[index].type(), data.getValue(index));
    }

    @Override
    public Timestamp getTimestamp(String column) {
        PgColumn pgColumn = getColumn(column);
        return getTimestamp(pgColumn.index());
    }

    @Override
    public Instant getInstant(int index) {
        return dataConverter.toInstant(columns[index].type(), data.getValue(index));
    }

    @Override
    public Instant getInstant(String column) {
        PgColumn pgColumn = getColumn(column);
        return getInstant(pgColumn.index());
    }

    @Override
    public byte[] getBytes(int index) {
        return dataConverter.toBytes(columns[index].type(), data.getValue(index));
    }

    @Override
    public byte[] getBytes(String column) {
        PgColumn pgColumn = getColumn(column);
        return getBytes(pgColumn.index());
    }

    @Override
    public Boolean getBoolean(int index) {
        return dataConverter.toBoolean(columns[index].type(), data.getValue(index));
    }

    @Override
    public Boolean getBoolean(String column) {
        PgColumn pgColumn = getColumn(column);
        return getBoolean(pgColumn.index());
    }

    @Override
    public <TArray> TArray getArray(int index, Class<TArray> arrayType) {
        return dataConverter.toArray(arrayType, columns[index].type(), data.getValue(index));
    }

    @Override
    public <TArray> TArray getArray(String column, Class<TArray> arrayType) {
        PgColumn pgColumn = getColumn(column);
        return getArray(pgColumn.index(), arrayType);
    }

    @Override
    public <T> T get(int index, Class<T> type) {
        return dataConverter.toObject(type, columns[index].type(), data.getValue(index));
    }

    @Override
    public <T> T get(String column, Class<T> type) {
        PgColumn pgColumn = getColumn(column);
        return get(pgColumn.index(), type);
    }

    public Object get(int index) {
        return dataConverter.toObject(columns[index].type(), data.getValue(index));
    }

    public Object get(String column) {
        PgColumn pgColumn = getColumn(column);
        return get(pgColumn.index());
    }

    private PgColumn getColumn(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Column name is required");
        }
        PgColumn column = columnsByName.get(name);
        if (column == null) {
            throw new SqlException("Unknown column '" + name + "'");
        }
        return column;
    }

}
