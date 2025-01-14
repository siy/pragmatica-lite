package com.github.pgasync.sasl;

import com.github.pgasync.message.backend.Authentication;
import com.github.pgasync.message.frontend.SASLInitialResponse;
import com.github.pgasync.message.frontend.SASLResponse;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SaslStepsTest {

    private static final String USER_NAME = "user";
    private static final String USER_PASSWORD = "pencil";
    private static final String CLIENT_NONCE = "fyko+d2lbbFgONRv9qkxdawL";
    private static final String SERVER_NONCE = "3rfcNHYJY1ZVvWVs7j";
    private static final String SERVER_SALT = "QSXCR+Q6sek8bf92";
    private static final int I = 4096;
    private static final String SERVER_FIRST_MESSAGE = "r=" + CLIENT_NONCE + SERVER_NONCE + ",s=" + SERVER_SALT + ",i=" + I;

    @Test
    public void clientFirstMessage() {
        SASLInitialResponse saslInitialResponse = new SASLInitialResponse(Authentication.SUPPORTED_SASL, null, USER_NAME, CLIENT_NONCE);
        assertEquals("n,,", saslInitialResponse.gs2Header());
        assertEquals("n,,n=" + USER_NAME + ",r=" + CLIENT_NONCE, saslInitialResponse.clientFirstMessage());
        // Here should be "n,,n=<USER_NAME>,r=" + CLIENT_NONCE, but Postgres expects an empty user name because of the startup message
    }

    @Test
    public void clientFinalMessage() {
        SASLInitialResponse saslInitialResponse = new SASLInitialResponse(Authentication.SUPPORTED_SASL, null, USER_NAME, CLIENT_NONCE);
        SASLResponse saslResponse = SASLResponse.of(USER_PASSWORD, SERVER_FIRST_MESSAGE, CLIENT_NONCE, saslInitialResponse.gs2Header(), saslInitialResponse.clientFirstMessageBare());

        assertEquals("n,,", saslResponse.gs2Header());
        assertEquals(CLIENT_NONCE + SERVER_NONCE, saslResponse.serverNonce());
        assertEquals("n=" + USER_NAME + ",r=" + CLIENT_NONCE, saslResponse.clientFirstMessageBare());
        // Here should be "n,,n=<USER_NAME>,r=" + CLIENT_NONCE, but Postgres expects an empty user name because of the startup message
        assertEquals(I, saslResponse.i());
        assertEquals(USER_PASSWORD, saslResponse.password());
        assertEquals(SERVER_FIRST_MESSAGE, saslResponse.serverFirstMessage());
        assertEquals(SERVER_SALT, saslResponse.serverSalt());
        assertEquals("c=biws,r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,p=v0X8v3Bz2T0CJGbJQyF0X+HI4Ts=", saslResponse.clientFinalMessage("HmacSHA1", "SHA-1"));
    }

}
