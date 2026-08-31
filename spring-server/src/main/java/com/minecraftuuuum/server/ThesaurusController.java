package com.minecraftuuuum.server;

import com.minecraftuuuum.lemma.LemmaEntry;
import com.minecraftuuuum.lemma.PromptParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ThesaurusController {
    private final ThesaurusService thesaurus;

    public ThesaurusController(ThesaurusService thesaurus) {
        this.thesaurus = thesaurus;
    }

    @GetMapping("/thesaurus/entries")
    public List<LemmaEntry> entries() {
        return thesaurus.merge();
    }

    @PostMapping("/thesaurus/entries")
    public LemmaEntry create(@RequestBody LemmaEntry entry) {
        return thesaurus.put(entry);
    }

    @PostMapping("/thesaurus/expand")
    public Map<String, Object> expand(@RequestBody Map<String, String> body) {
        String script = body.getOrDefault("script", "");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("script", script);
        out.put("expanded", thesaurus.expand(script));
        out.put("spans", PromptParser.parse(script).stream().map(s -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("kind", s.kind().name());
            row.put("term", s.term());
            row.put("properties", s.properties());
            row.put("start", s.start());
            row.put("end", s.end());
            return row;
        }).toList());
        return out;
    }

    @GetMapping("/wrapper-packs")
    public List<Map<String, Object>> wrappers() {
        return thesaurus.wrappers();
    }

    @PutMapping("/wrapper-packs")
    public LemmaEntry wrap(@RequestBody LemmaEntry entry) {
        thesaurus.registerWrapper(entry);
        return entry;
    }

    @GetMapping("/thesaurus/lookup")
    public LemmaEntry lookup(@RequestParam String phrase) {
        return thesaurus.resolve(phrase);
    }
}
