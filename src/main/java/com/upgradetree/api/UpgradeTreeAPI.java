package com.upgradetree.api;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.data.PlayerData;
import com.upgradetree.service.UpgradeService;
import com.upgradetree.tree.TreeRegistry;
import com.upgradetree.tree.UpgradeNode;
import org.bukkit.entity.Player;

/**
 * Small, stable API for other plugins. Example:
 * <pre>
 * int lvl = UpgradeTreeAPI.level(player, "keskinlik");
 * UpgradeTreeAPI.addPoints(player, 5);
 * </pre>
 * Events live in {@code com.upgradetree.api.event}.
 */
public final class UpgradeTreeAPI {

    private static UpgradeTreePlugin plugin;

    private UpgradeTreeAPI() {
    }

    public static void init(UpgradeTreePlugin instance) {
        plugin = instance;
    }

    private static UpgradeTreePlugin plugin() {
        if (plugin == null) {
            throw new IllegalStateException("UpgradeTree is not enabled");
        }
        return plugin;
    }

    public static TreeRegistry trees() {
        return plugin().trees();
    }

    public static int level(Player player, String nodeId) {
        UpgradeNode n = plugin().trees().node(nodeId);
        return n == null ? 0 : Math.min(n.maxLevel, plugin().data().get(player).level(n.id));
    }

    public static void setLevel(Player player, String nodeId, int level) {
        UpgradeNode n = plugin().trees().node(nodeId);
        if (n == null) {
            throw new IllegalArgumentException("Unknown upgrade: " + nodeId);
        }
        PlayerData d = plugin().data().get(player);
        d.setLevel(n.id, Math.max(0, Math.min(n.maxLevel, level)));
        plugin().data().save(d);
        plugin().effects().apply(player);
    }

    public static double points(Player player) {
        return plugin().data().get(player).points();
    }

    public static void addPoints(Player player, double amount) {
        plugin().data().get(player).addPoints(amount);
    }

    public static int prestige(Player player) {
        return plugin().data().get(player).prestige();
    }

    /** Buys the next level as if the player clicked it (all rules and costs apply). */
    public static UpgradeService.Result purchase(Player player, String nodeId) {
        UpgradeNode n = plugin().trees().node(nodeId);
        if (n == null) {
            throw new IllegalArgumentException("Unknown upgrade: " + nodeId);
        }
        return plugin().service().purchase(player, n);
    }

    public static void openMenu(Player player) {
        plugin().menus().open(player);
    }
}
