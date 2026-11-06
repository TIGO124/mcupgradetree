package com.upgradetree.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.logging.Level;

/** Stores player data as small YAML files in plugins/UpgradeTree/players/. */
public final class DataStore {

    private final JavaPlugin plugin;
    private final File folder;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "UpgradeTree-Saver");
        t.setDaemon(true);
        return t;
    });
    private final DoubleSupplier startingPoints;

    public DataStore(JavaPlugin plugin, DoubleSupplier startingPoints) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "players");
        this.startingPoints = startingPoints;
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("players klasörü oluşturulamadı: " + folder);
        }
    }

    /** Data of an online player (loaded on demand and cached). */
    public PlayerData get(Player p) {
        PlayerData d = cache.computeIfAbsent(p.getUniqueId(), id -> read(id, p.getName()));
        d.name(p.getName());
        return d;
    }

    /** Cached data if present, otherwise read from disk without caching. */
    public PlayerData peek(UUID uuid, String name) {
        PlayerData d = cache.get(uuid);
        return d != null ? d : read(uuid, name);
    }

    public boolean isCached(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public boolean exists(UUID uuid) {
        return cache.containsKey(uuid) || file(uuid).exists();
    }

    public void unload(UUID uuid) {
        PlayerData d = cache.remove(uuid);
        if (d != null) {
            save(d);
        }
    }

    private File file(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }

    private PlayerData read(UUID uuid, String name) {
        File f = file(uuid);
        PlayerData d = new PlayerData(uuid, name);
        if (!f.exists()) {
            d.points(startingPoints.getAsDouble());
            return d;
        }
        try {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            d.name(y.getString("name", name));
            d.points(y.getDouble("points", 0));
            ConfigurationSection lv = y.getConfigurationSection("levels");
            if (lv != null) {
                for (String k : lv.getKeys(false)) {
                    d.setLevel(k, lv.getInt(k));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Oyuncu verisi okunamadı: " + f.getName(), e);
        }
        return d;
    }

    private String serialize(PlayerData d) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", d.name());
        y.set("points", d.points());
        for (Map.Entry<String, Integer> e : d.levels().entrySet()) {
            y.set("levels." + e.getKey(), e.getValue());
        }
        if (d.levels().isEmpty()) {
            y.createSection("levels");
        }
        return y.saveToString();
    }

    /** Saves asynchronously (writes are serialized on one thread). */
    public void save(PlayerData d) {
        String content = serialize(d);
        UUID id = d.uuid;
        try {
            writer.execute(() -> write(id, content));
        } catch (Exception rejected) {
            write(id, content);
        }
    }

    private void write(UUID id, String content) {
        File f = file(id);
        File tmp = new File(folder, id + ".yml.tmp");
        try {
            Files.writeString(tmp.toPath(), content, StandardCharsets.UTF_8);
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Oyuncu verisi kaydedilemedi: " + f.getName(), e);
        }
    }

    /** Flushes everything synchronously (used on disable). */
    public void shutdown() {
        for (PlayerData d : cache.values()) {
            String content = serialize(d);
            UUID id = d.uuid;
            try {
                writer.execute(() -> write(id, content));
            } catch (Exception rejected) {
                write(id, content);
            }
        }
        writer.shutdown();
        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Bazı oyuncu verileri zamanında kaydedilemedi.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
