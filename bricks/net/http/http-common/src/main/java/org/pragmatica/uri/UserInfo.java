/*
 *  Copyright (c) 2022 Sergiy Yevtushenko.
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

package org.pragmatica.uri;

import org.pragmatica.lang.Option;

import static org.pragmatica.lang.Option.empty;
import static org.pragmatica.lang.Option.option;

public record UserInfo(Option<String> userName, Option<String> password) {
    public static final UserInfo EMPTY = new UserInfo(empty(), empty());

    public static UserInfo userInfo(String userName, String password) {
        return userInfo(option(userName), option(password));
    }

    public static UserInfo userInfo(Option<String> userName, Option<String> password) {
        return new UserInfo(userName, password);
    }

    public String forIRI() {
        return userName.map(name -> password.map(pass -> name + ":" + pass).or(name))
                .or("");
    }
}
