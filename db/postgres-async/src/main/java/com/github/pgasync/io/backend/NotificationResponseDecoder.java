package com.github.pgasync.io.backend;

import com.github.pgasync.io.Decoder;
import com.github.pgasync.message.backend.NotificationResponse;
import com.github.pgasync.io.IO;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * See <a href="https://www.postgresql.org/docs/11/protocol-message-formats.html">Postgres message formats</a>
 *
 * <pre>
 * NotificationResponse (B)
 *  Byte1('A')
 *      Identifies the message as a notification response.
 *  Int32
 *      Length of message contents in bytes, including self.
 *  Int32
 *      The process ID of the notifying backend process.
 *  String
 *      The name of the channel that the notify has been raised on.
 *  String
 *      The "payload" string passed from the notifying process.
 * </pre>
 *
 * @author Antti Laisi
 */
public class NotificationResponseDecoder implements Decoder<NotificationResponse> {

    @Override
    public NotificationResponse read(ByteBuffer buffer, int contentLength, Charset encoding) {
        return new NotificationResponse(buffer.getInt(), IO.getCString(buffer, encoding), IO.getCString(buffer, encoding));
    }

    @Override
    public byte getMessageId() {
        return 'A';
    }
}
