package com.upgradetree.config;

import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
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

/**
 * Messages of one language: built-in Turkish defaults, then the built-in file of the language, then
 * the server's lang/&lt;language&gt;.yml, then (server language only) the old "messages:" section of
 * config.yml. All languages together live in {@link Languages}.
 */
public final class Lang {

    public static final String[] BUNDLED = {"tr", "en"};
    /** Bumped when the built-in files change in a way old copies on servers must not hide. */
    public static final int VERSION = 4;

    private final String code;
    /** The server language: content written in other languages falls back to it (see {@link Localized}). */
    private final String fallback;
    private final Map<String, Object> messages = new LinkedHashMap<>();

    public Lang(JavaPlugin plugin, String language, ConfigurationSection legacy, String fallback) {
        this.code = language;
        this.fallback = fallback == null ? language : fallback;
        merge(bundled(plugin, "tr"));
        if (!"tr".equals(language)) {
            merge(bundled(plugin, language));
        }
        File file = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        if (file.exists()) {
            merge(YamlConfiguration.loadConfiguration(file));
        }
        if (legacy != null) {
            merge(legacy);
        }
    }

    /**
     * Writes the built-in files once. A server copy older than {@link #VERSION} (for example one that
     * still lists the old Turkish commands) is kept as lang/&lt;l&gt;.yml.v&lt;n&gt;.bak and replaced.
     */
    public static void saveDefaults(JavaPlugin plugin) {
        for (String l : BUNDLED) {
            File file = new File(plugin.getDataFolder(), "lang/" + l + ".yml");
            if (!file.exists()) {
                plugin.saveResource("lang/" + l + ".yml", false);
                continue;
            }
            int version = YamlConfiguration.loadConfiguration(file).getInt("lang-version", 1);
            if (version >= VERSION) {
                continue;
            }
            File backup = new File(file.getParentFile(), l + ".yml.v" + version + ".bak");
            if (backup.exists()) {
                backup = new File(file.getParentFile(), l + ".yml.v" + version + "-" + System.currentTimeMillis() + ".bak");
            }
            if (file.renameTo(backup)) {
                plugin.saveResource("lang/" + l + ".yml", true);
                plugin.getLogger().warning("lang/" + l + ".yml eskiydi, yenisi yazıldı. Eski dosya: lang/" + backup.getName()
                        + " (old language file replaced, your edits are in the backup)");
            } else {
                plugin.getLogger().warning("lang/" + l + ".yml güncellenemedi (could not replace the outdated language file)");
            }
        }
    }

    private static ConfigurationSection bundled(JavaPlugin plugin, String language) {
        InputStream in = plugin.getResource("lang/" + language + ".yml");
        if (in == null) {
            return null;
        }
        try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(r);
        } catch (Exception e) {
            return null;
        }
    }

    private void merge(ConfigurationSection sec) {
        if (sec == null) {
            return;
        }
        for (String key : sec.getKeys(false)) {
            Object v = sec.get(key);
            if (v instanceof List<?> list) {
                List<String> lines = new ArrayList<>();
                for (Object o : list) {
                    lines.add(String.valueOf(o));
                }
                messages.put(key, lines);
            } else if (v != null && !(v instanceof ConfigurationSection)) {
                messages.put(key, String.valueOf(v));
            }
        }
    }

    /** Language code, e.g. "tr". */
    public String code() {
        return code;
    }

    /** Code of the server language (the fallback for texts not written in this language). */
    public String fallback() {
        return fallback;
    }

    /** Raw MiniMessage string for a message key. */
    public String raw(String key) {
        Object v = messages.get(key);
        if (v instanceof List<?> list) {
            return String.join("\n", list.stream().map(String::valueOf).toList());
        }
        return v == null ? key : String.valueOf(v);
    }

    public List<String> rawList(String key) {
        Object v = messages.get(key);
        if (v instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return v == null ? List.of() : List.of(String.valueOf(v));
    }

    /** A chat message with prefix. */
    public Component msg(String key, String... kv) {
        return Text.mm(raw("prefix") + Text.apply(raw(key), kv));
    }

    /** A message without prefix (action bar, menu labels). */
    public Component plain(String key, String... kv) {
        return Text.mm(Text.apply(raw(key), kv));
    }

    /** The raw text with placeholders applied (for building bigger MiniMessage strings). */
    public String format(String key, String... kv) {
        return Text.apply(raw(key), kv);
    }
}
