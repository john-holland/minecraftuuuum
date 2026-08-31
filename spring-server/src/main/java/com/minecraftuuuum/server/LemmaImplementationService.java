package com.minecraftuuuum.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minecraftuuuum.lemma.LemmaEntry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LemmaImplementationService {
    static final Set<String> NEOFORGE_HANDLERS = Set.of("say", "place", "give", "spawn", "if");

    private final LemmaImplementationStore store;
    private final ThesaurusService thesaurus;
    private final ObjectMapper json = new ObjectMapper();

    @Value("${minecraftuuuum.mocap-root}")
    private String mocapRoot;

    @Value("${minecraftuuuum.modly-root:D:/Development/modly}")
    private String modlyRoot;

    public LemmaImplementationService(LemmaImplementationStore store, ThesaurusService thesaurus) {
        this.store = store;
        this.thesaurus = thesaurus;
    }

    @PostConstruct
    void seedIfEmpty() {
        thesaurus.wrappers();
        if (store.count() == 0) {
            sync();
        }
    }

    public int sync() {
        thesaurus.wrappers();
        int n = 0;
        for (LemmaEntry e : thesaurus.merge()) {
            if (e == null || e.id == null || e.term == null) {
                continue;
            }
            upsertEntry(e);
            n++;
        }
        return n;
    }

    public Map<String, Object> summary() {
        int[] c = store.summaryCounts();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", c[0]);
        out.put("builtin", c[1]);
        out.put("implemented", c[2]);
        out.put("hasHandler", c[3]);
        out.put("hasVoxelSkin", c[4]);
        return out;
    }

    public List<Map<String, Object>> entries(String q, String kind, Boolean implemented) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (LemmaImplementationStore.Row row : store.list(q, kind, implemented)) {
            out.add(toMap(row));
        }
        return out;
    }

    public Map<String, Object> get(long id) {
        LemmaImplementationStore.Row row = store.get(id);
        return row == null ? null : toMap(row);
    }

    public Map<String, Object> patch(long id, Map<String, Object> body) {
        Boolean implemented = body.containsKey("isImplemented") ? bool(body.get("isImplemented")) : null;
        String art = body.containsKey("voxelArtworkId") ? str(body.get("voxelArtworkId")) : null;
        String face = body.containsKey("voxelFace") ? str(body.get("voxelFace")) : null;
        boolean clearT = body.containsKey("voxelT") && body.get("voxelT") == null;
        Integer t = intVal(body.get("voxelT"));
        String features = null;
        if (body.containsKey("features")) {
            features = writeFeatures(asStringList(body.get("features")));
        }
        LemmaImplementationStore.Row row = store.patch(id, implemented, art, face, t, clearT, features);
        return row == null ? null : toMap(row);
    }

    public List<Map<String, Object>> features() {
        boolean mocap = pathExists(mocapRoot);
        boolean modly = pathExists(modlyRoot);
        return List.of(
                feature("mediapipe_holistic@v1", "MediaPipe Holistic (humanoid)", "pose",
                        "Install mediapipe (Python 3.12). Do not vendor .tflite", true),
                feature("mocapanything@v2", "MoCapAnything v2 (non-human)", "pose",
                        "MoCapAnything at " + mocapRoot + " (weights not vendored)", mocap),
                feature("modly@local", "Modly (image/prompt → 3D)", "mesh",
                        "https://github.com/lightningpixel/modly — not vendored; " + modlyRoot, modly),
                feature("root-motion", "Root-motion / voxel-only", "pose",
                        "Bounding-box ambulation without a pose network", true),
                feature("pixellight", "PixelLight voxel skin", "voxel",
                        "Visual is the bound artworkId:face[:t] grid", true));
    }

    private void upsertEntry(LemmaEntry e) {
        String kind = e.properties.getOrDefault("kind", e.builtInCategory == null || e.builtInCategory.isBlank()
                ? "lemma" : e.builtInCategory.toLowerCase());
        boolean handler = NEOFORGE_HANDLERS.contains(e.term);
        boolean builtin = e.isBuiltIn || (e.id != null && e.id.contains(":builtin:"));
        String registry = e.properties.get("registry-id");
        String pose = e.properties.get("pose-engine");
        String skeleton = e.properties.get("skeleton-kind");
        LemmaImplementationStore.Row existing = store.getByEntryId(e.id);
        if (existing == null) {
            store.insertNew(
                    e.term, e.id, e.posTag, kind, builtin, handler, handler,
                    writeFeatures(defaultFeatures(pose, kind)),
                    registry, pose, skeleton);
        } else {
            store.updateSeedFields(existing.id(), e.term, e.posTag, kind, builtin, handler, registry, pose, skeleton);
        }
    }

    private static List<String> defaultFeatures(String poseEngine, String kind) {
        if ("mediapipe_holistic@v1".equals(poseEngine)) {
            return List.of("mediapipe_holistic@v1");
        }
        if ("mocapanything@v2".equals(poseEngine)) {
            return List.of("mocapanything@v2");
        }
        if ("root-motion".equals(poseEngine) || "entity".equals(kind)) {
            return List.of("root-motion");
        }
        return List.of();
    }

    private Map<String, Object> toMap(LemmaImplementationStore.Row row) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", row.id());
        out.put("term", row.term());
        out.put("entryId", row.entryId());
        out.put("posTag", row.posTag());
        out.put("kind", row.kind());
        out.put("isBuiltin", row.isBuiltin());
        out.put("isImplemented", row.isImplemented());
        out.put("hasNeoForgeHandler", row.hasNeoForgeHandler());
        out.put("voxelArtworkId", row.voxelArtworkId());
        out.put("voxelFace", row.voxelFace() == null || row.voxelFace().isBlank() ? "north" : row.voxelFace());
        out.put("voxelT", row.voxelT());
        out.put("features", readFeatures(row.featuresJson()));
        out.put("registryId", row.registryId());
        out.put("poseEngine", row.poseEngine());
        out.put("skeletonKind", row.skeletonKind());
        out.put("updatedAt", row.updatedAt());
        String art = row.voxelArtworkId();
        String face = row.voxelFace() == null ? "north" : row.voxelFace();
        if (art != null && !art.isBlank()) {
            out.put("voxelAddress", art + ":" + face + ":0:0" + (row.voxelT() == null ? "" : ":t=" + row.voxelT()));
        } else {
            out.put("voxelAddress", null);
        }
        return out;
    }

    private List<String> readFeatures(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return json.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeFeatures(List<String> features) {
        try {
            return json.writeValueAsString(features == null ? List.of() : features);
        } catch (Exception e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object v) {
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
            return out;
        }
        return List.of();
    }

    private static Map<String, Object> feature(String id, String label, String kind, String hint, boolean available) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("kind", kind);
        m.put("hint", hint);
        m.put("available", available);
        return m;
    }

    private static boolean pathExists(String path) {
        return path != null && !path.isBlank() && Files.exists(Path.of(path));
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Boolean bool(Object v) {
        if (v instanceof Boolean b) {
            return b;
        }
        return v != null && Boolean.parseBoolean(String.valueOf(v));
    }

    private static Integer intVal(Object v) {
        if (v == null || "".equals(v)) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v));
    }
}
