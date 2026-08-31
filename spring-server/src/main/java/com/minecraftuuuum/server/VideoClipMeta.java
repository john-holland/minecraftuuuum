package com.minecraftuuuum.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Duration / size / fps from ffprobe, with client values as fallback. */
public record VideoClipMeta(
        String fileName,
        long fileBytes,
        double durationSec,
        double sourceFps,
        double extractFps,
        int expectedFrames) {

    private static final Pattern DURATION = Pattern.compile("duration=([0-9.]+)");
    private static final Pattern FPS = Pattern.compile("r_frame_rate=(\\d+)(?:/(\\d+))?");

    public static VideoClipMeta probe(Path video, String fileName, long fileBytes, Double clientDurationSec, Double extractFps) {
        double fps = extractFps == null || extractFps <= 0 ? 16.0 : extractFps;
        double duration = clientDurationSec == null ? 0 : clientDurationSec;
        double srcFps = 0;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe", "-v", "error",
                    "-show_entries", "format=duration,size",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=r_frame_rate,nb_frames",
                    "-of", "default=noprint_wrappers=1",
                    video.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            Matcher dm = DURATION.matcher(out);
            if (dm.find()) {
                duration = Double.parseDouble(dm.group(1));
            }
            Matcher fm = FPS.matcher(out);
            if (fm.find()) {
                double num = Double.parseDouble(fm.group(1));
                double den = fm.group(2) == null ? 1 : Double.parseDouble(fm.group(2));
                if (den != 0) {
                    srcFps = num / den;
                }
            }
            if (fileBytes <= 0) {
                Matcher sm = Pattern.compile("size=(\\d+)").matcher(out);
                if (sm.find()) {
                    fileBytes = Long.parseLong(sm.group(1));
                }
            }
        } catch (Exception ignored) {
            // ffprobe optional
        }
        if (fileBytes <= 0) {
            try {
                fileBytes = Files.size(video);
            } catch (Exception ignored) {
                fileBytes = 0;
            }
        }
        int expected = duration > 0 ? Math.max(1, (int) Math.round(duration * fps)) : 0;
        String name = fileName == null || fileName.isBlank() ? video.getFileName().toString() : fileName;
        return new VideoClipMeta(name, fileBytes, duration, srcFps, fps, expected);
    }

    public String summary() {
        return String.format(
                Locale.US,
                "%s · %s · %.2fs · extract %.0f fps · ~%d frames%s",
                fileName,
                humanSize(fileBytes),
                durationSec,
                extractFps,
                expectedFrames,
                sourceFps > 0 ? String.format(Locale.US, " (source %.2f fps)", sourceFps) : "");
    }

    static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
