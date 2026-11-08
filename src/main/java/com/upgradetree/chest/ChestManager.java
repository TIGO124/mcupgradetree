package com.upgradetree.chest;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.currency.Currency;
import com.upgradetree.data.PlayerData;
import com.upgradetree.gui.RouletteMenu;
import com.upgradetree.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The Chests section: buying, the per-player stash of unopened chests, opening (a chest in front of
 * Java players, a chest-menu roulette for Bedrock players, who cannot see display entities) and
 * handing out the prize. A prize is rolled when the opening starts and given when the strip stops;
 * the chest leaves the stash only then, so a crash mid-spin never loses a chest.
 */
public final class ChestManager implements Listener {

    public enum BuyResult { SUCCESS, NOT_ENOUGH, UNAVAILABLE, DISABLED }

    public enum OpenResult { SUMMONED, MENU, BUSY, NONE, DISABLED }

    private final UpgradeTreePlugin plugin;
    private final Map<UUID, CrateDrop> drops = new HashMap<>();
    private final Map<UUID, RouletteMenu> rolls = new HashMap<>();

    public ChestManager(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    public ChestRegistry registry() {
        return plugin.chestTypes();
    }

    // ------------------------------------------------------------------ buying & giving

    /** The current definition of a chest (menus may hold one from before a reload), or null. */
    private ChestType current(ChestType c) {
        return c == null || !registry().active() ? null : registry().chest(c.id);
    }

    public BuyResult buy(Player p, ChestType chest) {
        ChestType c = current(chest);
        if (c == null) {
            return BuyResult.DISABLED;
        }
        Currency cur = plugin.currency();
        if (c.price > 0) {
            if (!cur.available()) {
                return BuyResult.UNAVAILABLE;
            }
            if (cur.balance(p) + 1e-9 < c.price || !cur.withdraw(p, c.price)) {
                return BuyResult.NOT_ENOUGH;
            }
        }
        PlayerData d = plugin.data().get(p);
        d.addChests(c.id, 1);
        plugin.data().save(d);
        return BuyResult.SUCCESS;
    }

    /** True when the player can pay for the chest right now. */
    public boolean affordable(Player p, ChestType c) {
        Currency cur = plugin.currency();
        return c.price <= 0 || (cur.available() && cur.balance(p) + 1e-9 >= c.price);
    }

    /** Adds chests to a (possibly offline) player's stash. */
    public void give(PlayerData d, ChestType c, int amount) {
        d.addChests(c.id, amount);
        plugin.data().save(d);
    }

    // ------------------------------------------------------------------ opening

    /** True while one of the player's chests is being opened (only one at a time). */
    public boolean busy(Player p) {
        CrateDrop drop = drops.get(p.getUniqueId());
        return (drop != null && !drop.waiting() && !drop.done()) || rolls.containsKey(p.getUniqueId());
    }

    /**
     * Opens one of the player's chests: Java players get the chest in front of them (right click
     * opens it), Bedrock players the roulette menu right away.
     */
    public OpenResult open(Player p, ChestType chest) {
        ChestType c = current(chest);
        if (c == null) {
            return OpenResult.DISABLED;
        }
        if (busy(p)) {
            return OpenResult.BUSY;
        }
        if (plugin.data().get(p).chests(c.id) <= 0) {
            return OpenResult.NONE;
        }
        CrateDrop old = drops.remove(p.getUniqueId());
        if (old != null) {
            old.discard();
        }
        if (plugin.bedrock().isBedrock(p)) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            ChestReward prize = c.roll(rnd);
            Strip strip = Strip.build(c.chances(), prize.index, registry().spinTicks, rnd);
            rolls.put(p.getUniqueId(), RouletteMenu.open(plugin, p, c, prize, strip));
            return OpenResult.MENU;
        }
        CrateDrop drop = new CrateDrop(plugin, this, p, c, CrateDrop.spot(p));
        drop.spawn();
        drops.put(p.getUniqueId(), drop);
        return OpenResult.SUMMONED;
    }

