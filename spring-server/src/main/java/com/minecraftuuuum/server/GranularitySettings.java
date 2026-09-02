package com.minecraftuuuum.server;

import java.util.LinkedHashMap;
import java.util.Map;

/** Spatial granulation for PixelLight / Modly / VoxelRagdollActor. Distinct from webcam timeline ticks. */
public final class GranularitySettings {
    public static final String PRESET_MINECRAFT = "minecraft";
    public static final String PRESET_CUSTOM = "custom";

    public String preset = PRESET_MINECRAFT;
    public int pixelGrid = 16;
    public double blockMeters = 1.0;
    public int texelsPerMeter = 16;
    public double voxelCell = 1.0 / 16.0;
    public String skinLayout = "64x64";
    public int maxBones = 33;
    public boolean snapToGrid = true;
    public int faceGrid = 8;

    public static GranularitySettings minecraft() {
        return new GranularitySettings();
    }

    public Map<String, Object> asMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("preset", preset);
        m.put("pixelGrid", pixelGrid);
        m.put("blockMeters", blockMeters);
        m.put("texelsPerMeter", texelsPerMeter);
        m.put("voxelCell", voxelCell);
        m.put("skinLayout", skinLayout);
        m.put("maxBones", maxBones);
        m.put("snapToGrid", snapToGrid);
        m.put("faceGrid", faceGrid);
        return m;
    }

    public static GranularitySettings fromMap(Map<String, Object> body) {
        GranularitySettings g = minecraft();
        if (body == null || body.isEmpty()) {
            return g;
        }
        if (body.get("pixelGrid") != null) {
            g.pixelGrid = num(body.get("pixelGrid")).intValue();
        }
        if (body.get("blockMeters") != null) {
            g.blockMeters = num(body.get("blockMeters")).doubleValue();
        }
        if (body.get("texelsPerMeter") != null) {
            g.texelsPerMeter = num(body.get("texelsPerMeter")).intValue();
        }
        if (body.get("voxelCell") != null) {
            g.voxelCell = num(body.get("voxelCell")).doubleValue();
        }
        if (body.get("skinLayout") != null) {
            g.skinLayout = String.valueOf(body.get("skinLayout"));
        }
        if (body.get("maxBones") != null) {
            g.maxBones = num(body.get("maxBones")).intValue();
        }
        if (body.get("snapToGrid") != null) {
            g.snapToGrid = Boolean.parseBoolean(String.valueOf(body.get("snapToGrid")));
        }
        if (body.get("faceGrid") != null) {
            g.faceGrid = num(body.get("faceGrid")).intValue();
        }
        String preset = body.get("preset") == null ? null : String.valueOf(body.get("preset"));
        g.preset = PRESET_MINECRAFT.equals(preset) && matchesMinecraft(g) ? PRESET_MINECRAFT : PRESET_CUSTOM;
        if (PRESET_MINECRAFT.equals(preset) && body.size() <= 1) {
            return minecraft();
        }
        return g;
    }

    private static boolean matchesMinecraft(GranularitySettings g) {
        GranularitySettings m = minecraft();
        return g.pixelGrid == m.pixelGrid
                && Double.compare(g.blockMeters, m.blockMeters) == 0
                && g.texelsPerMeter == m.texelsPerMeter
                && Double.compare(g.voxelCell, m.voxelCell) == 0
                && m.skinLayout.equals(g.skinLayout)
                && g.maxBones == m.maxBones
                && g.snapToGrid == m.snapToGrid
                && g.faceGrid == m.faceGrid;
    }

    private static Number num(Object v) {
        if (v instanceof Number n) {
            return n;
        }
        return Double.parseDouble(String.valueOf(v));
    }
}
