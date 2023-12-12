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

import com.github.pgasync.net.ResultSet;
import com.github.pgasync.net.Row;

import java.util.*;

/**
 * {@link ResultSet} constructed from Query/Execute response messages.
 *
 * @author Antti Laisi
 */
public class PgResultSet implements ResultSet {

    private final List<Row> rows;
    private final Map<String, PgColumn> columnsByName;
    private final List<PgColumn> orderedColumns;
    private final int affectedRows;

    public PgResultSet(Map<String, PgColumn> columnsByName, PgColumn[] orderedColumns, List<Row> rows, int affectedRows) {
        this.columnsByName = columnsByName == null ? Map.of() : columnsByName;
        this.orderedColumns = orderedColumns == null ? List.of() : List.of(orderedColumns);
        this.rows = rows == null ? List.of() : rows;
        this.affectedRows = affectedRows;
    }

    @Override
    public Map<String, PgColumn> columnsByName() {
        return columnsByName;
    }

    @Override
    public List<PgColumn> orderedColumns() {
        return orderedColumns;
    }

    @Override
    public Iterator<Row> iterator() {
        return rows.iterator();
    }

    @Override
    public Row index(int index) {
        return rows.get(index);
    }

    @Override
    public int size() {
        return rows.size();
    }

    @Override
    public int affectedRows() {
        return affectedRows;
    }
}
