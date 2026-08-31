package com.minecraftuuuum.server;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lemma-implementation")
public class LemmaImplementationController {
    private final LemmaImplementationService impl;

    public LemmaImplementationController(LemmaImplementationService impl) {
        this.impl = impl;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return impl.summary();
    }

    @GetMapping("/features")
    public List<Map<String, Object>> features() {
        return impl.features();
    }

    @GetMapping("/entries")
    public List<Map<String, Object>> entries(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "kind", required = false) String kind,
            @RequestParam(value = "implemented", required = false) Boolean implemented) {
        return impl.entries(q, kind, implemented);
    }

    @GetMapping("/entries/{id}")
    public Map<String, Object> get(@PathVariable long id) {
        Map<String, Object> row = impl.get(id);
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "entry");
        }
        return row;
    }

    @PatchMapping("/entries/{id}")
    public Map<String, Object> patch(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> row = impl.patch(id, body);
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "entry");
        }
        return row;
    }

    @PostMapping("/sync")
    public Map<String, Object> sync() {
        int n = impl.sync();
        Map<String, Object> out = new LinkedHashMap<>(impl.summary());
        out.put("synced", n);
        return out;
    }
}
