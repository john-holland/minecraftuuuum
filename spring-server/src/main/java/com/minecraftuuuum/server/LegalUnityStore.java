package com.minecraftuuuum.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LegalUnityStore {
    private final Connection conn;
    private final ObjectMapper mapper;

    public LegalUnityStore(@Value("${minecraftuuuum.db}") String db, ObjectMapper mapper) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + db);
        this.mapper = mapper;
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS legal_settings (
                      id INTEGER PRIMARY KEY CHECK (id = 1),
                      iron_man INTEGER NOT NULL DEFAULT 1,
                      display_mode TEXT NOT NULL DEFAULT 'web',
                      updated_at TEXT NOT NULL
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS legal_unity_sessions (
                      id TEXT PRIMARY KEY,
                      mode TEXT NOT NULL,
                      display_mode TEXT NOT NULL,
                      status TEXT NOT NULL,
                      acknowledgments_json TEXT,
                      license_requirements_json TEXT,
                      start_sha TEXT,
                      start_tag TEXT,
                      start_commit_message TEXT,
                      backed_out_at TEXT,
                      backed_out_sha TEXT,
                      backed_out_tag TEXT,
                      created_at TEXT NOT NULL,
                      updated_at TEXT NOT NULL
                    )""");
            st.execute("""
                    INSERT OR IGNORE INTO legal_settings (id, iron_man, display_mode, updated_at)
                    VALUES (1, 1, 'web', datetime('now'))
                    """);
        }
    }

    @PreDestroy
    public void close() throws SQLException {
        conn.close();
    }

    public Map<String, Object> settings() {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT iron_man, display_mode, updated_at FROM legal_settings WHERE id = 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Object> out = new LinkedHashMap<>();
                if (!rs.next()) {
                    out.put("ironMan", true);
                    out.put("displayMode", "web");
                    return out;
                }
                out.put("ironMan", rs.getInt("iron_man") == 1);
                out.put("displayMode", rs.getString("display_mode"));
                out.put("updatedAt", rs.getString("updated_at"));
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setIronMan(boolean on) {
        updateSettings(on, null);
    }

    public void setDisplayMode(String mode) {
        updateSettings(null, mode);
    }

    private void updateSettings(Boolean ironMan, String displayMode) {
        String now = Instant.now().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                """
                UPDATE legal_settings SET
                  iron_man = COALESCE(?, iron_man),
                  display_mode = COALESCE(?, display_mode),
                  updated_at = ?
                WHERE id = 1
                """)) {
            if (ironMan == null) {
                ps.setObject(1, null);
            } else {
                ps.setInt(1, ironMan ? 1 : 0);
            }
            ps.setString(2, displayMode);
            ps.setString(3, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Map<String, Object> activeSession() {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM legal_unity_sessions WHERE status = 'active' ORDER BY created_at DESC LIMIT 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? row(rs) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Map<String, Object> latestSession() {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM legal_unity_sessions ORDER BY created_at DESC LIMIT 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? row(rs) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void insertSession(Map<String, Object> session) {
        String now = Instant.now().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO legal_unity_sessions (
                  id, mode, display_mode, status, acknowledgments_json, license_requirements_json,
                  start_sha, start_tag, start_commit_message, created_at, updated_at
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setString(1, str(session, "id"));
            ps.setString(2, str(session, "mode"));
            ps.setString(3, str(session, "displayMode"));
            ps.setString(4, str(session, "status"));
            ps.setString(5, str(session, "acknowledgmentsJson"));
            ps.setString(6, str(session, "licenseRequirementsJson"));
            ps.setString(7, str(session, "startSha"));
            ps.setString(8, str(session, "startTag"));
            ps.setString(9, str(session, "startCommitMessage"));
            ps.setString(10, now);
            ps.setString(11, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void backOut(String id, String sha, String tag) {
        String now = Instant.now().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                """
                UPDATE legal_unity_sessions SET
                  status = 'backed_out',
                  backed_out_at = ?,
                  backed_out_sha = ?,
                  backed_out_tag = ?,
                  updated_at = ?
                WHERE id = ?
                """)) {
            ps.setString(1, now);
            ps.setString(2, sha);
            ps.setString(3, tag);
            ps.setString(4, now);
            ps.setString(5, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> row(ResultSet rs) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", rs.getString("id"));
        out.put("mode", rs.getString("mode"));
        out.put("displayMode", rs.getString("display_mode"));
        out.put("status", rs.getString("status"));
        String acks = rs.getString("acknowledgments_json");
        out.put("acknowledgments", acks == null || acks.isBlank()
                ? List.of()
                : mapper.readValue(acks, new TypeReference<Object>() {}));
        out.put("startSha", rs.getString("start_sha"));
        out.put("startTag", rs.getString("start_tag"));
        out.put("startCommitMessage", rs.getString("start_commit_message"));
        out.put("backedOutAt", rs.getString("backed_out_at"));
        out.put("backedOutSha", rs.getString("backed_out_sha"));
        out.put("backedOutTag", rs.getString("backed_out_tag"));
        out.put("createdAt", rs.getString("created_at"));
        return out;
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
