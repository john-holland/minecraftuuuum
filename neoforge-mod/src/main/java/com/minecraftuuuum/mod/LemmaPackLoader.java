package com.minecraftuuuum.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Loads datapack lemma JSON (vanilla + wrapper packs). */
public final class LemmaPackLoader {
    private static final Logger LOG = LogUtils.getLogger();
    private static final List<String> LOADED = new ArrayList<>();

    private LemmaPackLoader() {}

    public static void loadFromDatapacks() {
        load("/data/minecraftuuuum/lemmas/vanilla_actors.json");
        load("/data/examplewrap/lemmas/create_wrap.json");
    }

    public static List<String> loaded() {
        return LOADED;
    }

    private static void load(String classpath) {
        try (var in = LemmaPackLoader.class.getResourceAsStream(classpath)) {
            if (in == null) {
                LOG.warn("lemma pack missing {}", classpath);
                return;
            }
            JsonObject obj = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray entries = obj.getAsJsonArray("entries");
            if (entries != null) {
                for (JsonElement e : entries) {
                    JsonObject o = e.getAsJsonObject();
                    LOADED.add(o.get("term").getAsString());
                }
            }
            LOG.info("loaded lemma pack {} ({} terms)", obj.get("pack"), LOADED.size());
        } catch (Exception e) {
            LOG.warn("lemma pack {}", classpath, e);
        }
    }
}
