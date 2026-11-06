package com.upgradetree.hook;

import com.upgradetree.UpgradeTreePlugin;

/** Kept separate so PlaceholderAPI classes are only touched when PlaceholderAPI is installed. */
public final class PapiHook {

    private PapiHook() {
    }

    public static boolean register(UpgradeTreePlugin plugin) {
        try {
            return new PapiExpansion(plugin).register();
        } catch (Throwable t) {
            plugin.getLogger().warning("PlaceholderAPI bağlantısı kurulamadı: " + t);
            return false;
        }
    }
}
