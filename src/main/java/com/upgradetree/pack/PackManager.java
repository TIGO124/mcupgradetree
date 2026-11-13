package com.upgradetree.pack;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Settings;
import com.upgradetree.util.Text;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.inventory.ItemStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The menu background picture. Players need it in a resource pack: the plugin builds the pack from
 * the picture in its folder and sends it to every player who joins (from its own small web server,
 * or from an address the owner uploaded it to). Until a player's client reports the pack as loaded,
 * their menu uses the plain background colour instead, so nobody ever sees a missing texture.
 */
public final class PackManager implements Listener {

    public static final String DEFAULT_IMAGE = "background.jpg";
    public static final String ZIP = "resourcepack.zip";

    private final UpgradeTreePlugin plugin;
    private final Set<UUID> loaded = ConcurrentHashMap.newKeySet();
    private volatile PackBuilder.Pack pack;
    private UUID packId;
    private String source = "";
    private PackServer server;
    private boolean warnedDownload;
    private boolean warnedUrl;

    public PackManager(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    private Settings settings() {
        return plugin.settings();
    }

    private String log(String key, String... kv) {
        return Text.plain(plugin.lang().format(key, kv));
    }

    // ------------------------------------------------------------------ building

    /** Builds the pack (again) and starts or stops the web server. Called on enable and on /tigo reload. */
    public void reload() {
        Settings s = settings();
        PackBuilder.Pack before = pack;
        UUID beforeId = packId;
        // problems are reported once; after a reload (settings may be fixed) once again
        warnedDownload = false;
        warnedUrl = false;
        boolean wanted = s.background == Settings.Background.IMAGE && s.packMode != Settings.ResourcePackMode.OFF;
        pack = wanted ? load(s, before) : null;
        packId = pack == null ? null : UUID.nameUUIDFromBytes(("upgradetree-pack:" + pack.sha1()).getBytes(StandardCharsets.UTF_8));

        if (pack != null && s.packMode == Settings.ResourcePackMode.SELF_HOST) {
            startServer(s.packPort);
        } else {
            stopServer();
        }
        if (pack != null && s.packMode == Settings.ResourcePackMode.URL && s.packUrl.isEmpty()) {
            plugin.getLogger().warning(log("log-pack-no-url", "file", ZIP));
        }
        if (beforeId != null && !beforeId.equals(packId)) {
            loaded.clear();
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.removeResourcePacks(beforeId);
            }
        }
        if (packId != null) {
            // a new pack goes to everyone; an unchanged one once more to the players who do not have
            // it (the port or address may just have been corrected in config.yml)
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!loaded.contains(p.getUniqueId())) {
                    send(p);
                }
            }
        }
    }

    /** The picture from the plugin folder as a pack; the previous pack when the file did not change. */
    private PackBuilder.Pack load(Settings s, PackBuilder.Pack before) {
        File folder = plugin.getDataFolder();
        File file = new File(folder, s.backgroundImage);
        try {
            if (!file.getCanonicalPath().startsWith(folder.getCanonicalPath() + File.separator)) {
                plugin.getLogger().warning(log("log-pack-missing", "file", s.backgroundImage));
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        if (!file.isFile() && s.backgroundImage.equals(DEFAULT_IMAGE)) {
            plugin.saveResource(DEFAULT_IMAGE, false);
        }
        if (!file.isFile()) {
            plugin.getLogger().warning(log("log-pack-missing", "file", s.backgroundImage));
            return null;
        }
        String key = file.getAbsolutePath() + "|" + file.lastModified() + "|" + file.length();
        if (before != null && key.equals(source)) {
            return before;
        }
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                plugin.getLogger().warning(log("log-pack-failed", "file", file.getName(), "error", "PNG / JPG"));
                return null;
            }
            PackBuilder.Pack built = PackBuilder.build(PackBuilder.fit(img, PackBuilder.ASPECT));
            Files.write(new File(folder, ZIP).toPath(), built.zip());
            source = key;
            plugin.getLogger().info(log("log-pack-built", "file", file.getName(), "size", String.valueOf(built.zip().length / 1024),
                    "zip", ZIP));
            return built;
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().warning(log("log-pack-failed", "file", file.getName(), "error", String.valueOf(e)));
            return null;
        }
    }

    private void startServer(int port) {
        if (server != null && server.port == port) {
            return;
        }
        stopServer();
        try {
            server = new PackServer(port, () -> pack);
            plugin.getLogger().info(log("log-pack-server", "port", String.valueOf(port)));
        } catch (IOException | RuntimeException | LinkageError e) {
            server = null;
            plugin.getLogger().warning(log("log-pack-server-failed", "port", String.valueOf(port), "error", String.valueOf(e)));
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    public void shutdown() {
        stopServer();
    }

    // ------------------------------------------------------------------ players

    /** True when this player's client has the picture (the menu may show it). */
    public boolean imageReady(Player p) {
        if (pack == null) {
            return false;
        }
        return settings().packMode == Settings.ResourcePackMode.EXTERNAL || loaded.contains(p.getUniqueId());
    }

    /** Treats a player as having the pack (previews, tests). */
    public void markLoaded(UUID player) {
        loaded.add(player);
    }

    /** The picture as it is in the pack (for previews), or null. */
    public BufferedImage image() {
        PackBuilder.Pack p = pack;
        return p == null ? null : p.image();
    }

    /** The item an item display shows to draw the picture. */
    public ItemStack item() {
        ItemStack it = new ItemStack(Material.PAPER);
        it.editMeta(m -> {
            m.setItemModel(NamespacedKey.fromString(PackBuilder.MODEL));
            m.setHideTooltip(true);
        });
        return it;
    }

    private void send(Player p) {
        Settings s = settings();
        PackBuilder.Pack current = pack;
        if (current == null || packId == null || !p.isOnline() || plugin.bedrock().isBedrock(p)
                || s.packMode == Settings.ResourcePackMode.EXTERNAL || s.packMode == Settings.ResourcePackMode.OFF) {
            return;
        }
        URI uri = uri(p, current, s);
        if (uri == null) {
            return;
        }
        p.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
                .packs(ResourcePackInfo.resourcePackInfo(packId, uri, current.sha1()))
                .required(s.packRequired)
                .replace(false)
                .prompt(plugin.lang().plain("pack-prompt"))
                .build());
    }

    /** Where this player downloads the pack. */
    private URI uri(Player p, PackBuilder.Pack current, Settings s) {
        try {
            if (s.packMode == Settings.ResourcePackMode.URL) {
                if (s.packUrl.isEmpty()) {
                    return null;
                }
                return URI.create(s.packUrl);
            }
            if (server == null) {
                return null;
            }
            String path = PackServer.path(current.sha1());
            if (s.packHost.contains("://")) {
                // a full address, e.g. behind a reverse proxy
                return URI.create(s.packHost.replaceAll("/+$", "") + path);
            }
            String host = s.packHost.isEmpty() ? virtualHost(p) : s.packHost;
            if (host.contains(":") && !host.startsWith("[")) {
                host = "[" + host + "]"; // IPv6
            }
            return URI.create("http://" + host + ":" + server.port + path);
        } catch (IllegalArgumentException e) {
            if (!warnedUrl) {
                warnedUrl = true;
                plugin.getLogger().warning(log("log-pack-bad-url", "error", String.valueOf(e.getMessage())));
            }
            return null;
        }
    }

    /** The address the player typed to join (the pack comes from the same machine). */
    private static String virtualHost(Player p) {
        InetSocketAddress v = null;
        try {
            v = p.getConnection().getVirtualHost();
        } catch (RuntimeException ignored) {
            // older connection objects: fall back below
        }
        if (v != null && v.getHostString() != null && !v.getHostString().isBlank()) {
            return v.getHostString();
        }
        String ip = Bukkit.getIp();
        return ip == null || ip.isBlank() ? "localhost" : ip;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> send(p), 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        loaded.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent e) {
        if (packId == null || !packId.equals(e.getID())) {
            return;
        }
        UUID id = e.getPlayer().getUniqueId();
        switch (e.getStatus()) {
            case SUCCESSFULLY_LOADED -> loaded.add(id);
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD -> {
                loaded.remove(id);
                if (!warnedDownload) {
                    warnedDownload = true;
                    plugin.getLogger().warning(log("log-pack-download-failed", "player", e.getPlayer().getName(),
                            "status", e.getStatus().name(), "port", String.valueOf(settings().packPort)));
                }
            }
            case DECLINED, DISCARDED -> loaded.remove(id);
            default -> {
            }
        }
    }
}
