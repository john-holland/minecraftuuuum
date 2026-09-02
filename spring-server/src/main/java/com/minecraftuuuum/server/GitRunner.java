package com.minecraftuuuum.server;

import java.nio.file.Path;

/** Runs git in a working directory. Tests replace this so no real repo is required. */
public interface GitRunner {
    String run(Path cwd, String... args) throws GitException;

    class GitException extends Exception {
        public GitException(String message) {
            super(message);
        }

        public GitException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
