package com.upgradetree.gui;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.data.PlayerData;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.tree.UpgradeTree;
import com.upgradetree.util.Icons;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Operator-only page for one player: points, prestige, every node level, reset/max, 3D editing. */
public final class AdminPlayerPage implements InventoryHolder {

    private static final int BACK = 0;
    private static final int HEAD = 4;
    private static final int CLOSE = 8;
    private static final int[] POINT_SLOTS = {9, 10, 11, 13, 14, 15};
    private static final int[] POINT_DELTAS = {-100, -10, -1, 1, 10, 100};
    private static final int POINTS_INFO = 12;
    private static final int PRESTIGE_DOWN = 18;
    private static final int PRESTIGE_INFO = 19;
    private static final int PRESTIGE_UP = 20;
    private static final int EDIT_3D = 22;
    private static final int MAX_ALL = 24;
    private static final int RESET_ALL = 25;
    private static final int CLEANUP = 26;
    private static final int NODE_PREV = 27;
    private static final int FIRST_TAB = 28;
    private static final int TABS = 7;
    private static final int NODE_NEXT = 35;
    private static final int FIRST_NODE = 36;
    private static final int NODES = 18;

    private final UpgradeTreePlugin plugin;
    private final Player viewer;
    private final UUID uuid;
    private final String name;
    private final PlayerData offline;
    private final Map<Integer, String> nodeSlots = new HashMap<>();
    private Inventory inventory;
    private UpgradeTree tree;
    private int page;
    private long maxUntil;
    private long resetUntil;

