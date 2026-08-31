package com.minecraftuuuum.lemma;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Source of truth for builtin lemmas (port of Continuuuum VocabularyBuiltInRegistry). */
public final class BuiltinVocabularyRegistry {
    private static final List<LemmaEntry> ALL = buildAll();

    public static List<LemmaEntry> all() {
        return ALL;
    }

    public static PhraseLookup lookup() {
        PhraseLookup l = new PhraseLookup();
        for (LemmaEntry e : ALL) {
            l.index(e);
        }
        return l;
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "builtin_vocabulary.json");
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("version", 1);
        doc.put("items", ALL);
        Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(doc), StandardCharsets.UTF_8);
        System.out.println("wrote " + ALL.size() + " builtins to " + out);
    }

    private static List<LemmaEntry> buildAll() {
        List<LemmaEntry> list = new ArrayList<>(256);
        add(list, "det", "the", "determiner", "Article");
        add(list, "det", "a", "determiner", "Article");
        add(list, "det", "an", "determiner", "Article");
        add(list, "det", "this", "determiner", "Determiner", "nsm", "prime");
        add(list, "det", "that", "determiner", "Determiner");
        for (String w : List.of("in", "on", "at", "to", "from", "with", "by", "near", "between", "through",
                "across", "around", "inside", "outside", "along", "above", "below", "left-of", "right-of")) {
            add(list, "prep", w, "preposition", "Preposition", "spatial");
        }
        for (String w : List.of("if", "then", "else", "but", "because", "when", "while", "and", "or", "not")) {
            add(list, "conj", w, "conjunction", "DiscourseCausality", "causality");
        }
        for (String w : List.of("go", "move", "open", "close", "take", "use", "place", "give", "say",
                "mine", "break", "craft", "smelt", "enchant", "spawn", "loot", "ambulate")) {
            add(list, "verb", w, "verb", "Action");
        }
        add(list, "noun", "player", "noun", "Subject", "world");
        add(list, "noun", "chunk", "noun", "Subject", "world");
        add(list, "noun", "biome", "noun", "Subject", "world");
        add(list, "noun", "dimension", "noun", "Subject", "world");
        add(list, "noun", "actor", "noun", "Subject", "world");
        add(list, "noun", "voxel-art", "noun", "Subject", "voxel");
        add(list, "noun", "pixellight-brush", "noun", "Subject", "voxel");
        add(list, "noun", "iso-face", "noun", "Subject", "voxel");
        add(list, "literal", "blockpos", "type_name", "LiteralType", "literal");
        add(list, "literal", "resource-location", "type_name", "LiteralType", "literal");
        add(list, "literal", "nbt", "type_name", "LiteralType", "literal");
        add(list, "literal", "component", "type_name", "LiteralType", "literal");
        add(list, "literal", "voxel-address", "type_name", "LiteralType", "literal");
        add(list, "literal", "pixellight-cell", "type_name", "LiteralType", "literal");
        for (String prime : List.of("I", "you", "someone", "something", "people", "body", "kind", "part",
                "one", "two", "some", "all", "good", "bad", "big", "small", "know", "think", "want",
                "feel", "see", "hear", "do", "happen", "have", "live", "die", "now", "before", "after",
                "here", "there", "maybe", "can", "very", "more", "like")) {
            add(list, "nsm", prime, posGuess(prime), "SemanticPrime", "nsm", "prime");
        }
        props(list, "place", Map.of("kind", "action", "registry-id", "minecraft:oak_planks"));
        props(list, "spawn", Map.of("kind", "action", "registry-id", "minecraft:creeper"));
        props(list, "voxel-art", Map.of("kind", "action"));
        props(list, "ambulate", Map.of("kind", "action"));
        props(list, "actor", Map.of("kind", "entity", "registry-id", "minecraft:player"));
        return list;
    }

    private static void add(List<LemmaEntry> list, String segment, String term, String pos, String cat, String... tags) {
        String id = BuiltInUrn.mint("en", segment, term);
        list.add(new LemmaEntry(id, term, pos, cat, List.of(tags), Map.of()));
    }

    private static void props(List<LemmaEntry> list, String term, Map<String, String> p) {
        for (LemmaEntry e : list) {
            if (term.equals(e.term)) {
                e.properties.putAll(p);
            }
        }
    }

    private static String posGuess(String term) {
        if (List.of("I", "you").contains(term)) {
            return "pronoun";
        }
        if (List.of("know", "think", "want", "feel", "see", "hear", "do", "happen", "have", "live", "die", "can")
                .contains(term)) {
            return "verb";
        }
        if (List.of("now", "before", "after", "maybe", "very", "more").contains(term)) {
            return "adverb";
        }
        if (List.of("good", "bad", "big", "small").contains(term)) {
            return "adjective";
        }
        return "noun";
    }
}
