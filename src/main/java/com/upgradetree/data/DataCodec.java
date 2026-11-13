package com.upgradetree.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Reads and writes player data as YAML text (files use it directly, SQL stores it in a column). */
public final class DataCodec {

    private DataCodec() {
    }

    public static String encode(PlayerData.Snapshot s) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", s.name());
        y.set("points", s.points());
        y.set("prestige", s.prestige());
        if (s.lastRespec() > 0) {
            y.set("last-respec", s.lastRespec());
        }
        if (!s.lastDaily().isEmpty()) {
            y.set("last-daily", s.lastDaily());
        }
        if (!s.menuMode().isEmpty()) {
            y.set("menu-mode", s.menuMode());
        }
        y.createSection("levels");
        for (Map.Entry<String, Integer> e : s.levels().entrySet()) {
            y.set("levels." + e.getKey(), e.getValue());
        }
        for (Map.Entry<String, Double> e : s.spent().entrySet()) {
            y.set("spent." + e.getKey(), e.getValue());
        }
        for (Map.Entry<String, Integer> e : s.chests().entrySet()) {
            if (e.getValue() > 0) {
                y.set("chests." + e.getKey(), e.getValue());
            }
        }
        return y.saveToString();
    }

    public static PlayerData.Snapshot decode(UUID uuid, String fallbackName, String text) throws InvalidConfigurationException {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(text);
        Map<String, Integer> levels = new LinkedHashMap<>();
        ConfigurationSection lv = y.getConfigurationSection("levels");
        if (lv != null) {
            for (String k : lv.getKeys(false)) {
                int v = lv.getInt(k);
                if (v > 0) {
                    levels.put(k, v);
                }
            }
        }
        Map<String, Double> spent = new LinkedHashMap<>();
        ConfigurationSection sp = y.getConfigurationSection("spent");
        if (sp != null) {
            for (String k : sp.getKeys(false)) {
                spent.put(k, sp.getDouble(k));
            }
        }
        Map<String, Integer> chests = new LinkedHashMap<>();
        ConfigurationSection ch = y.getConfigurationSection("chests");
        if (ch != null) {
            for (String k : ch.getKeys(false)) {
                int v = ch.getInt(k);
                if (v > 0) {
                    chests.put(k, v);
                }
            }
        }
        return new PlayerData.Snapshot(uuid, y.getString("name", fallbackName), levels, spent,
                Math.max(0, y.getDouble("points", 0)), Math.max(0, y.getInt("prestige", 0)),
                y.getLong("last-respec", 0), y.getString("last-daily", ""), y.getString("menu-mode", ""), chests);
    }
}