    private AdminPlayerPage(UpgradeTreePlugin plugin, Player viewer, UUID uuid, String name, PlayerData offline) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.uuid = uuid;
        this.name = name;
        this.offline = offline;
        this.tree = plugin.trees().first();
    }

    /** {@code offline} is the loaded data of an offline player, null for online players. */
    public static void open(UpgradeTreePlugin plugin, Player viewer, UUID uuid, String name, PlayerData offline) {
        if (!viewer.isOp()) {
            viewer.sendMessage(plugin.lang().msg("admin-only-op"));
            return;
        }
        AdminPlayerPage page = new AdminPlayerPage(plugin, viewer, uuid, name, offline);
        page.inventory = Bukkit.createInventory(page, 54, plugin.lang().plain("admin-player-title", "player", Text.escape(name)));
        page.render();
        viewer.openInventory(page.inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private Lang lang() {
        return plugin.lang();
    }

    private Player online() {
        return Bukkit.getPlayer(uuid);
    }

    private PlayerData data() {
        Player p = online();
        return p != null ? plugin.data().get(p) : offline;
    }

    void render() {
        inventory.clear();
        nodeSlots.clear();
        PlayerData d = data();
        if (d == null) {
            viewer.closeInventory();
            return;
        }
        ItemStack filler = GuiItems.filler();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(BACK, GuiItems.item(Material.ARROW, lang().plain("admin-back"), List.of()));
        inventory.setItem(CLOSE, GuiItems.item(Material.BARRIER, lang().plain("admin-close"), List.of()));
        List<Component> lore = lines("admin-player-stats", "points", Text.num(d.points()), "prestige", String.valueOf(d.prestige()),
                "levels", String.valueOf(plugin.service().totalLevels(d)), "owned", String.valueOf(plugin.service().ownedAll(d)),
                "total", String.valueOf(plugin.trees().nodes().size()));
        inventory.setItem(HEAD, GuiItems.head(uuid, Text.mm("<hi><b>" + Text.escape(name)), lore));

        for (int i = 0; i < POINT_SLOTS.length; i++) {
            int delta = POINT_DELTAS[i];
            inventory.setItem(POINT_SLOTS[i], GuiItems.item(delta < 0 ? Material.RED_DYE : Material.LIME_DYE,
                    lang().plain("admin-points-button", "amount", (delta > 0 ? "+" : "") + delta), List.of()));
        }
        List<Component> pointsLore = "POINTS".equals(plugin.settings().currencyType) ? List.of() : List.of(lang().plain("admin-points-note"));
        inventory.setItem(POINTS_INFO, GuiItems.item(Material.SUNFLOWER, lang().plain("admin-points-info", "points", Text.num(d.points())), pointsLore));
        inventory.setItem(PRESTIGE_DOWN, GuiItems.item(Material.RED_DYE, lang().plain("admin-prestige-button", "amount", "-1"), List.of()));
        inventory.setItem(PRESTIGE_INFO, GuiItems.item(Material.NETHER_STAR,
                lang().plain("admin-prestige-info", "prestige", String.valueOf(d.prestige())), List.of()));
        inventory.setItem(PRESTIGE_UP, GuiItems.item(Material.LIME_DYE, lang().plain("admin-prestige-button", "amount", "+1"), List.of()));
        inventory.setItem(EDIT_3D, GuiItems.glow(GuiItems.item(Material.ENDER_EYE, lang().plain("admin-edit3d"), lines("admin-edit3d-lore")), true));
        long now = System.currentTimeMillis();
        inventory.setItem(MAX_ALL, GuiItems.glow(GuiItems.item(Material.EXPERIENCE_BOTTLE, lang().plain("admin-max-all"),
                now < maxUntil ? List.of(lang().plain("admin-confirm")) : List.of()), now < maxUntil));
        inventory.setItem(RESET_ALL, GuiItems.glow(GuiItems.item(Material.TNT, lang().plain("admin-reset-all"),
                now < resetUntil ? List.of(lang().plain("admin-confirm")) : List.of()), now < resetUntil));
        if (online() != null) {
            inventory.setItem(CLEANUP, GuiItems.item(Material.MILK_BUCKET, lang().plain("admin-cleanup-player"), List.of()));
        }

        List<UpgradeTree> trees = plugin.trees().trees();
        for (int i = 0; i < TABS && i < trees.size(); i++) {
            UpgradeTree t = trees.get(i);
            inventory.setItem(FIRST_TAB + i, GuiItems.glow(GuiItems.item(t.tabIcon, t.tabName(), List.of(lang().plain("gui-tab-hint"))), t == tree));
        }
        if (tree == null) {
            return;
        }
        List<UpgradeNode> nodes = new ArrayList<>(tree.nodes());
        nodes.sort(Comparator.comparingInt((UpgradeNode n) -> tree.depth(n)).thenComparingDouble(n -> n.y).thenComparingDouble(n -> n.x));
        int pages = Math.max(1, (nodes.size() + NODES - 1) / NODES);
        page = Math.max(0, Math.min(page, pages - 1));
        if (page > 0) {
            inventory.setItem(NODE_PREV, GuiItems.item(Material.ARROW, lang().plain("admin-page-prev"), List.of()));
        }
        if (page < pages - 1) {
            inventory.setItem(NODE_NEXT, GuiItems.item(Material.ARROW, lang().plain("admin-page-next"), List.of()));
        }
        for (int i = 0; i < NODES; i++) {
            int idx = page * NODES + i;
            if (idx >= nodes.size()) {
                break;
            }
            UpgradeNode n = nodes.get(idx);
            int lvl = Math.min(d.level(n.id), n.maxLevel);
            ItemStack it = GuiItems.named(Icons.of(n), Text.mm("<b>" + n.name()),
                    lines("admin-node-lore", "level", String.valueOf(lvl), "max", String.valueOf(n.maxLevel)));
            it.setAmount(Math.max(1, Math.min(64, lvl)));
            inventory.setItem(FIRST_NODE + i, GuiItems.glow(it, lvl > 0));
            nodeSlots.put(FIRST_NODE + i, n.id);
        }
    }

    private List<Component> lines(String key, String... kv) {
        List<Component> out = new ArrayList<>();
        for (String line : lang().rawList(key)) {
            out.add(Text.mm(Text.apply(line, kv)));
        }
        return out;
    }

    void click(int slot, ClickType type) {
        if (!viewer.isOp()) {
            viewer.closeInventory();
            viewer.sendMessage(lang().msg("admin-only-op"));
            return;
        }
        PlayerData d = data();
        if (d == null) {
            return;
        }
        String nodeId = nodeSlots.get(slot);
        if (nodeId != null) {
            UpgradeNode n = plugin.trees().node(nodeId);
            if (n != null) {
                int lvl = Math.min(d.level(n.id), n.maxLevel);
                int next = switch (type) {
                    case SHIFT_LEFT -> n.maxLevel;
                    case SHIFT_RIGHT -> 0;
                    case RIGHT -> lvl - 1;
                    default -> lvl + 1;
                };
                next = Math.max(0, Math.min(n.maxLevel, next));
                if (next != lvl) {
                    d.setLevel(n.id, next);
                    changed(d, next == 0);
                    viewer.sendActionBar(lang().plain("admin-set", "player", Text.escape(name), "node", n.name(),
                            "level", String.valueOf(next)));
                }
            }
            return;
        }
        for (int i = 0; i < POINT_SLOTS.length; i++) {
            if (POINT_SLOTS[i] == slot) {
                d.addPoints(POINT_DELTAS[i]);
                changed(d, false);
                return;
            }
        }
        long now = System.currentTimeMillis();
        switch (slot) {
            case BACK -> AdminPanel.open(plugin, viewer);
            case CLOSE -> viewer.closeInventory();
            case PRESTIGE_DOWN, PRESTIGE_UP -> {
                d.prestige(d.prestige() + (slot == PRESTIGE_UP ? 1 : -1));
                changed(d, false);
            }
            case EDIT_3D -> {
                viewer.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.menus().openAdmin(viewer, uuid, name, offline));
            }
            case MAX_ALL -> {
                if (now >= maxUntil) {
                    maxUntil = now + 3000;
                    render();
                    return;
                }
                maxUntil = 0;
                for (UpgradeNode n : plugin.trees().nodes()) {
                    d.setLevel(n.id, n.maxLevel);
                }
                changed(d, false);
            }
            case RESET_ALL -> {
                if (now >= resetUntil) {
                    resetUntil = now + 3000;
                    render();
                    return;
                }
                resetUntil = 0;
                plugin.progress().adminReset(d, online());
                changed(d, true);
                viewer.sendMessage(lang().msg("reset-done", "player", Text.escape(name)));
            }
            case CLEANUP -> {
                Player t = online();
                if (t != null) {
                    plugin.menus().close(t, true);
                    plugin.effects().clearAll(t);
                    viewer.sendMessage(lang().msg("cleanup-done", "count", "1"));
                }
            }
            case NODE_PREV -> {
                page--;
                render();
            }
            case NODE_NEXT -> {
                page++;
                render();
            }
            default -> {
                int tab = slot - FIRST_TAB;
                List<UpgradeTree> trees = plugin.trees().trees();
                if (tab >= 0 && tab < TABS && tab < trees.size()) {
                    tree = trees.get(tab);
                    page = 0;
                    render();
                }
            }
        }
    }

    /** Saves, re-applies effects of an online target and refreshes the page. */
    private void changed(PlayerData d, boolean cleared) {
        plugin.data().save(d);
        Player t = online();
        if (t != null) {
            plugin.effects().apply(t);
            if (cleared) {
                plugin.effects().clearManagedPotions(t);
                plugin.effects().apply(t);
            }
            if (!t.equals(viewer)) {
                plugin.menus().refresh(t);
            }
        }
        plugin.leaderboard().touch(d);
        viewer.playSound(viewer.getLocation(), "minecraft:ui.button.click", SoundCategory.MASTER, 0.6f, 1.3f);
        render();
    }
}
