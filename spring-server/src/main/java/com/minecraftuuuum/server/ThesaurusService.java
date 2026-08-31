package com.minecraftuuuum.server;

import com.minecraftuuuum.lemma.ActorCatalog;
import com.minecraftuuuum.lemma.BuiltinVocabularyRegistry;
import com.minecraftuuuum.lemma.LemmaEntry;
import com.minecraftuuuum.lemma.PhraseLookup;
import com.minecraftuuuum.lemma.PromptParser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ThesaurusService {
    private final Map<String, LemmaEntry> custom = new ConcurrentHashMap<>();
    private final PhraseLookup lookup = BuiltinVocabularyRegistry.lookup();

    public ThesaurusService() {
        for (LemmaEntry e : ActorCatalog.asLemmas()) {
            custom.put(e.id, e);
            lookup.index(e);
        }
    }

    public List<LemmaEntry> merge() {
        List<LemmaEntry> out = new ArrayList<>(BuiltinVocabularyRegistry.all());
        out.addAll(custom.values());
        return out;
    }

    public LemmaEntry put(LemmaEntry entry) {
        if (entry.id == null || entry.id.isBlank()) {
            entry.id = "urn:minecraft:minecraftuuuum:custom:" + UUID.randomUUID();
        }
        custom.put(entry.id, entry);
        lookup.index(entry);
        return entry;
    }

    public String expand(String script) {
        return PromptParser.expand(script, (term, overlay) -> {
            LemmaEntry e = lookup.resolvePhrase(term);
            if (e == null) {
                return null;
            }
            Map<String, String> props = new LinkedHashMap<>(e.properties);
            props.putAll(overlay);
            String rid = props.getOrDefault("registry-id", term);
            return e.term + "[" + rid + "]";
        });
    }

    public List<Map<String, Object>> wrappers() {
        List<Map<String, Object>> packs = new ArrayList<>();
        packs.add(vanillaPack());
        packs.add(exampleThirdPartyPack());
        return packs;
    }

    public void registerWrapper(LemmaEntry entry) {
        put(entry);
    }

    private Map<String, Object> vanillaPack() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", "minecraftuuuum:vanilla");
        p.put("label", "Vanilla (builtin store pack)");
        p.put("entries", ActorCatalog.asLemmas().size());
        return p;
    }

    private Map<String, Object> exampleThirdPartyPack() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", "minecraftuuuum:example-create-wrap");
        p.put("label", "Example Create wrap (does not vendor Create)");
        p.put("note", "Binds lemmas to create:* registry ids if that mod is present");
        LemmaEntry piston = new LemmaEntry();
        piston.id = "urn:minecraft:minecraftuuuum:mod:create:v1:/en/noun/mechanical-piston";
        piston.term = "mechanical-piston";
        piston.posTag = "noun";
        piston.properties.put("kind", "block");
        piston.properties.put("registry-id", "create:mechanical_piston");
        put(piston);
        p.put("sampleTerm", piston.term);
        return p;
    }

    public LemmaEntry resolve(String phrase) {
        return lookup.resolvePhrase(phrase.toLowerCase(Locale.ROOT));
    }
}
