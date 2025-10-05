/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
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

package org.pragmatica.lang.parse;

import org.pragmatica.lang.Result;

import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Locale;

/// Functional wrappers for JDK internationalization APIs that return Result<T> instead of throwing exceptions
public sealed interface I18n {
    /// Parse a string as a Currency code
    ///
    /// @param currencyCode ISO 4217 currency code (e.g., "USD", "EUR")
    ///
    /// @return Result containing parsed Currency or parsing error
    static Result<Currency> parseCurrency(String currencyCode) {
        return Result.lift1(Currency::getInstance, currencyCode);
    }

    /// Parse a string as a Locale using IETF BCP 47 language tag
    ///
    /// @param languageTag IETF BCP 47 language tag (e.g., "en-US", "fr-FR")
    ///
    /// @return Result containing parsed Locale or parsing error
    static Result<Locale> parseLocale(String languageTag) {
        return Result.lift1(Locale::forLanguageTag, languageTag);
    }

    /// Parse a string as a Charset
    ///
    /// @param charsetName Charset name (e.g., "UTF-8", "ISO-8859-1")
    ///
    /// @return Result containing parsed Charset or parsing error
    static Result<Charset> parseCharset(String charsetName) {
        return Result.lift1(Charset::forName, charsetName);
    }

    /// Parse a string as a ZoneId
    ///
    /// @param zoneId Zone identifier (e.g., "America/New_York", "UTC", "Z")
    ///
    /// @return Result containing parsed ZoneId or parsing error
    static Result<ZoneId> parseZoneId(String zoneId) {
        return Result.lift1(ZoneId::of, zoneId);
    }

    /// Parse a string as a ZoneOffset
    ///
    /// @param offsetId Zone offset identifier (e.g., "+01:00", "-05:30", "Z")
    ///
    /// @return Result containing parsed ZoneOffset or parsing error
    static Result<ZoneOffset> parseZoneOffset(String offsetId) {
        return Result.lift1(ZoneOffset::of, offsetId);
    }

    record unused() implements I18n {}
}
