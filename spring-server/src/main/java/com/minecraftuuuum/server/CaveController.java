package com.minecraftuuuum.server;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CaveController {
    private final CaveRouteService routes;

    public CaveController(CaveRouteService routes) {
        this.routes = routes;
    }

    @PostMapping("/cave/route")
    public ResponseEntity<Map<String, Object>> route(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> out = routes.handle(body);
        boolean ok = !Boolean.FALSE.equals(out.get("ok"));
        int status = ok ? 200 : 400;
        if ("upstream_unavailable".equals(out.get("error")) || "continuuuum_unavailable".equals(out.get("error"))) {
            status = 502;
        }
        return ResponseEntity.status(status).body(out);
    }
}
