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

import com.github.pgasync.message.Message;
import com.github.pgasync.message.backend.Authentication;
import com.github.pgasync.message.backend.CommandComplete;
import com.github.pgasync.message.backend.DataRow;
import com.github.pgasync.message.backend.RowDescription;
import com.github.pgasync.message.frontend.Bind;
import com.github.pgasync.message.frontend.Describe;
import com.github.pgasync.message.frontend.Query;
import com.github.pgasync.message.frontend.StartupMessage;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.AsyncCloseable;

import java.util.function.Consumer;

/**
 * Stream of messages from/to backend server.
 *
 * @author Antti Laisi
 */
public interface ProtocolStream extends AsyncCloseable {
    Promise<Message> connect(StartupMessage startup);

    Promise<Message> authenticate(String userName, String password, Authentication authRequired);

    Promise<Message> send(Message message);

    Promise<Unit> send(Query query, Consumer<RowDescription.ColumnDescription[]> onColumns, Consumer<DataRow> onRow, Consumer<CommandComplete> onAffected);

    Promise<Integer> send(Bind bind, Describe describe, Consumer<RowDescription.ColumnDescription[]> onColumns, Consumer<DataRow> onRow);

    Promise<Integer> send(Bind bind, Consumer<DataRow> onRow);

    Runnable subscribe(String channel, Consumer<String> onNotification);

    boolean isConnected();
}
