package com.upgradetree.gui;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.chest.ChestReward;
import com.upgradetree.chest.ChestType;
import com.upgradetree.chest.Strip;
import com.upgradetree.config.Lang;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chest-menu version of the CS2 roulette, for Bedrock players (display entities do not render
 * there): the middle row scrolls under the marker with the same strip and easing as the 3D one.
 * Closing the menu early still gives the prize.
 */
public final class RouletteMenu implements InventoryHolder {

    private static final int FIRST = 9;
    private static final int MARKER_TOP = 4;
    private static final int MARKER_BOTTOM = 22;
    private static final int LINGER = 60;

    private final UpgradeTreePlugin plugin;
    private final Player player;
    private final ChestType chest;
    private final ChestReward prize;
    private final Strip strip;
    private final Map<Integer, ItemStack> icons = new HashMap<>();
    private Inventory inventory;
    private int tick;
    private int shown = Integer.MIN_VALUE;
    private int revealedAt = -1;
    private boolean committed;
    private boolean done;

    private RouletteMenu(UpgradeTreePlugin plugin, Player player, ChestType chest, ChestReward prize, Strip strip) {
        this.plugin = plugin;
        this.player = player;
        this.chest = chest;
        this.prize = prize;
        this.strip = strip;
    }

    public static RouletteMenu open(UpgradeTreePlugin plugin, Player p, ChestType c, ChestReward prize, Strip strip) {
        RouletteMenu m = new RouletteMenu(plugin, p, c, prize, strip);
        m.inventory = Bukkit.createInventory(m, 27, plugin.lang().plain("roulette-title", "chest", c.name()));
        m.render(Strip.under(strip.position(0)));
        p.openInventory(m.inventory);
        m.sound("minecraft:block.chest.open", 0.9f, 0.9f);
        return m;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private Lang lang() {
        return plugin.lang();
    }

    public void tick() {
        if (done) {
            return;
        }
        if (!player.isOnline() || player.getOpenInventory().getTopInventory().getHolder() != this) {
            finishNow();
            return;
        }
        tick++;
        if (revealedAt < 0) {
            int under = Strip.under(strip.position(tick));
            if (under != shown) {
                render(under);
                sound("minecraft:block.note_block.hat", 0.5f, 1.75f);
            }
            if (strip.finished(tick)) {
                reveal();
                revealedAt = tick;
            }
        } else if (tick - revealedAt >= LINGER) {
            finishNow();
        }
    }

    private void render(int center) {
        shown = center;
        ItemStack empty = GuiItems.filler();
        for (int i = 0; i < 9; i++) {
            int r = strip.reward(center - 4 + i);
            inventory.setItem(FIRST + i, r < 0 ? empty : icon(chest.rewards.get(r)));
        }
        int r = strip.reward(center);
        Material glass = r < 0 ? Material.GRAY_STAINED_GLASS_PANE : glass(chest.rewards.get(r).rarity.color());
        ItemStack pane = GuiItems.named(new ItemStack(glass), Component.empty(), List.of());
        ItemStack marker = GuiItems.named(new ItemStack(Material.YELLOW_STAINED_GLASS_PANE), Text.mm("<value><b>▼"), List.of());
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, i == MARKER_TOP ? marker : pane);
            inventory.setItem(18 + i, i == MARKER_BOTTOM - 18 ? marker : pane);
        }
    }

    private ItemStack icon(ChestReward r) {
        return icons.computeIfAbsent(r.index, i -> GuiItems.named(r.icon(), Text.mm("<b>" + r.coloredLabel(lang())),
                List.of(Text.mm(r.rarity.name()))).clone());
    }

    private void reveal() {
        render(strip.winner);
        ItemStack won = icon(prize).clone();
        won.editMeta(m -> m.setEnchantmentGlintOverride(true));
        inventory.setItem(FIRST + 4, won);
        committed = true;
        plugin.chests().commit(player, chest, prize);
        if (prize.rarity.announce()) {
            sound("minecraft:ui.toast.challenge_complete", 0.9f, 1.0f);
        } else if (prize.rarity.rank() == 0) {
            sound("minecraft:entity.experience_orb.pickup", 0.8f, 1.1f);
        } else {
            sound("minecraft:entity.player.levelup", 0.7f, 1.0f + 0.15f * Math.min(3, prize.rarity.rank()));
        }
    }

    /** Ends now (closed, quit, reload): the prize is given if it was not yet. */
    public void finishNow() {
        if (done) {
            return;
        }
        done = true;
        if (!committed) {
            committed = true;
            plugin.chests().commit(player, chest, prize);
        }
        plugin.chests().menuDone(player, this);
        if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() == this) {
            player.closeInventory();
        }
    }

    /** Called when the player closed the menu. */
    void closed() {
        if (!done) {
            Bukkit.getScheduler().runTask(plugin, this::finishNow);
        }
    }

    /** Stained glass pane closest to a rarity colour (ARGB). */
    static Material glass(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        DyeColor best = DyeColor.WHITE;
        long bestD = Long.MAX_VALUE;
        for (DyeColor d : DyeColor.values()) {
            int c = d.getColor().asRGB();
            long dr = ((c >> 16) & 0xFF) - r;
            long dg = ((c >> 8) & 0xFF) - g;
            long db = (c & 0xFF) - b;
            long dist = dr * dr + dg * dg + db * db;
            if (dist < bestD) {
                bestD = dist;
                best = d;
            }
        }
        Material m = Material.matchMaterial(best.name() + "_STAINED_GLASS_PANE");
        return m == null ? Material.GRAY_STAINED_GLASS_PANE : m;
    }

    private void sound(String key, float volume, float pitch) {
        if (player.isOnline()) {
            player.playSound(player.getLocation(), key, SoundCategory.MASTER, volume, pitch);
        }
    }
}
