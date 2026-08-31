package com.minecraftuuuum.server;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CaveRouteService {
    public static final String SERVICE = "minecraftuuuum";
    public static final List<String> SPELUNK_PAGES = List.of(
            "/library",
            "/lemma-library",
            "/lemma-implementation",
            "/video-animation",
            "/video-generation",
            "/pixellight",
            "/block-recipes",
            "/cave");

    private final ContinuuuumTenantClient tenantClient;
    private final LvmEventStore lvmEvents;

    public CaveRouteService(ContinuuuumTenantClient tenantClient, LvmEventStore lvmEvents) {
        this.tenantClient = tenantClient;
        this.lvmEvents = lvmEvents;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> handle(Map<String, Object> body) {
        Map<String, Object> req = body == null ? Map.of() : body;
        String traceId = string(req.get("trace_id"));
        if (traceId.isEmpty()) {
            traceId = string(req.get("traceId"));
        }
        if (traceId.isEmpty()) {
            traceId = "minecraftuuuum_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        String tenant = string(req.get("tenant"));
        if (tenant.isEmpty()) {
            tenant = tenantClient.tenantId();
        }
        Object payloadObj = req.get("payload");
        Map<String, Object> payload = payloadObj instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();

        String routeRaw = string(req.get("route"));
        String structural;
        String service = SERVICE;
        if (!routeRaw.isEmpty()) {
            int colon = routeRaw.indexOf(':');
            if (colon > 0) {
                service = routeRaw.substring(0, colon);
                structural = routeRaw.substring(colon + 1);
            } else {
                structural = routeRaw;
            }
        } else if (req.get("message") != null) {
            structural = messageToStructural(string(req.get("message")));
            if (structural == null) {
                return error("unknown_message", string(req.get("message")), traceId);
            }
        } else {
            return error("missing_route", null, traceId);
        }

        Map<String, Object> out = dispatch(structural, payload, tenant, traceId);
        if (isMutating(structural) && Boolean.TRUE.equals(out.get("ok"))) {
            List<String> appended = lvmEvents.afterCaveRouteMutation(structural, tenant, service, traceId);
            out = new LinkedHashMap<>(out);
            out.put("lvm_appended", appended);
        }
        out.putIfAbsent("ok", true);
        out.put("trace_id", traceId);
        out.put("route", service + ":" + structural);
        out.put("schema_version", "2.0");
        return out;
    }

    private Map<String, Object> dispatch(
            String structural, Map<String, Object> payload, String tenant, String traceId) {
        return switch (normalize(structural)) {
            case "ping", "health", "health/ping" -> ping(tenant);
            case "pages", "ucc/pages", "spelunk" -> pages();
            default -> {
                if (normalize(structural).startsWith("library")) {
                    yield tenantClient.forwardLibrary(stripService(structural), payload);
                }
                yield error("unknown_route", SERVICE + ":" + structural, traceId);
            }
        };
    }

    private Map<String, Object> ping(String tenant) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("service", SERVICE);
        out.put("tenant", tenant);
        out.put("continuuuumReachable", tenantClient.continuuuumReachable());
        return out;
    }

    private Map<String, Object> pages() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("pages", SPELUNK_PAGES);
        return out;
    }

    public Map<String, Object> configOverview() {
        Map<String, Object> messages = new LinkedHashMap<>();
        messages.put("PING", "ping");
        messages.put("HEALTH", "health");
        messages.put("PAGES", "pages");
        messages.put("LIBRARY_SEARCH", "library/search");
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schema_version", "2.0");
        manifest.put("service", SERVICE);
        manifest.put("messages", messages);
        manifest.put("structural", List.of("ping", "health", "pages", "library/search", "library/upload"));
        manifest.put("handlers", List.of("ping", "health", "pages", "library"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("cave", Map.of("name", SERVICE, "routes", spelunkRoutes()));
        out.put("manifest", manifest);
        out.put("tomes", List.of());
        out.put("caveRobit", Map.of());
        out.put("logViewMachine", Map.of("version", "2.1.1", "package", "log-view-machine"));
        out.put("pages", SPELUNK_PAGES);
        out.put("tenant", tenantClient.tenantId());
        out.put("robotCopy", Map.of("transport", "POST /cave/route"));
        return out;
    }

    public Map<String, Object> routesOverview() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("cave", Map.of("name", SERVICE, "routes", spelunkRoutes()));
        out.put("tomes", List.of());
        out.put("caveRobit", Map.of());
        out.put("manifest", configOverview().get("manifest"));
        out.put("robotCopy", Map.of("transport", "POST /cave/route", "legacyShim", ""));
        out.put("logViewMachine", Map.of("version", "2.1.1", "package", "log-view-machine", "routes", List.of()));
        out.put("pages", SPELUNK_PAGES);
        return out;
    }

    private List<Map<String, String>> spelunkRoutes() {
        return SPELUNK_PAGES.stream()
                .map(p -> Map.of("path", p, "method", "GET", "kind", "spelunk"))
                .toList();
    }

    private static boolean isMutating(String structural) {
        String s = normalize(structural);
        if (!s.startsWith("library")) {
            return false;
        }
        return s.contains("upload") || (s.contains("documents") && !s.contains("search"));
    }

    private static String messageToStructural(String message) {
        return switch (message.toUpperCase()) {
            case "PING" -> "ping";
            case "HEALTH" -> "health";
            case "PAGES" -> "pages";
            case "LIBRARY_SEARCH" -> "library/search";
            default -> null;
        };
    }

    private static Map<String, Object> error(String error, String extra, String traceId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", false);
        out.put("error", error);
        if (extra != null) {
            out.put("route", extra);
        }
        out.put("trace_id", traceId);
        out.put("schema_version", "2.0");
        return out;
    }

    private static String normalize(String structural) {
        return stripService(structural).replace('\\', '/').replaceAll("^/+", "");
    }

    private static String stripService(String structural) {
        if (structural == null) {
            return "";
        }
        int colon = structural.indexOf(':');
        return colon > 0 ? structural.substring(colon + 1) : structural;
    }

    private static String string(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }
}
