package com.upgradetree.gui;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.chest.ChestManager;
import com.upgradetree.chest.ChestReward;
import com.upgradetree.chest.ChestType;
import com.upgradetree.config.Lang;
import com.upgradetree.currency.Currency;
import com.upgradetree.data.PlayerData;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chest-menu version of the Chests section (Bedrock players, or /tigo menu chest): every chest with
 * its price and contents. Left click buys, right click opens one you own.
 */
public final class ChestShopMenu implements InventoryHolder {

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int BACK = 45;
    private static final int INFO = 49;
    private static final int CLOSE = 53;
    private static final int MAX_LINES = 12;

    private final UpgradeTreePlugin plugin;
    private final Player player;
    private final Map<Integer, String> slots = new HashMap<>();
    private Inventory inventory;
    private String confirmId;
    private long confirmUntil;

    private ChestShopMenu(UpgradeTreePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public static void open(UpgradeTreePlugin plugin, Player p) {
        ChestShopMenu m = new ChestShopMenu(plugin, p);
        m.inventory = Bukkit.createInventory(m, 54, plugin.lang(p).plain("chests-gui-title"));
        m.render();
        p.openInventory(m.inventory);
        p.playSound(p.getLocation(), "minecraft:block.chest.open", SoundCategory.MASTER, 0.5f, 1.2f);
    }

    public static boolean isOpen(Player p) {
        return p.getOpenInventory().getTopInventory().getHolder() instanceof ChestShopMenu;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private Lang lang() {
        return plugin.lang(player);
    }

    void render() {
        inventory.clear();
        slots.clear();
        ItemStack filler = GuiItems.filler();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
            inventory.setItem(45 + i, filler);
        }
        List<ChestType> chests = plugin.chestTypes().chests();
        for (int i = 0; i < chests.size() && i < SLOTS.length; i++) {
            inventory.setItem(SLOTS[i], chestItem(chests.get(i)));
            slots.put(SLOTS[i], chests.get(i).id);
        }
        Currency c = plugin.currency();
        inventory.setItem(INFO, GuiItems.item(Material.SUNFLOWER, lang().plain("chests-tab"), List.of(
                lang().plain("menu-currency", "symbol", Text.escape(plugin.settings().currencySymbol),
                        "amount", Text.escape(c.available() ? c.formatShort(c.balance(player)) : "-"),
                        "currency", Text.escape(plugin.currencyName(lang()))),
                lang().plain("chests-progress", "count", String.valueOf(plugin.data().get(player).totalChests())))));
        inventory.setItem(BACK, GuiItems.item(Material.ARROW, lang().plain("admin-back"), List.of()));
        inventory.setItem(CLOSE, GuiItems.item(Material.BARRIER, lang().plain("gui-close"), List.of()));
    }

    private ItemStack chestItem(ChestType c) {
        PlayerData d = plugin.data().get(player);
        Currency cur = plugin.currency();
        int owned = d.chests(c.id);
        List<Component> lore = new ArrayList<>();
        for (String line : c.description(lang())) {
            lore.add(Text.mm(line));
        }
        lore.add(c.price <= 0 ? lang().plain("info-free")
                : lang().plain(plugin.chests().affordable(player, c) ? "info-cost" : "info-cost-poor", "cost", Text.escape(plugin.money(lang(), c.price))));
        lore.add(lang().plain("chest-owned", "count", String.valueOf(owned)));
        lore.add(Component.empty());
        lore.add(lang().plain("chests-contents", "chest", c.name(lang())));
        List<ChestReward> rewards = c.sortedRewards();
        for (int i = 0; i < rewards.size() && i < MAX_LINES; i++) {
            ChestReward r = rewards.get(i);
            lore.add(Text.mm(r.rarity.tag() + "■</color> <white>" + r.label(lang()) + "</white> <dark_gray>"
                    + lang().format("chest-percent", "chance", Text.num(c.chance(r) * 100))));
        }
        if (rewards.size() > MAX_LINES) {
            lore.add(lang().plain("chests-more", "count", String.valueOf(rewards.size() - MAX_LINES)));
        }
        lore.add(Component.empty());
        boolean confirm = c.id.equals(confirmId) && System.currentTimeMillis() < confirmUntil;
        lore.add(lang().plain(confirm ? "chests-gui-confirm" : "chests-gui-buy"));
        if (owned > 0) {
            lore.add(lang().plain("chests-gui-open", "count", String.valueOf(owned)));
        }
        ItemStack it = GuiItems.named(Icons.of(c.icon), Text.mm("<b>" + c.name(lang())), lore);
        it.setAmount(Math.max(1, Math.min(64, owned)));
        return GuiItems.glow(it, c.glint || confirm);
    }

    void click(int slot, ClickType type) {
        String id = slots.get(slot);
        if (id != null) {
            ChestType c = plugin.chestTypes().chest(id);
            if (c == null) {
                render();
                return;
            }
            if (type.isRightClick()) {
                open(c);
            } else {
                buy(c);
            }
            return;
        }
        switch (slot) {
            case BACK -> plugin.menus().open(player);
            case CLOSE -> player.closeInventory();
            default -> {
            }
        }
    }

    private void buy(ChestType c) {
        long now = System.currentTimeMillis();
        double confirmCost = plugin.settings().confirmCost;
        if (confirmCost > 0 && c.price >= confirmCost && plugin.chests().affordable(player, c)
                && !(c.id.equals(confirmId) && now < confirmUntil)) {
            confirmId = c.id;
            confirmUntil = now + 3000;
            sound("minecraft:block.note_block.pling", 1.2f);
            render();
            return;
        }
        confirmId = null;
        Currency cur = plugin.currency();
        switch (plugin.chests().buy(player, c)) {
            case SUCCESS -> {
                sound("minecraft:entity.player.levelup", 1.4f);
                if (plugin.chests().busy(player)) {
                    player.sendMessage(lang().msg("chest-bought-stash", "chest", c.name(lang())));
                    render();
                } else {
                    player.sendMessage(lang().msg("chest-bought", "chest", c.name(lang())));
                    open(c);
                }
            }
            case NOT_ENOUGH -> {
                sound("minecraft:block.note_block.bass", 0.7f);
                player.sendActionBar(lang().plain("purchase-not-enough", "currency", Text.escape(plugin.currencyName(lang())),
                        "cost", Text.escape(plugin.money(lang(), c.price)),
                        "balance", Text.escape(plugin.money(lang(), cur.available() ? cur.balance(player) : 0))));
                render();
            }
            case UNAVAILABLE -> player.sendActionBar(lang().plain("currency-unavailable"));
            case DISABLED -> player.sendMessage(lang().msg("chests-disabled"));
        }
    }

    /** Closes the shop first (the chest appears in front, or the roulette menu replaces this one). */
    private void open(ChestType c) {
        if (plugin.data().get(player).chests(c.id) <= 0) {
            sound("minecraft:block.note_block.bass", 0.7f);
            player.sendActionBar(lang().plain("chest-none", "chest", c.name(lang())));
            return;
        }
        if (plugin.chests().busy(player)) {
            player.sendActionBar(lang().plain("chest-busy"));
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (isOpen(player)) {
                player.closeInventory();
            }
            ChestManager.OpenResult r = plugin.chests().open(player, c);
            plugin.chests().tell(player, r, c);
        });
    }

    private void sound(String key, float pitch) {
        if (plugin.settings().sounds) {
            player.playSound(player.getLocation(), key, SoundCategory.MASTER, 0.7f, pitch);
        }
    }
}
