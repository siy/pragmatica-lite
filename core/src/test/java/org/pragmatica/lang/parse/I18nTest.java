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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class I18nTest {

    @Test
    void testParseCurrencySuccess() {
        I18n.parseCurrency("USD")
                .onFailureRun(Assertions::fail)
                .onSuccess(currency -> {
                    assertEquals(Currency.getInstance("USD"), currency);
                    assertEquals("USD", currency.getCurrencyCode());
                });
    }

    @Test
    void testParseCurrencyEuro() {
        I18n.parseCurrency("EUR")
                .onFailureRun(Assertions::fail)
                .onSuccess(currency -> assertEquals("EUR", currency.getCurrencyCode()));
    }

    @Test
    void testParseCurrencyFailure() {
        I18n.parseCurrency("INVALID")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseCurrencyNull() {
        I18n.parseCurrency(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseCurrencyEmpty() {
        I18n.parseCurrency("")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseLocaleSuccess() {
        I18n.parseLocale("en-US")
                .onFailureRun(Assertions::fail)
                .onSuccess(locale -> {
                    assertEquals("en", locale.getLanguage());
                    assertEquals("US", locale.getCountry());
                });
    }

    @Test
    void testParseLocaleFrench() {
        I18n.parseLocale("fr-FR")
                .onFailureRun(Assertions::fail)
                .onSuccess(locale -> {
                    assertEquals("fr", locale.getLanguage());
                    assertEquals("FR", locale.getCountry());
                });
    }

    @Test
    void testParseLocaleLanguageOnly() {
        I18n.parseLocale("de")
                .onFailureRun(Assertions::fail)
                .onSuccess(locale -> assertEquals("de", locale.getLanguage()));
    }

    @Test
    void testParseLocaleNull() {
        // null throws NullPointerException in Locale.forLanguageTag
        I18n.parseLocale(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseLocaleEmpty() {
        I18n.parseLocale("")
                .onFailureRun(Assertions::fail)
                .onSuccess(locale -> assertEquals(Locale.ROOT, locale));
    }

    @Test
    void testParseCharsetSuccess() {
        I18n.parseCharset("UTF-8")
                .onFailureRun(Assertions::fail)
                .onSuccess(charset -> assertEquals(StandardCharsets.UTF_8, charset));
    }

    @Test
    void testParseCharsetISO() {
        I18n.parseCharset("ISO-8859-1")
                .onFailureRun(Assertions::fail)
                .onSuccess(charset -> assertEquals(StandardCharsets.ISO_8859_1, charset));
    }

    @Test
    void testParseCharsetFailure() {
        I18n.parseCharset("INVALID-CHARSET")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseCharsetNull() {
        I18n.parseCharset(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseCharsetEmpty() {
        I18n.parseCharset("")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseZoneIdSuccess() {
        I18n.parseZoneId("America/New_York")
                .onFailureRun(Assertions::fail)
                .onSuccess(zoneId -> assertEquals(ZoneId.of("America/New_York"), zoneId));
    }

    @Test
    void testParseZoneIdUTC() {
        I18n.parseZoneId("UTC")
                .onFailureRun(Assertions::fail)
                .onSuccess(zoneId -> assertEquals(ZoneId.of("UTC"), zoneId));
    }

    @Test
    void testParseZoneIdZ() {
        I18n.parseZoneId("Z")
                .onFailureRun(Assertions::fail)
                .onSuccess(zoneId -> assertEquals(ZoneId.of("Z"), zoneId));
    }

    @Test
    void testParseZoneIdEurope() {
        I18n.parseZoneId("Europe/Paris")
                .onFailureRun(Assertions::fail)
                .onSuccess(zoneId -> assertEquals(ZoneId.of("Europe/Paris"), zoneId));
    }

    @Test
    void testParseZoneIdFailure() {
        I18n.parseZoneId("Invalid/Zone")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseZoneIdNull() {
        I18n.parseZoneId(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseZoneIdEmpty() {
        I18n.parseZoneId("")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseZoneOffsetSuccess() {
        I18n.parseZoneOffset("+01:00")
                .onFailureRun(Assertions::fail)
                .onSuccess(offset -> assertEquals(ZoneOffset.of("+01:00"), offset));
    }

    @Test
    void testParseZoneOffsetNegative() {
        I18n.parseZoneOffset("-05:30")
                .onFailureRun(Assertions::fail)
                .onSuccess(offset -> assertEquals(ZoneOffset.of("-05:30"), offset));
    }

    @Test
    void testParseZoneOffsetZ() {
        I18n.parseZoneOffset("Z")
                .onFailureRun(Assertions::fail)
                .onSuccess(offset -> assertEquals(ZoneOffset.UTC, offset));
    }

    @Test
    void testParseZoneOffsetShortForm() {
        I18n.parseZoneOffset("+05")
                .onFailureRun(Assertions::fail)
                .onSuccess(offset -> assertEquals(ZoneOffset.of("+05:00"), offset));
    }

    @Test
    void testParseZoneOffsetFailure() {
        I18n.parseZoneOffset("+25:00")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseZoneOffsetNull() {
        I18n.parseZoneOffset(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseZoneOffsetEmpty() {
        I18n.parseZoneOffset("")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }
}
