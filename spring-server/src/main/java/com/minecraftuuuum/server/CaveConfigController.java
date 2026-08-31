package com.minecraftuuuum.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@RestController
public class CaveConfigController {
    private final CaveRouteService routes;
    private final String caveBase;

    public CaveConfigController(CaveRouteService routes, @Value("${minecraftuuuum.cave-base:}") String caveBase) {
        this.routes = routes;
        this.caveBase = caveBase == null ? "" : caveBase.replaceAll("/+$", "");
    }

    @GetMapping("/api/config/overview")
    public Map<String, Object> overview() {
        return routes.configOverview();
    }

    @GetMapping("/api/cave/config-overview")
    public Map<String, Object> caveOverview() {
        return routes.configOverview();
    }

    @GetMapping("/api/routes")
    public Map<String, Object> apiRoutes() {
        return routes.routesOverview();
    }

    @GetMapping("/api/cave/hierarchy")
    public Map<String, Object> hierarchy() {
        return routes.routesOverview();
    }

    /** Optional LVM Node proxy. Empty stub when {@code CAVE_BASE_URL} is unset or down. */
    @GetMapping(value = "/api/tome/container", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> tomeContainer(@RequestParam(defaultValue = "header") String slot) {
        if (caveBase.isBlank()) {
            return ResponseEntity.ok("");
        }
        try {
            String html = RestClient.builder()
                    .baseUrl(caveBase)
                    .build()
                    .get()
                    .uri("/api/tome/container?slot={slot}", slot)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok(html == null ? "" : html);
        } catch (RestClientException e) {
            return ResponseEntity.ok("");
        }
    }
}
