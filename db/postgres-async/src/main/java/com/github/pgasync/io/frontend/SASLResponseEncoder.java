package com.github.pgasync.io.frontend;

import com.github.pgasync.message.frontend.SASLResponse;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class SASLResponseEncoder extends SkipableEncoder<SASLResponse> {

    @Override
    protected byte getMessageId() {
        return 'p';
    }

    @Override
    protected void writeBody(SASLResponse msg, ByteBuffer buffer, Charset encoding) {
        String clientFinalMessage = msg.clientFinalMessage(SASLResponse.HMAC256_NAME, SASLResponse.SHA256_DIGEST_NAME);
        buffer.put(clientFinalMessage.getBytes(encoding));
    }

    @Override
    public Class<SASLResponse> getMessageType() {
        return SASLResponse.class;
    }
}
