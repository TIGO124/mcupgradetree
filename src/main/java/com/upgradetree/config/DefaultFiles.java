package com.upgradetree.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Brings content files written by an older version (trees, chests) up to date with the bundled
 * ones that carry every language. A server file is only replaced (and kept as a backup) when it is
 * still exactly the old default, i.e. equal to the bundled file with only its Turkish texts;
 * a file the owner changed is never touched.
 */
public final class DefaultFiles {

    private static final Set<String> LANGUAGE_KEYS = Set.of("tr", "en");

    private DefaultFiles() {
    }

    /** Upgrades {@code resource} (e.g. "trees/ana.yml") if needed. Returns true if the file was replaced. */
    public static boolean upgrade(JavaPlugin plugin, String resource) {
        File file = new File(plugin.getDataFolder(), resource);
        if (!file.isFile()) {
            return false;
        }
        try (InputStream in = plugin.getResource(resource)) {
            if (in == null) {
                return false;
            }
            Object bundled = plain(load(new InputStreamReader(in, StandardCharsets.UTF_8)));
            Object server = plain(YamlConfiguration.loadConfiguration(file));
            if (hasLanguages(server) || !reduce(bundled).equals(server)) {
                return false;
            }
            File backup = new File(file.getParentFile(), file.getName() + ".tr.bak");
            if (backup.exists()) {
                backup = new File(file.getParentFile(), file.getName() + ".tr-" + System.currentTimeMillis() + ".bak");
            }
            if (!file.renameTo(backup)) {
                return false;
            }
            plugin.saveResource(resource, true);
            plugin.getLogger().info(resource + " artık Türkçe ve İngilizce metinli (eski dosya: " + backup.getName()
                    + "). Now with Turkish and English texts.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning(resource + " güncellenemedi: " + e.getMessage());
            return false;
        }
    }

    private static YamlConfiguration load(InputStreamReader r) {
        return YamlConfiguration.loadConfiguration(r);
    }

    /** YAML sections turned into plain maps (so two files can be compared with equals). */
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

    /** True for a {tr: ..., en: ...} map of texts. */
    static boolean isLanguageMap(Object o) {
        if (!(o instanceof Map<?, ?> m) || m.isEmpty() || !m.containsKey("tr") || !LANGUAGE_KEYS.containsAll(m.keySet())) {
            return false;
        }
        for (Object v : m.values()) {
            if (!(v instanceof String) && !(v instanceof List<?>)) {
                return false;
            }
        }
        return true;
    }

    /** The data with every language map replaced by its Turkish text (what older versions wrote). */
    static Object reduce(Object o) {
        if (isLanguageMap(o)) {
            return ((Map<?, ?>) o).get("tr");
        }
        if (o instanceof Map<?, ?> map) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                m.put(String.valueOf(e.getKey()), reduce(e.getValue()));
            }
            return m;
        }
        if (o instanceof List<?> list) {
            List<Object> out = new ArrayList<>();
            for (Object x : list) {
                out.add(reduce(x));
            }
            return out;
        }
        return o;
    }

    /** True if the data already has texts per language. */
    static boolean hasLanguages(Object o) {
        if (isLanguageMap(o)) {
            return true;
        }
        if (o instanceof Map<?, ?> map) {
            for (Object v : map.values()) {
                if (hasLanguages(v)) {
                    return true;
                }
            }
        } else if (o instanceof List<?> list) {
            for (Object v : list) {
                if (hasLanguages(v)) {
                    return true;
                }
            }
        }
        return false;
    }
}
