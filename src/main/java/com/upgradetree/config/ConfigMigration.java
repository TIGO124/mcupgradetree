package com.upgradetree.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Brings a config.yml written by 2.2 up to date with the menu styles. Values still at the old
 * defaults (the dark-blue theme, the star background, the ✦ symbol) are removed or replaced, so
 * the chosen style applies; values the owner changed are kept and override the style.
 */
public final class ConfigMigration {

    /** Theme colours that 2.2 wrote by default. */
    static final Map<String, List<String>> OLD_THEME = Map.of(
            "background", List.of("#0A0D1A"),
            "panel-border", List.of("#262C4A"),
            "header", List.of("#151A30"),
            "card", List.of("#121629"),
            "locked", List.of("#2A3046"),
            "line", List.of("#8C96B4"),
            "hidden-line", List.of("#1F2438", "#1C2135"),
            "buy", List.of("#22C55E"),
            "deny", List.of("#E5484D"),
            "gold", List.of("#FFD54A"));

    private ConfigMigration() {
    }

    /** Updates old default values in place. Returns true if anything changed (the caller saves the file). */
    public static boolean apply(ConfigurationSection c) {
        boolean changed = false;
        for (Map.Entry<String, List<String>> e : OLD_THEME.entrySet()) {
            String path = "theme." + e.getKey();
            String now = c.getString(path);
            if (now != null && e.getValue().contains(now.trim().toUpperCase(Locale.ROOT))) {
                c.set(path, null);
                changed = true;
            }
        }
        if (changed && c.getConfigurationSection("theme") != null && c.getConfigurationSection("theme").getKeys(false).isEmpty()) {
            c.set("theme", null);
        }
        if ("✦".equals(c.getString("currency.symbol"))) {
            c.set("currency.symbol", "");
            changed = true;
        }
        // only what the file itself says counts (the plugin's defaults already have "background")
        if (c.contains("menu.stars", true) && !c.contains("menu.background", true)) {
            c.set("menu.background", c.getBoolean("menu.stars", true) ? "IMAGE" : "NONE");
            c.set("menu.stars", null);
            c.setComments("menu.background", List.of(
                    "Background of the panel: IMAGE (the background-image picture, via the resource pack), NONE (plain), STARS, PLANKS"));
            changed = true;
        }
        return changed;
    }
}
