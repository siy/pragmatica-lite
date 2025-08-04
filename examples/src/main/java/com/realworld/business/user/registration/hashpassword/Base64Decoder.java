package com.realworld.business.user.registration.hashpassword;

import com.realworld.business.user.registration.hashpassword.domain.Error;
import org.pragmatica.lang.Result;

import java.util.Base64;

public interface Base64Decoder {
    Result<byte[]> decode(String encoded);

    static Base64Decoder base64Decoder() {
        return encoded -> Result.lift1(Error.Base64Encoder::map, Base64.getDecoder()::decode, encoded);
    }
}
