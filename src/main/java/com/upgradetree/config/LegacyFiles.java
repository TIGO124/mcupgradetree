package com.upgradetree.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Brings the files of versions before 2.3 up to date. Those versions shipped Turkish texts and
 * Turkish ids; the plugin is English only now.
 * <ul>
 * <li>A built-in tree or chests.yml that the owner never changed is replaced by the current file
 *     (the old one is kept as *.bak). A changed file is kept as it is.</li>
 * <li>config.yml gets the current comments; every value of the old file is kept.</li>
 * <li>The lang/ folder is no longer read (messages.yml holds every message).</li>
 * </ul>
 */
public final class LegacyFiles {

    /** Old built-in file → the current file that replaces it. */
    static final Map<String, String> RENAMED = Map.of(
            "trees/main.yml", "trees/main.yml", // the tree.yml of 1.x, moved here by TreeLoader.migrateLegacy
            "trees/ana.yml", "trees/main.yml",
            "trees/yetenekler.yml", "trees/abilities.yml",
            "trees/toplayici.yml", "trees/gatherer.yml",
            "chests.yml", "chests.yml");

    /**
     * Fingerprints ({@link #fingerprint}) of every earlier built-in version of a file, by the current
     * file: 1.x (tree.yml), 2.0/2.1, 2.2, the 2.3 development builds and the first English 2.3 build.
     */
    static final Map<String, Set<String>> OLD_DEFAULTS = Map.of(
            "trees/main.yml", Set.of("78ad04fd8615c0a82f31", "e9c24987b5652b99d95b", "476418e6da15190e4b1f",
                    "f0a4d943a4909130bc30", "35b9aac4c073f322fef0", "839967a1c31564b04f3c"),
            "trees/abilities.yml", Set.of("55e899c979c5af061ef3", "24da616f9f912e6f0df0", "2dead666b8191edddf34",
                    "b183d459f35fe11802ea"),
            "trees/gatherer.yml", Set.of("f15d7f36bcf2b5cb22de", "342b5d9d8cdc7eac3cfe", "e2430ff64c1661793c59",
                    "74fee34b9277365c60ba"),
            "chests.yml", Set.of("7fa5025c9bff6d550745", "67475b34ab8f443c8eba", "6cac5878b8e781dc8cfe"));

    /**
     * Fingerprints of the built-in currency names ("Levels", "Points", "Money") and menu item name of
     * versions before 2.3: such a name is cleared so the one of messages.yml shows.
     */
    private static final Set<String> OLD_NAMES = Set.of("1fd67f6677eaa16f6256", "44d8ce6a425372dfb77c",
            "2ac5c9c2bb0c3a253048", "418ee8441c40bd03eb21");

    /** Keys of old config files that are gone. */
    private static final Set<String> REMOVED_KEYS = Set.of("language", "messages", "config-version");

    /** Sections whose keys are chosen by the owner: copied as a whole. */
    private static final Set<String> FREE_SECTIONS = Set.of("points.mob-points", "theme");

    private LegacyFiles() {
    }

    /** Runs every step; each one is skipped when there is nothing to do. */
    public static void upgrade(JavaPlugin plugin) {
        for (Map.Entry<String, String> e : RENAMED.entrySet()) {
            replaceOldDefault(plugin, e.getKey(), e.getValue());
        }
        upgradeConfig(plugin);
        File lang = new File(plugin.getDataFolder(), "lang");
        File note = new File(lang, "NOT-USED.txt");
        if (lang.isDirectory() && !note.exists()) {
            plugin.getLogger().info("The lang folder is no longer used: every message is in messages.yml now. You can delete plugins/"
                    + plugin.getName() + "/lang.");
            try {
                java.nio.file.Files.writeString(note.toPath(), "UpgradeTree no longer reads this folder. All messages are in messages.yml.\n");
            } catch (IOException ignored) {
                // only a reminder
            }
        }
    }

    // ------------------------------------------------------------------ trees and chests

    private static void replaceOldDefault(JavaPlugin plugin, String oldName, String current) {
        File file = new File(plugin.getDataFolder(), oldName);
        if (!file.isFile()) {
            return;
        }
        String print;
        try {
            print = fingerprint(file);
        } catch (IOException | InvalidConfigurationException e) {
            return; // broken file: the loader reports it
        }
        if (!OLD_DEFAULTS.getOrDefault(current, Set.of()).contains(print)) {
            return; // current file, or changed by the owner
        }
        File target = new File(plugin.getDataFolder(), current);
        File backup = backup(file);
        if (!file.renameTo(backup)) {
            plugin.getLogger().warning(oldName + " is a built-in file of an older version but could not be replaced.");
            return;
        }
        if (!target.exists()) {
            plugin.saveResource(current, false);
        }
        plugin.getLogger().info(oldName.equals(current)
                ? current + " was replaced by the new built-in file (old file: " + backup.getName() + ")."
                : oldName + " was replaced by the new built-in " + current + " (old file: " + backup.getName() + ").");
    }

