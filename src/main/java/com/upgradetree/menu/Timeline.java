package com.upgradetree.menu;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Delayed animation steps of one menu, run on menu ticks. Also hides the viewer's hand. */
final class Timeline {

    private record Timed(int at, Runnable task) {
    }

    private final List<Timed> items = new ArrayList<>();
    private final Logger log;

    Timeline(Logger log) {
        this.log = log;
    }

    void add(int now, int delay, Runnable task) {
        items.add(new Timed(now + Math.max(1, delay), task));
    }

    void clear() {
        items.clear();
    }

    void run(int now) {
        if (items.isEmpty()) {
            return;
        }
        List<Timed> due = null;
        for (Iterator<Timed> it = items.iterator(); it.hasNext(); ) {
            Timed t = it.next();
            if (t.at() <= now) {
                if (due == null) {
                    due = new ArrayList<>();
                }
                due.add(t);
                it.remove();
            }
        }
        if (due != null) {
            for (Timed t : due) {
                try {
                    t.task().run();
                } catch (Exception ex) {
                    log.log(Level.WARNING, "Menü animasyon hatası", ex);
                }
            }
        }
    }

    /** Shows an invisible item in the player's hands (client side only) so no arm covers the UI. */
    static void hideHand(Player player) {
        ItemStack fake = new ItemStack(Material.PAPER);
        fake.editMeta(m -> {
            m.setItemModel(NamespacedKey.minecraft("air"));
            m.customName(Component.empty());
            m.setHideTooltip(true);
        });
        Map<EquipmentSlot, ItemStack> map = new EnumMap<>(EquipmentSlot.class);
        map.put(EquipmentSlot.HAND, fake);
        if (!player.getInventory().getItemInOffHand().getType().isAir()) {
            map.put(EquipmentSlot.OFF_HAND, fake.clone());
        }
        player.sendEquipmentChange(player, map);
    }
}
