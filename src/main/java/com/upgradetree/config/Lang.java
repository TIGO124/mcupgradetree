package com.upgradetree.config;

import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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
 * Every message of the plugin: the built-in messages.yml, overridden by the copy in the plugin
 * folder (plugins/UpgradeTree/messages.yml). A key missing from the copy keeps its built-in text.
 */
public final class Lang {

    public static final String FILE = "messages.yml";
    /** Bumped when the built-in texts change in a way an old copy on a server must not hide. */
    public static final int VERSION = 1;

    private final Map<String, Object> messages = new LinkedHashMap<>();

    public Lang(JavaPlugin plugin) {
        merge(bundled(plugin));
        File file = new File(plugin.getDataFolder(), FILE);
        if (file.exists()) {
            merge(YamlConfiguration.loadConfiguration(file));
        }
    }

    /** Messages read from a YAML section only (tests). */
    public Lang(ConfigurationSection messages) {
        merge(messages);
    }

    /**
     * Writes the built-in file once. A copy older than {@link #VERSION} is kept as
     * messages.yml.v&lt;n&gt;.bak and replaced.
     */
    public static void saveDefault(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), FILE);
        if (!file.exists()) {
            plugin.saveResource(FILE, false);
            return;
        }
        int version = YamlConfiguration.loadConfiguration(file).getInt("messages-version", 0);
        if (version >= VERSION) {
            return;
        }
        File backup = new File(file.getParentFile(), FILE + ".v" + version + ".bak");
        if (backup.exists()) {
            backup = new File(file.getParentFile(), FILE + ".v" + version + "-" + System.currentTimeMillis() + ".bak");
        }
        if (file.renameTo(backup)) {
            plugin.saveResource(FILE, true);
            plugin.getLogger().warning(FILE + " was outdated and has been replaced. Your old texts are in " + backup.getName() + ".");
        } else {
            plugin.getLogger().warning(FILE + " is outdated but could not be replaced.");
        }
    }

    private static ConfigurationSection bundled(JavaPlugin plugin) {
        InputStream in = plugin.getResource(FILE);
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

    /** True if the key has a text. */
    public boolean has(String key) {
        return messages.containsKey(key);
    }

    /** Raw MiniMessage string for a message key (the key itself when it has no text). */
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

    /**
     * A chat message with prefix whose {name} placeholders of {@code parts} are whole components
     * (item names with their hover tooltip...); the other placeholders are text as usual.
     */
    public Component msg(String key, Map<String, Component> parts, String... kv) {
        String s = raw("prefix") + Text.apply(raw(key), kv);
        TagResolver.Builder tags = TagResolver.builder();
        for (Map.Entry<String, Component> e : parts.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", "<" + e.getKey() + ">");
            tags.resolver(Placeholder.component(e.getKey(), e.getValue()));
        }
        return Text.mm(s, tags.build());
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
