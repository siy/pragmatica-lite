/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.json;

import tools.jackson.core.Version;
import tools.jackson.databind.module.SimpleModule;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

/// Jackson module for Pragmatica-Lite types (Result, Option).
/// Registers custom serializers and deserializers for functional types.
///
/// Result serialization format:
/// - Success: {"success": true, "value": <T>}
/// - Failure: {"success": false, "error": {"message": "...", "type": "..."}}
///
/// Option serialization format:
/// - Some: <T> (the wrapped value)
/// - None: null
public class PragmaticaModule extends SimpleModule {
    public PragmaticaModule() {
        super("pragmatica", Version.unknownVersion());

        // Register Result serializers
        addSerializer((Class)Result.class, new ResultSerializer());
        addDeserializer((Class)Result.class, new ResultDeserializer());

        // Register Option serializers
        addSerializer((Class)Option.class, new OptionSerializer());
        addDeserializer((Class)Option.class, new OptionDeserializer());
    }
}
