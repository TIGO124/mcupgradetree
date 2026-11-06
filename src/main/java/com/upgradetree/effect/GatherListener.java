package com.upgradetree.effect;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Settings;
import com.upgradetree.tree.NodeEffect;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Double drops, auto smelting, vein mining and tree felling. */
public final class GatherListener implements Listener {

    private final UpgradeTreePlugin plugin;
    private final PlacedBlocks placed;
    private final Set<Location> brokenPlaced = new HashSet<>();
    private final Set<UUID> busy = new HashSet<>();
    private final Map<Material, ItemStack> smeltCache = new EnumMap<>(Material.class);

    public GatherListener(UpgradeTreePlugin plugin, PlacedBlocks placed) {
        this.plugin = plugin;
        this.placed = placed;
    }

    private boolean tracked(Material m) {
        Settings s = plugin.settings();
        return s.doubleDropBlocks.contains(m) || s.miningBlocks.contains(m) || s.veinMinerBlocks.contains(m)
                || plugin.trackedBlocks().contains(m);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (tracked(e.getBlockPlaced().getType())) {
            placed.mark(e.getBlockPlaced());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (placed.isPlaced(b)) {
            Location key = b.getLocation();
            brokenPlaced.add(key);
            Bukkit.getScheduler().runTask(plugin, () -> brokenPlaced.remove(key));
            placed.unmark(b);
            return;
        }
        Player p = e.getPlayer();
        if (!p.isSneaking() || busy.contains(p.getUniqueId()) || p.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        Material type = b.getType();
        if (Tag.LOGS.isTagged(type)) {
            fellTree(p, b);
        } else {
            veinMine(p, b, type);
        }
    }

    // ------------------------------------------------------------------ drops

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent e) {
        if (brokenPlaced.remove(e.getBlock().getLocation())) {
            return;
        }
        Player p = e.getPlayer();
        Material type = e.getBlockState().getType();
        Settings s = plugin.settings();

        double chance = 0;
        for (EffectManager.Typed<NodeEffect.DoubleDrop> t : plugin.effects().of(p, NodeEffect.DoubleDrop.class)) {
            Set<Material> blocks = t.effect().blocks().isEmpty() ? s.doubleDropBlocks : t.effect().blocks();
            if (blocks.contains(type)) {
                chance += t.effect().amount() * t.level();
            }
        }
        boolean doubled = false;
        boolean silk = p.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH);
        boolean crop = Tag.CROPS.isTagged(type) || type == Material.NETHER_WART || type == Material.COCOA
                || type == Material.SWEET_BERRY_BUSH;
        boolean unripe = crop && e.getBlockState().getBlockData() instanceof Ageable age && age.getAge() < age.getMaximumAge();
        if (!unripe && chance > 0 && ThreadLocalRandom.current().nextDouble() < Math.min(1.0, chance)) {
            for (Item item : e.getItems()) {
                ItemStack stack = item.getItemStack();
                if ((silk && stack.getType() == type) || stack.getType().isAir()) {
                    continue; // silk touch: never duplicate the block itself
                }
                ItemStack more = stack.clone();
                more.setAmount(Math.min(stack.getMaxStackSize(), stack.getAmount() * 2));
                item.setItemStack(more);
                doubled = true;
            }
        }

        if (s.doubleDropBlocks.contains(type)) {
            double smelt = plugin.effects().sum(p, NodeEffect.AutoSmelt.class, NodeEffect.AutoSmelt::amount);
            if (smelt > 0 && ThreadLocalRandom.current().nextDouble() < Math.min(1.0, smelt)) {
                for (Item item : e.getItems()) {
                    ItemStack stack = item.getItemStack();
                    ItemStack result = smelted(stack.getType());
                    if (result != null && stack.getType() != type) {
                        ItemStack out = result.clone();
                        out.setAmount(Math.min(out.getMaxStackSize(), stack.getAmount()));
                        item.setItemStack(out);
                    }
                }
                Location loc = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 6, 0.25, 0.25, 0.25, 0.01);
            }
        }
        if (doubled && s.particles) {
            Location loc = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 8, 0.3, 0.3, 0.3, 0);
        }
    }

    private ItemStack smelted(Material input) {
        if (smeltCache.containsKey(input)) {
            return smeltCache.get(input);
        }
        ItemStack probe = new ItemStack(input);
        ItemStack found = null;
        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
            if (it.next() instanceof FurnaceRecipe fr && fr.getInputChoice().test(probe)) {
                found = fr.getResult();
                break;
            }
        }
        smeltCache.put(input, found);
        return found;
    }

    // ------------------------------------------------------------------ vein miner & tree feller

    private void veinMine(Player p, Block origin, Material type) {
        Settings s = plugin.settings();
        double budget = 0;
        for (EffectManager.Typed<NodeEffect.VeinMiner> t : plugin.effects().of(p, NodeEffect.VeinMiner.class)) {
            Set<Material> blocks = t.effect().blocks().isEmpty() ? s.veinMinerBlocks : t.effect().blocks();
            if (blocks.contains(type)) {
                budget += t.effect().amount() * t.level();
            }
        }
        int max = (int) Math.floor(budget);
        if (max <= 0) {
            return;
        }
        breakAll(p, collect(origin, max, b -> b.getType() == type, false));
    }

    private void fellTree(Player p, Block origin) {
        double budget = plugin.effects().sum(p, NodeEffect.TreeFeller.class, NodeEffect.TreeFeller::amount);
        int max = Math.min(plugin.settings().treeFellerMax, (int) Math.floor(budget));
        if (max <= 0) {
            return;
        }
        List<Block> logs = collect(origin, max, b -> Tag.LOGS.isTagged(b.getType()), true);
        if (!naturalTree(logs.isEmpty() ? List.of(origin) : logs)) {
            return; // no natural leaves around: probably a building
        }
        breakAll(p, logs);
    }

    private interface BlockTest {
        boolean test(Block b);
    }

    /** Connected blocks (26-neighbourhood) except the origin, nearest first. */
    private static List<Block> collect(Block origin, int max, BlockTest test, boolean notBelow) {
        List<Block> out = new ArrayList<>();
        Set<Block> seen = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && out.size() < max) {
            Block cur = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Block n = cur.getRelative(dx, dy, dz);
                        if (notBelow && n.getY() < origin.getY()) {
                            continue;
                        }
                        if (seen.add(n) && test.test(n)) {
                            out.add(n);
                            queue.add(n);
                            if (out.size() >= max) {
                                return out;
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    private static boolean naturalTree(List<Block> logs) {
        for (Block log : logs) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Block n = log.getRelative(dx, dy, dz);
                        if (n.getBlockData() instanceof Leaves leaves && !leaves.isPersistent()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void breakAll(Player p, List<Block> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        Material tool = p.getInventory().getItemInMainHand().getType();
        busy.add(p.getUniqueId());
        try {
            for (Block b : blocks) {
                if (p.getInventory().getItemInMainHand().getType() != tool || !p.isOnline()) {
                    break; // the tool broke
                }
                if (!p.breakBlock(b)) {
                    break;
                }
            }
        } finally {
            busy.remove(p.getUniqueId());
        }
    }
}
