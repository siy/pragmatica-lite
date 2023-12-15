package org.pragmatica.dns.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.dns.inet.InetUtils;

import java.net.InetSocketAddress;
import java.util.stream.Stream;

import static org.pragmatica.dns.DomainName.domainName;
import static org.pragmatica.lang.Promise.all;
import static org.pragmatica.lang.io.Timeout.timeout;

@SuppressWarnings("deprecation")
class DnsClientTest {

    @Test
    void simpleRequestIsProcessed() {
        var client = DnsClient.create();
        var serverIp = InetUtils.forBytes(new byte[]{1, 1, 1, 1}).unwrap();
        var serverAddress = new InetSocketAddress(serverIp, 53);

        var ibm = client.resolve(domainName("www.ibm.com"), serverAddress);
        var google = client.resolve(domainName("www.google.com"), serverAddress);
        var github = client.resolve(domainName("www.github.com"), serverAddress);
        var twitter = client.resolve(domainName("www.twitter.com"), serverAddress);

        all(ibm, google, github, twitter)
            .map(Stream::of)
            .await(timeout(15).seconds())
            .onSuccess(list -> list.forEach(System.out::println))
            .onFailureRun(Assertions::fail);
    }
}