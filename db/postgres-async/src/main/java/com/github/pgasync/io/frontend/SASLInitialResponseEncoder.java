package com.github.pgasync.io.frontend;

import com.github.pgasync.io.IO;
import com.github.pgasync.message.frontend.SASLInitialResponse;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class SASLInitialResponseEncoder extends SkipableEncoder<SASLInitialResponse> {

    @Override
    protected byte getMessageId() {
        return 'p';
    }

    @Override
    protected void writeBody(SASLInitialResponse msg, ByteBuffer buffer, Charset encoding) {
        IO.putCString(buffer, msg.getSaslMechanism(), encoding);
        byte[] clientFirstMessageContent = msg.getClientFirstMessage().getBytes(encoding);
        buffer.putInt(clientFirstMessageContent.length);
        buffer.put(clientFirstMessageContent);
    }

    @Override
    public Class<SASLInitialResponse> getMessageType() {
        return SASLInitialResponse.class;
    }
}
