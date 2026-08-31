package com.minecraftuuuum.server;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Map;

@RestController
@RequestMapping("/api/voxel")
public class VoxelController {
    private final VoxelService voxels;

    public VoxelController(VoxelService voxels) {
        this.voxels = voxels;
    }

    @PostMapping("/ingest")
    public Map<String, Object> ingest(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "artworkId", required = false) String artworkId,
            @RequestParam(value = "face", defaultValue = "north") String face,
            @RequestParam(value = "t", required = false) Integer t)
            throws Exception {
        return voxels.ingestFile(file, artworkId, face, t);
    }

    @PostMapping("/folds")
    public Map<String, Object> folds(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "artworkId", defaultValue = "demo") String artworkId,
            @RequestParam(value = "face", defaultValue = "north") String face,
            @RequestParam(value = "t", required = false) Integer t)
            throws Exception {
        BufferedImage img = ImageIO.read(file.getInputStream());
        return voxels.foldsFromImage(img, artworkId, face, t);
    }

    @PostMapping("/stamp")
    public Map<String, Object> stamp(@RequestBody Map<String, Object> body) throws Exception {
        try {
            return voxels.stamp(
                    str(body, "artworkId"),
                    str(body, "face"),
                    intVal(body, "t"),
                    intVal(body, "u") == null ? 0 : intVal(body, "u"),
                    intVal(body, "v") == null ? 0 : intVal(body, "v"),
                    str(body, "colorHex"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/pixellight")
    public Map<String, Object> pixellight(
            @RequestParam String artworkId,
            @RequestParam(required = false) Integer t)
            throws Exception {
        return voxels.convertPixelLight(artworkId, t);
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Integer intVal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || "".equals(v)) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v));
    }
}
