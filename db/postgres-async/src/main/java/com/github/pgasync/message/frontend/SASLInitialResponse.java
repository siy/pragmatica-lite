package com.github.pgasync.message.frontend;

import com.github.pgasync.message.Message;
import com.github.pgasync.sasl.SaslPrep;

/**
 * it is a bit weired to have a username both here and in a {@link StartupMessage}.
 * But according to the SCRAM specification in RFC5802 we have to have it :(
 */
public class SASLInitialResponse implements Message {

    private final String saslMechanism;
    private final String channelBindingType;
    private final String userName;
    private final String nonce;
    private final String gs2Header;
    private final String clientFirstMessage;
    private final String clientFirstMessageBare;

    public SASLInitialResponse(String saslMechanism, String channelBindingType, String userName, String clientNonce) {
        this.saslMechanism = saslMechanism;
        this.channelBindingType = channelBindingType;
        this.userName = userName;
        this.nonce = clientNonce;

        String channelBinding = channelBindingType != null && !channelBindingType.isBlank() ? "p=" + channelBindingType + "," : "n,";
        gs2Header = channelBinding + ","; // It could be '"a=" + msg.getUsername() + ",";' but it is not needed unless we are using impersonate techniques.

        clientFirstMessageBare = "n=" + SaslPrep.asQueryString(userName) + ",r=" + clientNonce;
        clientFirstMessage = gs2Header + clientFirstMessageBare;
    }

    @SuppressWarnings("unused")
    public String userName() {
        return userName;
    }

    @SuppressWarnings("unused")
    public String channelBindingType() {
        return channelBindingType;
    }

    public String saslMechanism() {
        return saslMechanism;
    }

    @SuppressWarnings("unused")
    public String nonce() {
        return nonce;
    }

    public String gs2Header() {
        return gs2Header;
    }

    public String clientFirstMessageBare() {
        return clientFirstMessageBare;
    }

    public String clientFirstMessage() {
        return clientFirstMessage;
    }
}
