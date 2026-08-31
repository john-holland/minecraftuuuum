package com.minecraftuuuum.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Outbound Continuuuum / USC HTTP with {@code X-Tenant-ID}. Spring UCC stays on
 * 5050; Continuuuum library URL is {@code minecraftuuuum.continuuuum-base}.
 */
@Component
public class ContinuuuumTenantClient {
    public static final String TENANT_HEADER = "X-Tenant-ID";

    private final String tenantId;
    private final String fallbackTenant;
    private final String continuuuumBase;
    private final String uscBase;
    private final RestClient continuuuum;
    private final RestClient usc;
    private final ObjectMapper mapper;

    public ContinuuuumTenantClient(
            @Value("${minecraftuuuum.tenant-id}") String tenantId,
            @Value("${minecraftuuuum.fallback-tenant:default}") String fallbackTenant,
            @Value("${minecraftuuuum.continuuuum-base}") String continuuuumBase,
            @Value("${minecraftuuuum.usc-base:}") String uscBase,
            ObjectMapper mapper) {
        this.tenantId = tenantId;
        this.fallbackTenant = fallbackTenant;
        this.continuuuumBase = stripSlash(continuuuumBase);
        this.uscBase = stripSlash(uscBase);
        this.mapper = mapper;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.continuuuum = this.continuuuumBase.isEmpty()
                ? null
                : RestClient.builder().baseUrl(this.continuuuumBase).requestFactory(factory).build();
        this.usc = this.uscBase.isEmpty()
                ? null
                : RestClient.builder().baseUrl(this.uscBase).requestFactory(factory).build();
    }

    public String tenantId() {
        return tenantId;
    }

    public String fallbackTenant() {
        return fallbackTenant;
    }

    public String continuuuumBase() {
        return continuuuumBase;
    }

    public String uscBase() {
        return uscBase;
    }

    public boolean continuuuumReachable() {
        if (continuuuum == null) {
            return false;
        }
        try {
            Integer status = continuuuum.get().uri("/api/library/search?limit=1")
                    .header(TENANT_HEADER, tenantId)
                    .exchange((req, res) -> res.getStatusCode().value());
            return status != null && status < 500;
        } catch (RestClientException e) {
            return false;
        }
    }

    public Map<String, Object> defaultRetainerSplit() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tenantId", tenantId);
        out.put("creatorPct", 0.70);
        out.put("platformPct", 0.30);
        out.put("continuuuumHwmPct", 0.10);
        out.put("platformKind", "platform_microsoft");
        out.put("platformEnabled", true);
        out.put("serviceUnityEnabled", false);
        out.put("serviceCursorEnabled", false);
        out.put("serviceUnrealEnabled", false);
        out.put("retainer", true);
        out.put("source", "local-default");
        return out;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchTenantSplit() {
        if (continuuuum == null) {
            return defaultRetainerSplit();
        }
        try {
            Object json = continuuuum.get()
                    .uri("/api/payroll/tenants/{tenant}/split", tenantId)
                    .header(TENANT_HEADER, tenantId)
                    .exchange((req, res) -> {
                        byte[] bytes = res.getBody().readAllBytes();
                        if (bytes == null || bytes.length == 0) {
                            return null;
                        }
                        return mapper.readValue(bytes, Object.class);
                    });
            if (json instanceof Map<?, ?> m) {
                Map<String, Object> out = new LinkedHashMap<>((Map<String, Object>) m);
                out.put("source", "continuuuum");
                return out;
            }
        } catch (Exception ignored) {
            // Continuuuum down: local Marketplace defaults.
        }
        return defaultRetainerSplit();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchOauthStatus() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tenantId", tenantId);
        out.put("items", List.of());
        if (continuuuum == null) {
            return out;
        }
        try {
            Object json = continuuuum.get()
                    .uri("/api/tenant/oauth-connections")
                    .header(TENANT_HEADER, tenantId)
                    .exchange((req, res) -> {
                        byte[] bytes = res.getBody().readAllBytes();
                        if (bytes == null || bytes.length == 0) {
                            return null;
                        }
                        return mapper.readValue(bytes, Object.class);
                    });
            if (json instanceof Map<?, ?> m) {
                return new LinkedHashMap<>((Map<String, Object>) m);
            }
        } catch (Exception ignored) {
            // leave empty items
        }
        return out;
    }

