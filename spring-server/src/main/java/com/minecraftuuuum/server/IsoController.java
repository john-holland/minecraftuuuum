package com.minecraftuuuum.server;

import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/iso")
public class IsoController {
    private final IsoService iso;

    public IsoController(IsoService iso) {
        this.iso = iso;
    }

    @PostMapping("/screenshots")
    public Map<String, Object> screenshot(
            @RequestParam(value = "artworkId", required = false) String artworkId,
            @RequestParam(value = "face", defaultValue = "north") String face,
            @RequestParam(value = "t", required = false) Integer t,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "granularity", required = false) String granularityJson)
            throws Exception {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image");
        }
        return iso.storeScreenshot(artworkId, face, t, image, parseGran(granularityJson));
    }

    @PostMapping("/points")
    public Map<String, Object> points(@RequestBody Map<String, Object> body) {
        try {
            return iso.upsertPoint(body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/points/{artworkId}")
    public Map<String, Object> listPoints(@PathVariable String artworkId) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("artworkId", artworkId);
        out.put("points", iso.listPoints(artworkId));
        return out;
    }

    @PostMapping("/points/{artworkId}/suggest-face")
    public Map<String, Object> suggest(@PathVariable String artworkId, @RequestParam(defaultValue = "16") int grid)
            throws Exception {
        return iso.suggestFaceLandmarks(artworkId, grid);
    }

    @PostMapping("/extrapolate")
    public Map<String, Object> extrapolate(@RequestBody Map<String, Object> body) {
        try {
            return iso.extrapolate(body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/join")
    public Map<String, Object> join(@RequestBody Map<String, Object> body) throws Exception {
        String artworkId = body.get("artworkId") == null ? null : String.valueOf(body.get("artworkId"));
        if (artworkId == null || artworkId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "artworkId");
        }
        Integer t = body.get("t") instanceof Number n ? n.intValue() : null;
        GranularitySettings gran = GranularitySettings.minecraft();
        if (body.get("granularity") instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> gm = (Map<String, Object>) m;
            gran = GranularitySettings.fromMap(gm);
        }
        return iso.join(artworkId, t, gran);
    }

    @PostMapping("/split-tree")
    public Map<String, Object> split(@RequestBody Map<String, Object> body) throws Exception {
        String artworkId = body.get("artworkId") == null ? null : String.valueOf(body.get("artworkId"));
        if (artworkId == null || artworkId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "artworkId");
        }
        Integer t = body.get("t") instanceof Number n ? n.intValue() : null;
        return iso.splitTree(artworkId, t);
    }

    @GetMapping("/tree/{artworkId}")
    public Map<String, Object> tree(
            @PathVariable String artworkId,
            @RequestParam(value = "t", required = false) Integer t)
            throws Exception {
        return iso.tree(artworkId, t);
    }

    @GetMapping("/slim/{artworkId}")
    public Map<String, Object> slim(
            @PathVariable String artworkId,
            @RequestParam(value = "t", required = false) Integer t)
            throws Exception {
        return iso.slim(artworkId, t);
    }

    @PostMapping("/stopmo")
    public Map<String, Object> stopmo(@RequestBody Map<String, Object> body) throws Exception {
        String artworkId = body.get("artworkId") == null ? null : String.valueOf(body.get("artworkId"));
        if (artworkId == null || artworkId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "artworkId");
        }
        Integer t = body.get("t") instanceof Number n ? n.intValue() : null;
        return iso.cacheStopmo(artworkId, t);
    }

    private static GranularitySettings parseGran(String granularityJson) {
        if (granularityJson == null || granularityJson.isBlank()) {
            return GranularitySettings.minecraft();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(granularityJson, Map.class);
            return GranularitySettings.fromMap(m);
        } catch (Exception e) {
            return GranularitySettings.minecraft();
        }
    }
}
