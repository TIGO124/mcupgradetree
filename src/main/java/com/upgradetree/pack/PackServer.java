package com.upgradetree.pack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * A tiny web server that hands out the resource pack (and nothing else) at
 * {@code /upgradetree-<sha1>.zip}. Runs on two background threads.
 */
final class PackServer {

    private final HttpServer server;
    private final ExecutorService threads;
    final int port;

    PackServer(int port, Supplier<PackBuilder.Pack> pack) throws IOException {
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress(port), 16);
        this.threads = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "UpgradeTree-Pack");
            t.setDaemon(true);
            return t;
        });
        server.createContext("/", ex -> handle(ex, pack.get()));
        server.setExecutor(threads);
        server.start();
    }

    static String path(String sha1) {
        return "/upgradetree-" + sha1 + ".zip";
    }

    private static void handle(HttpExchange ex, PackBuilder.Pack pack) throws IOException {
        try (ex) {
            String method = ex.getRequestMethod();
            boolean head = "HEAD".equals(method);
            if (!head && !"GET".equals(method)) {
                ex.sendResponseHeaders(405, -1);
                return;
            }
            if (pack == null || !path(pack.sha1()).equals(ex.getRequestURI().getPath())) {
                ex.sendResponseHeaders(404, -1);
                return;
            }
            ex.getResponseHeaders().set("Content-Type", "application/zip");
            ex.getResponseHeaders().set("Cache-Control", "public, max-age=31536000, immutable");
            if (head) {
                ex.getResponseHeaders().set("Content-Length", String.valueOf(pack.zip().length));
                ex.sendResponseHeaders(200, -1);
                return;
            }
            ex.sendResponseHeaders(200, pack.zip().length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(pack.zip());
            }
        }
    }

    void stop() {
        server.stop(0);
        threads.shutdownNow();
    }
}
