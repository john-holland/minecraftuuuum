package com.minecraftuuuum.lemma;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeCatalogTest {
    @Test
    void oakPlanksCraftsFromLogAndUsedInStickAndTable() {
        List<RecipeCatalog.Recipe> crafts = RecipeCatalog.crafts("minecraft:oak_planks");
        assertTrue(crafts.stream().anyMatch(r -> r.ingredients().contains("minecraft:oak_log")));

        List<RecipeCatalog.Recipe> uses = RecipeCatalog.uses("oak_planks");
        assertTrue(uses.stream().anyMatch(r -> "minecraft:stick".equals(r.result())));
        assertTrue(uses.stream().anyMatch(r -> "minecraft:crafting_table".equals(r.result())));
        assertFalse(RecipeCatalog.crafts("minecraft:oak_log").stream()
                .anyMatch(r -> r.ingredients().contains("minecraft:oak_planks")));
    }

    @Test
    void searchFiltersWoodFamily() {
        assertTrue(BlockTypeCatalog.filter("oak", "wood").stream()
                .anyMatch(b -> b.registryId().equals("minecraft:oak_planks")));
        assertTrue(BlockTypeCatalog.filter("", "ore").stream()
                .noneMatch(b -> b.family().equals("wood")));
    }
}
