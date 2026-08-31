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

@Component
public class LemmaImplementationStore {
    public record Row(
            long id,
            String term,
            String entryId,
            String posTag,
            String kind,
            boolean isBuiltin,
            boolean isImplemented,
            boolean hasNeoForgeHandler,
            String voxelArtworkId,
            String voxelFace,
            Integer voxelT,
            String featuresJson,
            String registryId,
            String poseEngine,
            String skeletonKind,
            String updatedAt) {}

    private final Connection conn;

    public LemmaImplementationStore(@Value("${minecraftuuuum.db}") String db) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + db);
        migrate();
    }

    @PreDestroy
    void close() throws SQLException {
        conn.close();
    }

    private void migrate() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS lemma_implementation (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      term TEXT NOT NULL,
                      entry_id TEXT NOT NULL UNIQUE,
                      pos_tag TEXT,
                      kind TEXT,
                      is_builtin INTEGER NOT NULL DEFAULT 0,
                      is_implemented INTEGER NOT NULL DEFAULT 0,
                      has_neoforge_handler INTEGER NOT NULL DEFAULT 0,
                      voxel_artwork_id TEXT,
                      voxel_face TEXT,
                      voxel_t INTEGER,
                      features_json TEXT NOT NULL DEFAULT '[]',
                      registry_id TEXT,
                      pose_engine TEXT,
                      skeleton_kind TEXT,
                      updated_at TEXT NOT NULL
                    )""");
        }
    }

    public int count() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM lemma_implementation")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Row get(long id) {
        try (PreparedStatement ps = conn.prepareStatement(selectSql() + " WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? read(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Row getByEntryId(String entryId) {
        try (PreparedStatement ps = conn.prepareStatement(selectSql() + " WHERE entry_id = ?")) {
            ps.setString(1, entryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? read(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Row> list(String q, String kind, Boolean implemented) {
        StringBuilder sql = new StringBuilder(selectSql()).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            sql.append(" AND (term LIKE ? OR registry_id LIKE ? OR entry_id LIKE ?)");
            String like = "%" + q.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (kind != null && !kind.isBlank()) {
            sql.append(" AND kind = ?");
            args.add(kind);
        }
        if (implemented != null) {
            sql.append(" AND is_implemented = ?");
            args.add(implemented ? 1 : 0);
        }
        sql.append(" ORDER BY term");
        List<Row> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                Object a = args.get(i);
                if (a instanceof Integer n) {
                    ps.setInt(i + 1, n);
                } else {
                    ps.setString(i + 1, String.valueOf(a));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(read(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    public void insertNew(
            String term,
            String entryId,
            String posTag,
            String kind,
            boolean builtin,
            boolean implemented,
            boolean handler,
            String featuresJson,
            String registryId,
            String poseEngine,
            String skeletonKind) {
        String now = Instant.now().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO lemma_implementation
                  (term, entry_id, pos_tag, kind, is_builtin, is_implemented, has_neoforge_handler,
                   features_json, registry_id, pose_engine, skeleton_kind, updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setString(1, term);
            ps.setString(2, entryId);
            ps.setString(3, posTag);
            ps.setString(4, kind);
            ps.setInt(5, builtin ? 1 : 0);
            ps.setInt(6, implemented ? 1 : 0);
            ps.setInt(7, handler ? 1 : 0);
            ps.setString(8, featuresJson == null ? "[]" : featuresJson);
            ps.setString(9, registryId);
            ps.setString(10, poseEngine);
            ps.setString(11, skeletonKind);
            ps.setString(12, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void updateSeedFields(
            long id,
            String term,
            String posTag,
            String kind,
            boolean builtin,
            boolean handler,
            String registryId,
            String poseEngine,
            String skeletonKind) {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                UPDATE lemma_implementation SET
                  term = ?, pos_tag = ?, kind = ?, is_builtin = ?, has_neoforge_handler = ?,
                  registry_id = ?, pose_engine = ?, skeleton_kind = ?, updated_at = ?
                WHERE id = ?
                """)) {
            ps.setString(1, term);
            ps.setString(2, posTag);
            ps.setString(3, kind);
            ps.setInt(4, builtin ? 1 : 0);
            ps.setInt(5, handler ? 1 : 0);
            ps.setString(6, registryId);
            ps.setString(7, poseEngine);
            ps.setString(8, skeletonKind);
            ps.setString(9, Instant.now().toString());
            ps.setLong(10, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Row patch(
            long id,
            Boolean implemented,
            String voxelArtworkId,
            String voxelFace,
            Integer voxelT,
            boolean clearT,
            String featuresJson) {
        Row cur = get(id);
        if (cur == null) {
            return null;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                """
                UPDATE lemma_implementation SET
                  is_implemented = ?, voxel_artwork_id = ?, voxel_face = ?, voxel_t = ?,
                  features_json = ?, updated_at = ?
                WHERE id = ?
                """)) {
            ps.setInt(1, implemented == null ? (cur.isImplemented() ? 1 : 0) : (implemented ? 1 : 0));
            ps.setString(2, voxelArtworkId != null ? voxelArtworkId : cur.voxelArtworkId());
            ps.setString(3, voxelFace != null ? voxelFace : cur.voxelFace());
            Integer t = clearT ? null : (voxelT != null ? voxelT : cur.voxelT());
            if (t == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, t);
            }
            ps.setString(5, featuresJson != null ? featuresJson : cur.featuresJson());
            ps.setString(6, Instant.now().toString());
            ps.setLong(7, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return get(id);
    }

    public int[] summaryCounts() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     """
                     SELECT COUNT(*),
                            SUM(is_builtin),
                            SUM(is_implemented),
                            SUM(has_neoforge_handler),
                            SUM(CASE WHEN voxel_artwork_id IS NOT NULL AND voxel_artwork_id != '' THEN 1 ELSE 0 END)
                     FROM lemma_implementation
                     """)) {
            if (!rs.next()) {
                return new int[] {0, 0, 0, 0, 0};
            }
            return new int[] {
                    rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5)
            };
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String selectSql() {
        return "SELECT id, term, entry_id, pos_tag, kind, is_builtin, is_implemented, has_neoforge_handler, "
                + "voxel_artwork_id, voxel_face, voxel_t, features_json, registry_id, pose_engine, skeleton_kind, updated_at "
                + "FROM lemma_implementation";
    }

    private static Row read(ResultSet rs) throws SQLException {
        Object t = rs.getObject("voxel_t");
        return new Row(
                rs.getLong("id"),
                rs.getString("term"),
                rs.getString("entry_id"),
                rs.getString("pos_tag"),
                rs.getString("kind"),
                rs.getInt("is_builtin") != 0,
                rs.getInt("is_implemented") != 0,
                rs.getInt("has_neoforge_handler") != 0,
                rs.getString("voxel_artwork_id"),
                rs.getString("voxel_face"),
                t == null ? null : ((Number) t).intValue(),
                rs.getString("features_json"),
                rs.getString("registry_id"),
                rs.getString("pose_engine"),
                rs.getString("skeleton_kind"),
                rs.getString("updated_at"));
    }
}
