package com.minecraftuuuum.lemma;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Longest-first hyphen/space match so "open chest" does not bind generic "open".
 */
public final class PhraseLookup {
    private final Map<String, LemmaEntry> byCanonical = new LinkedHashMap<>();

    public void index(LemmaEntry entry) {
        if (entry == null || entry.term == null) {
            return;
        }
        for (String key : aliases(entry.term)) {
            byCanonical.putIfAbsent(key, entry);
        }
    }

    public LemmaEntry resolvePhrase(String phrase) {
        List<String> tokens = tokenize(phrase);
        if (tokens.isEmpty()) {
            return null;
        }
        for (int len = tokens.size(); len >= 1; len--) {
            String hyphen = String.join("-", tokens.subList(0, len));
            String space = String.join(" ", tokens.subList(0, len));
            LemmaEntry hit = byCanonical.get(hyphen);
            if (hit == null) {
                hit = byCanonical.get(space);
            }
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    public static List<String> tokenize(String phrase) {
        List<String> out = new ArrayList<>();
        if (phrase == null) {
            return out;
        }
        for (String part : phrase.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!part.isEmpty()) {
                out.add(part);
            }
        }
        return out;
    }

    public static List<String> aliases(String term) {
        String t = term.trim().toLowerCase(Locale.ROOT);
        List<String> keys = new ArrayList<>();
        keys.add(t);
        keys.add(t.replace('_', '-'));
        keys.add(t.replace('-', ' '));
        keys.add(t.replace('_', ' '));
        keys.add(t.replace(' ', '-'));
        return keys;
    }
}
