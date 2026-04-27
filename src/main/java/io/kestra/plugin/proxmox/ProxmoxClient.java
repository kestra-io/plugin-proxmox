package io.kestra.plugin.proxmox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.http.client.configurations.SslOptions;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;

import java.net.URI;
import java.util.Map;

public class ProxmoxClient implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int POLL_INTERVAL_MS = 2_000;
    private static final int DEFAULT_TIMEOUT_MINUTES = 5;

    private final HttpClient http;
    private final String baseUrl;
    private final String node;

    private String authCookie;
    private String csrfToken;
    private String tokenAuthHeader;

    public ProxmoxClient(String baseUrl, String node, String username, String password, boolean verifySsl, RunContext runContext)
        throws Exception {
        this.baseUrl = baseUrl;
        this.node = node;
        this.http = HttpClient.builder().runContext(runContext).configuration(sslConfig(verifySsl)).build();
        authenticateWithTicket(username, password);
    }

    public static ProxmoxClient withToken(String baseUrl, String node, String tokenId, String tokenSecret, boolean verifySsl, RunContext runContext)
        throws Exception {
        var client = new ProxmoxClient(baseUrl, node, verifySsl, runContext);
        client.tokenAuthHeader = "PVEAPIToken=" + tokenId + "=" + tokenSecret;
        return client;
    }

    private ProxmoxClient(String baseUrl, String node, boolean verifySsl, RunContext runContext) throws Exception {
        this.baseUrl = baseUrl;
        this.node = node;
        this.http = HttpClient.builder().runContext(runContext).configuration(sslConfig(verifySsl)).build();
    }

    private static HttpConfiguration sslConfig(boolean verifySsl) {
        return HttpConfiguration.builder()
            .ssl(SslOptions.builder()
                .insecureTrustAllCertificates(Property.ofValue(!verifySsl))
                .build())
            .build();
    }

    private void authenticateWithTicket(String username, String password) throws Exception {
        var request = HttpRequest.builder()
            .uri(URI.create(baseUrl + "/access/ticket"))
            .method("POST")
            .body(HttpRequest.UrlEncodedRequestBody.builder()
                .content(Map.of("username", username, "password", password))
                .build())
            .build();

        var response = executeRequest(request, "POST /access/ticket");
        var data = requireData(response, "POST /access/ticket");
        this.authCookie = data.get("ticket").asText();
        this.csrfToken = data.get("CSRFPreventionToken").asText();
    }

    private void addAuthHeaders(HttpRequest.HttpRequestBuilder builder) {
        if (tokenAuthHeader != null) {
            builder.addHeader("Authorization", tokenAuthHeader);
        } else {
            builder.addHeader("Cookie", "PVEAuthCookie=" + authCookie)
                .addHeader("CSRFPreventionToken", csrfToken);
        }
    }

    public JsonNode get(String path) throws Exception {
        var builder = HttpRequest.builder()
            .uri(URI.create(baseUrl + path))
            .method("GET");
        addAuthHeaders(builder);
        var response = executeRequest(builder.build(), "GET " + path);
        return requireData(response, "GET " + path);
    }

    public JsonNode post(String path, Map<String, String> params) throws Exception {
        var builder = HttpRequest.builder()
            .uri(URI.create(baseUrl + path))
            .method("POST");
        builder.body(HttpRequest.UrlEncodedRequestBody.builder()
            .content(params != null && !params.isEmpty() ? Map.copyOf(params) : Map.of())
            .build());
        addAuthHeaders(builder);
        var response = executeRequest(builder.build(), "POST " + path);
        return requireData(response, "POST " + path);
    }

    public JsonNode put(String path, Map<String, String> params) throws Exception {
        var builder = HttpRequest.builder()
            .uri(URI.create(baseUrl + path))
            .method("PUT");
        builder.body(HttpRequest.UrlEncodedRequestBody.builder()
            .content(params != null && !params.isEmpty() ? Map.copyOf(params) : Map.of())
            .build());
        addAuthHeaders(builder);
        var response = executeRequest(builder.build(), "PUT " + path);
        return requireData(response, "PUT " + path);
    }

    public JsonNode delete(String path) throws Exception {
        var builder = HttpRequest.builder()
            .uri(URI.create(baseUrl + path))
            .method("DELETE");
        addAuthHeaders(builder);
        var response = executeRequest(builder.build(), "DELETE " + path);
        return requireData(response, "DELETE " + path);
    }

    public String postAndWait(String path, Map<String, String> params) throws Exception {
        var result = post(path, params);
        var upid = result.isNull() ? "" : result.asText();
        if (!upid.isBlank()) {
            waitForTask(upid);
        }
        return upid;
    }

    public int resolveVmId(String nameOrId) throws Exception {
        if (nameOrId.matches("\\d+")) {
            return Integer.parseInt(nameOrId);
        }
        var resources = get("/cluster/resources?type=vm");
        for (var item : resources) {
            var name = item.path("name").asText(null);
            if (nameOrId.equals(name)) {
                return item.path("vmid").asInt();
            }
        }
        throw new IllegalArgumentException("No VM or container found with name: " + nameOrId);
    }

    public void waitForTask(String upid) throws Exception {
        waitForTask(upid, DEFAULT_TIMEOUT_MINUTES * 60);
    }

    public void waitForTask(String upid, int timeoutSeconds) throws Exception {
        var encodedUpid = java.net.URLEncoder.encode(upid, java.nio.charset.StandardCharsets.UTF_8);
        var statusPath = "/nodes/" + node + "/tasks/" + encodedUpid + "/status";

        var deadline = System.currentTimeMillis() + (long) timeoutSeconds * 1_000;
        while (System.currentTimeMillis() < deadline) {
            var status = get(statusPath);
            var taskStatus = status.path("status").asText("");

            if ("stopped".equals(taskStatus)) {
                var exitStatus = status.path("exitstatus").asText("");
                if ("OK".equals(exitStatus)) {
                    return;
                }
                throw new RuntimeException("Proxmox task failed with exitstatus: " + exitStatus + " (upid=" + upid + ")");
            }

            Thread.sleep(POLL_INTERVAL_MS);
        }

        throw new RuntimeException("Proxmox task timed out after " + timeoutSeconds + "s (upid=" + upid + ")");
    }

    private HttpResponse<String> executeRequest(HttpRequest request, String context) throws Exception {
        try {
            return http.request(request, String.class);
        } catch (HttpClientResponseException e) {
            throw new RuntimeException(context + " failed with HTTP " + e.getResponse().getStatus().getCode() + ": " + e.getResponse().getBody());
        }
    }

    private JsonNode requireData(HttpResponse<String> response, String context) {
        var bodyStr = response.getBody() != null ? response.getBody() : "";
        try {
            var tree = MAPPER.readTree(bodyStr);
            var data = tree.get("data");
            return data != null ? data : MAPPER.nullNode();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response from " + context + ": " + bodyStr, e);
        }
    }

    @Override
    public void close() throws Exception {
        http.close();
    }
}
