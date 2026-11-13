package com.upgradetree.config;

import java.util.logging.Logger;

/** Where configuration problems (tree and chest files, config.yml) are reported. */
public final class Problems {

    /** Receives one problem at a time. */
    @FunctionalInterface
    public interface Sink {
        void warn(String problem);
    }

    private Problems() {
    }

    /** Logs every problem as a console warning. */
    public static Sink log(Logger log) {
        return log::warning;
    }
}
