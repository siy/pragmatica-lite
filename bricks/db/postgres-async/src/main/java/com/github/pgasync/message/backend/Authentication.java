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

package com.github.pgasync.message.backend;

import com.github.pgasync.message.Message;

import static com.github.pgasync.util.HexConverter.printHexBinary;

/**
 * @author Antti Laisi, Marat Gainullin
 */
public record Authentication(boolean authenticationOk, boolean saslScramSha256, byte[] md5salt,
                             String saslContinueData, byte[] saslAdditionalData) implements Message {
    public static final Authentication OK = new Authentication(true, false, null, null, null);
    public static final Authentication CLEAR_TEXT = new Authentication(false, false, null, null, null);
    public static final Authentication SCRAM_SHA_256 = new Authentication(false, true, null, null, null);
    public static final String SUPPORTED_SASL = "SCRAM-SHA-256";

    public boolean saslServerFinalResponse() {
        return saslAdditionalData != null;
    }

    @Override
    public String toString() {
        return String.format("Authentication(success=%s, md5salt=%s, scramSha256=%s, saslContinueData=%s, saslAdditionalData=%s)",
                             authenticationOk,
                             md5salt != null ? printHexBinary(md5salt) : "",
                             saslScramSha256,
                             saslContinueData != null ? saslContinueData : "",
                             saslAdditionalData != null ? printHexBinary(saslAdditionalData) : ""
        );
    }

    public record SaslContinueServerFirstMessage(String augmentedNonce, String salt, int iterations) {
        @Override
        public String toString() {
            return "SaslContinueServerFirstMessage{augmentedNonce='" + augmentedNonce + '\'' + ", salt='" + salt + '\'' + ", iterations=" + iterations + '}';
        }

        public static SaslContinueServerFirstMessage parse(String text) {
            var attributes = text.split(",");
            String augmentedNonce = null;
            String salt = null;
            int iterations = -1;

            for (String a : attributes) {
                char aName = a.charAt(0);
                switch (aName) {
                    case 'r':
                        augmentedNonce = a.substring(2);
                        break;
                    case 's':
                        salt = a.substring(2);
                        break;
                    case 'i':
                        iterations = Integer.parseInt(a.substring(2));
                        break;
                }
            }

            if (augmentedNonce != null && !augmentedNonce.isBlank() && salt != null && !salt.isBlank() && iterations != -1) {
                return new SaslContinueServerFirstMessage(augmentedNonce, salt, iterations);
            } else {
                throw new IllegalStateException("'r', 's' and 'i' attributes should present in 'server-first-message'");
            }
        }
    }
}
