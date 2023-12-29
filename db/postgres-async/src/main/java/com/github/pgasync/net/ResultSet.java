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

package com.github.pgasync.net;

import com.github.pgasync.PgColumn;

import java.util.List;
import java.util.Map;

/**
 * SQL result set. Consists of 0-n result rows and amount of affected
 * (INSERT/UPDATE/DELETE) rows.
 *
 * @author Antti Laisi
 */
public interface ResultSet extends Iterable<Row> {
    Map<String, PgColumn> columnsByName();

    List<PgColumn> orderedColumns();

    /**
     * @param index Row index starting from 0
     * @return Row, never null
     */
    Row index(int index);

    /**
     * @return Amount of result rows.
     */
    int size();

    /**
     * @return Amount of modified rows.
     */
    int affectedRows();
}
