package org.pragmatica.dns;

import org.pragmatica.annotation.Template;

import java.net.InetAddress;
import java.util.List;

@Template
public record DnsServers(List<InetAddress> servers) {
    static DnsServersTemplate template() {
        return DnsServersTemplate.INSTANCE;
    }
}
