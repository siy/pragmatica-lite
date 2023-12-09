package com.github.pgasync.io.backend;

import com.github.pgasync.io.Decoder;
import com.github.pgasync.io.IO;
import com.github.pgasync.message.backend.ParameterStatus;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * ParameterStatus (B)
 *  Byte1('S')
 *      Identifies the message as a run-time parameter status report.
 *
 *  Int32
 *       Length of message contents in bytes, including self.
 *
 *  String
 *       The name of the run-time parameter being reported.
 *
 *  String
 *       The current value of the parameter.
 */
public class ParameterStatusDecoder implements Decoder<ParameterStatus> {

    @Override
    public byte getMessageId() {
        return 'S';
    }

    @Override
    public ParameterStatus read(ByteBuffer buffer, int contentLength, Charset encoding) {
        return new ParameterStatus(IO.getCString(buffer, encoding), IO.getCString(buffer, encoding));
    }
}
