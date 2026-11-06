package com.upgradetree.menu;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Keeps the player still while the menu is open without changing their field of view:
 * walk/fly speed 0 (a zero walking speed disables the client's speed-based FOV change),
 * jump strength 0 and interaction ranges 0 (so clicks never hit blocks or entities).
 * Speeds are stored in the player's persistent data so a crash can never leave them frozen.
 */
final class Freeze {

    private static final String[] KEYS = {"freeze_move", "freeze_jump", "freeze_block", "freeze_entity"};

    private Freeze() {
    }

    private static NamespacedKey store(Plugin plugin) {
        return new NamespacedKey(plugin, "frozen_speeds");
    }

    static void apply(Plugin plugin, Player p) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        NamespacedKey key = store(plugin);
        if (!pdc.has(key, PersistentDataType.STRING)) {
            // setWalkSpeed also overwrites the movement_speed base value, so that is remembered too
            AttributeInstance move = p.getAttribute(Attribute.MOVEMENT_SPEED);
            String base = move == null ? "" : ";" + move.getBaseValue();
            pdc.set(key, PersistentDataType.STRING, p.getWalkSpeed() + ";" + p.getFlySpeed() + base);
        }
        try {
            p.setWalkSpeed(0f);
            p.setFlySpeed(0f);
        } catch (IllegalArgumentException ignored) {
            // never happens for 0
        }
        modifier(plugin, p, Attribute.MOVEMENT_SPEED, KEYS[0]);
        modifier(plugin, p, Attribute.JUMP_STRENGTH, KEYS[1]);
        modifier(plugin, p, Attribute.BLOCK_INTERACTION_RANGE, KEYS[2]);
        modifier(plugin, p, Attribute.ENTITY_INTERACTION_RANGE, KEYS[3]);
    }

    private static void modifier(Plugin plugin, Player p, Attribute attr, String id) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) {
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, id);
        inst.removeModifier(key);
        inst.addTransientModifier(new AttributeModifier(key, -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
    }

    /** Undoes {@link #apply}. Safe to call when the player is not frozen. */
    static void release(Plugin plugin, Player p) {
        Attribute[] attrs = {Attribute.MOVEMENT_SPEED, Attribute.JUMP_STRENGTH, Attribute.BLOCK_INTERACTION_RANGE, Attribute.ENTITY_INTERACTION_RANGE};
        for (int i = 0; i < attrs.length; i++) {
            AttributeInstance inst = p.getAttribute(attrs[i]);
            if (inst != null) {
                inst.removeModifier(new NamespacedKey(plugin, KEYS[i]));
            }
        }
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        NamespacedKey key = store(plugin);
        String saved = pdc.get(key, PersistentDataType.STRING);
        if (saved == null) {
            return;
        }
        float walk = 0.2f;
        float fly = 0.1f;
        double base = -1;
        try {
            String[] parts = saved.split(";");
            walk = Float.parseFloat(parts[0]);
            fly = Float.parseFloat(parts[1]);
            if (parts.length > 2) {
                base = Double.parseDouble(parts[2]);
            }
        } catch (Exception ignored) {
            // defaults
        }
        if (walk == 0f) {
            walk = 0.2f;
        }
        if (fly == 0f) {
            fly = 0.1f;
        }
        try {
            p.setWalkSpeed(clamp(walk));
            p.setFlySpeed(clamp(fly));
        } catch (IllegalArgumentException ignored) {
            p.setWalkSpeed(0.2f);
            p.setFlySpeed(0.1f);
        }
        AttributeInstance move = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (move != null && base > 0 && Double.isFinite(base)) {
            move.setBaseValue(base);
        }
        pdc.remove(key);
    }

    private static float clamp(float v) {
        return Math.max(-1f, Math.min(1f, v));
    }
}
