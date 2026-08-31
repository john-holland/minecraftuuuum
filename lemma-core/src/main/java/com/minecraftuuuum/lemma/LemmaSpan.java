package com.minecraftuuuum.lemma;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LemmaSpan {
    public enum Kind { P, M }

    private final Kind kind;
    private final String term;
    private final Map<String, String> properties;
    private final int start;
    private final int end;

    public LemmaSpan(Kind kind, String term, Map<String, String> properties, int start, int end) {
        this.kind = kind;
        this.term = term;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
        this.start = start;
        this.end = end;
    }

    public Kind kind() {
        return kind;
    }

    public String term() {
        return term;
    }

    public Map<String, String> properties() {
        return properties;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append('{').append(kind == Kind.P ? 'P' : 'M').append(':').append(term);
        properties.forEach((k, v) -> sb.append('|').append(k).append('=').append(v));
        sb.append('}');
        return sb.toString();
    }
}
