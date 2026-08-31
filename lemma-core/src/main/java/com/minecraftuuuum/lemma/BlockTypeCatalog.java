package com.minecraftuuuum.lemma;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Vanilla block (and block-form item) ids with family tags for UCC search/filter. */
public final class BlockTypeCatalog {
    public record BlockType(String registryId, String term, String family, List<String> tags) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("registryId", registryId);
            m.put("term", term);
            m.put("family", family);
            m.put("tags", tags);
            m.put("lemmaPlace", "{P:place|registry-id=" + registryId + "}");
            m.put("lemmaRecipe", "{P:recipe|output=" + registryId + "}");
            return m;
        }
    }

    private static final List<BlockType> ALL = seed();

    public static List<BlockType> all() {
        return ALL;
    }

    public static BlockType byId(String registryId) {
        String id = normalize(registryId);
        for (BlockType b : ALL) {
            if (b.registryId.equals(id)) {
                return b;
            }
        }
        return null;
    }

    public static List<BlockType> filter(String q, String family) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String fam = family == null || family.isBlank() || "all".equalsIgnoreCase(family)
                ? ""
                : family.trim().toLowerCase(Locale.ROOT);
        List<BlockType> out = new ArrayList<>();
        for (BlockType b : ALL) {
            if (!fam.isEmpty() && !b.family.equals(fam) && !b.tags.contains(fam)) {
                continue;
            }
            if (query.isEmpty()
                    || b.registryId.contains(query)
                    || b.term.contains(query)
                    || b.family.contains(query)) {
                out.add(b);
            }
        }
        return out;
    }

    public static List<String> families() {
        return List.of("wood", "stone", "ore", "redstone", "decorative", "utility", "glass", "wool");
    }

    static String normalize(String registryId) {
        if (registryId == null || registryId.isBlank()) {
            return "";
        }
        return registryId.contains(":") ? registryId : "minecraft:" + registryId;
    }

    private static List<BlockType> seed() {
        List<BlockType> list = new ArrayList<>();
        wood(list, "oak");
        wood(list, "spruce");
        wood(list, "birch");
        wood(list, "jungle");
        wood(list, "acacia");
        wood(list, "dark_oak");
        wood(list, "mangrove");
        wood(list, "cherry");
        add(list, "bamboo_block", "wood");
        add(list, "bamboo_planks", "wood");
        add(list, "crafting_table", "utility", "wood");
        add(list, "chest", "utility", "wood");
        add(list, "barrel", "utility", "wood");
        add(list, "stick", "wood");

        add(list, "cobblestone", "stone");
        add(list, "stone", "stone");
        add(list, "smooth_stone", "stone");
        add(list, "stone_bricks", "stone");
        add(list, "mossy_cobblestone", "stone");
        add(list, "andesite", "stone");
        add(list, "diorite", "stone");
        add(list, "granite", "stone");
        add(list, "deepslate", "stone");
        add(list, "cobbled_deepslate", "stone");
        add(list, "furnace", "utility", "stone");
        add(list, "blast_furnace", "utility", "stone");
        add(list, "smoker", "utility", "stone");
        add(list, "stonecutter", "utility", "stone");

        add(list, "coal_ore", "ore");
        add(list, "iron_ore", "ore");
        add(list, "copper_ore", "ore");
        add(list, "gold_ore", "ore");
        add(list, "redstone_ore", "ore");
        add(list, "lapis_ore", "ore");
        add(list, "diamond_ore", "ore");
        add(list, "coal_block", "ore");
        add(list, "iron_block", "ore");
        add(list, "gold_block", "ore");
        add(list, "copper_block", "ore");
        add(list, "diamond_block", "ore");
        add(list, "iron_ingot", "ore");
        add(list, "gold_ingot", "ore");
        add(list, "copper_ingot", "ore");
        add(list, "diamond", "ore");
        add(list, "coal", "ore");
        add(list, "raw_iron", "ore");
        add(list, "raw_gold", "ore");
        add(list, "raw_copper", "ore");

        add(list, "redstone", "redstone");
        add(list, "redstone_block", "redstone");
        add(list, "redstone_torch", "redstone");
        add(list, "repeater", "redstone");
        add(list, "comparator", "redstone");
        add(list, "piston", "redstone");
        add(list, "sticky_piston", "redstone");
        add(list, "observer", "redstone");
        add(list, "dispenser", "redstone");
        add(list, "dropper", "redstone");
        add(list, "hopper", "redstone");
        add(list, "lever", "redstone");
        add(list, "stone_button", "redstone");
        add(list, "oak_button", "redstone", "wood");

        add(list, "glass", "glass");
        add(list, "glass_pane", "glass");
        add(list, "sand", "glass");
        add(list, "white_wool", "wool");
        add(list, "white_carpet", "wool");
        add(list, "white_bed", "wool");
        add(list, "string", "wool");

        add(list, "torch", "decorative");
        add(list, "lantern", "decorative");
        add(list, "bookshelf", "decorative", "wood");
        add(list, "ladder", "utility", "wood");
        add(list, "oak_door", "utility", "wood");
        add(list, "oak_trapdoor", "utility", "wood");
        add(list, "oak_fence", "utility", "wood");
        add(list, "oak_slab", "wood");
        add(list, "oak_stairs", "wood");
        add(list, "anvil", "utility", "ore");
        add(list, "iron_nugget", "ore");
        add(list, "gold_nugget", "ore");
        add(list, "slime_ball", "redstone");
        add(list, "quartz", "stone");
        add(list, "nether_quartz_ore", "ore");
        return List.copyOf(list);
    }

    private static void wood(List<BlockType> list, String species) {
        add(list, species + "_log", "wood");
        add(list, species + "_wood", "wood");
        add(list, species + "_planks", "wood");
    }

    private static void add(List<BlockType> list, String path, String family, String... extraTags) {
        List<String> tags = new ArrayList<>();
        tags.add(family);
        tags.addAll(List.of(extraTags));
        list.add(new BlockType("minecraft:" + path, path.replace('_', '-'), family, List.copyOf(tags)));
    }
}
