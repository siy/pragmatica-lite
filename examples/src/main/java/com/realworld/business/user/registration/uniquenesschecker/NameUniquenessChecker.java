package com.realworld.business.user.registration.uniquenesschecker;

import com.realworld.business.user.registration.domain.Name;
import org.pragmatica.lang.Promise;

public interface NameUniquenessChecker {
    Promise<Name> perform(Name username);
}
