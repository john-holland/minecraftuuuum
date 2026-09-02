package com.minecraftuuuum.server;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/legal-unity")
public class LegalUnityController {
    private final LegalUnityService legal;

    public LegalUnityController(LegalUnityService legal) {
        this.legal = legal;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return legal.status();
    }

    @PostMapping("/iron-man")
    public Map<String, Object> ironMan(@RequestBody Map<String, Object> body) {
        return legal.setIronMan(bool(body.get("ironMan")));
    }

    @PostMapping("/display-mode")
    public Map<String, Object> displayMode(@RequestBody Map<String, Object> body) {
        try {
            return legal.setDisplayMode(body.get("displayMode") == null ? "web" : String.valueOf(body.get("displayMode")));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/requirements")
    public Map<String, Object> requirements(@RequestBody(required = false) Map<String, Object> body) {
        return legal.requirements(body == null ? Map.of() : body);
    }

    @PostMapping("/install")
    public Map<String, Object> install(@RequestBody Map<String, Object> body) {
        try {
            return legal.install(body == null ? Map.of() : body);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (GitRunner.GitException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/back-out")
    public Map<String, Object> backOut() {
        try {
            return legal.backOut();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/commit-log")
    public Map<String, Object> commitLog() {
        try {
            return legal.commitLog();
        } catch (GitRunner.GitException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }

    private static boolean bool(Object v) {
        if (v instanceof Boolean b) {
            return b;
        }
        return v != null && Boolean.parseBoolean(String.valueOf(v));
    }
}
