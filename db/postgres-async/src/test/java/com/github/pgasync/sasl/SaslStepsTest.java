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
        assertEquals("n,,", saslInitialResponse.getGs2Header());
        assertEquals("n,,n=" + USER_NAME + ",r=" + CLIENT_NONCE, saslInitialResponse.getClientFirstMessage());
        // Here should be "n,,n=<USER_NAME>,r=" + CLIENT_NONCE, but Postgres expects an empty user name because of the startup message
    }

    @Test
    public void clientFinalMessage() {
        SASLInitialResponse saslInitialResponse = new SASLInitialResponse(Authentication.SUPPORTED_SASL, null, USER_NAME, CLIENT_NONCE);
        SASLResponse saslResponse = SASLResponse.of(USER_PASSWORD, SERVER_FIRST_MESSAGE, CLIENT_NONCE, saslInitialResponse.getGs2Header(), saslInitialResponse.getClientFirstMessageBare());

        assertEquals("n,,", saslResponse.getGs2Header());
        assertEquals(CLIENT_NONCE + SERVER_NONCE, saslResponse.getServerNonce());
        assertEquals("n=" + USER_NAME + ",r=" + CLIENT_NONCE, saslResponse.getClientFirstMessageBare());
        // Here should be "n,,n=<USER_NAME>,r=" + CLIENT_NONCE, but Postgres expects an empty user name because of the startup message
        assertEquals(I, saslResponse.getI());
        assertEquals(USER_PASSWORD, saslResponse.getPassword());
        assertEquals(SERVER_FIRST_MESSAGE, saslResponse.getServerFirstMessage());
        assertEquals(SERVER_SALT, saslResponse.getServerSalt());
        assertEquals("c=biws,r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,p=v0X8v3Bz2T0CJGbJQyF0X+HI4Ts=", saslResponse.clientFinalMessage("HmacSHA1", "SHA-1"));
    }

}
