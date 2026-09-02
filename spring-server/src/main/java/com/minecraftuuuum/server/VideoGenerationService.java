package com.minecraftuuuum.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimined.craftpressor.db.CraftpressorDb;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VideoGenerationService {
    private final CraftpressorDb db;
    private final ObjectMapper json = new ObjectMapper();

    @Value("${minecraftuuuum.modly-root:D:/Development/modly}")
    private String modlyRoot;

    @Value("${minecraftuuuum.webgl-preview:http://127.0.0.1:5050/continuuuum_editor/index.html}")
    private String webglPreview;

    @Value("${minecraftuuuum.mocap-root}")
    private String mocapRoot;

    private final LegalUnityService legal;

    public VideoGenerationService(CraftpressorDb db, LegalUnityService legal) {
        this.db = db;
        this.legal = legal;
    }

    public Map<String, Object> features() {
        boolean modly = Files.isDirectory(Path.of(modlyRoot));
        boolean mocap = Files.isDirectory(Path.of(mocapRoot));
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(feat("modly@local", "Modly image-to-model", "mesh",
                "https://github.com/lightningpixel/modly — not vendored", modly));
        list.add(feat("mediapipe_holistic@v1", "MediaPipe Holistic", "pose",
                "Humanoid pose. Do not vendor .tflite", true));
        list.add(feat("mocapanything@v2", "MoCapAnything v2", "pose",
                mocapRoot, mocap));
        list.add(feat("pixellight", "PixelLight voxel skin", "voxel",
                "16-grid default; overridden by granularity.pixelGrid", true));
        list.add(feat("voxel-ragdoll", "VoxelRagdollActor", "actor",
                "Minecraft-scale ragdoll analog (1 block = 1 m)", true));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("features", list);
        boolean webglBuild = getClass().getResource("/static/continuuuum_editor/Build") != null;
        Map<String, Object> legalStatus = legal.status();
        out.put("webglPreview", webglPreview);
        out.put("webglBuild", webglBuild);
        out.put("modlyRoot", modlyRoot);
        out.put("granularityMinecraft", GranularitySettings.minecraft().asMap());
        out.put("displayMode", legalStatus.get("displayMode"));
        out.put("ironMan", legalStatus.get("ironMan"));
        out.put("unityAllowed", legalStatus.get("unityAllowed"));
        out.put("unitySession", legalStatus.get("session"));
        return out;
    }

    public Map<String, Object> storeMedia(
            String artworkId,
            Integer t,
            MultipartFile image,
            MultipartFile mask,
            GranularitySettings gran)
            throws Exception {
        String id = artworkId == null || artworkId.isBlank() ? "art_" + UUID.randomUUID().toString().substring(0, 8) : artworkId;
        int frame = t == null ? -1 : t;
        String granJson = json.writeValueAsString(gran.asMap());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", id);
        out.put("t", frame);
        out.put("granularity", gran.asMap());
        if (image != null && !image.isEmpty()) {
            byte[] bytes = image.getBytes();
            Integer[] wh = sizeOf(bytes);
            out.put("source_image", db.upsertArtworkMedia(
                    id, frame, "source_image", mimeOf(image), wh[0], wh[1], bytes, granJson));
        }
        if (mask != null && !mask.isEmpty()) {
            byte[] bytes = mask.getBytes();
            Integer[] wh = sizeOf(bytes);
            out.put("source_mask", db.upsertArtworkMedia(
                    id, frame, "source_mask", mimeOf(mask), wh[0], wh[1], bytes, granJson));
        }
        out.put("media", db.listArtworkMedia(id));
        return out;
    }

    public Map<String, Object> getMediaMeta(String artworkId) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", artworkId);
        out.put("media", db.listArtworkMedia(artworkId));
        return out;
    }

    public Map<String, Object> getBlob(String artworkId, int t, String kind) throws Exception {
        return db.getArtworkMedia(artworkId, t, kind);
    }

    public Map<String, Object> invokeModly(String artworkId, Integer t, String prompt, String meshFormat, Integer steps)
            throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", artworkId);
        Path root = Path.of(modlyRoot);
        if (!Files.isDirectory(root)) {
            out.put("ok", false);
            out.put("available", false);
            out.put("hint", "Set MODLY_ROOT; Modly is not vendored. https://github.com/lightningpixel/modly");
            return out;
        }
        int frame = t == null ? -1 : t;
        Map<String, Object> img = db.getArtworkMedia(artworkId, frame, "face_atlas");
        String imageKind = "face_atlas";
        if (img == null || img.get("blob") == null) {
            img = db.getArtworkMedia(artworkId, frame, "source_image");
            imageKind = "source_image";
        }
        if (img == null || img.get("blob") == null) {
            out.put("ok", false);
            out.put("error", "no source_image or face_atlas for artwork");
            return out;
        }
        Path work = Files.createTempDirectory("modly-" + artworkId);
        Path in = work.resolve("input.png");
        Files.write(in, (byte[]) img.get("blob"));
        out.put("imageKind", imageKind);
        String fmt = meshFormat == null || meshFormat.isBlank() ? "glb" : meshFormat.replaceAll("[^a-z0-9]", "");
        Path outMesh = work.resolve("model." + fmt);
        List<String> cmd = new ArrayList<>();
        if (Files.isRegularFile(root.resolve("modly.exe"))) {
            cmd.add(root.resolve("modly.exe").toString());
        } else if (Files.isRegularFile(root.resolve("modly"))) {
            cmd.add(root.resolve("modly").toString());
        } else {
            cmd.add("modly");
        }
        cmd.add("--image");
        cmd.add(in.toAbsolutePath().toString());
        cmd.add("--out");
        cmd.add(outMesh.toAbsolutePath().toString());
        if (prompt != null && !prompt.isBlank()) {
            cmd.add("--prompt");
            cmd.add(prompt);
        }
        if (steps != null && steps > 0) {
            cmd.add("--steps");
            cmd.add(String.valueOf(steps));
        }
        Map<String, Object> mask = db.getArtworkMedia(artworkId, frame, "source_mask");
        if (mask != null && mask.get("blob") != null) {
            Path maskPath = work.resolve("mask.png");
            Files.write(maskPath, (byte[]) mask.get("blob"));
            cmd.add("--mask");
            cmd.add(maskPath.toAbsolutePath().toString());
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(root.toFile());
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            String log = new String(p.getInputStream().readAllBytes());
            int code = p.waitFor();
            out.put("exit", code);
            out.put("log", log.length() > 4000 ? log.substring(0, 4000) : log);
            if (code == 0 && Files.isRegularFile(outMesh)) {
                byte[] mesh = Files.readAllBytes(outMesh);
                String gran = (String) img.get("granularityJson");
                db.upsertArtworkMedia(artworkId, frame, "generated_mesh",
                        "model/" + fmt, null, null, mesh, gran);
                out.put("ok", true);
                out.put("bytes", mesh.length);
                out.put("kind", "generated_mesh");
            } else {
                out.put("ok", false);
                out.put("hint", "Modly CLI did not write " + outMesh.getFileName());
            }
        } catch (Exception e) {
            out.put("ok", false);
            out.put("available", true);
            out.put("error", e.getMessage());
            out.put("hint", "Install Modly at " + modlyRoot);
        }
        return out;
    }

    private static String mimeOf(MultipartFile f) {
        String m = f.getContentType();
        return m == null || m.isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : m;
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

    private static Map<String, Object> feat(String id, String label, String kind, String hint, boolean available) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("kind", kind);
        m.put("hint", hint);
        m.put("available", available);
        return m;
    }
}
