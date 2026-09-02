package com.minecraftuuuum.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Not legal advice. Decides which acknowledgments apply for web vs Unity preview.
 */
public final class LicenseRequirements {
    public record Item(String id, String label, String href) {}

    private LicenseRequirements() {}

    public static List<Item> required(
            String displayMode,
            boolean redistributeWebgl,
            boolean publishMinecraft,
            String unityTier) {
        List<Item> out = new ArrayList<>();
        out.add(new Item("ack_mojang_disclaimer",
                "Unofficial analog — not affiliated with Mojang AB / Microsoft. Minecraft is their trademark.",
                null));
        out.add(new Item("ack_no_unofficial_assets",
                "Do not vendor unofficial Minecraft textures, models, or sounds in this repository.",
                null));
        out.add(new Item("ack_tools_local_only",
                "Modly, MediaPipe, and MoCapAnything are invoked locally and not redistributed from this repo.",
                null));
        boolean unity = "unity_webgl".equals(displayMode);
        if (unity) {
            String tier = unityTier == null ? "none" : unityTier.toLowerCase(Locale.ROOT);
            out.add(new Item("ack_unity_editor_license",
                    "I have my own Unity Editor license (" + tier + "). This project does not vendor Unity.",
                    "https://unity.com/legal/terms-of-service"));
            out.add(new Item("ack_unity_player_local",
                    "Unity WebGL / Player terms apply to local preview. I will not commit Build/ player binaries.",
                    "https://unity.com/legal"));
            out.add(new Item("ack_no_commit_build",
                    "static/continuuuum_editor/Build stays gitignored. I export my own WebGL player.",
                    null));
            if (redistributeWebgl) {
                out.add(new Item("ack_unity_redistribution",
                        "Redistributing a Unity WebGL player requires Unity's current runtime / redistribution terms.",
                        "https://unity.com/legal"));
            }
        }
        if (publishMinecraft) {
            out.add(new Item("ack_marketplace_retainer",
                    "Minecraft / Marketplace publish uses the existing 70/30 Mojang-Microsoft retainer. Unity service retainer stays off.",
                    null));
        }
        return out;
    }

    public static List<String> missing(List<Item> required, List<String> acknowledged) {
        Set<String> have = acknowledged == null ? Set.of() : Set.copyOf(acknowledged);
        List<String> miss = new ArrayList<>();
        for (Item item : required) {
            if (!have.contains(item.id())) {
                miss.add(item.id());
            }
        }
        return miss;
    }

    public static Map<String, Object> asMap(List<Item> items) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("disclaimer", "Not legal advice. Confirm current Unity and Mojang terms yourself.");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Item item : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", item.id());
            m.put("label", item.label());
            m.put("href", item.href());
            list.add(m);
        }
        out.put("items", list);
        return out;
    }
}
