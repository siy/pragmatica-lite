package org.pragmatica.net;

import java.net.InetSocketAddress;

/// Network node address. At present TCP/IP address structure is assumed
public interface NodeAddress {
    String host();

    int port();

    static NodeAddress nodeAddress(String host, int port) {
        record nodeAddress(String host, int port) implements NodeAddress {}

        return new nodeAddress(host, port);
    }

    static NodeAddress nodeAddress(InetSocketAddress socketAddress) {
        return nodeAddress(socketAddress.getHostName(), socketAddress.getPort());
    }
}
