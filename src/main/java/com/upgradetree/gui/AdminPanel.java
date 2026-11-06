package com.upgradetree.gui;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.config.Settings;
import com.upgradetree.data.PlayerData;
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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Operator-only admin panel: online players (open one to edit), server settings that can be
 * switched with one click, and tools (reload, tree check, hologram, cleanup).
 */
public final class AdminPanel implements InventoryHolder {

    private static final int PREV = 0;
    private static final int INFO = 4;
    private static final int NEXT = 8;
    private static final int FIRST_HEAD = 9;
    private static final int HEADS = 36;
    private static final int RELOAD = 45;
    private static final int REVEAL = 46;
    private static final int CLICK = 47;
    private static final int LOCK = 48;
    private static final int CURRENCY = 49;
    private static final int MODE = 50;
    private static final int HOLOGRAM = 51;
    private static final int CLEANUP = 52;
    private static final int CLOSE = 53;
    private static final String[] CURRENCIES = {"XP_LEVELS", "POINTS", "VAULT"};
    private static final String[] MODES = {"AUTO", "DISPLAY", "CHEST"};

    private final UpgradeTreePlugin plugin;
    private final Player viewer;
    private final Map<Integer, UUID> heads = new HashMap<>();
    private Inventory inventory;
    private int page;
    private long cleanupUntil;

    private AdminPanel(UpgradeTreePlugin plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    public static void open(UpgradeTreePlugin plugin, Player viewer) {
        if (!viewer.isOp()) {
            viewer.sendMessage(plugin.lang().msg("admin-only-op"));
            return;
        }
        plugin.menus().close(viewer, true);
        AdminPanel panel = new AdminPanel(plugin, viewer);
        panel.inventory = Bukkit.createInventory(panel, 54, plugin.lang().plain("admin-title"));
        panel.render();
        viewer.openInventory(panel.inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private Lang lang() {
        return plugin.lang();
    }

    void render() {
        inventory.clear();
        heads.clear();
        ItemStack filler = GuiItems.filler();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
            inventory.setItem(45 + i, filler);
        }
        Settings s = plugin.settings();
        inventory.setItem(INFO, GuiItems.item(Material.COMPARATOR, lang().plain("admin-info-name", "version",
                plugin.getPluginMeta().getVersion()), lines("admin-info-lore",
                "trees", String.valueOf(plugin.trees().trees().size()),
                "nodes", String.valueOf(plugin.trees().nodes().size()),
                "currency", s.currencyType, "storage", plugin.data().storage().describe(),
                "issues", String.valueOf(plugin.trees().issues().size()),
                "online", String.valueOf(Bukkit.getOnlinePlayers().size()))));

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparing(p -> p.getName().toLowerCase(Locale.ROOT)));
        int pages = Math.max(1, (players.size() + HEADS - 1) / HEADS);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < HEADS; i++) {
            int idx = page * HEADS + i;
            if (idx >= players.size()) {
                break;
            }
            Player p = players.get(idx);
            PlayerData d = plugin.data().get(p);
            inventory.setItem(FIRST_HEAD + i, GuiItems.head(p.getUniqueId(), Text.mm("<white><b>" + Text.escape(p.getName())),
                    playerLore(d)));
            heads.put(FIRST_HEAD + i, p.getUniqueId());
        }
        if (page > 0) {
            inventory.setItem(PREV, GuiItems.item(Material.ARROW, lang().plain("admin-page-prev"), List.of()));
        }
        if (page < pages - 1) {
            inventory.setItem(NEXT, GuiItems.item(Material.ARROW, lang().plain("admin-page-next"), List.of()));
        }

        inventory.setItem(RELOAD, GuiItems.item(Material.CLOCK, lang().plain("admin-reload"), List.of()));
        inventory.setItem(REVEAL, toggle("admin-toggle-reveal", s.progressiveReveal));
        inventory.setItem(CLICK, toggle("admin-toggle-click", s.clickToBuy));
        inventory.setItem(LOCK, toggle("admin-toggle-lock", s.lockPlayer));
        inventory.setItem(CURRENCY, GuiItems.item(Material.GOLD_INGOT, lang().plain("admin-currency"),
                List.of(lang().plain("admin-value", "value", s.currencyType), lang().plain("admin-click-toggle"))));
        inventory.setItem(MODE, GuiItems.item(Material.PAINTING, lang().plain("admin-menu-mode"),
                List.of(lang().plain("admin-value", "value", s.menuMode.name()), lang().plain("admin-click-toggle"))));
        inventory.setItem(HOLOGRAM, GuiItems.item(Material.OAK_SIGN, lang().plain("admin-hologram"), lines("admin-hologram-lore")));
        boolean confirm = System.currentTimeMillis() < cleanupUntil;
        inventory.setItem(CLEANUP, GuiItems.glow(GuiItems.item(Material.MILK_BUCKET, lang().plain("admin-cleanup-all"),
                confirm ? List.of(lang().plain("admin-confirm")) : List.of()), confirm));
        inventory.setItem(CLOSE, GuiItems.item(Material.BARRIER, lang().plain("admin-close"), List.of()));
    }

