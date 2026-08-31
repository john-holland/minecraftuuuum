package com.minecraftuuuum.lemma;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Continuuuum {P:} / {M:} grammar. */
public final class PromptParser {
    private static final Pattern SPAN = Pattern.compile(
            "\\{\\{?([PM]):([^}|]+)(?:\\|([^}]+))?\\}?\\}?|\\{([PM]):([^}|]+)(?:\\|([^}]+))?\\}",
            Pattern.CASE_INSENSITIVE);

    private PromptParser() {}

    public static List<LemmaSpan> parse(String text) {
        List<LemmaSpan> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        Matcher m = SPAN.matcher(text);
        while (m.find()) {
            String kindRaw = first(m.group(1), m.group(4));
            String term = first(m.group(2), m.group(5));
            String props = first(m.group(3), m.group(6));
            LemmaSpan.Kind kind = "M".equalsIgnoreCase(kindRaw) ? LemmaSpan.Kind.M : LemmaSpan.Kind.P;
            out.add(new LemmaSpan(kind, term.trim().toLowerCase(Locale.ROOT), parseProps(props), m.start(), m.end()));
        }
        return out;
    }

    public static String expand(String text, LemmaLookup lookup) {
        if (text == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        Matcher m = SPAN.matcher(text);
        while (m.find()) {
            String term = first(m.group(2), m.group(5));
            String props = first(m.group(3), m.group(6));
            Map<String, String> overlay = parseProps(props);
            String replacement = lookup.resolve(term.trim().toLowerCase(Locale.ROOT), overlay);
            if (replacement == null) {
                replacement = m.group();
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static Map<String, String> parseProps(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split("\\|")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            out.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
        }
        return out;
    }

    private static String first(String a, String b) {
        return a != null ? a : b;
    }

    public interface LemmaLookup {
        String resolve(String term, Map<String, String> overlay);
    }
}
