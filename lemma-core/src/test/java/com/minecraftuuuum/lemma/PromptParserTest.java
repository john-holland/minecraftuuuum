package com.minecraftuuuum.lemma;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PromptParserTest {
    @Test
    void parsesPAndM() {
        List<LemmaSpan> spans = PromptParser.parse(
                "{P:place|registry-id=minecraft:oak_planks} {M:weapon-slot|registry-id=modpack:steel_sword}");
        assertEquals(2, spans.size());
        assertEquals(LemmaSpan.Kind.P, spans.get(0).kind());
        assertEquals("place", spans.get(0).term());
        assertEquals("minecraft:oak_planks", spans.get(0).properties().get("registry-id"));
        assertEquals(LemmaSpan.Kind.M, spans.get(1).kind());
        assertEquals("weapon-slot", spans.get(1).term());
    }

    @Test
    void expandUsesLookup() {
        String out = PromptParser.expand("{P:say|text=Minecraftuuuum!}", (term, overlay) ->
                "SAY:" + overlay.getOrDefault("text", term));
        assertEquals("SAY:Minecraftuuuum!", out);
    }

    @Test
    void openChestDoesNotBindGenericOpen() {
        PhraseLookup lookup = BuiltinVocabularyRegistry.lookup();
        LemmaEntry chest = new LemmaEntry(
                BuiltInUrn.mint("en", "noun", "open-chest"),
                "open-chest",
                "verb",
                "Action",
                List.of(),
                Map.of());
        lookup.index(chest);
        assertSame(chest, lookup.resolvePhrase("open chest"));
        assertEquals("open", lookup.resolvePhrase("open").term);
    }
}
