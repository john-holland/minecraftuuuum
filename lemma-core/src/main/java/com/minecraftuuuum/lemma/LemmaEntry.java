package com.minecraftuuuum.lemma;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LemmaEntry {
    public String id;
    public String term;
    public String posTag;
    public String languageCode = "en";
    public String definition = "";
    public String builtInCategory = "";
    public boolean isBuiltIn;
    public List<String> tags = new ArrayList<>();
    public Map<String, String> properties = new LinkedHashMap<>();

    public LemmaEntry() {}

    public LemmaEntry(
            String id,
            String term,
            String posTag,
            String category,
            List<String> tags,
            Map<String, String> properties) {
        this.id = id;
        this.term = term;
        this.posTag = posTag;
        this.builtInCategory = category;
        this.isBuiltIn = BuiltInUrn.isBuiltin(id);
        if (tags != null) {
            this.tags.addAll(tags);
        }
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }
}