    /** Opens a chest a few ticks later (after the 3D menu has closed) and reports problems. */
    public void openLater(Player p, ChestType c, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                tell(p, open(p, c), c);
            }
        }, delay);
    }

    /** Chat feedback for a failed {@link #open}. */
    public void tell(Player p, OpenResult r, ChestType c) {
        Lang lang = plugin.lang(p);
        switch (r) {
            case BUSY -> p.sendMessage(lang.msg("chest-busy"));
            case NONE -> p.sendMessage(lang.msg("chest-none", "chest", c.name(lang)));
            case DISABLED -> p.sendMessage(lang.msg("chests-disabled"));
            default -> {
            }
        }
    }

    /** The roulette menu of a Bedrock player ended (it gives the prize itself). */
    public void menuDone(Player p, RouletteMenu menu) {
        rolls.remove(p.getUniqueId(), menu);
    }

    /**
     * Gives the prize and takes the chest from the stash. False (nothing given, the chest stays) if
     * the player is dead (a respawn would wipe the prize) or the chest is no longer in the stash.
     */
    public boolean commit(Player p, ChestType c, ChestReward prize) {
        if (p.isDead()) {
            return false;
        }
        PlayerData d = plugin.data().get(p);
        if (!d.takeChest(c.id)) {
            plugin.getLogger().warning(p.getName() + ": açılan sandık (" + c.id + ") artık yok, ödül verilmedi.");
            return false;
        }
        plugin.data().save(d);
        Lang lang = plugin.lang(p);
        if (prize.type == ChestReward.Type.LEVELS) {
            p.giveExpLevels(prize.levels);
        } else {
            for (ItemStack left : p.getInventory().addItem(prize.create(lang)).values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), left);
            }
        }
        p.sendMessage(lang.msg("chest-won", "chest", c.name(lang), "reward", prize.coloredLabel(lang),
                "rarity", prize.rarity.name(lang)));
        if (prize.rarity.announce()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                Lang l = plugin.lang(online);
                online.sendMessage(l.msg("chest-broadcast", "player", Text.escape(p.getName()), "chest", c.name(l),
                        "reward", prize.coloredLabel(l), "rarity", prize.rarity.name(l)));
            }
            Lang console = plugin.lang();
            Bukkit.getConsoleSender().sendMessage(console.msg("chest-broadcast", "player", Text.escape(p.getName()),
                    "chest", c.name(console), "reward", prize.coloredLabel(console), "rarity", prize.rarity.name(console)));
        }
        return true;
    }

    // ------------------------------------------------------------------ ticking & shutdown

    public void tick() {
        if (!drops.isEmpty()) {
            for (CrateDrop drop : new ArrayList<>(drops.values())) {
                try {
                    drop.tick();
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Sandık animasyonu hatası (" + drop.owner.getName() + "): " + ex);
                    drop.finishNow();
                }
                if (drop.done()) {
                    drops.remove(drop.owner.getUniqueId(), drop);
                }
            }
        }
        if (!rolls.isEmpty()) {
            for (RouletteMenu menu : new ArrayList<>(rolls.values())) {
                menu.tick();
            }
        }
    }

    /** Ends every opening at once (prizes rolled so far are given) and removes waiting chests. */
    public void finishAll() {
        for (CrateDrop drop : new ArrayList<>(drops.values())) {
            drop.finishNow();
        }
        drops.clear();
        for (RouletteMenu menu : new ArrayList<>(rolls.values())) {
            menu.finishNow();
        }
        rolls.clear();
    }

    /** Quit, world change or a long teleport: an opening ends at once, a waiting chest goes back. */
    private void finish(Player p, boolean tell) {
        CrateDrop drop = drops.remove(p.getUniqueId());
        if (drop != null) {
            boolean waiting = drop.waiting();
            drop.finishNow();
            if (waiting && tell) {
                p.sendMessage(plugin.lang(p).msg("chest-returned"));
            }
        }
        RouletteMenu menu = rolls.remove(p.getUniqueId());
        if (menu != null) {
            menu.finishNow();
        }
    }

    // ------------------------------------------------------------------ events

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEntityEvent e) {
        for (CrateDrop drop : drops.values()) {
            if (!drop.owns(e.getRightClicked())) {
                continue;
            }
            e.setCancelled(true);
            if (e.getHand() == EquipmentSlot.HAND && drop.owner.equals(e.getPlayer())) {
                drop.click();
            }
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        finish(e.getPlayer(), false);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        finish(e.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        CrateDrop drop = drops.get(e.getPlayer().getUniqueId());
        if (drop != null && (e.getFrom().getWorld() != e.getTo().getWorld() || e.getFrom().distanceSquared(e.getTo()) > 32 * 32)) {
            Player p = e.getPlayer();
            Bukkit.getScheduler().runTask(plugin, () -> finish(p, true));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || !registry().active()) {
                return;
            }
            int count = plugin.data().get(p).totalChests();
            if (count > 0) {
                p.sendMessage(plugin.lang(p).msg("chests-waiting", "count", String.valueOf(count)));
            }
        }, 80L);
    }
}
