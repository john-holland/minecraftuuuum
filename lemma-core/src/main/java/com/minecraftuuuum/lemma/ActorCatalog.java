package com.minecraftuuuum.lemma;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Official vanilla EntityType actor ids. Humanoid → MediaPipe, others → MoCapAnything
 * when a species exists, else root-motion / voxel-only.
 */
public final class ActorCatalog {
    public enum SkeletonKind { HUMANOID, QUADRUPED, AQUATIC, AVIAN, AMORPHOUS, VEHICLE, OTHER }

    public record Actor(
            String registryId,
            String term,
            SkeletonKind skeleton,
            String poseEngine,
            String species) {
        public Map<String, String> properties() {
            Map<String, String> p = new LinkedHashMap<>();
            p.put("kind", "entity");
            p.put("registry-id", registryId);
            p.put("skeleton-kind", skeleton.name().toLowerCase(Locale.ROOT));
            p.put("pose-engine", poseEngine);
            if (species != null) {
                p.put("species", species);
            }
            return p;
        }
    }

    /** Vanilla EntityType path ids (Java Edition 1.21+ / 26.1 official names). */
    static final String[] VANILLA_ENTITY_TYPES = {
            "allay", "area_effect_cloud", "armadillo", "armor_stand", "arrow", "axolotl", "bat", "bee",
            "blaze", "block_display", "boat", "bogged", "breeze", "breeze_wind_charge", "camel", "cat",
            "cave_spider", "chest_boat", "chest_minecart", "chicken", "cod", "command_block_minecart",
            "cow", "creaking", "creeper", "dolphin", "donkey", "dragon_fireball", "drowned", "egg",
            "elder_guardian", "end_crystal", "ender_dragon", "ender_pearl", "enderman", "endermite",
            "evoker", "evoker_fangs", "experience_bottle", "experience_orb", "eye_of_ender", "falling_block",
            "fireball", "firework_rocket", "fishing_bobber", "fox", "frog", "furnace_minecart", "ghast",
            "giant", "glow_item_frame", "glow_squid", "goat", "guardian", "happy_ghast", "hoglin", "hopper_minecart",
            "horse", "husk", "illusioner", "interaction", "iron_golem", "item", "item_display", "item_frame",
            "leash_knot", "lightning_bolt", "llama", "llama_spit", "magma_cube", "marker", "minecart",
            "mooshroom", "mule", "ocelot", "ominous_item_spawner", "painting", "panda", "parrot", "phantom",
            "pig", "piglin", "piglin_brute", "pillager", "player", "polar_bear", "potion", "pufferfish",
            "rabbit", "ravager", "salmon", "sheep", "shulker", "shulker_bullet", "silverfish", "skeleton",
            "skeleton_horse", "slime", "small_fireball", "sniffer", "snow_golem", "snowball", "spawner_minecart",
            "spectral_arrow", "spider", "squid", "stray", "strider", "tadpole", "text_display", "tnt",
            "tnt_minecart", "trader_llama", "trident", "tropical_fish", "turtle", "vex", "villager",
            "vindicator", "wandering_trader", "warden", "wind_charge", "witch", "wither", "wither_skeleton",
            "wither_skull", "wolf", "zoglin", "zombie", "zombie_horse", "zombie_villager", "zombified_piglin"
    };

    private static final Set<String> HUMANOID = Set.of(
            "player", "villager", "wandering_trader", "zombie", "zombie_villager", "husk", "drowned",
            "skeleton", "stray", "bogged", "wither_skeleton", "piglin", "piglin_brute", "zombified_piglin",
            "witch", "evoker", "vindicator", "pillager", "illusioner", "vex", "giant", "creaking");

    private static final List<Actor> ALL = build();

    public static List<Actor> all() {
        return ALL;
    }

    public static Actor byId(String registryId) {
        String id = registryId.contains(":") ? registryId : "minecraft:" + registryId;
        for (Actor a : ALL) {
            if (a.registryId.equals(id)) {
                return a;
            }
        }
        return null;
    }

    public static List<LemmaEntry> asLemmas() {
        List<LemmaEntry> out = new ArrayList<>();
        for (Actor a : ALL) {
            String term = a.term;
            String id = "urn:minecraft:minecraftuuuum:mod:vanilla:v1:/en/noun/" + BuiltInUrn.slug(term);
            out.add(new LemmaEntry(id, term, "noun", "Actor", List.of("actor", "vanilla"), a.properties()));
        }
        return out;
    }

    private static List<Actor> build() {
        String[] ids = VANILLA_ENTITY_TYPES;
        List<Actor> list = new ArrayList<>(ids.length);
        for (String raw : ids) {
            String id = "minecraft:" + raw;
            SkeletonKind sk = kind(raw);
            String engine = sk == SkeletonKind.HUMANOID ? "mediapipe_holistic@v1"
                    : (sk == SkeletonKind.VEHICLE || sk == SkeletonKind.OTHER ? "root-motion"
                    : "mocapanything@v2");
            String species = (engine.startsWith("mocap")) ? speciesOf(raw) : null;
            list.add(new Actor(id, raw.replace('_', '-'), sk, engine, species));
        }
        return List.copyOf(list);
    }

    private static SkeletonKind kind(String raw) {
        if (HUMANOID.contains(raw)) {
            return SkeletonKind.HUMANOID;
        }
        if (raw.contains("boat") || raw.contains("minecart") || raw.contains("raft")) {
            return SkeletonKind.VEHICLE;
        }
        if (Set.of("cod", "salmon", "tropical_fish", "pufferfish", "squid", "glow_squid", "dolphin",
                "guardian", "elder_guardian", "tadpole", "axolotl").contains(raw)) {
            return SkeletonKind.AQUATIC;
        }
        if (Set.of("parrot", "chicken", "bee", "bat", "phantom", "ghast", "allay", "happy_ghast",
                "breeze").contains(raw)) {
            return SkeletonKind.AVIAN;
        }
        if (Set.of("slime", "magma_cube", "shulker", "endermite", "silverfish").contains(raw)) {
            return SkeletonKind.AMORPHOUS;
        }
        if (Set.of("cow", "mooshroom", "pig", "sheep", "horse", "donkey", "mule", "skeleton_horse",
                "zombie_horse", "wolf", "cat", "ocelot", "fox", "panda", "polar_bear", "goat",
                "camel", "sniffer", "hoglin", "zoglin", "ravager", "llama", "trader_llama",
                "armadillo", "turtle").contains(raw)) {
            return SkeletonKind.QUADRUPED;
        }
        return SkeletonKind.OTHER;
    }

    private static String speciesOf(String raw) {
        return switch (raw) {
            case "mooshroom" -> "cow";
            case "zoglin" -> "hoglin";
            case "trader_llama" -> "llama";
            case "zombie_horse", "skeleton_horse" -> "horse";
            case "glow_squid" -> "squid";
            case "elder_guardian" -> "guardian";
            default -> raw.replace('_', ' ');
        };
    }

}
