package com.minecraftuuuum.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalUnityServiceTest {
    @TempDir
    Path tmp;

    @Test
    void ironManBlocksInstallWithoutAcks() throws Exception {
        Fixture f = service(false);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("acknowledgments", List.of());
            body.put("unityTier", "personal");
            assertThrows(IllegalStateException.class, () -> f.svc.install(body));
        } finally {
            f.close();
        }
    }

    @Test
    void dirtyTreeRefusesMarker() throws Exception {
        Fixture f = service(true);
        try {
            assertThrows(Exception.class, () -> f.svc.install(allAcks()));
        } finally {
            f.close();
        }
    }

    @Test
    void installWithAcksCreatesActiveSession() throws Exception {
        Fixture f = service(false);
        try {
            Map<String, Object> out = f.svc.install(allAcks());
            assertEquals("unity_webgl", out.get("displayMode"));
            assertTrue(Boolean.TRUE.equals(out.get("unityAllowed")));
            @SuppressWarnings("unchecked")
            Map<String, Object> session = (Map<String, Object>) out.get("session");
            assertEquals("active", session.get("status"));
            assertTrue(String.valueOf(session.get("startTag")).startsWith("unity-use-start-"));
        } finally {
            f.close();
        }
    }

    @Test
    void dirtyLinesIgnoreUnityMarker() {
        List<String> dirty = GitMarkerService.dirtyLines(" M src/Foo.java\n M legal/unity-use.json\n");
        assertEquals(List.of("src/Foo.java"), dirty);
    }

    private static Map<String, Object> allAcks() {
        List<LicenseRequirements.Item> req = LicenseRequirements.required("unity_webgl", false, false, "personal");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("acknowledgments", req.stream().map(LicenseRequirements.Item::id).toList());
        body.put("unityTier", "personal");
        return body;
    }

    private Fixture service(boolean dirty) throws Exception {
        Path db = tmp.resolve("legal-" + (dirty ? "d" : "c") + ".db");
        ObjectMapper mapper = new ObjectMapper();
        LegalUnityStore store = new LegalUnityStore(db.toString(), mapper);
        LvmEventStore lvm = new LvmEventStore(db.toString(), mapper);
        FakeGit git = new FakeGit(dirty);
        GitMarkerService markers = new GitMarkerService(git, tmp.toString());
        return new Fixture(new LegalUnityService(store, markers, lvm, mapper,
                "http://127.0.0.1:5050/continuuuum_editor/index.html"), store, lvm);
    }

    private record Fixture(LegalUnityService svc, LegalUnityStore store, LvmEventStore lvm) {
        void close() throws Exception {
            store.close();
            lvm.close();
        }
    }

    private static final class FakeGit implements GitRunner {
        private final boolean dirty;
        private String head = "aaa111";
        private final List<String> tags = new ArrayList<>();

        FakeGit(boolean dirty) {
            this.dirty = dirty;
        }

        @Override
        public String run(Path cwd, String... args) throws GitException {
            if (args.length == 0) {
                return "";
            }
            if ("status".equals(args[0])) {
                return dirty ? " M src/Foo.java\n" : "";
            }
            if ("rev-parse".equals(args[0])) {
                return head + "\n";
            }
            if ("commit".equals(args[0])) {
                head = "bbb222";
                return "";
            }
            if ("tag".equals(args[0]) && args.length > 1) {
                tags.add(args[1]);
                return "";
            }
            if ("log".equals(args[0])) {
                return head + "\tlegal: start Unity WebGL viewer use (iron-man)\n";
            }
            if ("add".equals(args[0])) {
                return "";
            }
            return "";
        }
    }
}
