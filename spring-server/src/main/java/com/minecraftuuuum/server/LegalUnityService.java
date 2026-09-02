package com.minecraftuuuum.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LegalUnityService {
    private final LegalUnityStore store;
    private final GitMarkerService git;
    private final LvmEventStore lvm;
    private final ObjectMapper mapper;
    private final String webglPreview;

    public LegalUnityService(
            LegalUnityStore store,
            GitMarkerService git,
            LvmEventStore lvm,
            ObjectMapper mapper,
            @Value("${minecraftuuuum.webgl-preview:http://127.0.0.1:5050/continuuuum_editor/index.html}")
            String webglPreview) {
        this.store = store;
        this.git = git;
        this.lvm = lvm;
        this.mapper = mapper;
        this.webglPreview = webglPreview;
    }

    public Map<String, Object> status() {
        Map<String, Object> settings = store.settings();
        Map<String, Object> session = store.activeSession();
        Map<String, Object> out = new LinkedHashMap<>(settings);
        out.put("session", session);
        out.put("latestSession", store.latestSession());
        out.put("webglPreview", webglPreview);
        out.put("webglBuild", getClass().getResource("/static/continuuuum_editor/Build") != null);
        out.put("unityHub", detectUnityHub());
        out.put("git", git.inspect());
        out.put("unityAllowed", session != null && "active".equals(session.get("status")));
        return out;
    }

    public Map<String, Object> setIronMan(boolean on) {
        store.setIronMan(on);
        lvm.legalEvent("legal.mode.flipped", Map.of("ironMan", on));
        return status();
    }

    public Map<String, Object> setDisplayMode(String mode) {
        String m = "unity_webgl".equals(mode) ? "unity_webgl" : "web";
        if ("unity_webgl".equals(m)) {
            Map<String, Object> session = store.activeSession();
            Map<String, Object> settings = store.settings();
            boolean iron = Boolean.TRUE.equals(settings.get("ironMan"));
            if (iron && session == null) {
                throw new IllegalStateException("Iron Man: install Unity WebGL viewer and acknowledge licenses first");
            }
        }
        store.setDisplayMode(m);
        lvm.legalEvent("legal.display.mode.changed", Map.of("displayMode", m));
        return status();
    }

    public Map<String, Object> requirements(Map<String, Object> body) {
        String display = body.get("displayMode") == null ? "web" : String.valueOf(body.get("displayMode"));
        boolean redistribute = bool(body.get("willRedistributeWebgl"));
        boolean publish = bool(body.get("willPublishMinecraft"));
        String tier = body.get("unityTier") == null ? "none" : String.valueOf(body.get("unityTier"));
        List<LicenseRequirements.Item> items = LicenseRequirements.required(display, redistribute, publish, tier);
        Map<String, Object> out = LicenseRequirements.asMap(items);
        out.put("displayMode", display);
        out.put("willRedistributeWebgl", redistribute);
        out.put("willPublishMinecraft", publish);
        out.put("unityTier", tier);
        return out;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> install(Map<String, Object> body) throws Exception {
        Map<String, Object> settings = store.settings();
        boolean iron = Boolean.TRUE.equals(settings.get("ironMan"));
        if (store.activeSession() != null) {
            throw new IllegalStateException("Unity session already active — back out first");
        }
        String display = "unity_webgl";
        boolean redistribute = bool(body.get("willRedistributeWebgl"));
        boolean publish = bool(body.get("willPublishMinecraft"));
        String tier = body.get("unityTier") == null ? "none" : String.valueOf(body.get("unityTier"));
        List<LicenseRequirements.Item> required = LicenseRequirements.required(display, redistribute, publish, tier);
        List<String> acks = new ArrayList<>();
        Object raw = body.get("acknowledgments");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                acks.add(String.valueOf(o));
            }
        }
        List<String> missing = LicenseRequirements.missing(required, acks);
        if (iron && !missing.isEmpty()) {
            throw new IllegalStateException("Iron Man: missing acknowledgments " + missing);
        }
        String sessionId = "lu_" + UUID.randomUUID().toString().substring(0, 8);
        String ackJson = mapper.writeValueAsString(acks);
        String reqJson = mapper.writeValueAsString(LicenseRequirements.asMap(required));
        Map<String, Object> marker;
        if (iron) {
            marker = git.markStart(sessionId, ackJson);
        } else {
            marker = new LinkedHashMap<>();
            marker.put("sha", git.inspect().get("head"));
            marker.put("tag", null);
            marker.put("message", "iron-man off — SHA recorded only");
        }
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("id", sessionId);
        session.put("mode", iron ? "iron_man" : "relaxed");
        session.put("displayMode", display);
        session.put("status", "active");
        session.put("acknowledgmentsJson", ackJson);
        session.put("licenseRequirementsJson", reqJson);
        session.put("startSha", marker.get("sha"));
        session.put("startTag", marker.get("tag"));
        session.put("startCommitMessage", marker.get("message"));
        store.insertSession(session);
        store.setDisplayMode(display);
        lvm.legalEvent("legal.unity.use.started", Map.of(
                "sessionId", sessionId,
                "startSha", String.valueOf(marker.get("sha")),
                "startTag", String.valueOf(marker.get("tag"))));
        lvm.legalEvent("legal.ack.recorded", Map.of("acknowledgments", acks));
        Map<String, Object> out = status();
        out.put("installHint",
                "Export a Unity WebGL player into static/continuuuum_editor/Build or set MINECRAFTUUUUM_WEBGL. Do not download Unity from this app.");
        return out;
    }

    public Map<String, Object> backOut() throws Exception {
        Map<String, Object> session = store.activeSession();
        if (session == null) {
            throw new IllegalStateException("no active Unity session");
        }
        String id = String.valueOf(session.get("id"));
        Map<String, Object> marker;
        try {
            marker = git.markBackout(id, "[]");
        } catch (GitRunner.GitException e) {
            marker = new LinkedHashMap<>();
            marker.put("sha", git.inspect().get("head"));
            marker.put("tag", null);
            marker.put("error", e.getMessage());
        }
        store.backOut(id, marker.get("sha") == null ? null : String.valueOf(marker.get("sha")),
                marker.get("tag") == null ? null : String.valueOf(marker.get("tag")));
        store.setDisplayMode("web");
        lvm.legalEvent("legal.unity.use.backed_out", Map.of(
                "sessionId", id,
                "startTag", session.get("startTag"),
                "backoutTag", marker.get("tag")));
        Map<String, Object> out = status();
        out.put("backout", marker);
        if (session.get("startTag") != null) {
            try {
                out.put("commitLog", git.logSince(String.valueOf(session.get("startTag"))));
            } catch (GitRunner.GitException e) {
                out.put("commitLogError", e.getMessage());
            }
        }
        return out;
    }

    public Map<String, Object> commitLog() throws GitRunner.GitException {
        Map<String, Object> session = store.latestSession();
        Map<String, Object> out = new LinkedHashMap<>();
        if (session == null || session.get("startTag") == null) {
            out.put("commits", List.of());
            out.put("hint", "no Unity start tag yet");
            return out;
        }
        String tag = String.valueOf(session.get("startTag"));
        out.put("startTag", tag);
        out.put("startSha", session.get("startSha"));
        out.put("commits", git.logSince(tag));
        return out;
    }

    public static Map<String, Object> detectUnityHub() {
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> found = new ArrayList<>();
        String local = System.getenv("LOCALAPPDATA");
        String pf = System.getenv("ProgramFiles");
        List<Path> candidates = new ArrayList<>();
        if (pf != null) {
            candidates.add(Path.of(pf, "Unity Hub", "Unity Hub.exe"));
            candidates.add(Path.of(pf, "Unity", "Hub", "Editor"));
        }
        if (local != null) {
            candidates.add(Path.of(local, "UnityHub"));
        }
        candidates.add(Path.of(System.getProperty("user.home"), "Unity", "Hub"));
        for (Path p : candidates) {
            if (Files.exists(p)) {
                found.add(p.toString());
            }
        }
        out.put("found", found);
        out.put("present", !found.isEmpty());
        out.put("docs", "https://docs.unity3d.com/Manual/webgl.html");
        return out;
    }

    private static boolean bool(Object v) {
        if (v instanceof Boolean b) {
            return b;
        }
        return v != null && Boolean.parseBoolean(String.valueOf(v));
    }
}
