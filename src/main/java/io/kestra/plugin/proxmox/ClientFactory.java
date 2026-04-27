package io.kestra.plugin.proxmox;

import io.kestra.core.runners.RunContext;

public final class ClientFactory {

    private ClientFactory() {}

    public static ProxmoxClient create(
        String host,
        int port,
        String node,
        String username,
        String password,
        String tokenId,
        String tokenSecret,
        boolean verifySsl,
        RunContext runContext
    ) throws Exception {
        String baseUrl = (host.startsWith("http://") || host.startsWith("https://"))
            ? host + "/api2/json"
            : "https://" + host + ":" + port + "/api2/json";

        if (tokenId != null && !tokenId.isBlank() && tokenSecret != null && !tokenSecret.isBlank()) {
            return ProxmoxClient.withToken(baseUrl, node, tokenId, tokenSecret, runContext);
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Proxmox auth: provide either username+password or tokenId+tokenSecret.");
        }
        return new ProxmoxClient(baseUrl, node, username, password, runContext);
    }
}
