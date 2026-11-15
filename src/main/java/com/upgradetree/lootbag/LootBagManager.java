package com.upgradetree.lootbag;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.chest.ChestManager;
import com.upgradetree.config.Lang;
import com.upgradetree.util.Palette;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Loot bags in the game: a mob killed by a player sometimes drops one (hostile mobs more often),
 * and a right click with a bag opens it into its items. Bags are plain items marked with their
 * rarity id, so they can be traded, stored and stacked like any other item.
 */
public final class LootBagManager implements Listener {

    /** Bags are rabbit hide underneath (no use of its own); the item model makes it look like a bundle. */
    private static final Material BASE = Material.RABBIT_HIDE;
    /** Bags in both hands, or a held-down button, never open two bags at once. */
    private static final long OPEN_COOLDOWN_MS = 250;

    private final UpgradeTreePlugin plugin;
    private final NamespacedKey key;
    private final Map<UUID, Long> lastOpen = new HashMap<>();

    public LootBagManager(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "lootbag");
    }

    private LootBagRegistry types() {
        return plugin.lootBagTypes();
    }

    // ------------------------------------------------------------------ items

    /** A stack of bags of one rarity (at most 64). */
    public ItemStack create(LootBagType t, int amount) {
        ItemStack it = new ItemStack(BASE, Math.max(1, Math.min(64, amount)));
        String items = t.minItems() == t.maxItems() ? String.valueOf(t.maxItems()) : t.minItems() + "-" + t.maxItems();
        List<Component> lore = new ArrayList<>();
        for (String line : plugin.lang().rawList("lootbag-lore")) {
            lore.add(Text.mm(Text.apply(line, "items", items)).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        it.editMeta(m -> {
            m.itemName(Text.mm(t.name));
            m.lore(lore);
            NamespacedKey model = NamespacedKey.fromString(t.model);
            if (model != null) {
                m.setItemModel(model);
            }
            if (t.glint) {
                m.setEnchantmentGlintOverride(true);
            }
            m.getPersistentDataContainer().set(key, PersistentDataType.STRING, t.id);
        });
        return it;
    }

    /** The rarity id of a loot bag, or null when the item is not one. */
    public String idOf(ItemStack it) {
        if (it == null || it.isEmpty() || !it.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = it.getItemMeta();
        return meta == null ? null : meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    /** Puts bags into a player's inventory; what does not fit drops at their feet. */
    public void give(Player p, LootBagType t, int amount) {
        for (int n : ChestManager.stacks(amount, 64)) {
            for (ItemStack left : p.getInventory().addItem(create(t, n)).values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), left);
            }
        }
    }

    // ------------------------------------------------------------------ dropping

    /** Only mobs killed by a player drop bags; a death another plugin cancelled drops nothing. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        LootBagRegistry r = types();
        LivingEntity dead = e.getEntity();
        if (!r.active() || !(dead instanceof Mob) || dead.hasMetadata("NPC")) {
            return;
        }
        Player killer = dead.getKiller();
        if (killer == null || plugin.settings().worldDisabled(dead.getWorld().getName())
                || r.ignoredSpawnReasons.contains(dead.getEntitySpawnReason())) {
            return;
        }
        boolean hostile = dead instanceof Enemy;
        if (!hostile && dead instanceof Ageable baby && !baby.isAdult()) {
            return; // like in vanilla, baby animals drop nothing (breeding farms)
        }
        LootBagType t = r.roll(hostile, ThreadLocalRandom.current().nextDouble());
        if (t != null) {
            drop(t, dead, killer);
        }
    }

    private void drop(LootBagType t, LivingEntity dead, Player killer) {
        Location at = dead.getLocation().add(0, 0.5, 0);
        boolean showName = types().showName;
        dead.getWorld().dropItemNaturally(at, create(t, 1), item -> {
            if (showName) {
                item.customName(Text.mm(t.name));
                item.setCustomNameVisible(true);
            }
            item.setGlowing(t.glow);
        });
        dead.getWorld().spawnParticle(Particle.DUST, at, 14 + 4 * Math.min(6, t.rank), 0.3, 0.3, 0.3, 0, dust(t));
        Lang lang = plugin.lang();
        killer.sendActionBar(lang.plain("lootbag-dropped", "bag", t.name));
        if (t.announce) {
            sound(killer, "minecraft:ui.toast.challenge_complete", 0.8f, 1.0f);
            Component news = lang.msg("lootbag-broadcast", "player", Text.escape(killer.getName()), "bag", t.name,
                    "mob", "<lang:" + dead.getType().translationKey() + ">");
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(news);
            }
            Bukkit.getConsoleSender().sendMessage(news);
        } else {
            sound(killer, "minecraft:block.amethyst_block.chime", 1.0f, 0.8f + 0.15f * Math.min(4, t.rank));
        }
    }

    // ------------------------------------------------------------------ opening

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent e) {
        Action a = e.getAction();
        if ((a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) || e.getHand() == null) {
            return;
        }
        String id = idOf(e.getItem());
        if (id == null) {
            return;
        }
        Player p = e.getPlayer();
        if (e.useItemInHand() == Event.Result.DENY || plugin.menus().capturesInput(p)) {
            return; // the 3D menu (or another plugin) took this click
        }
        Block block = e.getClickedBlock();
        if (a == Action.RIGHT_CLICK_BLOCK && block != null && block.getType().isInteractable() && !p.isSneaking()) {
            return; // chests, doors, buttons... work as usual
        }
        e.setCancelled(true);
        open(p, e.getHand(), id);
    }

    private void open(Player p, EquipmentSlot hand, String id) {
        Lang lang = plugin.lang();
        LootBagRegistry r = types();
        if (!r.enabled) {
            p.sendActionBar(lang.plain("lootbags-disabled"));
            return;
        }
        LootBagType t = r.type(id);
        if (t == null) {
            p.sendActionBar(lang.plain("lootbag-unknown"));
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastOpen.get(p.getUniqueId());
        ItemStack held = p.getInventory().getItem(hand);
        if ((last != null && now - last < OPEN_COOLDOWN_MS) || !id.equals(idOf(held))) {
            return;
        }
        lastOpen.put(p.getUniqueId(), now);
        // the bag is used up before anything is handed out
        held.setAmount(held.getAmount() - 1);
        p.getInventory().setItem(hand, held.getAmount() > 0 ? held : null);

        List<LootBagType.Roll> rolls = t.open(ThreadLocalRandom.current());
        List<Component> names = new ArrayList<>();
        boolean spilled = false;
        for (LootBagType.Roll roll : rolls) {
            LootEntry entry = roll.entry();
            if (entry.type == LootEntry.Type.LEVELS) {
                p.giveExpLevels(roll.amount());
                names.add(lang.plain("lootbag-levels", "amount", String.valueOf(roll.amount())));
                continue;
            }
            ItemStack stack = entry.create();
            names.add(label(stack, roll.amount()));
            // handed out in legal stacks (16 pearls, 1 totem...), never as one oversized stack
            for (int n : ChestManager.stacks(roll.amount(), stack.getMaxStackSize())) {
                ItemStack part = stack.clone();
                part.setAmount(n);
                for (ItemStack left : p.getInventory().addItem(part).values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), left);
                    spilled = true;
                }
            }
        }
        Component list = Component.join(JoinConfiguration.separator(Text.mm("<dim>, ")), names);
        p.sendMessage(lang.msg("lootbag-opened", Map.of("items", list), "bag", t.name, "count", String.valueOf(rolls.size())));
        if (spilled) {
            p.sendMessage(lang.msg("lootbag-full"));
        }
        celebrate(p, t, rolls.size());
    }

    /** "Diamond ×3" with the item's own tooltip on hover (enchantments of gear are visible). */
    private static Component label(ItemStack stack, int amount) {
        ItemStack shown = stack.clone();
        shown.setAmount(Math.max(1, Math.min(amount, shown.getMaxStackSize())));
        Component name = shown.effectiveName().hoverEvent(shown.asHoverEvent());
        return Text.mm(amount > 1 ? "<hi><name></hi><dim> ×" + amount : "<hi><name>", Placeholder.component("name", name));
    }

    private void celebrate(Player p, LootBagType t, int count) {
        sound(p, "minecraft:item.bundle.drop_contents", 1.0f, 1.0f);
        Location at = p.getLocation().add(0, 1, 0);
        p.getWorld().spawnParticle(Particle.DUST, at, 20 + 6 * Math.min(6, t.rank), 0.4, 0.5, 0.4, 0, dust(t));
        if (t.announce) {
            sound(p, "minecraft:ui.toast.challenge_complete", 0.8f, 1.0f);
            p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, at, 40, 0.5, 0.6, 0.5, 0.3);
            p.showTitle(Title.title(Text.mm(t.name), plugin.lang().plain("lootbag-subtitle", "count", String.valueOf(count)),
                    Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1800), Duration.ofMillis(500))));
        } else if (t.rank >= 2) {
            sound(p, "minecraft:entity.player.levelup", 0.6f, 1.0f + 0.1f * Math.min(4, t.rank));
        } else {
            sound(p, "minecraft:entity.experience_orb.pickup", 0.7f, 1.1f);
        }
    }

    private static Particle.DustOptions dust(LootBagType t) {
        return new Particle.DustOptions(Color.fromRGB(Palette.rgb(t.color)), 1.2f);
    }

    private static void sound(Player p, String key, float volume, float pitch) {
        p.playSound(p.getLocation(), key, SoundCategory.MASTER, volume, pitch);
    }

    // ------------------------------------------------------------------ guards

    /** A bag is never crafting material (it is rabbit hide underneath). */
    @EventHandler
    public void onCraft(PrepareItemCraftEvent e) {
        for (ItemStack it : e.getInventory().getMatrix()) {
            if (idOf(it) != null) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastOpen.remove(e.getPlayer().getUniqueId());
    }
}
