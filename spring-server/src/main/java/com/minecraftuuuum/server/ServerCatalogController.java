package com.minecraftuuuum.server;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ServerCatalogController {
    private final ServerCatalogStore store;
    private final ServerProcessService processes;

    public ServerCatalogController(ServerCatalogStore store, ServerProcessService processes) {
        this.store = store;
        this.processes = processes;
    }

    @GetMapping("/api/server-configs")
    public List<ServerCatalogStore.ConfigRow> listConfigs() {
        return store.listConfigs();
    }

    @PostMapping("/api/server-configs")
    public ServerCatalogStore.ConfigRow create(@RequestBody Map<String, Object> body) {
        String name = str(body, "name");
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name");
        }
        try {
            return store.createConfig(name, str(body, "workingDir"), str(body, "startCommand"),
                    intVal(body, "port"), str(body, "script"));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @GetMapping("/api/server-configs/{id}")
    public Map<String, Object> getConfig(@PathVariable String id) {
        ServerCatalogStore.ConfigRow cfg = store.getConfig(id);
        if (cfg == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "config");
        }
        List<ServerCatalogStore.VersionRow> versions = store.listVersions(id);
        ServerCatalogStore.VersionRow head = store.headVersion(id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", cfg.id());
        out.put("name", cfg.name());
        out.put("workingDir", cfg.workingDir());
        out.put("startCommand", cfg.startCommand());
        out.put("port", cfg.port());
        out.put("headVersion", cfg.headVersion());
        out.put("createdAt", cfg.createdAt());
        out.put("updatedAt", cfg.updatedAt());
        out.put("versions", versions);
        out.put("headScript", head == null ? "" : head.scriptText());
        return out;
    }

    @PutMapping("/api/server-configs/{id}")
    public ServerCatalogStore.ConfigRow update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ServerCatalogStore.ConfigRow cur = store.getConfig(id);
        if (cur == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "config");
        }
        String name = body.containsKey("name") ? str(body, "name") : cur.name();
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name");
        }
        try {
            ServerCatalogStore.ConfigRow updated = store.updateConfig(
                    id,
                    name,
                    body.containsKey("workingDir") ? str(body, "workingDir") : cur.workingDir(),
                    body.containsKey("startCommand") ? str(body, "startCommand") : cur.startCommand(),
                    body.containsKey("port") ? intVal(body, "port") : cur.port());
            if (updated == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "config");
            }
            return updated;
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PutMapping("/api/server-configs/{id}/script")
    public ServerCatalogStore.VersionRow saveScript(@PathVariable String id, @RequestBody Map<String, String> body) {
        String script = body.get("script");
        if (script == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "script");
        }
        ServerCatalogStore.VersionRow ver = store.saveScript(id, script);
        if (ver == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "config");
        }
        return ver;
    }

    @GetMapping("/api/servers")
    public List<ServerCatalogStore.ServerRow> listServers() {
        processes.refreshLiveness();
        return store.listServers();
    }

    @PostMapping("/api/servers")
    public ServerCatalogStore.ServerRow register(
            @RequestBody Map<String, Object> body,
            @RequestParam(value = "launch", defaultValue = "false") boolean launch) {
        String configId = str(body, "configId");
        if (configId == null || configId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "configId");
        }
        try {
            return processes.register(configId, longVal(body, "scriptVersionId"), intVal(body, "scriptVersion"),
                    str(body, "name"), launch);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/api/servers/{id}/start")
    public ServerCatalogStore.ServerRow start(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body == null ? Map.of() : body;
        try {
            return processes.start(id, longVal(b, "scriptVersionId"), intVal(b, "scriptVersion"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/api/servers/{id}/restart")
    public ServerCatalogStore.ServerRow restart(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body == null ? Map.of() : body;
        try {
            return processes.restart(id, longVal(b, "scriptVersionId"), intVal(b, "scriptVersion"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/api/servers/{id}/stop")
    public ServerCatalogStore.ServerRow stop(@PathVariable String id) {
        try {
            return processes.stop(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Integer intVal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || "".equals(v)) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v));
    }

    private static Long longVal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || "".equals(v)) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }
}
