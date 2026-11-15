package com.upgradetree.update;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/** Name, version and api-version written in the paper-plugin.yml (or plugin.yml) of a plugin jar. */
public record PluginJar(String name, String version, String apiVersion) {

    /** Reads a jar; null when it is no plugin jar (not a zip, or no plugin description). */
    public static PluginJar read(Path jar) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("paper-plugin.yml");
            if (entry == null) {
                entry = zip.getEntry("plugin.yml");
            }
            if (entry == null) {
                return null;
            }
            String text;
            try (InputStream in = zip.getInputStream(entry)) {
                text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(text);
            return new PluginJar(y.getString("name", ""), y.getString("version", ""), y.getString("api-version", ""));
        } catch (ZipException | InvalidConfigurationException e) {
            return null;
        }
    }
}