    private static File backup(File file) {
        File b = new File(file.getParentFile(), file.getName() + ".bak");
        return b.exists() ? new File(file.getParentFile(), file.getName() + "." + System.currentTimeMillis() + ".bak") : b;
    }

    static String fingerprint(File file) throws IOException, InvalidConfigurationException {
        YamlConfiguration y = new YamlConfiguration();
        y.load(file);
        return fingerprint(plain(y));
    }

    /** A short hash of the data of a YAML file (comments and formatting do not count). */
    static String fingerprint(Object plain) {
        StringBuilder sb = new StringBuilder();
        canonical(plain, sb);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 10);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void canonical(Object o, StringBuilder sb) {
        if (o instanceof Map<?, ?> m) {
            sb.append('{');
            for (Map.Entry<?, ?> e : m.entrySet()) {
                sb.append(e.getKey()).append('=');
                canonical(e.getValue(), sb);
                sb.append(';');
            }
            sb.append('}');
        } else if (o instanceof List<?> l) {
            sb.append('[');
            for (Object x : l) {
                canonical(x, sb);
                sb.append(',');
            }
            sb.append(']');
        } else if (o == null) {
            sb.append('~');
        } else {
            sb.append(o.getClass().getSimpleName().charAt(0)).append(o);
        }
    }

    /** YAML sections turned into plain maps (so files can be compared). */
    static Object plain(Object o) {
        if (o instanceof ConfigurationSection s) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (String k : s.getKeys(false)) {
                m.put(k, plain(s.get(k)));
            }
            return m;
        }
        if (o instanceof Map<?, ?> map) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                m.put(String.valueOf(e.getKey()), plain(e.getValue()));
            }
            return m;
        }
        if (o instanceof List<?> list) {
            List<Object> out = new ArrayList<>();
            for (Object x : list) {
                out.add(plain(x));
            }
            return out;
        }
        return o;
    }

    // ------------------------------------------------------------------ config.yml

    private static void upgradeConfig(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.isFile()) {
            return;
        }
        YamlConfiguration old = new YamlConfiguration();
        try {
            old.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            return; // Bukkit reports a broken config itself
        }
        if (old.contains("config-version")) {
            return;
        }
        YamlConfiguration fresh = new YamlConfiguration();
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in == null) {
                return;
            }
            fresh.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException e) {
            return;
        }
        merge(old, fresh);
        File backup = backup(file);
        try {
            java.nio.file.Files.copy(file.toPath(), backup.toPath());
            fresh.save(file);
            plugin.getLogger().info("config.yml was updated to the new layout, your settings were kept (old file: " + backup.getName() + ").");
        } catch (IOException e) {
            plugin.getLogger().warning("config.yml could not be updated: " + e.getMessage());
        }
    }

    /**
     * Copies the values of an old config.yml into the current file (whose comments stay). Old
     * defaults of the menu colours are cleared first ({@link ConfigMigration}); keys that are gone
     * are dropped, unknown keys are kept.
     */
    static void merge(YamlConfiguration old, YamlConfiguration fresh) {
        ConfigMigration.apply(old);
        if (old.contains("menu.hide-held-item", true) && !old.contains("menu.hide-hand", true)) {
            old.set("menu.hide-hand", old.getBoolean("menu.hide-held-item", true));
            old.set("menu.hide-held-item", null);
        }
        copy(old, fresh, "");
        fresh.set("currency.name", name(old.get("currency.name")));
        fresh.set("access.item.name", name(old.get("access.item.name")));
    }

    private static void copy(ConfigurationSection from, YamlConfiguration to, String prefix) {
        for (String key : from.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (REMOVED_KEYS.contains(path)) {
                continue;
            }
            Object v = from.get(key);
            if (v instanceof ConfigurationSection s) {
                if (!FREE_SECTIONS.contains(path) && (to.isConfigurationSection(path) || !to.contains(path, true))) {
                    if (!to.isConfigurationSection(path)) {
                        to.createSection(path);
                    }
                    copy(s, to, path);
                } else if (to.isConfigurationSection(path)) {
                    // replaced as a whole, in place (keeps its position and comments)
                    ConfigurationSection target = to.getConfigurationSection(path);
                    for (String k : target.getKeys(false)) {
                        target.set(k, null);
                    }
                    for (Map.Entry<String, Object> e : s.getValues(false).entrySet()) {
                        target.set(e.getKey(), e.getValue());
                    }
                } else {
                    List<String> comments = to.getComments(path);
                    to.set(path, null);
                    to.createSection(path, s.getValues(false));
                    to.setComments(path, comments);
                }
            } else {
                to.set(path, v);
            }
        }
    }

    /** A currency or item name: per-language maps give their English text, old built-in names become "". */
    static String name(Object raw) {
        String n = Texts.text(raw, "");
        return OLD_NAMES.contains(fingerprint(n.trim().toLowerCase(Locale.ROOT))) ? "" : n;
    }
}
