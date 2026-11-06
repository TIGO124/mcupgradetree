package com.upgradetree.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Detects Bedrock players (Geyser / Floodgate) through reflection, so neither plugin is required.
 * Display entities do not render properly for them, so they get the chest menu instead.
 */
public final class BedrockHook {

    private Object floodgate;
    private Method floodgateCheck;
    private Object geyser;
    private Method geyserCheck;

    public BedrockHook() {
        try {
            if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
                Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                floodgate = api.getMethod("getInstance").invoke(null);
                floodgateCheck = api.getMethod("isFloodgatePlayer", UUID.class);
            }
        } catch (Throwable ignored) {
            floodgate = null;
        }
        try {
            if (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null) {
                Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi");
                geyser = api.getMethod("api").invoke(null);
                geyserCheck = api.getMethod("isBedrockPlayer", UUID.class);
            }
        } catch (Throwable ignored) {
            geyser = null;
        }
    }

    public boolean active() {
        return floodgate != null || geyser != null;
    }

    public boolean isBedrock(Player p) {
        try {
            if (floodgate != null && Boolean.TRUE.equals(floodgateCheck.invoke(floodgate, p.getUniqueId()))) {
                return true;
            }
            if (geyser != null && Boolean.TRUE.equals(geyserCheck.invoke(geyser, p.getUniqueId()))) {
                return true;
            }
        } catch (Throwable ignored) {
            // treat as Java player
        }
        return false;
    }
}
