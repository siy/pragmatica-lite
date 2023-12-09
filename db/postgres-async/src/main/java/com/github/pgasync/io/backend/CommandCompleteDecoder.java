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

import com.github.pgasync.io.Decoder;
import com.github.pgasync.message.backend.CommandComplete;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static com.github.pgasync.io.IO.getCString;

/**
 * See <a href="www.postgresql.org/docs/9.3/static/protocol-message-formats.html">Postgres message formats</a>
 *
 * <pre>
 * CommandComplete (B)
 *  Byte1('C')
 *      Identifies the message as a command-completed response.
 *  Int32
 *      Length of message contents in bytes, including self.
 *  String
 *      The command tag. This is usually a single word that identifies which SQL command was completed.
 *      For an INSERT command, the tag is INSERT oid rows, where rows is the number of rows inserted. oid is the object ID of the inserted row if rows is 1 and the target table has OIDs; otherwise oid is 0.
 *      For a DELETE command, the tag is DELETE rows where rows is the number of rows deleted.
 *      For an UPDATE command, the tag is UPDATE rows where rows is the number of rows updated.
 *      For a SELECT or CREATE TABLE AS command, the tag is SELECT rows where rows is the number of rows retrieved.
 *      For a MOVE command, the tag is MOVE rows where rows is the number of rows the cursor's position has been changed by.
 *      For a FETCH command, the tag is FETCH rows where rows is the number of rows that have been retrieved from the cursor.
 *      For a COPY command, the tag is COPY rows where rows is the number of rows copied. (Note: the row count appears only in Postgres 8.2 and later.)
 * </pre>
 *
 * @author Antti Laisi
 */
public class CommandCompleteDecoder implements Decoder<CommandComplete> {

    @Override
    public byte getMessageId() {
        return 'C';
    }

    @Override
    public CommandComplete read(ByteBuffer buffer, int contentLength, Charset encoding) {
        String tag = getCString(buffer, encoding);
        int affectedRows;
        if (tag.contains("INSERT") || tag.contains("UPDATE") || tag.contains("DELETE")) {
            String[] parts = tag.split(" ");
            affectedRows = Integer.parseInt(parts[parts.length - 1]);
        } else {
            affectedRows = 0;
        }
        return new CommandComplete(tag, affectedRows);
    }

}
