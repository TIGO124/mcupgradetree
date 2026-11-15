package com.upgradetree.update;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * /tigo update: downloads the newest release of the plugin from GitHub into the server's update
 * folder (plugins/update), and the server switches to it on its next start: a running plugin cannot
 * be swapped safely. Nothing is lost: player data is written first, a backup of the plugin folder
 * and of the running jar is made, and the download is checked (size, SHA-256, plugin name, version,
 * Minecraft version) before it is used. When any step fails nothing changes.
 */
public final class Updater {

    /** GitHub's API. A local test server can be set with -Dupgradetree.update.api=http://127.0.0.1:port */
    private static final String API = System.getProperty("upgradetree.update.api", "https://api.github.com");
    private static final Pattern REPOSITORY = Pattern.compile("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+");
    private static final long MAX_JAR_BYTES = 64L * 1024 * 1024;
    private static final int RESTART_SECONDS = 10;
    private static final long SAVE_TIMEOUT_MS = 20_000;

    private final UpgradeTreePlugin plugin;
    private final AtomicBoolean busy = new AtomicBoolean();
    /** Redirects are followed by hand, so the token never leaves GitHub's API. */
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public Updater(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    /** One message for the sender; {@code ready} when the new version waits in the update folder. */
    private record Report(boolean ready, String key, String... kv) {
    }

    /** Looks for a new version and installs it; {@code restart} restarts the server once it is ready. */
    public void update(CommandSender sender, boolean restart) {
        Lang lang = plugin.lang();
        String repo = plugin.settings().updateRepository;
        if (!REPOSITORY.matcher(repo).matches()) {
            sender.sendMessage(lang.msg("update-failed", "error", Text.escape("update.repository in config.yml must look like owner/name")));
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            sender.sendMessage(lang.msg("update-busy"));
            return;
        }
        sender.sendMessage(lang.msg("update-checking", "repo", Text.escape(repo)));
        plugin.data().autosave(); // queued now, written before the backup is made
        String token = plugin.settings().updateToken;
        String current = plugin.getPluginMeta().getVersion();
        Path jar = plugin.jarFile().toPath();
        Path data = plugin.getDataFolder().toPath();
        Path updates = Bukkit.getUpdateFolderFile().toPath();
        String server = Bukkit.getMinecraftVersion();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Report report;
            try {
                report = run(repo, token, current, jar, data, updates, server);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "The update failed", ex);
                report = new Report(false, "update-failed", "error", Text.escape(reason(ex)));
            } finally {
                busy.set(false);
            }
            Report done = report;
            Bukkit.getScheduler().runTask(plugin, () -> finish(sender, done, restart));
        });
    }

    private Report run(String repo, String token, String current, Path jar, Path data, Path updates, String server)
            throws IOException, InterruptedException {
        URI latestUri = URI.create(API + "/repos/" + repo + "/releases/latest");
        HttpResponse<String> res = http.send(request(latestUri, token, "application/vnd.github+json", current, Duration.ofSeconds(30)),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 404) {
            return new Report(false, "update-no-release", "repo", Text.escape(repo));
        }
        if (res.statusCode() == 401 || res.statusCode() == 403) {
            throw new IOException("GitHub refused access (HTTP " + res.statusCode() + "), check update.token");
        }
        if (res.statusCode() != 200) {
            throw new IOException("GitHub answered HTTP " + res.statusCode());
        }
        Release release = Release.parse(res.body(), plugin.getName());
        if (release == null) {
            return new Report(false, "update-no-jar", "repo", Text.escape(repo));
        }
        Version latest = Version.parse(release.version());
        Version now = Version.parse(current);
        if (latest == null) {
            throw new IOException("the release tag " + release.tag() + " is not a version");
        }
        if (now != null && !latest.newerThan(now)) {
            return new Report(false, "update-latest", "version", Text.escape(current));
        }
        Path target = updates.resolve(jar.getFileName().toString());
        if (Files.isRegularFile(target)) {
            PluginJar staged = PluginJar.read(target);
            if (staged != null && plugin.getName().equals(staged.name()) && latest.equals(Version.parse(staged.version()))) {
                return new Report(true, "update-already", "version", Text.escape(staged.version()));
            }
        }
        Files.createDirectories(updates);
        Path part = updates.resolve(jar.getFileName() + ".part");
        try {
            String sha = download(release, token, current, part);
            if (release.size() > 0 && Files.size(part) != release.size()) {
                throw new IOException("the download is incomplete");
            }
            if (release.sha256() != null && !release.sha256().equals(sha)) {
                throw new IOException("the download is damaged (its SHA-256 does not match the release)");
            }
            PluginJar got = PluginJar.read(part);
            if (got == null || !plugin.getName().equals(got.name())) {
                throw new IOException("the downloaded file is not " + plugin.getName());
            }
            Version gotVersion = Version.parse(got.version());
            if (gotVersion == null || (now != null && !gotVersion.newerThan(now))) {
                throw new IOException("the downloaded jar is version " + got.version());
            }
            Version api = Version.parse(got.apiVersion());
            Version mc = Version.parse(server);
            if (api != null && mc != null && api.newerThan(mc)) {
                return new Report(false, "update-too-new", "version", Text.escape(got.version()), "api",
                        Text.escape(got.apiVersion()), "server", Text.escape(server));
            }
            if (!plugin.data().flush(SAVE_TIMEOUT_MS)) {
                throw new IOException("the player data could not be saved first");
            }
            Path backup = Backup.create(data, jar, current, LocalDateTime.now());
            move(part, target);
            plugin.getLogger().info("UpgradeTree " + got.version() + " is in " + target + " and starts with the next server start."
                    + " Backup of the plugin folder and the old jar: " + backup);
            return new Report(true, "update-ready", "version", Text.escape(got.version()), "backup", Text.escape(shown(backup)));
        } finally {
            Files.deleteIfExists(part);
        }
    }

    /** Downloads the release jar to {@code to}; returns its SHA-256 (hex). */
    private String download(Release release, String token, String current, Path to) throws IOException, InterruptedException {
        URI uri = URI.create(release.assetUrl());
        HttpResponse<InputStream> res = http.send(request(uri, token, "application/octet-stream", current, Duration.ofMinutes(5)),
                HttpResponse.BodyHandlers.ofInputStream());
        for (int hops = 0; res.statusCode() / 100 == 3; hops++) {
            res.body().close();
            String location = res.headers().firstValue("location").orElse(null);
            if (location == null || hops >= 5) {
                throw new IOException("the download was redirected too often");
            }
            uri = uri.resolve(location);
            // the file server gets no GitHub token
            res = http.send(request(uri, null, "application/octet-stream", current, Duration.ofMinutes(5)),
                    HttpResponse.BodyHandlers.ofInputStream());
        }
        if (res.statusCode() != 200) {
            res.body().close();
            throw new IOException("the download answered HTTP " + res.statusCode());
        }
        MessageDigest sha = sha256();
        long total = 0;
        try (InputStream in = res.body(); OutputStream out = Files.newOutputStream(to)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > MAX_JAR_BYTES) {
                    throw new IOException("the download is too big");
                }
                sha.update(buf, 0, n);
                out.write(buf, 0, n);
            }
        }
        return HexFormat.of().formatHex(sha.digest());
    }

    private static HttpRequest request(URI uri, String token, String accept, String current, Duration timeout) throws IOException {
        if (API.startsWith("https://") && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("refused to download over " + uri.getScheme());
        }
        HttpRequest.Builder b = HttpRequest.newBuilder(uri).timeout(timeout).GET()
                .header("Accept", accept)
                .header("User-Agent", "UpgradeTree-Updater/" + current);
        if (uri.getHost() != null && uri.getHost().equalsIgnoreCase(URI.create(API).getHost())) {
            b.header("X-GitHub-Api-Version", "2022-11-28");
            if (token != null && !token.isBlank()) {
                b.header("Authorization", "Bearer " + token.trim());
            }
        }
        return b.build();
    }

    private void finish(CommandSender sender, Report r, boolean restart) {
        Lang lang = plugin.lang();
        Component msg = lang.msg(r.key(), r.kv());
        tell(sender, msg);
        if (!r.ready()) {
            return;
        }
        if (!restart) {
            tell(sender, lang.msg("update-restart-hint"));
            return;
        }
        Bukkit.broadcast(lang.msg("update-restarting", "seconds", String.valueOf(RESTART_SECONDS)));
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.getServer().restart(), RESTART_SECONDS * 20L);
    }

    /** The sender (if still online) and the console get the result. */
    private static void tell(CommandSender sender, Component msg) {
        if (!(sender instanceof Player p) || p.isOnline()) {
            sender.sendMessage(msg);
        }
        if (!(sender instanceof ConsoleCommandSender)) {
            Bukkit.getConsoleSender().sendMessage(msg);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** The backup folder as seen from the server folder (plugins/UpgradeTree/backups/...). */
    private static String shown(Path backup) {
        try {
            return Path.of("").toAbsolutePath().relativize(backup.toAbsolutePath()).toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            return backup.toAbsolutePath().toString();
        }
    }

    private static String reason(Exception ex) {
        if (ex instanceof java.net.http.HttpTimeoutException) {
            return "GitHub did not answer in time";
        }
        if (ex instanceof java.net.ConnectException || ex instanceof java.nio.channels.UnresolvedAddressException
                || ex.getCause() instanceof java.net.UnknownHostException) {
            return "no connection to GitHub";
        }
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