    public Map<String, Object> forwardLibrary(String structural, Map<String, Object> payload) {
        String path = structural.startsWith("library") ? "/api/" + structural : "/api/library/" + structural;
        if (payload != null && payload.get("path") instanceof String extra) {
            path = extra.startsWith("/") ? extra : "/api/library/" + extra;
        }
        String method = payload != null && payload.get("method") instanceof String m ? m : inferMethod(structural);
        String query = payload != null && payload.get("query") instanceof String q ? q : queryFromPayload(payload);
        Object body = payload == null ? null : payload.get("body");
        if (body == null && payload != null && !payload.isEmpty() && !"GET".equalsIgnoreCase(method)) {
            Map<String, Object> copy = new LinkedHashMap<>(payload);
            copy.remove("method");
            copy.remove("query");
            copy.remove("path");
            body = copy;
        }
        ProxyResponse resp = exchange(continuuuum, continuuuumBase, method, path, query, body, "application/json");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", resp.ok());
        out.put("status", resp.status());
        out.put("tenant", resp.tenant());
        out.put("path", path);
        if (resp.json() != null) {
            out.put("data", resp.json());
        } else if (resp.body() != null) {
            out.put("body", resp.body());
        }
        if (resp.error() != null) {
            out.put("error", resp.error());
        }
        return out;
    }

    public ProxyResponse proxyLibrary(String method, String pathAndQuery, byte[] body, String contentType) {
        String path = pathAndQuery;
        String query = "";
        int q = pathAndQuery.indexOf('?');
        if (q >= 0) {
            path = pathAndQuery.substring(0, q);
            query = pathAndQuery.substring(q + 1);
        }
        if (!path.startsWith("/")) {
            path = "/api/library/" + path;
        }
        return exchange(continuuuum, continuuuumBase, method, path, query, body, contentType);
    }

    public ProxyResponse proxyUsc(String method, String pathAndQuery, byte[] body, String contentType) {
        return exchange(usc, uscBase, method, pathAndQuery, "", body, contentType);
    }

    private ProxyResponse exchange(
            RestClient client,
            String base,
            String method,
            String path,
            String query,
            Object body,
            String contentType) {
        if (client == null || base == null || base.isBlank()) {
            return unavailable("not_configured");
        }
        String uri = path + (query == null || query.isBlank() ? "" : (path.contains("?") ? "&" : "?") + query);
        try {
            RestClient.RequestBodySpec spec = client.method(HttpMethod.valueOf(method.toUpperCase()))
                    .uri(uri)
                    .header(TENANT_HEADER, tenantId);
            if (contentType != null && !contentType.isBlank()) {
                spec.header(HttpHeaders.CONTENT_TYPE, contentType);
            }
            if (body != null && !"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                spec.body(body);
            }
            return spec.exchange((req, res) -> {
                byte[] bytes = res.getBody().readAllBytes();
                return ProxyResponse.from(res.getStatusCode().value(), bytes, tenantId, mapper);
            });
        } catch (RestClientException e) {
            return unavailable(e.getMessage());
        }
    }

    private ProxyResponse unavailable(String message) {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("ok", false);
        stub.put("error", "continuuuum_unavailable");
        stub.put("detail", message);
        stub.put("tenant", fallbackTenant);
        return new ProxyResponse(false, 503, fallbackTenant, stub, null, "continuuuum_unavailable");
    }

    private static String inferMethod(String structural) {
        String s = structural == null ? "" : structural;
        if (s.contains("upload")) {
            return "POST";
        }
        if (s.contains("estimate") || s.endsWith("/lighting") && s.contains("query")) {
            return "GET";
        }
        return "GET";
    }

    private static String queryFromPayload(Map<String, Object> payload) {
        if (payload == null) {
            return "";
        }
        StringBuilder q = new StringBuilder();
        for (String key : List.of("q", "document_type", "lat", "lon", "distance_mi", "limit")) {
            Object v = payload.get(key);
            if (v != null) {
                if (q.length() > 0) {
                    q.append('&');
                }
                q.append(key).append('=').append(v);
            }
        }
        return q.toString();
    }

    private static String stripSlash(String url) {
        if (url == null) {
            return "";
        }
        String t = url.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    public record ProxyResponse(
            boolean ok,
            int status,
            String tenant,
            Object json,
            String body,
            String error) {
        static ProxyResponse from(int status, byte[] bytes, String tenant, ObjectMapper mapper) {
            String text = bytes == null || bytes.length == 0
                    ? null
                    : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            Object json = text;
            if (text != null && (text.startsWith("{") || text.startsWith("["))) {
                try {
                    json = mapper.readValue(text, Object.class);
                } catch (Exception ignored) {
                    json = text;
                }
            }
            return new ProxyResponse(status >= 200 && status < 300, status, tenant, json, text, null);
        }
    }
}
