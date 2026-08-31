package com.minecraftuuuum.lemma;

import java.util.Locale;

public final class BuiltInUrn {
    public static final String PREFIX = "urn:minecraft:minecraftuuuum:builtin:v1:";

    private BuiltInUrn() {}

    public static String mint(String language, String segment, String term) {
        return PREFIX + "/" + language + "/" + segment + "/" + slug(term);
    }

    public static String slug(String term) {
        return term.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    public static boolean isBuiltin(String id) {
        return id != null && id.startsWith(PREFIX);
    }
}
