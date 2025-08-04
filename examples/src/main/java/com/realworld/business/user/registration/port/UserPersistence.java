package com.realworld.business.user.registration.port;

import com.realworld.business.user.registration.domain.Email;
import org.pragmatica.lang.Promise;

public interface UserPersistence {
    <T> Promise<Email> byEmail(Email email);
}
