package com.minecraftuuuum.server;

import com.minecraftuuuum.lemma.ActorCatalog;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/video-animation")
public class VideoAnimationController {
    private final VideoAnimationService video;

    public VideoAnimationController(VideoAnimationService video) {
        this.video = video;
    }

    @GetMapping("/actors")
    public List<ActorCatalog.Actor> actors() {
        return ActorCatalog.all();
    }

    @GetMapping
    public List<VideoAnimationService.Recording> list() {
        return video.list();
    }

    @GetMapping("/{id}")
    public VideoAnimationService.Recording get(@PathVariable String id) {
        VideoAnimationService.Recording rec = video.get(id);
        if (rec == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "recording");
        }
        return rec;
    }

    @GetMapping("/{id}/frames/{t}")
    public ResponseEntity<byte[]> frame(@PathVariable String id, @PathVariable int t) throws Exception {
        Path path = video.framePath(id, t);
        if (path == null || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "frame");
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(Files.readAllBytes(path));
    }

    @PostMapping
    public VideoAnimationService.Recording create(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "kind", defaultValue = "ambulatory") String kind,
            @RequestParam(value = "actorId", defaultValue = "minecraft:player") String actorId,
            @RequestParam(value = "detectorProfile", required = false) String detectorProfile,
            @RequestParam(value = "durationSec", required = false) Double durationSec,
            @RequestParam(value = "extractFps", required = false) Double extractFps)
            throws Exception {
        return video.enqueue(file, kind, actorId, detectorProfile, durationSec, extractFps);
    }

    @PostMapping("/live-pose")
    public Map<String, Object> livePose(@RequestParam(value = "profile", required = false) String profile) {
        return video.livePose(profile);
    }

    @GetMapping("/models")
    public Map<String, Object> models() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("detectors", List.of(
                Map.of("id", "mediapipe_holistic@v1", "label", "MediaPipe Holistic (humanoid)"),
                Map.of("id", "mocapanything@v2", "label", "MoCapAnything v2 (non-human)"),
                Map.of("id", "root-motion", "label", "Root-motion / voxel-only")));
        return out;
    }
}
