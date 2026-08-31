package com.minecraftuuuum.server;

import com.unimined.craftpressor.compressors.CompressionLoop;
import com.unimined.craftpressor.compressors.VoxelCompressor;
import com.unimined.craftpressor.db.CraftpressorDb;
import com.unimined.craftpressor.voxel.SixFaceVoxel;
import com.unimined.craftpressor.voxel.VoxelAddress;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class VoxelService {
    private final CraftpressorDb db;
    private final VoxelCompressor compressor;

    public VoxelService(CraftpressorDb db) {
        this.db = db;
        this.compressor = new VoxelCompressor(db);
    }

    public Map<String, Object> ingestFile(MultipartFile file, String artworkId, String face, Integer t)
            throws Exception {
        Path tmp = Files.createTempFile("voxel-", "-" + file.getOriginalFilename());
        file.transferTo(tmp.toFile());
        try {
            return ingestPath(tmp, artworkId, face, t);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public Map<String, Object> ingestPath(Path path, String artworkId, String face, Integer t) throws Exception {
        String id = artworkId == null || artworkId.isBlank() ? VoxelCompressor.artworkIdFrom(path) : artworkId;
        CompressionLoop.Result r = compressor.compress(path, "image", id, face == null ? "north" : face, t, 16);
        Map<String, Object> out = new LinkedHashMap<>(CompressionLoop.asMap(r));
        out.put("artworkId", id);
        out.put("face", VoxelAddress.normalizeFace(face == null ? "north" : face));
        out.put("t", t);
        out.put("addressPrefix", id + ":" + VoxelAddress.normalizeFace(face == null ? "north" : face));
        out.put("complete", false);
        return out;
    }

    public Map<String, Object> foldsFromImage(BufferedImage img, String artworkId, String face, Integer t) {
        SixFaceVoxel art = new SixFaceVoxel(artworkId, SixFaceVoxel.DEFAULT_GRID, t);
        String f = VoxelAddress.normalizeFace(face);
        art.ingestImage(f, img);
        for (String other : VoxelAddress.FACES) {
            if (!other.equals(f)) {
                art.ingestImage(other, img);
            }
        }
        Map<String, Object> fold1 = Map.of("kind", "image", "face", f, "width", img.getWidth(), "height", img.getHeight());
        Map<String, Object> fold2 = new LinkedHashMap<>();
        fold2.put("kind", "image-mask");
        fold2.put("grid", art.grid());
        fold2.put("squareIsTextureAndBrush", true);
        fold2.put("maskTouched", countMask(art, f));
        Map<String, Object> fold3 = new LinkedHashMap<>();
        fold3.put("kind", "pixellight-grid");
        fold3.put("pixelatedDefault", true);
        fold3.put("slimNeighbors", VoxelAddress.FACES);
        fold3.put("currentFace", f);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", artworkId);
        out.put("t", t);
        out.put("fold1", fold1);
        out.put("fold2", fold2);
        out.put("fold3", fold3);
        out.put("sixFaceComplete", art.sixFacesTouched());
        return out;
    }

    public Map<String, Object> stamp(String artworkId, String face, Integer t, int u, int v, String colorHex)
            throws Exception {
        if (artworkId == null || artworkId.isBlank()) {
            throw new IllegalArgumentException("artworkId");
        }
        int argb = parseHex(colorHex);
        String hex = SixFaceVoxel.hex(argb);
        String f = VoxelAddress.normalizeFace(face == null ? "north" : face);
        SixFaceVoxel art = new SixFaceVoxel(artworkId, SixFaceVoxel.DEFAULT_GRID, t);
        art.stampBrush(f, u, v, hex, argb);
        Map<String, Object> out = new LinkedHashMap<>(db.upsertVoxelCell(artworkId, f, u, v, t, hex, hex));
        out.put("voxelStatus", "stored");
        return out;
    }

    private static int parseHex(String colorHex) {
        if (colorHex == null) {
            throw new IllegalArgumentException("colorHex");
        }
        String h = colorHex.trim();
        if (h.startsWith("#")) {
            h = h.substring(1);
        }
        if (h.length() != 6) {
            throw new IllegalArgumentException("colorHex");
        }
        int rgb = Integer.parseInt(h, 16);
        return 0xff000000 | rgb;
    }

    public Map<String, Object> convertPixelLight(String artworkId, Integer t) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", artworkId);
        out.put("t", t);
        out.put("faces", VoxelAddress.FACES);
        out.put("format", "minecraft-face-png + block-model voxels");
        out.put("addresses", db.queryTable("voxel_addresses", 64));
        return out;
    }

    private static int countMask(SixFaceVoxel art, String face) {
        int n = 0;
        boolean[][] m = art.mask(face);
        for (boolean[] row : m) {
            for (boolean b : row) {
                if (b) {
                    n++;
                }
            }
        }
        return n;
    }
}
