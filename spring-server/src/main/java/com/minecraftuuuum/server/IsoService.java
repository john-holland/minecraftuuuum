package com.minecraftuuuum.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimined.craftpressor.db.CraftpressorDb;
import com.unimined.craftpressor.voxel.ConvexTreeSplitter;
import com.unimined.craftpressor.voxel.IsoExtrapolator;
import com.unimined.craftpressor.voxel.SixFaceVoxel;
import com.unimined.craftpressor.voxel.VoxelAddress;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class IsoService {
    private final CraftpressorDb db;
    private final ObjectMapper json;

    public IsoService(CraftpressorDb db, ObjectMapper json) {
        this.db = db;
        this.json = json;
    }

    public Map<String, Object> storeScreenshot(
            String artworkId,
            String face,
            Integer t,
            MultipartFile image,
            GranularitySettings gran)
            throws Exception {
        String id = blank(artworkId) ? "art_" + UUID.randomUUID().toString().substring(0, 8) : artworkId;
        String f = VoxelAddress.normalizeFace(face == null ? "north" : face);
        int frame = t == null ? -1 : t;
        Map<String, Object> extra = gran == null ? GranularitySettings.minecraft().asMap() : gran.asMap();
        extra.put("face", f);
        extra.put("camera", f);
        byte[] bytes = image.getBytes();
        Integer[] wh = sizeOf(bytes);
        String kind = "screenshot_" + f;
        Map<String, Object> stored = db.upsertArtworkMedia(
                id, frame, kind, mimeOf(image), wh[0], wh[1], bytes, json.writeValueAsString(extra));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", id);
        out.put("face", f);
        out.put("t", frame);
        out.put("media", stored);
        return out;
    }

    public Map<String, Object> pullFrameAsScreenshot(String artworkId, String face, int t, byte[] png) throws Exception {
        GranularitySettings gran = GranularitySettings.minecraft();
        String f = VoxelAddress.normalizeFace(face);
        Map<String, Object> extra = gran.asMap();
        extra.put("face", f);
        extra.put("fromVideoFrame", true);
        db.upsertArtworkMedia(artworkId, t, "screenshot_" + f, "image/png", null, null, png, json.writeValueAsString(extra));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", artworkId);
        out.put("face", f);
        out.put("t", t);
        out.put("kind", "screenshot_" + f);
        return out;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> upsertPoint(Map<String, Object> body) throws Exception {
        String artworkId = str(body, "artworkId");
        if (blank(artworkId)) {
            throw new IllegalArgumentException("artworkId");
        }
        String pointId = blank(str(body, "pointId"))
                ? "pt_" + UUID.randomUUID().toString().substring(0, 6)
                : str(body, "pointId");
        String kind = blank(str(body, "kind")) ? "custom" : str(body, "kind");
        Object views = body.get("views");
        String viewsJson = views == null ? "[]" : json.writeValueAsString(views);
        db.upsertIsoPoint(
                UUID.randomUUID().toString(),
                artworkId,
                pointId,
                str(body, "label") == null ? pointId : str(body, "label"),
                kind,
                viewsJson,
                num(body.get("x")),
                num(body.get("y")),
                num(body.get("z")));
        return Map.of("artworkId", artworkId, "pointId", pointId, "points", listPoints(artworkId));
    }

    public List<Map<String, Object>> listPoints(String artworkId) throws Exception {
        List<Map<String, Object>> rows = db.listIsoPoints(artworkId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new LinkedHashMap<>(row);
            Object raw = row.get("viewsJson");
            if (raw != null) {
                m.put("views", json.readValue(String.valueOf(raw), new TypeReference<List<Map<String, Object>>>() {}));
            }
            m.remove("viewsJson");
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> suggestFaceLandmarks(String artworkId, int grid) throws Exception {
        int g = grid <= 0 ? 16 : grid;
        String[][] presets = {
                {"leftEye", "face_landmark", "4", "2"},
                {"rightEye", "face_landmark", "11", "2"},
                {"nose", "face_landmark", "8", "4"},
                {"mouthLeft", "face_landmark", "5", "6"},
                {"mouthRight", "face_landmark", "10", "6"},
                {"mouthOpen", "face_landmark", "8", "6"},
                {"jaw", "face_landmark", "8", "7"}
        };
        for (String[] p : presets) {
            double u = Double.parseDouble(p[2]) * g / 16.0;
            double v = Double.parseDouble(p[3]) * g / 16.0;
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("artworkId", artworkId);
            body.put("pointId", p[0]);
            body.put("label", p[0]);
            body.put("kind", p[1]);
            body.put("views", List.of(Map.of("face", "north", "u", u, "v", v, "xPx", u * 10, "yPx", v * 10)));
            upsertPoint(body);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", artworkId);
        out.put("faceGrid", 8);
        out.put("points", listPoints(artworkId));
        return out;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> extrapolate(Map<String, Object> body) throws Exception {
        String artworkId = str(body, "artworkId");
        if (blank(artworkId)) {
            throw new IllegalArgumentException("artworkId");
        }
        int t = body.get("t") instanceof Number n ? n.intValue() : -1;
        int grid = body.get("grid") instanceof Number n ? n.intValue() : SixFaceVoxel.DEFAULT_GRID;
        Set<String> accepted = new HashSet<>();
        Object acc = body.get("acceptedExtrapolated");
        if (acc instanceof List<?> list) {
            for (Object o : list) {
                accepted.add(VoxelAddress.normalizeFace(String.valueOf(o)));
            }
        }
        Map<String, BufferedImage> uploaded = loadScreenshots(artworkId, t);
        List<IsoExtrapolator.CommonPoint> points = toPoints(listPoints(artworkId));
        IsoExtrapolator.Result r = IsoExtrapolator.extrapolate(artworkId, grid, t < 0 ? null : t, uploaded, points, accepted);
        db.storeVoxelArt(r.art(), "screenshot");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", artworkId);
        out.put("t", t);
        out.put("faceOrigin", r.faceOrigin());
        out.put("sixFaceComplete", r.sixFaceComplete());
        out.put("cloned", !r.art().facesUnique());
        out.put("slimNeighbors", r.slimNeighbors());
        out.put("uploadedFaces", uploaded.keySet());
        return out;
    }

    public Map<String, Object> join(String artworkId, Integer t, GranularitySettings gran) throws Exception {
        int frame = t == null ? -1 : t;
        GranularitySettings g = gran == null ? GranularitySettings.minecraft() : gran;
        List<Map<String, Object>> cells = db.listVoxelAddresses(artworkId, frame < 0 ? null : frame);
        BufferedImage atlas = assembleAtlas(artworkId, frame, g.pixelGrid);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(atlas, "png", bos);
        byte[] png = bos.toByteArray();
        db.upsertArtworkMedia(artworkId, frame, "face_atlas", "image/png",
                atlas.getWidth(), atlas.getHeight(), png, json.writeValueAsString(g.asMap()));
        List<Map<String, Object>> pts = listPoints(artworkId);
        if (g.snapToGrid) {
            for (Map<String, Object> p : pts) {
                snapPoint(p, g);
            }
        }
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("artworkId", artworkId);
        spec.put("t", frame);
        spec.put("faces", VoxelAddress.FACES);
        spec.put("cellCount", cells.size());
        spec.put("pointIds", pts.stream().map(p -> p.get("pointId")).toList());
        spec.put("granularity", g.asMap());
        spec.put("faceGrid", g.faceGrid);
        spec.put("atlasKind", "face_atlas");
        db.upsertJoinSpec(artworkId, frame, json.writeValueAsString(spec));
        Map<String, Object> out = new LinkedHashMap<>(spec);
        out.put("atlasBytes", png.length);
        out.put("points", pts);
        return out;
    }

    public Map<String, Object> splitTree(String artworkId, Integer t) throws Exception {
        int frame = t == null ? -1 : t;
        SixFaceVoxel art = loadArtFromAddresses(artworkId, frame);
        Map<String, String> prev = new LinkedHashMap<>();
        int prevT = frame < 0 ? -2 : frame - 1;
        for (Map<String, Object> n : db.listAnimTree(artworkId, prevT)) {
            prev.put(String.valueOf(n.get("nodeId")), String.valueOf(n.get("contentHash")));
        }
        List<ConvexTreeSplitter.Node> nodes = ConvexTreeSplitter.split(art, prev);
        List<Map<String, Object>> stored = new ArrayList<>();
        for (ConvexTreeSplitter.Node n : nodes) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", artworkId + ":" + frame + ":" + n.nodeId());
            row.put("nodeId", n.nodeId());
            row.put("parentId", n.parentId());
            row.put("quadPath", n.quadPath());
            row.put("hullJson", json.writeValueAsString(n.hull()));
            row.put("meshBlobRef", "stopmo_" + n.nodeId());
            row.put("contentHash", n.contentHash());
            row.put("dirty", n.dirty());
            stored.add(row);
            for (String addr : n.cellAddresses()) {
                db.updateChunkTree(addr, null, n.quadPath());
            }
        }
        db.replaceAnimTree(artworkId, frame, stored);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", artworkId);
        out.put("t", frame);
        out.put("nodes", db.listAnimTree(artworkId, frame));
        out.put("dirty", ConvexTreeSplitter.dirtyOnly(nodes).stream().map(ConvexTreeSplitter.Node::nodeId).toList());
        return out;
    }

    public Map<String, Object> tree(String artworkId, Integer t) throws Exception {
        int frame = t == null ? -1 : t;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", artworkId);
        out.put("t", frame);
        List<Map<String, Object>> nodes = db.listAnimTree(artworkId, frame);
        out.put("nodes", nodes);
        out.put("dirty", nodes.stream().filter(n -> Boolean.TRUE.equals(n.get("dirty"))).map(n -> n.get("nodeId")).toList());
        return out;
    }

    public Map<String, Object> cacheStopmo(String artworkId, Integer t) throws Exception {
        int frame = t == null ? -1 : t;
        Map<String, Object> tree = splitTree(artworkId, frame);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) tree.get("nodes");
        List<Map<String, Object>> cached = new ArrayList<>();
        for (Map<String, Object> n : nodes) {
            if (!Boolean.TRUE.equals(n.get("dirty"))) {
                continue;
            }
            String nodeId = String.valueOf(n.get("nodeId"));
            String payload = json.writeValueAsString(Map.of(
                    "nodeId", nodeId,
                    "hash", n.get("contentHash"),
                    "quadPath", n.get("quadPath"),
                    "t", frame));
            String kind = "stopmo_" + nodeId;
            db.upsertArtworkMedia(artworkId, frame, kind, "application/json",
                    null, null, payload.getBytes(), json.writeValueAsString(GranularitySettings.minecraft().asMap()));
            cached.add(Map.of("nodeId", nodeId, "kind", kind, "bytes", payload.length()));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", artworkId);
        out.put("t", frame);
        out.put("cached", cached);
        out.put("dirty", tree.get("dirty"));
        out.put("media", db.listArtworkMedia(artworkId));
        return out;
    }

    public Map<String, Object> playback(String artworkId, List<Integer> frames, boolean stopMotion, String displayMode)
            throws Exception {
        List<Map<String, Object>> clip = new ArrayList<>();
        if (frames == null || frames.isEmpty()) {
            frames = List.of(-1);
        }
        for (Integer t : frames) {
            Map<String, Object> tree = tree(artworkId, t);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) tree.get("nodes");
            List<Map<String, Object>> swaps = new ArrayList<>();
            for (Map<String, Object> n : nodes) {
                if (!Boolean.TRUE.equals(n.get("dirty"))) {
                    continue;
                }
                Map<String, Object> swap = new LinkedHashMap<>();
                swap.put("nodeId", n.get("nodeId"));
                swap.put("quadPath", n.get("quadPath"));
                swap.put("hash", n.get("contentHash"));
                swap.put("meshKind", "stopmo_" + n.get("nodeId"));
                swaps.add(swap);
            }
            Map<String, Object> fr = new LinkedHashMap<>();
            fr.put("t", t);
            fr.put("swaps", swaps);
            clip.add(fr);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", artworkId);
        out.put("stopMotion", stopMotion);
        out.put("displayMode", displayMode == null ? "web" : displayMode);
        out.put("frames", clip);
        out.put("voxelRagdoll", Map.of(
                "preset", "minecraft",
                "pixelGrid", 16,
                "texelsPerMeter", 16,
                "maxBones", 33));
        return out;
    }

    public Map<String, Object> slim(String artworkId, Integer t) throws Exception {
        int frame = t == null ? -1 : t;
        SixFaceVoxel art = loadArtFromAddresses(artworkId, frame);
        Map<String, List<String>> slim = new LinkedHashMap<>();
        for (String face : VoxelAddress.FACES) {
            slim.put(face, art.slimRow(face, 0));
        }
        return Map.of("artworkId", artworkId, "t", frame, "slimNeighbors", slim);
    }

    private SixFaceVoxel loadArtFromAddresses(String artworkId, int t) throws Exception {
        SixFaceVoxel art = new SixFaceVoxel(artworkId, SixFaceVoxel.DEFAULT_GRID, t < 0 ? null : t);
        for (Map<String, Object> row : db.listVoxelAddresses(artworkId, t < 0 ? null : t)) {
            String hex = String.valueOf(row.get("colorHex"));
            if (hex == null || hex.length() < 7) {
                continue;
            }
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            int rgb = Integer.parseInt(h, 16);
            art.setCell(String.valueOf(row.get("face")),
                    ((Number) row.get("u")).intValue(),
                    ((Number) row.get("v")).intValue(),
                    0xff000000 | rgb);
        }
        return art;
    }

    private Map<String, BufferedImage> loadScreenshots(String artworkId, int t) throws Exception {
        Map<String, BufferedImage> out = new LinkedHashMap<>();
        for (String face : VoxelAddress.FACES) {
            Map<String, Object> row = db.getArtworkMedia(artworkId, t, "screenshot_" + face);
            if (row != null && row.get("blob") != null) {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream((byte[]) row.get("blob")));
                if (img != null) {
                    out.put(face, img);
                }
            }
        }
        return out;
    }

    private BufferedImage assembleAtlas(String artworkId, int t, int grid) throws Exception {
        int g = grid <= 0 ? 16 : grid;
        BufferedImage atlas = new BufferedImage(g * 3, g * 2, BufferedImage.TYPE_INT_ARGB);
        String[] order = {"north", "east", "south", "west", "up", "down"};
        SixFaceVoxel art = loadArtFromAddresses(artworkId, t);
        for (int i = 0; i < order.length; i++) {
            int ox = (i % 3) * g;
            int oy = (i / 3) * g;
            int[][] cells = art.face(order[i]);
            for (int v = 0; v < g && v < cells.length; v++) {
                for (int u = 0; u < g && u < cells[v].length; u++) {
                    atlas.setRGB(ox + u, oy + v, cells[v][u] == 0 ? 0x00000000 : cells[v][u]);
                }
            }
        }
        return atlas;
    }

    private List<IsoExtrapolator.CommonPoint> toPoints(List<Map<String, Object>> rows) {
        List<IsoExtrapolator.CommonPoint> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<IsoExtrapolator.View> views = new ArrayList<>();
            Object raw = row.get("views");
            if (raw instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> m) {
                        views.add(new IsoExtrapolator.View(
                                String.valueOf(m.get("face")),
                                num(m.get("u")) == null ? 0 : num(m.get("u")),
                                num(m.get("v")) == null ? 0 : num(m.get("v")),
                                num(m.get("xPx")) == null ? 0 : num(m.get("xPx")),
                                num(m.get("yPx")) == null ? 0 : num(m.get("yPx"))));
                    }
                }
            }
            out.add(new IsoExtrapolator.CommonPoint(
                    String.valueOf(row.get("pointId")),
                    row.get("label") == null ? null : String.valueOf(row.get("label")),
                    String.valueOf(row.get("kind")),
                    views,
                    num(row.get("x")),
                    num(row.get("y")),
                    num(row.get("z"))));
        }
        return out;
    }

    private void snapPoint(Map<String, Object> p, GranularitySettings g) {
        Double x = num(p.get("x"));
        if (x != null) {
            p.put("x", Math.round(x / g.voxelCell) * g.voxelCell);
        }
        Double y = num(p.get("y"));
        if (y != null) {
            p.put("y", Math.round(y / g.voxelCell) * g.voxelCell);
        }
        Double z = num(p.get("z"));
        if (z != null) {
            p.put("z", Math.round(z / g.voxelCell) * g.voxelCell);
        }
    }

    private static Integer[] sizeOf(byte[] bytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                return new Integer[] {null, null};
            }
            return new Integer[] {img.getWidth(), img.getHeight()};
        } catch (Exception e) {
            return new Integer[] {null, null};
        }
    }

    private static String mimeOf(MultipartFile f) {
        String m = f.getContentType();
        return m == null || m.isBlank() ? "application/octet-stream" : m;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Double num(Object v) {
        if (v == null || "".equals(v)) {
            return null;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(v));
    }
}