    List<Component> playerLore(PlayerData d) {
        List<Component> lore = lines("admin-player-stats", "points", Text.num(d.points()), "prestige", String.valueOf(d.prestige()),
                "levels", String.valueOf(plugin.service().totalLevels(d)),
                "owned", String.valueOf(plugin.service().ownedAll(d)), "total", String.valueOf(plugin.trees().nodes().size()));
        lore.add(Component.empty());
        lore.add(lang().plain("admin-click-edit"));
        return lore;
    }

    private ItemStack toggle(String key, boolean on) {
        return GuiItems.item(on ? Material.LIME_DYE : Material.GRAY_DYE, lang().plain(key),
                List.of(lang().plain("admin-state", "state", lang().raw(on ? "admin-on" : "admin-off")), lang().plain("admin-click-toggle")));
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
        UUID head = heads.get(slot);
        if (head != null) {
            Player p = Bukkit.getPlayer(head);
            if (p != null) {
                AdminPlayerPage.open(plugin, viewer, p.getUniqueId(), p.getName(), null);
            }
            return;
        }
        Settings s = plugin.settings();
        switch (slot) {
            case PREV -> page--;
            case NEXT -> page++;
            case INFO -> {
                List<String> issues = plugin.trees().issues();
                viewer.sendMessage(issues.isEmpty()
                        ? lang().msg("check-ok", "trees", String.valueOf(plugin.trees().trees().size()),
                        "nodes", String.valueOf(plugin.trees().nodes().size()))
                        : lang().msg("check-header", "count", String.valueOf(issues.size())));
                for (String issue : issues) {
                    viewer.sendMessage(lang().plain("check-line", "issue", Text.escape(issue)));
                }
            }
            case RELOAD -> {
                boolean ok = plugin.reloadAll();
                viewer.sendMessage(ok ? lang().msg("reloaded", "trees", String.valueOf(plugin.trees().trees().size()),
                        "nodes", String.valueOf(plugin.trees().nodes().size())) : lang().msg("reload-failed"));
            }
            case REVEAL -> change("menu.progressive-reveal", !s.progressiveReveal, "admin-toggle-reveal");
            case CLICK -> change("menu.click-to-buy", !s.clickToBuy, "admin-toggle-click");
            case LOCK -> change("menu.lock-player", !s.lockPlayer, "admin-toggle-lock");
            case CURRENCY -> {
                String next = next(CURRENCIES, s.currencyType);
                String name = plugin.getConfig().getString("currency.name", "");
                if (List.of("Seviye", "Puan", "Para", "Level", "Levels", "Points", "Money", "").contains(name)) {
                    plugin.getConfig().set("currency.name", switch (next) {
                        case "POINTS" -> lang().raw("admin-default-points");
                        case "VAULT" -> lang().raw("admin-default-money");
                        default -> lang().raw("admin-default-levels");
                    });
                }
                change("currency.type", next, "admin-currency");
            }
            case MODE -> change("menu.mode", next(MODES, s.menuMode.name()), "admin-menu-mode");
            case HOLOGRAM -> {
                boolean place = !type.isRightClick();
                plugin.leaderboard().setHologram(place ? viewer.getLocation() : null);
                viewer.sendMessage(lang().msg(place ? "hologram-set" : "hologram-removed"));
            }
            case CLEANUP -> {
                long now = System.currentTimeMillis();
                if (now >= cleanupUntil) {
                    cleanupUntil = now + 3000;
                    sound("minecraft:block.note_block.pling", 0.9f);
                    break;
                }
                cleanupUntil = 0;
                int count = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    plugin.menus().close(p, true);
                    if (plugin.effects().clearAll(p)) {
                        count++;
                    }
                }
                viewer.sendMessage(lang().msg("cleanup-done", "count", String.valueOf(count)));
            }
            case CLOSE -> {
                viewer.closeInventory();
                return;
            }
            default -> {
                return;
            }
        }
        sound("minecraft:ui.button.click", 1.2f);
        render();
    }

    private void change(String path, Object value, String nameKey) {
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
        plugin.reloadAll();
        String shown = value instanceof Boolean b ? lang().raw(b ? "admin-on" : "admin-off") : String.valueOf(value);
        viewer.sendMessage(lang().msg("admin-setting-changed", "setting", lang().raw(nameKey), "value", shown));
    }

    private static String next(String[] values, String current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) {
                return values[(i + 1) % values.length];
            }
        }
        return values[0];
    }

    private void sound(String key, float pitch) {
        viewer.playSound(viewer.getLocation(), key, SoundCategory.MASTER, 0.6f, pitch);
    }
}
