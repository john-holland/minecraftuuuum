package com.minecraftuuuum.server;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProcessGitRunner implements GitRunner {
    @Override
    public String run(Path cwd, String... args) throws GitException {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        for (String a : args) {
            cmd.add(a);
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(cwd.toFile());
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = p.waitFor();
            if (code != 0) {
                throw new GitException("git " + String.join(" ", args) + " exited " + code + ": " + out.trim());
            }
            return out;
        } catch (GitException e) {
            throw e;
        } catch (Exception e) {
            throw new GitException("git failed: " + e.getMessage(), e);
        }
    }
}
