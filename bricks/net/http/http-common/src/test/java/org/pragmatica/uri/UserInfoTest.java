package org.pragmatica.uri;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pragmatica.lang.Option.empty;
import static org.pragmatica.lang.Option.option;

class UserInfoTest {
    @Test
    void fullUserInfoResultsInColonSeparatedPair() {
        assertEquals("user:password", UserInfo.userInfo("user", "password").forIRI());
    }

    @Test
    void emptyNameResultsInEmptyString() {
        assertEquals("", UserInfo.userInfo(empty(), option("password")).forIRI());
        assertEquals("", UserInfo.userInfo(empty(), empty()).forIRI());
    }

    @Test
    void emptyPasswordResultsInNameString() {
        assertEquals("name", UserInfo.userInfo(option("name"), empty()).forIRI());
    }
}