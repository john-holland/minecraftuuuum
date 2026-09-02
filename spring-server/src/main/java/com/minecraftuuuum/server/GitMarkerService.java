package com.minecraftuuuum.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GitMarkerService {
    public static final String MARKER_REL = "legal/unity-use.json";
    public static final String START_PREFIX = "unity-use-start-";
    public static final String BACKOUT_PREFIX = "unity-use-backout-";

    private final GitRunner git;
    private final Path configuredRoot;

    public GitMarkerService(
            GitRunner git,
            @Value("${minecraftuuuum.git-root:}") String gitRoot) {
        this.git = git;
        this.configuredRoot = gitRoot == null || gitRoot.isBlank() ? null : Path.of(gitRoot).toAbsolutePath();
    }

    public Path resolveRoot() {
        if (configuredRoot != null && Files.isDirectory(configuredRoot.resolve(".git"))) {
            return configuredRoot;
        }
        Path walk = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6 && walk != null; i++) {
            if (Files.isDirectory(walk.resolve(".git"))) {
                return walk;
            }
            walk = walk.getParent();
        }
        return configuredRoot != null ? configuredRoot : Path.of("").toAbsolutePath();
    }

    public Map<String, Object> inspect() {
        Path root = resolveRoot();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("root", root.toString());
        try {
            out.put("head", git.run(root, "rev-parse", "HEAD").trim());
            String porcelain = git.run(root, "status", "--porcelain");
            List<String> dirty = dirtyLines(porcelain);
            out.put("dirty", dirty);
            out.put("clean", dirty.isEmpty());
            out.put("ok", true);
        } catch (GitRunner.GitException e) {
            out.put("ok", false);
            out.put("error", e.getMessage());
            out.put("dirty", List.of());
            out.put("clean", false);
        }
        return out;
    }

    public Map<String, Object> markStart(String sessionId, String acknowledgmentsJson) throws GitRunner.GitException {
        return mark(sessionId, acknowledgmentsJson, "started",
                "legal: start Unity WebGL viewer use (iron-man)", START_PREFIX);
    }

    public Map<String, Object> markBackout(String sessionId, String acknowledgmentsJson) throws GitRunner.GitException {
        return mark(sessionId, acknowledgmentsJson, "backed_out",
                "legal: back out Unity WebGL viewer use", BACKOUT_PREFIX);
    }

    public List<Map<String, String>> logSince(String startTag) throws GitRunner.GitException {
        Path root = resolveRoot();
        String raw = git.run(root, "log", "--format=%H%x09%s", startTag + "..HEAD");
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            int tab = line.indexOf('\t');
            Map<String, String> row = new LinkedHashMap<>();
            if (tab < 0) {
                row.put("sha", line.trim());
                row.put("message", "");
            } else {
                row.put("sha", line.substring(0, tab).trim());
                row.put("message", line.substring(tab + 1).trim());
            }
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> mark(
            String sessionId,
            String acknowledgmentsJson,
            String status,
            String message,
            String tagPrefix)
            throws GitRunner.GitException {
        Path root = resolveRoot();
        String porcelain = git.run(root, "status", "--porcelain");
        List<String> dirty = dirtyLines(porcelain);
        if (!dirty.isEmpty()) {
            throw new GitRunner.GitException("dirty working tree; commit first: " + String.join(", ", dirty));
        }
        String stamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC).format(Instant.now());
        String tag = tagPrefix + stamp;
        Path marker = root.resolve(MARKER_REL);
        String json = """
                {
                  "sessionId": "%s",
                  "status": "%s",
                  "timestamp": "%s",
                  "acknowledgments": %s
                }
                """.formatted(
                esc(sessionId),
                esc(status),
                Instant.now().toString(),
                acknowledgmentsJson == null || acknowledgmentsJson.isBlank() ? "[]" : acknowledgmentsJson);
        try {
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GitRunner.GitException("cannot write " + MARKER_REL + ": " + e.getMessage(), e);
        }
        git.run(root, "add", "--", MARKER_REL);
        git.run(root, "commit", "-m", message);
        git.run(root, "tag", tag);
        String sha = git.run(root, "rev-parse", "HEAD").trim();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sha", sha);
        out.put("tag", tag);
        out.put("message", message);
        out.put("root", root.toString());
        return out;
    }

    static List<String> dirtyLines(String porcelain) {
        List<String> dirty = new ArrayList<>();
        if (porcelain == null || porcelain.isBlank()) {
            return dirty;
        }
        for (String line : porcelain.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String path = line.length() >= 4 ? line.substring(3).trim().replace('\\', '/') : line.trim();
            if (path.startsWith("legal/unity-use.json")) {
                continue;
            }
            dirty.add(path);
        }
        return dirty;
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
