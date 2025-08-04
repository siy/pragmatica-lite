package com.realworld.business.user.registration.hashpassword;

import com.realworld.business.user.registration.hashpassword.domain.Error;
import org.pragmatica.lang.Result;

import java.util.Base64;

public interface Base64Encoder {
    Result<String> encode(byte[] bytes);

    static Base64Encoder base64Encoder() {
        return bytes -> Result.lift1(Error.Base64Decoder::map, Base64.getEncoder()::encodeToString, bytes);
    }
}
