package com.minecraftuuuum.server;

import com.minecraftuuuum.lemma.ActorCatalog;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class VideoAnimationService {
    public record Recording(
            String id,
            String kind,
            String actorId,
            String poseEngine,
            String species,
            String fileName,
            long fileBytes,
            double durationSec,
            double extractFps,
            double sourceFps,
            int expectedFrames,
            int extractedCount,
            int ingestedCount,
            String status,
            String error,
            String clipSummary,
            List<Map<String, Object>> frames) {}

    private static final class Job {
        final String id;
        final String kind;
        final String actorId;
        final String poseEngine;
        final String species;
        volatile String fileName = "";
        volatile long fileBytes;
        volatile double durationSec;
        volatile double extractFps;
        volatile double sourceFps;
        volatile int expectedFrames;
        volatile int extractedCount;
        volatile int ingestedCount;
        volatile String status = "queued";
        volatile String error;
        volatile String clipSummary = "";
        final List<Map<String, Object>> frames = new CopyOnWriteArrayList<>();

        Job(String id, String kind, String actorId, String poseEngine, String species) {
            this.id = id;
            this.kind = kind;
            this.actorId = actorId;
            this.poseEngine = poseEngine;
            this.species = species;
        }

        Recording snapshot() {
            List<Map<String, Object>> copy = new ArrayList<>(frames.size());
            for (Map<String, Object> row : frames) {
                copy.add(new LinkedHashMap<>(row));
            }
            return new Recording(
                    id, kind, actorId, poseEngine, species,
                    fileName, fileBytes, durationSec, extractFps, sourceFps,
                    expectedFrames, extractedCount, ingestedCount,
                    status, error, clipSummary, List.copyOf(copy));
        }
    }

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<String> order = new ConcurrentLinkedDeque<>();
    private final Map<String, List<Path>> frameFiles = new ConcurrentHashMap<>();
    private final VoxelService voxels;
    private final ExecutorService ingestPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "video-anim-ingest");
        t.setDaemon(true);
        return t;
    });

    @Value("${minecraftuuuum.mocap-root}")
    private String mocapRoot;

    public VideoAnimationService(VoxelService voxels) {
        this.voxels = voxels;
    }

    @PreDestroy
    void shutdown() {
        ingestPool.shutdownNow();
    }

    public Recording enqueue(
            MultipartFile file,
            String kind,
            String actorId,
            String detectorProfile,
            Double clientDurationSec,
            Double extractFps)
            throws Exception {
        String id = "rec_" + UUID.randomUUID().toString().substring(0, 8);
        ActorCatalog.Actor actor = ActorCatalog.byId(actorId == null ? "minecraft:player" : actorId);
        String engine = detectorProfile != null && !detectorProfile.isBlank()
                ? detectorProfile
                : (actor == null ? "mediapipe_holistic@v1" : actor.poseEngine());
        String species = actor == null ? null : actor.species();
        Job job = new Job(id, kind, actor == null ? actorId : actor.registryId(), engine, species);
        job.status = "uploading";
        String originalName = file.getOriginalFilename() == null ? "clip.webm" : file.getOriginalFilename();
        job.fileName = originalName;
        job.fileBytes = file.getSize();
        jobs.put(id, job);
        order.addFirst(id);

        Path dir = Files.createTempDirectory("va-" + id);
        Path video = dir.resolve(originalName.replaceAll("[^a-zA-Z0-9._-]", "_"));
        file.transferTo(video.toFile());
        job.status = "extracting";
        job.clipSummary = originalName + " · extracting…";

        ingestPool.execute(() -> runJob(job, video, dir, originalName, clientDurationSec, extractFps));
        return job.snapshot();
    }

    public Recording get(String id) {
        Job job = jobs.get(id);
        return job == null ? null : job.snapshot();
    }

    public List<Recording> list() {
        List<Recording> out = new ArrayList<>();
        for (String id : order) {
            Job job = jobs.get(id);
            if (job != null) {
                out.add(job.snapshot());
            }
        }
        return out;
    }

    public Map<String, Object> livePose(String profile) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("engine", profile == null ? "mediapipe_holistic@v1" : profile);
        out.put("hint", "Install mediapipe in detector env; weights are not vendored");
        out.put("landmarks", List.of());
        return out;
    }

    public Path framePath(String id, int t) {
        List<Path> files = frameFiles.get(id);
        if (files == null || t < 0 || t >= files.size()) {
            return null;
        }
        return files.get(t);
    }

    private void runJob(
            Job job,
            Path video,
            Path dir,
            String originalName,
            Double clientDurationSec,
            Double extractFps) {
        try {
            VideoClipMeta meta = VideoClipMeta.probe(video, originalName, job.fileBytes, clientDurationSec, extractFps);
            job.fileName = meta.fileName();
            job.fileBytes = meta.fileBytes();
            job.durationSec = meta.durationSec();
            job.extractFps = meta.extractFps();
            job.sourceFps = meta.sourceFps();
            job.expectedFrames = meta.expectedFrames();
            job.clipSummary = meta.summary();

            List<Path> extracted = extractFrames(video, dir, meta.extractFps());
            frameFiles.put(job.id, extracted);
            job.extractedCount = extracted.size();
            for (int i = 0; i < extracted.size(); i++) {
                job.frames.add(pendingRow(job, i, meta.extractFps()));
            }
            job.status = "ingesting";

            for (int i = 0; i < extracted.size(); i++) {
                if (Thread.currentThread().isInterrupted()) {
                    job.status = "error";
                    job.error = "interrupted";
                    return;
                }
                Path frame = extracted.get(i);
                Map<String, Object> voxel = voxels.ingestPath(frame, job.id, "north", i);
                BufferedImage img = ImageIO.read(frame.toFile());
                Map<String, Object> folds = voxels.foldsFromImage(img, job.id, "north", i);
                Map<String, Object> pose = poseHop(job.poseEngine, job.species, frame, i);
                Map<String, Object> row = pendingRow(job, i, meta.extractFps());
                row.put("voxel", voxel);
                row.put("folds", folds);
                row.put("pose", pose);
                row.put("voxelStatus", "stored");
                job.frames.set(i, row);
                job.ingestedCount = i + 1;
            }
            job.status = "done";
        } catch (Exception e) {
            job.status = "error";
            job.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
    }

    private static Map<String, Object> pendingRow(Job job, int t, double extractFps) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("t", t);
        row.put("timeSec", extractFps > 0 ? t / extractFps : t);
        row.put("thumbnail", "/api/video-animation/" + job.id + "/frames/" + t);
        row.put("voxelStatus", "pending");
        return row;
    }

    private Map<String, Object> poseHop(String engine, String species, Path frame, int t) {
        Map<String, Object> pose = new LinkedHashMap<>();
        pose.put("t", t);
        pose.put("engine", engine);
        pose.put("species", species);
        if (engine.startsWith("mediapipe")) {
            pose.put("hint", "install mediapipe (Python 3.12). Do not vendor .tflite");
            pose.put("bones", List.of("Head", "Hips", "LeftFoot", "RightFoot"));
        } else if (engine.startsWith("mocap")) {
            pose.put("hint", "MoCapAnything v2 at " + mocapRoot + " (video2pose2rot). Weights not vendored");
            pose.put("format", "bvh/npy");
        } else {
            pose.put("hint", "root-motion / bounding-box ambulation + voxel-only");
        }
        return pose;
    }

    private List<Path> extractFrames(Path video, Path dir, double fps) throws Exception {
        Path outPattern = dir.resolve("frame_%04d.png");
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-i", video.toAbsolutePath().toString(),
                "-vf", "fps=" + fps,
                outPattern.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            p.getInputStream().readAllBytes();
            p.waitFor();
        } catch (Exception ignored) {
            // no ffmpeg
        }
        List<Path> frames = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(p -> {
                        String n = p.getFileName().toString();
                        return n.startsWith("frame_") && n.endsWith(".png");
                    })
                    .sorted()
                    .forEach(frames::add);
        }
        if (frames.isEmpty()) {
            Path still = dir.resolve("frame_0001.png");
            if (video.getFileName().toString().matches("(?i).*\\.(png|jpg|jpeg|gif|bmp)")) {
                Files.copy(video, still);
            } else {
                BufferedImage ph = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                ImageIO.write(ph, "png", still.toFile());
            }
            frames.add(still);
        }
        return frames;
    }
}
