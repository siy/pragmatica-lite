/*
 *  Copyright (c) 2022-2025 Sergiy Yevtushenko.
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
 */

package org.pragmatica.net.dns;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;
import org.pragmatica.net.dns.ResolverError.InvalidIpAddress;
import org.pragmatica.net.dns.ResolverError.UnknownError;

import java.net.InetAddress;
import java.net.UnknownHostException;

/// Utilities for working with InetAddress.
public final class InetUtils {
    public static Result<InetAddress> forBytes(byte[] address) {
        return Result.lift(InetUtils::exceptionMapper, () -> InetAddress.getByAddress(address));
    }

    static Cause exceptionMapper(Throwable throwable) {
        if (throwable instanceof UnknownHostException) {
            return new InvalidIpAddress(throwable.getMessage());
        }
        return new UnknownError(throwable.getMessage());
    }

    private InetUtils() {}
}
