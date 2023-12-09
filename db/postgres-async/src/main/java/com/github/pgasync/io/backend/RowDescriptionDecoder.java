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

package com.github.pgasync.io.backend;

import com.github.pgasync.Oid;
import com.github.pgasync.io.Decoder;
import com.github.pgasync.message.backend.RowDescription;
import com.github.pgasync.io.IO;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * See <a href="www.postgresql.org/docs/9.3/static/protocol-message-formats.html">Postgres message formats</a>
 *
 * <pre>
 * RowDescription (B)
 *  Byte1('T')
 *      Identifies the message as a row description.
 *  Int32
 *      Length of message contents in bytes, including self.
 *  Int16
 *      Specifies the number of fields in a row (can be zero).
 *  Then, for each field, there is the following:
 *  String
 *      The field name.
 *  Int32
 *      If the field can be identified as a column of a specific table, the object ID of the table; otherwise zero.
 *  Int16
 *      If the field can be identified as a column of a specific table, the attribute number of the column; otherwise zero.
 *  Int32
 *      The object ID of the field's data type.
 *  Int16
 *      The data type size (see pg_type.typlen). Note that negative values denote variable-width types.
 *  Int32
 *      The type modifier (see pg_attribute.atttypmod). The meaning of the modifier is type-specific.
 *  Int16
 *      The format code being used for the field. Currently will be zero (text) or one (binary). In a RowDescription returned from the statement variant of Describe, the format code is not yet known and will always be zero.
 * </pre>
 *
 * @author Antti Laisi
 */
public class RowDescriptionDecoder implements Decoder<RowDescription> {

    @Override
    public byte getMessageId() {
        return 'T';
    }

    @Override
    public RowDescription read(ByteBuffer buffer, int contentLength, Charset encoding) {
        RowDescription.ColumnDescription[] columns = new RowDescription.ColumnDescription[buffer.getShort()];

        for (int i = 0; i < columns.length; i++) {
            String name = IO.getCString(buffer, encoding);
            buffer.position(buffer.position() + 6);
            Oid type = Oid.valueOfId(buffer.getInt());
            buffer.position(buffer.position() + 8);
            columns[i] = new RowDescription.ColumnDescription(name, type);
        }

        return new RowDescription(columns);
    }

}
