package com.minecraftuuuum.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** SQLite {@code lvm_events} rows in {@code lvm2.0} envelope, analog of Continuuuum {@code lvm_hooks.py}. */
@Component
public class LvmEventStore {
    private final Connection conn;
    private final ObjectMapper mapper;

    public LvmEventStore(@Value("${minecraftuuuum.db}") String db, ObjectMapper mapper) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + db);
        this.mapper = mapper;
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS lvm_events (
                      id TEXT PRIMARY KEY,
                      trace_id TEXT NOT NULL,
                      event_type TEXT NOT NULL,
                      payload_json TEXT NOT NULL,
                      created_at TEXT NOT NULL
                    )""");
        }
    }

    @PreDestroy
    public void close() throws SQLException {
        conn.close();
    }

    public Map<String, Object> lvm20Event(String eventType, String traceId, Map<String, Object> meta) {
        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("schema_version", "lvm2.0");
        ev.put("id", UUID.randomUUID().toString());
        ev.put("type", eventType);
        ev.put("trace_id", traceId);
        ev.put("timestamp", Instant.now().toString());
        ev.put("meta", meta == null ? Map.of() : meta);
        return ev;
    }

    public void legalEvent(String eventType, Map<String, Object> meta) {
        String trace = "legal-unity";
        Map<String, Object> ev = lvm20Event(eventType, trace, meta);
        append(trace, List.of(ev));
    }

    public List<String> afterCaveRouteMutation(String route, String tenant, String service, String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return List.of();
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("route", route);
        meta.put("tenant", tenant);
        meta.put("service", service == null ? "minecraftuuuum" : service);
        Map<String, Object> ev = lvm20Event("cave.route.mutated", traceId, meta);
        append(traceId, List.of(ev));
        return List.of("cave.route.mutated");
    }

    public void append(String traceId, List<Map<String, Object>> events) {
        if (traceId == null || traceId.isBlank() || events == null || events.isEmpty()) {
            return;
        }
        String now = Instant.now().toString();
        try {
            for (Map<String, Object> ev : events) {
                String id = String.valueOf(ev.getOrDefault("id", UUID.randomUUID().toString()));
                String type = String.valueOf(ev.getOrDefault("type", "Unknown"));
                String json = mapper.writeValueAsString(ev);
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO lvm_events (id, trace_id, event_type, payload_json, created_at) "
                                + "VALUES (?,?,?,?,?)")) {
                    ps.setString(1, id);
                    ps.setString(2, traceId);
                    ps.setString(3, type);
                    ps.setString(4, json);
                    ps.setString(5, now);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Map<String, Object>> listByTrace(String traceId) {
        List<Map<String, Object>> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT payload_json FROM lvm_events WHERE trace_id = ? ORDER BY created_at")) {
            ps.setString(1, traceId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapper.readValue(rs.getString(1), new TypeReference<Map<String, Object>>() {}));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return out;
    }
}
