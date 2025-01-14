/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pgasync.io.backend;

import com.github.pgasync.io.Decoder;
import com.github.pgasync.io.IO;
import com.github.pgasync.message.backend.Authentication;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * See <a href="www.postgresql.org/docs/9.3/static/protocol-message-formats.html">Postgres message formats</a>
 *
 * <pre>
 * AuthenticationOk (B)
 *  Byte1('R')
 *      Identifies the message as an authentication request.
 *  Int32(8)
 *      Length of message contents in bytes, including self.
 *  Int32(0)
 *      Specifies that the authentication was successful.
 *
 * AuthenticationMD5Password (B)
 *  Byte1('R')
 *      Identifies the message as an authentication request.
 *  Int32(12)
 *      Length of message contents in bytes, including self.
 *  Int32(5)
 *      Specifies that an MD5-encrypted password is required.
 *  Byte4
 *      The salt to use when encrypting the password.
 * </pre>
 *
 * @author Antti Laisi
 */
public class AuthenticationDecoder implements Decoder<Authentication> {
    private static final int OK = 0;
    private static final int SASL_FINAL = 12;
    private static final int SASL_CONTINUE = 11;
    private static final int SASL = 10;
    private static final int AUTHENTICATION_SSPI = 9;
    private static final int AUTHENTICATION_GSS = 7;
    private static final int AUTHENTICATION_SCM_CREDENTIAL = 6;
    private static final int PASSWORD_MD5_CHALLENGE = 5;
    private static final int CLEARTEXT_PASSWORD = 3;
    private static final int AUTHENTICATION_KERBEROS_V5 = 2;
    public static final String AUTHENTICATION_IS_NOT_SUPPORTED = " authentication is not supported";

    @Override
    public byte getMessageId() {
        return 'R';
    }

    @Override
    public Authentication read(ByteBuffer buffer, int contentLength, Charset encoding) {
        int type = buffer.getInt();
        return switch (type) {
            case CLEARTEXT_PASSWORD -> Authentication.CLEAR_TEXT;
            case PASSWORD_MD5_CHALLENGE -> {
                byte[] md5Salt = new byte[4];
                buffer.get(md5Salt);
                yield  new Authentication(false, false, md5Salt, null, null);
            }
            case AUTHENTICATION_SSPI -> throw new UnsupportedOperationException(AUTHENTICATION_IS_NOT_SUPPORTED);
            case AUTHENTICATION_GSS -> throw new UnsupportedOperationException("AuthenticationGss" + AUTHENTICATION_IS_NOT_SUPPORTED);
            case AUTHENTICATION_SCM_CREDENTIAL ->
                throw new UnsupportedOperationException("AuthenticationScmCredential" + AUTHENTICATION_IS_NOT_SUPPORTED);
            case AUTHENTICATION_KERBEROS_V5 ->
                throw new UnsupportedOperationException("AuthenticationKerberosV5" + AUTHENTICATION_IS_NOT_SUPPORTED);
            case OK -> Authentication.OK;
            case SASL -> {
                boolean scramSha256Met = false;
                String sasl = IO.getCString(buffer, encoding);
                while (!sasl.isEmpty()) {
                    if (Authentication.SUPPORTED_SASL.equals(sasl)) {
                        scramSha256Met = true;
                    }
                    sasl = IO.getCString(buffer, encoding);
                }
                if (scramSha256Met) {
                    yield  Authentication.SCRAM_SHA_256;
                } else {
                    throw new UnsupportedOperationException("The server doesn't support " + Authentication.SUPPORTED_SASL + " SASL mechanism");
                }
            }
            case SASL_CONTINUE -> {
                byte[] saslContinueData = new byte[contentLength - 4]; // Minus type field size
                buffer.get(saslContinueData);
                yield new Authentication(false, false, null, new String(saslContinueData, encoding), null);
            }
            case SASL_FINAL -> {
                byte[] saslAdditionalData = new byte[contentLength - 4]; // Minus type field size
                buffer.get(saslAdditionalData);
                yield new Authentication(false, false, null, null, saslAdditionalData);
            }
            default -> throw new UnsupportedOperationException("Unsupported authentication type: " + type);
        };
    }

}
