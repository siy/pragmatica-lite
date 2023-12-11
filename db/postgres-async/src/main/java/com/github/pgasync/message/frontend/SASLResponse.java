package com.github.pgasync.message.frontend;

import com.github.pgasync.message.Message;
import com.github.pgasync.message.backend.Authentication;
import com.github.pgasync.sasl.SaslPrep;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SASLResponse implements Message {

    // These three fields contain the data, which have been already sent to a server, so we should recalculate them.
    private final String gs2Header;
    private final String clientFirstMessageBare;
    private final String serverFirstMessage;
    //
    private final String password;
    private final String serverSalt;
    private final int i;
    private final String serverNonce;

    private SASLResponse(String password,
                         String serverSalt,
                         int i,
                         String serverNonce,
                         String gs2Header,
                         String clientFirstMessageBare,
                         String serverFirstMessage) {
        this.password = password;
        this.serverSalt = serverSalt;
        this.i = i;
        this.serverNonce = serverNonce;
        this.gs2Header = gs2Header;
        this.clientFirstMessageBare = clientFirstMessageBare;
        this.serverFirstMessage = serverFirstMessage;
    }

    public String password() {
        return password;
    }

    public String serverSalt() {
        return serverSalt;
    }

    public int i() {
        return i;
    }

    public String serverNonce() {
        return serverNonce;
    }

    public String gs2Header() {
        return gs2Header;
    }

    public String clientFirstMessageBare() {
        return clientFirstMessageBare;
    }

    public String serverFirstMessage() {
        return serverFirstMessage;
    }

    public String clientFinalMessage(String hMacName, String digestName) {
        var encoded = new String(Base64.getEncoder().withoutPadding().encode(gs2Header.getBytes(StandardCharsets.US_ASCII)),
                                 StandardCharsets.UTF_8);
        var clientFinalMessageWithoutProof = STR."c=\{encoded},r=\{serverNonce}";
        var clientProof = saslClientProof(password,
                                          serverSalt,
                                          i,
                                          clientFirstMessageBare,
                                          serverFirstMessage,
                                          clientFinalMessageWithoutProof,
                                          hMacName,
                                          digestName);
        return STR."\{clientFinalMessageWithoutProof},p=\{clientProof}";
    }

    private static final byte[] INT_1 = {0, 0, 0, 1};
    public static final String SHA256_DIGEST_NAME = "SHA-256";
    public static final String HMAC256_NAME = "HmacSHA256";

    /**
     * Generates salted password.
     *
     * @param password        Clear form password, i.e. what user typed
     * @param salt            Salt to be used
     * @param iterationsCount Iterations for 'salting'
     * @param hmacName        HMAC to be used
     *
     * @return salted password
     * @throws InvalidKeyException      if internal error occur while working with SecretKeySpec
     * @throws NoSuchAlgorithmException if hmacName is not supported by the java
     */
    public static byte[] generateSaltedPassword(final String password,
                                                byte[] salt,
                                                int iterationsCount,
                                                String hmacName) throws InvalidKeyException, NoSuchAlgorithmException {
        Mac mac = createHmac(SaslPrep.asQueryString(password).getBytes(StandardCharsets.US_ASCII), hmacName);
        mac.update(salt);
        mac.update(INT_1);
        byte[] result = mac.doFinal();

        byte[] previous = null;
        for (int i = 1; i < iterationsCount; i++) {
            mac.update(previous != null ? previous : result);
            previous = mac.doFinal();
            for (int x = 0; x < result.length; x++) {
                result[x] ^= previous[x];
            }
        }
        return result;
    }

    /**
     * Creates HMAC
     *
     * @param keyBytes key
     * @param hmacName HMAC name
     *
     * @return Mac
     * @throws InvalidKeyException      if internal error occur while working with SecretKeySpec
     * @throws NoSuchAlgorithmException if hmacName is not supported by the java
     */
    public static Mac createHmac(final byte[] keyBytes, String hmacName) throws NoSuchAlgorithmException,
        InvalidKeyException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, hmacName);
        Mac mac = Mac.getInstance(hmacName);
        mac.init(key);
        return mac;
    }

    /**
     * Computes HMAC byte array for given string
     *
     * @param key      key
     * @param hmacName HMAC name
     * @param string   string for which HMAC will be computed
     *
     * @return computed HMAC
     * @throws InvalidKeyException      if internal error occur while working with SecretKeySpec
     * @throws NoSuchAlgorithmException if hmacName is not supported by the java
     */
    public static byte[] computeHmac(final byte[] key, String hmacName, final String string)
        throws InvalidKeyException, NoSuchAlgorithmException {

        Mac mac = createHmac(key, hmacName);
        mac.update(string.getBytes(StandardCharsets.US_ASCII));
        return mac.doFinal();
    }

    private static String saslClientProof(String password,
                                          String salt,
                                          int i,
                                          String clientFirstMessageBare,
                                          String serverFirstMessage,
                                          String clientFinalMessageWithoutProof,
                                          String hMacName,
                                          String digestName) {
        try {
            byte[] saltedPassword = generateSaltedPassword(password,
                                                           Base64.getDecoder().decode(salt.getBytes(StandardCharsets.UTF_8)),
                                                           i,
                                                           hMacName);

            String authMessage = clientFirstMessageBare + "," + serverFirstMessage + "," + clientFinalMessageWithoutProof;

            byte[] clientKey = computeHmac(saltedPassword, hMacName, "Client Key");
            byte[] storedKey = MessageDigest.getInstance(digestName).digest(clientKey);

            byte[] clientSignature = computeHmac(storedKey, hMacName, authMessage);

            // clientProof here is computed in place of clientKey
            for (int j = 0; j < clientKey.length; j++) {
                clientKey[j] ^= clientSignature[j];
            }
            return new String(Base64.getEncoder().encode(clientKey), StandardCharsets.UTF_8);
        } catch (java.security.InvalidKeyException | java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static SASLResponse of(String password, String serverFirstMessage, String clientNonce, String gs2Header, String clientFirstMessageBare) {
        var continueData = Authentication.SaslContinueServerFirstMessage.parse(serverFirstMessage);
        var serverNonce = continueData.augmentedNonce();

        if (serverNonce != null &&
            serverNonce.length() > clientNonce.length() &&
            serverNonce.startsWith(clientNonce)) {
            return new SASLResponse(password,
                                    continueData.salt(),
                                    continueData.iterations(),
                                    serverNonce,
                                    gs2Header,
                                    clientFirstMessageBare,
                                    serverFirstMessage);
        } else {
            throw new IllegalStateException("Bad server nonce detected on 'server-first-message' step");
        }
    }
}
