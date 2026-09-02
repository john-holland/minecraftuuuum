package com.minecraftuuuum.mod;

/**
 * Minecraft-scale voxel ragdoll analog (Drawer 2 {@code VoxelRagdollActor}).
 * 1 block = 1 m, 16 texels per meter. Full voxel ragdoll physics is out of scope
 * for this pass — this stub documents the contract for a later NeoForge implementation.
 *
 * <p>Minecraft® is a trademark of Mojang AB / Microsoft Corporation. All rights reserved.
 * Unofficial analog; not affiliated with Mojang or Microsoft. No unofficial Minecraft
 * assets are vendored.
 */
public final class VoxelRagdollActor {
    public static final String PRESET_MINECRAFT = "minecraft";
    public static final int DEFAULT_PIXEL_GRID = 16;
    public static final double BLOCK_METERS = 1.0;
    public static final int TEXELS_PER_METER = 16;
    public static final double VOXEL_CELL_BLOCKS = 1.0 / 16.0;
    public static final String SKIN_LAYOUT = "64x64";
    public static final int MAX_BONES_STEVE = 33;

    public String preset = PRESET_MINECRAFT;
    public int pixelGrid = DEFAULT_PIXEL_GRID;
    public double blockMeters = BLOCK_METERS;
    public int texelsPerMeter = TEXELS_PER_METER;
    public double voxelCell = VOXEL_CELL_BLOCKS;
    public boolean snapToGrid = true;

    public double voxelCellMeters() {
        return voxelCell * blockMeters;
    }

    public double snap(double world) {
        if (!snapToGrid) {
            return world;
        }
        double cell = voxelCellMeters();
        if (cell <= 0) {
            return world;
        }
        return Math.round(world / cell) * cell;
    }

    private VoxelRagdollActor() {
    }

    public static VoxelRagdollActor minecraft() {
        return new VoxelRagdollActor();
    }

    /**
     * Playback clip contract from UCC {@code GET /api/video-generation/playback/{artworkId}}:
     * artworkId, frame t, dirty tree-node mesh refs, granularity. Physics still out of scope.
     */
    public record PlaybackClip(
            String artworkId,
            int t,
            String nodeId,
            String meshKind,
            int pixelGrid) {}
}
