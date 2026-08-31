package com.minecraftuuuum.server;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/video-generation")
public class VideoGenerationController {
    private final VideoGenerationService gen;

    public VideoGenerationController(VideoGenerationService gen) {
        this.gen = gen;
    }

    @GetMapping("/features")
    public Map<String, Object> features() {
        return gen.features();
    }

    @GetMapping("/granularity/minecraft")
    public Map<String, Object> minecraftGranularity() {
        return GranularitySettings.minecraft().asMap();
    }

    @PostMapping("/media")
    public Map<String, Object> media(
            @RequestParam(value = "artworkId", required = false) String artworkId,
            @RequestParam(value = "t", required = false) Integer t,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "mask", required = false) MultipartFile mask,
            @RequestParam(value = "granularity", required = false) String granularityJson)
            throws Exception {
        GranularitySettings gran = GranularitySettings.minecraft();
        if (granularityJson != null && !granularityJson.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(granularityJson, Map.class);
                gran = GranularitySettings.fromMap(m);
            } catch (Exception ignored) {
                gran = GranularitySettings.minecraft();
            }
        }
        if ((image == null || image.isEmpty()) && (mask == null || mask.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image or mask");
        }
        return gen.storeMedia(artworkId, t, image, mask, gran);
    }

    @GetMapping("/media/{artworkId}")
    public Map<String, Object> listMedia(@PathVariable String artworkId) throws Exception {
        return gen.getMediaMeta(artworkId);
    }

    @GetMapping("/media/{artworkId}/{kind}")
    public ResponseEntity<byte[]> blob(
            @PathVariable String artworkId,
            @PathVariable String kind,
            @RequestParam(value = "t", defaultValue = "-1") int t)
            throws Exception {
        Map<String, Object> row = gen.getBlob(artworkId, t, kind);
        if (row == null || row.get("blob") == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "media");
        }
        String mime = row.get("mime") == null ? "application/octet-stream" : String.valueOf(row.get("mime"));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime)).body((byte[]) row.get("blob"));
    }

    @PostMapping("/modly")
    public Map<String, Object> modly(@RequestBody Map<String, Object> body) throws Exception {
        String artworkId = body.get("artworkId") == null ? null : String.valueOf(body.get("artworkId"));
        if (artworkId == null || artworkId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "artworkId");
        }
        Integer t = null;
        if (body.get("t") instanceof Number n) {
            t = n.intValue();
        }
        String prompt = body.get("prompt") == null ? null : String.valueOf(body.get("prompt"));
        String fmt = body.get("meshFormat") == null ? "glb" : String.valueOf(body.get("meshFormat"));
        Integer steps = null;
        if (body.get("steps") instanceof Number n) {
            steps = n.intValue();
        }
        return gen.invokeModly(artworkId, t, prompt, fmt, steps);
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }
}
