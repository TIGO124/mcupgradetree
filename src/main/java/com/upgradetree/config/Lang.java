package com.upgradetree.config;

import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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
 * Messages: built-in Turkish defaults, then the built-in file of the chosen language, then the
 * server's lang/&lt;language&gt;.yml, then the old "messages:" section of config.yml (still honoured).
 */
public final class Lang {

    public static final String[] BUNDLED = {"tr", "en"};

    private final Map<String, Object> messages = new LinkedHashMap<>();

    public Lang(JavaPlugin plugin, String language, FileConfiguration config) {
        merge(bundled(plugin, "tr"));
        if (!"tr".equals(language)) {
            merge(bundled(plugin, language));
        }
        File file = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        if (file.exists()) {
            merge(YamlConfiguration.loadConfiguration(file));
        } else if (!"tr".equals(language) && !"en".equals(language)) {
            plugin.getLogger().warning("lang/" + language + ".yml bulunamadı, Türkçe kullanılıyor.");
        }
        ConfigurationSection legacy = config.getConfigurationSection("messages");
        if (legacy != null) {
            merge(legacy);
        }
    }

    public static void saveDefaults(JavaPlugin plugin) {
        for (String l : BUNDLED) {
            if (!new File(plugin.getDataFolder(), "lang/" + l + ".yml").exists()) {
                plugin.saveResource("lang/" + l + ".yml", false);
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
