package com.minecraftuuuum.server;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServerProcessService {
    private final ServerCatalogStore store;
    private final Map<String, Process> live = new ConcurrentHashMap<>();

    public ServerProcessService(ServerCatalogStore store) {
        this.store = store;
        refreshLiveness();
    }

    @PreDestroy
    void shutdownWatchers() {
        // Leave OS processes running; UCC restart should not kill game servers.
        live.clear();
    }

    public void refreshLiveness() {
        for (ServerCatalogStore.ServerRow row : store.listServers()) {
            if (!isLiveStatus(row.status())) {
                continue;
            }
            Process local = live.get(row.id());
            if (local != null && local.isAlive()) {
                continue;
            }
            boolean alive = row.pid() != null
                    && ProcessHandle.of(row.pid()).map(ProcessHandle::isAlive).orElse(false);
            if (!alive) {
                live.remove(row.id());
                store.updateServer(row.id(), null, "stopped", null, row.port(), row.logPath(), row.error());
            }
        }
    }

    public ServerCatalogStore.ServerRow register(
            String configId,
            Long scriptVersionId,
            Integer scriptVersion,
            String name,
            boolean launch) {
        ServerCatalogStore.ConfigRow cfg = store.getConfig(configId);
        if (cfg == null) {
            throw new IllegalArgumentException("unknown config");
        }
        ServerCatalogStore.VersionRow ver = resolveVersion(cfg, scriptVersionId, scriptVersion);
        if (ver == null) {
            throw new IllegalArgumentException("unknown script version");
        }
        String srvName = name == null || name.isBlank()
                ? cfg.name() + "-" + cfg.id().substring(Math.max(0, cfg.id().length() - 4))
                : name;
        ServerCatalogStore.ServerRow row = store.insertServer(srvName, cfg.id(), ver.id(), "stopped", cfg.port());
        if (launch) {
            return start(row.id(), ver.id(), null);
        }
        return row;
    }

    public ServerCatalogStore.ServerRow start(String serverId, Long scriptVersionId, Integer scriptVersion) {
        refreshLiveness();
        ServerCatalogStore.ServerRow row = store.getServer(serverId);
        if (row == null) {
            throw new IllegalArgumentException("unknown server");
        }
        if ("running".equals(row.status()) || "starting".equals(row.status())) {
            Process local = live.get(serverId);
            if (local != null && local.isAlive()) {
                return row;
            }
            if (row.pid() != null && ProcessHandle.of(row.pid()).map(ProcessHandle::isAlive).orElse(false)) {
                return row;
            }
        }
        ServerCatalogStore.ConfigRow cfg = store.getConfig(row.configId());
        ServerCatalogStore.VersionRow ver = resolveVersion(cfg, scriptVersionId, scriptVersion);
        if (ver == null) {
            ver = store.getVersion(row.scriptVersionId());
        }
        if (ver == null) {
            throw new IllegalArgumentException("unknown script version");
        }
        if (cfg.startCommand() == null || cfg.startCommand().isBlank()) {
            return store.updateServer(serverId, ver.id(), "error", null, cfg.port(), row.logPath(),
                    "no start command on config");
        }
        Path work = workingDir(cfg, serverId);
        try {
            Files.createDirectories(work);
            Files.writeString(work.resolve("minecraftuuuum-boot.script"), ver.scriptText(), StandardCharsets.UTF_8);
            Path log = work.resolve("minecraftuuuum-server-" + serverId + ".log");
            store.updateServer(serverId, ver.id(), "starting", null, cfg.port(), log.toString(), null);
            ProcessBuilder pb = command(cfg.startCommand());
            pb.directory(work.toFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()));
            Process p = pb.start();
            live.put(serverId, p);
            store.updateServer(serverId, ver.id(), "running", p.pid(), cfg.port(), log.toString(), null);
            Thread waiter = new Thread(() -> awaitExit(serverId, p), "srv-wait-" + serverId);
            waiter.setDaemon(true);
            waiter.start();
            return store.getServer(serverId);
        } catch (Exception e) {
            return store.updateServer(serverId, ver.id(), "error", null, cfg.port(), row.logPath(),
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    public ServerCatalogStore.ServerRow stop(String serverId) {
        ServerCatalogStore.ServerRow row = store.getServer(serverId);
        if (row == null) {
            throw new IllegalArgumentException("unknown server");
        }
        store.updateServer(serverId, null, "stopping", row.pid(), row.port(), row.logPath(), null);
        Process local = live.remove(serverId);
        if (local != null && local.isAlive()) {
            local.destroy();
            try {
                if (!local.waitFor(8, java.util.concurrent.TimeUnit.SECONDS)) {
                    local.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                local.destroyForcibly();
            }
        } else if (row.pid() != null) {
            ProcessHandle.of(row.pid()).ifPresent(handle -> {
                handle.destroy();
                try {
                    handle.onExit().orTimeout(8, java.util.concurrent.TimeUnit.SECONDS).join();
                } catch (Exception ignored) {
                    handle.destroyForcibly();
                }
            });
        }
        return store.updateServer(serverId, null, "stopped", null, row.port(), row.logPath(), null);
    }

    public ServerCatalogStore.ServerRow restart(String serverId, Long scriptVersionId, Integer scriptVersion) {
        ServerCatalogStore.ServerRow row = store.getServer(serverId);
        if (row == null) {
            throw new IllegalArgumentException("unknown server");
        }
        if (isLiveStatus(row.status())) {
            stop(serverId);
        }
        return start(serverId, scriptVersionId, scriptVersion);
    }

    private void awaitExit(String serverId, Process p) {
        try {
            int code = p.waitFor();
            live.remove(serverId, p);
            ServerCatalogStore.ServerRow row = store.getServer(serverId);
            if (row == null || "stopping".equals(row.status()) || "stopped".equals(row.status())) {
                return;
            }
            String err = code == 0 ? null : "exited " + code;
            store.updateServer(serverId, null, code == 0 ? "stopped" : "error", null, row.port(), row.logPath(), err);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ServerCatalogStore.VersionRow resolveVersion(
            ServerCatalogStore.ConfigRow cfg,
            Long scriptVersionId,
            Integer scriptVersion) {
        if (scriptVersionId != null) {
            ServerCatalogStore.VersionRow byId = store.getVersion(scriptVersionId);
            if (byId != null && byId.configId().equals(cfg.id())) {
                return byId;
            }
        }
        if (scriptVersion != null) {
            return store.getVersionByNumber(cfg.id(), scriptVersion);
        }
        return store.headVersion(cfg.id());
    }

    private static Path workingDir(ServerCatalogStore.ConfigRow cfg, String serverId) {
        if (cfg.workingDir() != null && !cfg.workingDir().isBlank()) {
            return Path.of(cfg.workingDir());
        }
        return Path.of(System.getProperty("java.io.tmpdir"), "minecraftuuuum-servers", serverId);
    }

    private static ProcessBuilder command(String startCommand) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return new ProcessBuilder("cmd.exe", "/c", startCommand);
        }
        return new ProcessBuilder("sh", "-c", startCommand);
    }

    private static boolean isLiveStatus(String status) {
        return "running".equals(status) || "starting".equals(status);
    }
}
