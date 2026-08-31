package com.minecraftuuuum.lemma;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** Vanilla-shaped recipes indexed by result and by ingredient. */
public final class RecipeCatalog {
    public record Recipe(
            String id,
            String type,
            String result,
            int count,
            List<String> ingredients) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("type", type);
            m.put("result", result);
            m.put("count", count);
            m.put("ingredients", ingredients);
            return m;
        }
    }

    private static final List<Recipe> ALL = new CopyOnWriteArrayList<>(seed());

    public static List<Recipe> all() {
        return List.copyOf(ALL);
    }

    public static void add(Recipe recipe) {
        ALL.add(recipe);
    }

    public static List<Recipe> crafts(String item) {
        String id = BlockTypeCatalog.normalize(item);
        List<Recipe> out = new ArrayList<>();
        for (Recipe r : ALL) {
            if (r.result.equals(id)) {
                out.add(r);
            }
        }
        return out;
    }

    public static List<Recipe> uses(String item) {
        String id = BlockTypeCatalog.normalize(item);
        List<Recipe> out = new ArrayList<>();
        for (Recipe r : ALL) {
            if (r.ingredients.contains(id)) {
                out.add(r);
            }
        }
        return out;
    }

    public static Map<String, Object> forItem(String item) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("item", BlockTypeCatalog.normalize(item));
        out.put("crafts", crafts(item).stream().map(Recipe::toMap).toList());
        out.put("uses", uses(item).stream().map(Recipe::toMap).toList());
        return out;
    }

    private static List<Recipe> seed() {
        List<Recipe> list = new ArrayList<>();
        String[] woods = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"};
        for (String w : woods) {
            shapeless(list, w + "_planks", mc(w + "_planks"), 4, mc(w + "_log"));
            shapeless(list, w + "_planks_from_wood", mc(w + "_planks"), 4, mc(w + "_wood"));
        }
        shapeless(list, "bamboo_planks", mc("bamboo_planks"), 2, mc("bamboo_block"));
        shaped(list, "stick", mc("stick"), 4, mc("oak_planks"), mc("oak_planks"));
        shaped(list, "crafting_table", mc("crafting_table"), 1,
                mc("oak_planks"), mc("oak_planks"), mc("oak_planks"), mc("oak_planks"));
        shaped(list, "chest", mc("chest"), 1,
                mc("oak_planks"), mc("oak_planks"), mc("oak_planks"),
                mc("oak_planks"), mc("oak_planks"), mc("oak_planks"),
                mc("oak_planks"), mc("oak_planks"));
        shaped(list, "oak_slab", mc("oak_slab"), 6, mc("oak_planks"), mc("oak_planks"), mc("oak_planks"));
        shaped(list, "oak_stairs", mc("oak_stairs"), 4,
                mc("oak_planks"), mc("oak_planks"), mc("oak_planks"),
                mc("oak_planks"), mc("oak_planks"), mc("oak_planks"));
        shaped(list, "oak_fence", mc("oak_fence"), 3, mc("oak_planks"), mc("stick"), mc("oak_planks"), mc("stick"));
        shaped(list, "oak_door", mc("oak_door"), 3,
                mc("oak_planks"), mc("oak_planks"), mc("oak_planks"),
                mc("oak_planks"), mc("oak_planks"), mc("oak_planks"));
        shaped(list, "oak_trapdoor", mc("oak_trapdoor"), 2,
                mc("oak_planks"), mc("oak_planks"), mc("oak_planks"),
                mc("oak_planks"), mc("oak_planks"), mc("oak_planks"));
        shaped(list, "oak_button", mc("oak_button"), 1, mc("oak_planks"));
        shaped(list, "ladder", mc("ladder"), 3,
                mc("stick"), mc("stick"), mc("stick"), mc("stick"), mc("stick"), mc("stick"), mc("stick"));
        shaped(list, "bookshelf", mc("bookshelf"), 1, mc("oak_planks"), mc("oak_planks"), mc("oak_planks"));
        shaped(list, "barrel", mc("barrel"), 1, mc("oak_planks"), mc("oak_slab"), mc("oak_planks"));

        smelting(list, "stone", mc("stone"), mc("cobblestone"));
        smelting(list, "smooth_stone", mc("smooth_stone"), mc("stone"));
        shaped(list, "stone_bricks", mc("stone_bricks"), 4,
                mc("stone"), mc("stone"), mc("stone"), mc("stone"));
        stonecutting(list, "stone_bricks_from_stone", mc("stone_bricks"), 1, mc("stone"));
        stonecutting(list, "stone_from_cobble_cut", mc("stone"), 1, mc("cobblestone"));
        shaped(list, "furnace", mc("furnace"), 1,
                mc("cobblestone"), mc("cobblestone"), mc("cobblestone"),
                mc("cobblestone"), mc("cobblestone"), mc("cobblestone"),
                mc("cobblestone"), mc("cobblestone"));
        shaped(list, "stonecutter", mc("stonecutter"), 1, mc("iron_ingot"), mc("stone"));
        shaped(list, "blast_furnace", mc("blast_furnace"), 1,
                mc("iron_ingot"), mc("iron_ingot"), mc("iron_ingot"),
                mc("iron_ingot"), mc("iron_ingot"), mc("furnace"), mc("smooth_stone"));
        shaped(list, "smoker", mc("smoker"), 1, mc("oak_log"), mc("furnace"));

        smelting(list, "iron_ingot_from_ore", mc("iron_ingot"), mc("iron_ore"));
        smelting(list, "iron_ingot_from_raw", mc("iron_ingot"), mc("raw_iron"));
        smelting(list, "gold_ingot_from_ore", mc("gold_ingot"), mc("gold_ore"));
        smelting(list, "gold_ingot_from_raw", mc("gold_ingot"), mc("raw_gold"));
        smelting(list, "copper_ingot_from_ore", mc("copper_ingot"), mc("copper_ore"));
        smelting(list, "copper_ingot_from_raw", mc("copper_ingot"), mc("raw_copper"));
        smelting(list, "coal_from_ore", mc("coal"), mc("coal_ore"));
        smelting(list, "diamond_from_ore", mc("diamond"), mc("diamond_ore"));
        shaped(list, "iron_block", mc("iron_block"), 1,
                mc("iron_ingot"), mc("iron_ingot"), mc("iron_ingot"),
                mc("iron_ingot"), mc("iron_ingot"), mc("iron_ingot"),
                mc("iron_ingot"), mc("iron_ingot"), mc("iron_ingot"));
        shapeless(list, "iron_ingot_from_block", mc("iron_ingot"), 9, mc("iron_block"));
        shaped(list, "gold_block", mc("gold_block"), 1,
                mc("gold_ingot"), mc("gold_ingot"), mc("gold_ingot"),
                mc("gold_ingot"), mc("gold_ingot"), mc("gold_ingot"),
                mc("gold_ingot"), mc("gold_ingot"), mc("gold_ingot"));
        shapeless(list, "gold_ingot_from_block", mc("gold_ingot"), 9, mc("gold_block"));
        shaped(list, "copper_block", mc("copper_block"), 1,
                mc("copper_ingot"), mc("copper_ingot"), mc("copper_ingot"),
                mc("copper_ingot"), mc("copper_ingot"), mc("copper_ingot"),
                mc("copper_ingot"), mc("copper_ingot"), mc("copper_ingot"));
        shapeless(list, "copper_ingot_from_block", mc("copper_ingot"), 9, mc("copper_block"));
        shaped(list, "diamond_block", mc("diamond_block"), 1,
                mc("diamond"), mc("diamond"), mc("diamond"),
                mc("diamond"), mc("diamond"), mc("diamond"),
                mc("diamond"), mc("diamond"), mc("diamond"));
        shapeless(list, "diamond_from_block", mc("diamond"), 9, mc("diamond_block"));
        shaped(list, "coal_block", mc("coal_block"), 1,
                mc("coal"), mc("coal"), mc("coal"),
                mc("coal"), mc("coal"), mc("coal"),
                mc("coal"), mc("coal"), mc("coal"));
        shapeless(list, "coal_from_block", mc("coal"), 9, mc("coal_block"));
        shapeless(list, "iron_nuggets", mc("iron_nugget"), 9, mc("iron_ingot"));
        shaped(list, "iron_ingot_from_nuggets", mc("iron_ingot"), 1,
                mc("iron_nugget"), mc("iron_nugget"), mc("iron_nugget"),
                mc("iron_nugget"), mc("iron_nugget"), mc("iron_nugget"),
                mc("iron_nugget"), mc("iron_nugget"), mc("iron_nugget"));
        shaped(list, "anvil", mc("anvil"), 1,
                mc("iron_block"), mc("iron_block"), mc("iron_block"),
                mc("iron_ingot"), mc("iron_ingot"), mc("iron_ingot"));

        shaped(list, "redstone_block", mc("redstone_block"), 1,
                mc("redstone"), mc("redstone"), mc("redstone"),
                mc("redstone"), mc("redstone"), mc("redstone"),
                mc("redstone"), mc("redstone"), mc("redstone"));
        shapeless(list, "redstone_from_block", mc("redstone"), 9, mc("redstone_block"));
        shaped(list, "redstone_torch", mc("redstone_torch"), 1, mc("redstone"), mc("stick"));
        shaped(list, "repeater", mc("repeater"), 1,
                mc("redstone_torch"), mc("redstone"), mc("redstone_torch"),
                mc("stone"), mc("stone"), mc("stone"));
        shaped(list, "comparator", mc("comparator"), 1,
                mc("redstone_torch"), mc("quartz"), mc("redstone_torch"),
                mc("stone"), mc("stone"), mc("stone"));
        shaped(list, "piston", mc("piston"), 1,
                mc("oak_planks"), mc("oak_planks"), mc("oak_planks"),
                mc("cobblestone"), mc("iron_ingot"), mc("cobblestone"),
                mc("cobblestone"), mc("redstone"), mc("cobblestone"));
        shaped(list, "sticky_piston", mc("sticky_piston"), 1, mc("slime_ball"), mc("piston"));
        shaped(list, "observer", mc("observer"), 1, mc("cobblestone"), mc("redstone"), mc("quartz"));
        shaped(list, "dispenser", mc("dispenser"), 1,
                mc("cobblestone"), mc("cobblestone"), mc("cobblestone"),
                mc("cobblestone"), mc("stick"), mc("cobblestone"),
                mc("cobblestone"), mc("redstone"), mc("cobblestone"));
        shaped(list, "dropper", mc("dropper"), 1,
                mc("cobblestone"), mc("cobblestone"), mc("cobblestone"),
                mc("cobblestone"), mc("cobblestone"),
                mc("cobblestone"), mc("redstone"), mc("cobblestone"));
        shaped(list, "hopper", mc("hopper"), 1,
                mc("iron_ingot"), mc("chest"), mc("iron_ingot"),
                mc("iron_ingot"), mc("iron_ingot"));
        shaped(list, "lever", mc("lever"), 1, mc("stick"), mc("cobblestone"));
        shaped(list, "stone_button", mc("stone_button"), 1, mc("stone"));
        shaped(list, "torch", mc("torch"), 4, mc("coal"), mc("stick"));
        shaped(list, "lantern", mc("lantern"), 1, mc("iron_nugget"), mc("torch"));

        smelting(list, "glass", mc("glass"), mc("sand"));
        shaped(list, "glass_pane", mc("glass_pane"), 16,
                mc("glass"), mc("glass"), mc("glass"),
                mc("glass"), mc("glass"), mc("glass"));
        shaped(list, "white_wool", mc("white_wool"), 1, mc("string"), mc("string"), mc("string"), mc("string"));
        shaped(list, "white_carpet", mc("white_carpet"), 3, mc("white_wool"), mc("white_wool"));
        shaped(list, "white_bed", mc("white_bed"), 1,
                mc("white_wool"), mc("white_wool"), mc("white_wool"),
                mc("oak_planks"), mc("oak_planks"), mc("oak_planks"));
        smelting(list, "quartz", mc("quartz"), mc("nether_quartz_ore"));
        smithing(list, "netherite_ingot", mc("netherite_ingot"), 1, mc("gold_ingot"), mc("netherite_scrap"));
        list.add(new Recipe(
                "minecraftuuuum:examplewrap/mechanical_piston",
                "crafting_shaped",
                "create:mechanical_piston",
                1,
                List.of(mc("piston"), mc("oak_planks"))));
        return list;
    }

    private static String mc(String path) {
        return "minecraft:" + path;
    }

    private static void shaped(List<Recipe> list, String id, String result, int count, String... ingredients) {
        list.add(new Recipe("minecraft:" + id, "crafting_shaped", result, count, List.of(ingredients)));
    }

    private static void shapeless(List<Recipe> list, String id, String result, int count, String... ingredients) {
        list.add(new Recipe("minecraft:" + id, "crafting_shapeless", result, count, List.of(ingredients)));
    }

    private static void smelting(List<Recipe> list, String id, String result, String input) {
        list.add(new Recipe("minecraft:" + id, "smelting", result, 1, List.of(input)));
    }

    private static void stonecutting(List<Recipe> list, String id, String result, int count, String input) {
        list.add(new Recipe("minecraft:" + id, "stonecutting", result, count, List.of(input)));
    }

    private static void smithing(List<Recipe> list, String id, String result, int count, String... ingredients) {
        list.add(new Recipe("minecraft:" + id, "smithing", result, count, List.of(ingredients)));
    }
}
