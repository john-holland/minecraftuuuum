package com.minecraftuuuum.server;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ServerCatalogStore {
    public static final String DEMO_SCRIPT = "{P:say|text=Minecraftuuuum!}\n"
            + "{P:place|registry-id=minecraft:oak_planks}\n"
            + "{P:spawn|registry-id=minecraft:creeper}";

    public record ConfigRow(
            String id,
            String name,
            String workingDir,
            String startCommand,
            Integer port,
            int headVersion,
            String createdAt,
            String updatedAt) {}

    public record VersionRow(
            long id,
            String configId,
            int version,
            String scriptText,
            String createdAt) {}

    public record ServerRow(
            String id,
            String name,
            String configId,
            String configName,
            long scriptVersionId,
            int scriptVersion,
            String status,
            Long pid,
            Integer port,
            String logPath,
            String error,
            String createdAt,
            String updatedAt) {}

    private final Connection conn;

    public ServerCatalogStore(@Value("${minecraftuuuum.db}") String db) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + db);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        migrate();
        seedDemo();
    }

    @PreDestroy
    void close() throws SQLException {
        conn.close();
    }

    private void migrate() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS server_configs (
                      id TEXT PRIMARY KEY,
                      name TEXT NOT NULL UNIQUE,
                      working_dir TEXT NOT NULL DEFAULT '',
                      start_command TEXT NOT NULL DEFAULT '',
                      port INTEGER,
                      head_version INTEGER NOT NULL DEFAULT 1,
                      created_at TEXT NOT NULL,
                      updated_at TEXT NOT NULL
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS script_versions (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      config_id TEXT NOT NULL,
                      version INTEGER NOT NULL,
                      script_text TEXT NOT NULL,
                      created_at TEXT NOT NULL,
                      UNIQUE(config_id, version),
                      FOREIGN KEY(config_id) REFERENCES server_configs(id)
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS active_servers (
                      id TEXT PRIMARY KEY,
                      name TEXT NOT NULL,
                      config_id TEXT NOT NULL,
                      script_version_id INTEGER NOT NULL,
                      status TEXT NOT NULL,
                      pid INTEGER,
                      port INTEGER,
                      log_path TEXT,
                      error TEXT,
                      created_at TEXT NOT NULL,
                      updated_at TEXT NOT NULL,
                      FOREIGN KEY(config_id) REFERENCES server_configs(id),
                      FOREIGN KEY(script_version_id) REFERENCES script_versions(id)
                    )""");
        }
    }

    private void seedDemo() throws SQLException {
        if (!listConfigs().isEmpty()) {
            return;
        }
        createConfig("demo", "", "", null, DEMO_SCRIPT);
    }

    public List<ConfigRow> listConfigs() {
        List<ConfigRow> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, working_dir, start_command, port, head_version, created_at, updated_at "
                        + "FROM server_configs ORDER BY name")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(readConfig(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    public ConfigRow getConfig(String id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, working_dir, start_command, port, head_version, created_at, updated_at "
                        + "FROM server_configs WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readConfig(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public ConfigRow createConfig(String name, String workingDir, String startCommand, Integer port, String script) {
        String id = "cfg_" + UUID.randomUUID().toString().substring(0, 8);
        String now = Instant.now().toString();
        String text = script == null || script.isBlank() ? DEMO_SCRIPT : script;
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO server_configs (id, name, working_dir, start_command, port, head_version, created_at, updated_at) "
                            + "VALUES (?,?,?,?,?,1,?,?)")) {
                ps.setString(1, id);
                ps.setString(2, name);
                ps.setString(3, workingDir == null ? "" : workingDir);
                ps.setString(4, startCommand == null ? "" : startCommand);
                setInt(ps, 5, port);
                ps.setString(6, now);
                ps.setString(7, now);
                ps.executeUpdate();
            }
            insertVersion(id, 1, text, now);
            return getConfig(id);
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public ConfigRow updateConfig(String id, String name, String workingDir, String startCommand, Integer port) {
        String now = Instant.now().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE server_configs SET name = ?, working_dir = ?, start_command = ?, port = ?, updated_at = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setString(2, workingDir == null ? "" : workingDir);
            ps.setString(3, startCommand == null ? "" : startCommand);
            setInt(ps, 4, port);
            ps.setString(5, now);
            ps.setString(6, id);
            if (ps.executeUpdate() == 0) {
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return getConfig(id);
    }

    public VersionRow saveScript(String configId, String script) {
        ConfigRow cfg = getConfig(configId);
        if (cfg == null) {
            return null;
        }
        String now = Instant.now().toString();
        int next = cfg.headVersion() + 1;
        try {
            VersionRow row = insertVersion(configId, next, script, now);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE server_configs SET head_version = ?, updated_at = ? WHERE id = ?")) {
                ps.setInt(1, next);
                ps.setString(2, now);
                ps.setString(3, configId);
                ps.executeUpdate();
            }
            return row;
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public List<VersionRow> listVersions(String configId) {
        List<VersionRow> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, config_id, version, script_text, created_at FROM script_versions "
                        + "WHERE config_id = ? ORDER BY version DESC")) {
            ps.setString(1, configId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(readVersion(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    public VersionRow getVersion(long id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, config_id, version, script_text, created_at FROM script_versions WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readVersion(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public VersionRow getVersionByNumber(String configId, int version) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, config_id, version, script_text, created_at FROM script_versions "
                        + "WHERE config_id = ? AND version = ?")) {
            ps.setString(1, configId);
            ps.setInt(2, version);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readVersion(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public VersionRow headVersion(String configId) {
        ConfigRow cfg = getConfig(configId);
        if (cfg == null) {
            return null;
        }
        return getVersionByNumber(configId, cfg.headVersion());
    }

    public List<ServerRow> listServers() {
        List<ServerRow> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT s.id, s.name, s.config_id, c.name AS config_name, s.script_version_id, v.version, "
                        + "s.status, s.pid, s.port, s.log_path, s.error, s.created_at, s.updated_at "
                        + "FROM active_servers s "
                        + "JOIN server_configs c ON c.id = s.config_id "
                        + "JOIN script_versions v ON v.id = s.script_version_id "
                        + "ORDER BY s.updated_at DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(readServer(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    public ServerRow getServer(String id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT s.id, s.name, s.config_id, c.name AS config_name, s.script_version_id, v.version, "
                        + "s.status, s.pid, s.port, s.log_path, s.error, s.created_at, s.updated_at "
                        + "FROM active_servers s "
                        + "JOIN server_configs c ON c.id = s.config_id "
                        + "JOIN script_versions v ON v.id = s.script_version_id "
                        + "WHERE s.id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readServer(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public ServerRow insertServer(String name, String configId, long scriptVersionId, String status, Integer port) {
        String id = "srv_" + UUID.randomUUID().toString().substring(0, 8);
        String now = Instant.now().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO active_servers (id, name, config_id, script_version_id, status, pid, port, log_path, error, created_at, updated_at) "
                        + "VALUES (?,?,?,?,?,NULL,?,NULL,NULL,?,?)")) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, configId);
            ps.setLong(4, scriptVersionId);
            ps.setString(5, status);
            setInt(ps, 6, port);
            ps.setString(7, now);
            ps.setString(8, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return getServer(id);
    }

    public ServerRow updateServer(
            String id,
            Long scriptVersionId,
            String status,
            Long pid,
            Integer port,
            String logPath,
            String error) {
        ServerRow cur = getServer(id);
        if (cur == null) {
            return null;
        }
        String now = Instant.now().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE active_servers SET script_version_id = ?, status = ?, pid = ?, port = ?, log_path = ?, error = ?, updated_at = ? "
                        + "WHERE id = ?")) {
            ps.setLong(1, scriptVersionId == null ? cur.scriptVersionId() : scriptVersionId);
            ps.setString(2, status == null ? cur.status() : status);
            if (pid == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setLong(3, pid);
            }
            setInt(ps, 4, port != null ? port : cur.port());
            ps.setString(5, logPath != null ? logPath : cur.logPath());
            ps.setString(6, error);
            ps.setString(7, now);
            ps.setString(8, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return getServer(id);
    }

    private VersionRow insertVersion(String configId, int version, String script, String now) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO script_versions (config_id, version, script_text, created_at) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, configId);
            ps.setInt(2, version);
            ps.setString(3, script);
            ps.setString(4, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                long id = keys.next() ? keys.getLong(1) : -1;
                return new VersionRow(id, configId, version, script, now);
            }
        }
    }

    private static ConfigRow readConfig(ResultSet rs) throws SQLException {
        return new ConfigRow(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("working_dir"),
                rs.getString("start_command"),
                intOrNull(rs, "port"),
                rs.getInt("head_version"),
                rs.getString("created_at"),
                rs.getString("updated_at"));
    }

    private static VersionRow readVersion(ResultSet rs) throws SQLException {
        return new VersionRow(
                rs.getLong("id"),
                rs.getString("config_id"),
                rs.getInt("version"),
                rs.getString("script_text"),
                rs.getString("created_at"));
    }

    private static ServerRow readServer(ResultSet rs) throws SQLException {
        Object pid = rs.getObject("pid");
        return new ServerRow(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("config_id"),
                rs.getString("config_name"),
                rs.getLong("script_version_id"),
                rs.getInt("version"),
                rs.getString("status"),
                pid == null ? null : ((Number) pid).longValue(),
                intOrNull(rs, "port"),
                rs.getString("log_path"),
                rs.getString("error"),
                rs.getString("created_at"),
                rs.getString("updated_at"));
    }

    private static Integer intOrNull(ResultSet rs, String col) throws SQLException {
        Object v = rs.getObject(col);
        return v == null ? null : ((Number) v).intValue();
    }

    private static void setInt(PreparedStatement ps, int idx, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.INTEGER);
        } else {
            ps.setInt(idx, value);
        }
    }
}
