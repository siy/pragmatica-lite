package com.realworld.business.user.registration.uniquenesschecker;

import com.realworld.business.user.registration.ParsedRequest;
import com.realworld.business.user.registration.uniquenesschecker.domain.UniqueRequest;
import org.pragmatica.lang.Promise;

public interface UniquenessChecker {
    Promise<UniqueRequest> perform(ParsedRequest request);

    static UniquenessChecker uniquenessChecker(NameUniquenessChecker nameChecker, EmailUniquenessChecker emailChecker) {
        return request -> Promise.all(
                                         nameChecker.perform(request.username()),
                                         emailChecker.perform(request.email()),
                                         Promise.success(request.password()))
                                 .map(UniqueRequest::uniqueRequest);
    }
}
