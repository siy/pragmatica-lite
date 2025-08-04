package com.realworld.business.user.registration.uniquenesschecker;

import com.realworld.business.user.registration.domain.Email;
import com.realworld.business.user.registration.port.UserPersistence;
import org.pragmatica.lang.Promise;

public interface EmailUniquenessChecker {
    Promise<Email> perform(Email email);

    static EmailUniquenessChecker emailUniquenessChecker(UserPersistence userPersistence) {
        return email -> userPersistence.byEmail(email).map(email);
    }
}
