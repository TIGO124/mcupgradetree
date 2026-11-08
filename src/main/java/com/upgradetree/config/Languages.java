package com.upgradetree.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Every language the server offers: the built-in ones plus each lang/&lt;code&gt;.yml. One of them is
 * the server language (config.yml "language"); players may pick their own with /tigo language.
 */
public final class Languages {

    private static final Pattern CODE = Pattern.compile("[a-z0-9_-]{2,10}");

    private final Map<String, Lang> all = new LinkedHashMap<>();
    private final Lang server;

    public Languages(JavaPlugin plugin, String serverLanguage, FileConfiguration config) {
        Set<String> codes = new LinkedHashSet<>(List.of(Lang.BUNDLED));
        File[] files = new File(plugin.getDataFolder(), "lang").listFiles((d, n) -> n.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String c = normalize(f.getName().substring(0, f.getName().length() - 4));
                if (c != null) {
                    codes.add(c);
                }
            }
        }
        String sv = normalize(serverLanguage);
        if (sv == null || !codes.contains(sv)) {
            plugin.getLogger().warning("lang/" + serverLanguage + ".yml bulunamadı, Türkçe kullanılıyor."
                    + " (language file not found, using Turkish)");
            sv = "tr";
        }
        for (String c : codes) {
            all.put(c, new Lang(plugin, c, c.equals(sv) ? config.getConfigurationSection("messages") : null, sv));
        }
        server = all.get(sv);
    }

    /** Lower-cased code if it is a valid language code ("tr", "en", "pt_br"...), else null. */
    public static String normalize(String code) {
        if (code == null) {
            return null;
        }
        String c = code.trim().toLowerCase(Locale.ROOT);
        return CODE.matcher(c).matches() ? c : null;
    }

    public Lang server() {
        return server;
    }

    /** The language with this code, or the server language when it is empty or unknown. */
    public Lang get(String code) {
        Lang l = code == null || code.isEmpty() ? null : all.get(code);
        return l == null ? server : l;
    }

    public boolean has(String code) {
        return code != null && all.containsKey(code);
    }

    public Set<String> codes() {
        return Collections.unmodifiableSet(all.keySet());
    }
}
