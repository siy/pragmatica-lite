package com.github.pgasync.io.backend;

import com.github.pgasync.io.Decoder;
import com.github.pgasync.message.backend.BackendKeyData;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * BackendKeyData (B)
 *  Byte1('K')
 *      Identifies the message as cancellation key data. The frontend must save these values if it wishes to be able to issue CancelRequest messages later.
 *
 *  Int32(12)
 *      Length of message contents in bytes, including self.
 *
 *  Int32
 *      The process ID of this backend.
 *
 *  Int32
 *      The secret key of this backend.
 */
public class BackendKeyDataDecoder implements Decoder<BackendKeyData> {

    @Override
    public byte getMessageId() {
        return 'K';
    }

    @Override
    public BackendKeyData read(ByteBuffer buffer, int contentLength, Charset encoding) {
        return new BackendKeyData(buffer.getInt(), buffer.getInt());
    }
}
