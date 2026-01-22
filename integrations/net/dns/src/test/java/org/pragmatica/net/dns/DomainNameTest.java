/*
 *  Copyright (c) 2022-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.pragmatica.net.dns;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.net.dns.DomainName.domainName;

class DomainNameTest {

    @Test
    void domainName_creates_instance_with_provided_name() {
        var domain = domainName("example.com").unwrap();

        assertThat(domain.name()).isEqualTo("example.com");
    }

    @Test
    void domainName_preserves_case() {
        var domain = domainName("Example.COM").unwrap();

        assertThat(domain.name()).isEqualTo("Example.COM");
    }

    @Test
    void domainName_equality_based_on_name() {
        var domain1 = domainName("example.com").unwrap();
        var domain2 = domainName("example.com").unwrap();
        var domain3 = domainName("other.com").unwrap();

        assertThat(domain1).isEqualTo(domain2);
        assertThat(domain1).isNotEqualTo(domain3);
    }

    @Test
    void domainName_hashCode_consistent_with_equality() {
        var domain1 = domainName("example.com").unwrap();
        var domain2 = domainName("example.com").unwrap();

        assertThat(domain1.hashCode()).isEqualTo(domain2.hashCode());
    }
}
