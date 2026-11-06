package com.upgradetree.tree;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;

/** One effect granted by an upgrade node (or by prestige). Amounts are per level. */
public sealed interface NodeEffect {

    /** Per-level amount used for the "bonus" preview line (0 if not numeric). */
    default double perLevel() {
        return 0;
    }

    /** Attribute modifier, e.g. max_health +2 per level. */
    record AttributeBonus(Attribute attribute, String attributeKey, double amount,
                          AttributeModifier.Operation operation) implements NodeEffect {
        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Permanent potion effect. Amplifier = base + (level - 1) * perLevel. */
    record PotionBonus(PotionEffectType type, int amplifier, int amplifierPerLevel) implements NodeEffect {
        public int amplifierAt(int level) {
            return Math.max(0, Math.min(255, amplifier + (level - 1) * amplifierPerLevel));
        }

        @Override
        public double perLevel() {
            return 1;
        }
    }

    /** Extra experience from orbs, e.g. 0.1 = +10% per level. */
    record XpBoost(double amount) implements NodeEffect {
        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Chance per level to double block drops. Empty block set = config default list. */
    record DoubleDrop(double amount, Set<Material> blocks) implements NodeEffect {
        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Console commands run every time a level is purchased. */
    record Commands(List<String> commands) implements NodeEffect {
    }

    /** Heals this fraction of the melee damage dealt. */
    record Lifesteal(double amount) implements NodeEffect {
        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Chance to deal {@code multiplier} times damage. */
    record Crit(double amount, double multiplier) implements NodeEffect {
        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Chance to avoid an attack completely. */
    record Dodge(double amount) implements NodeEffect {
        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Chance to smelt block drops (raw iron becomes an iron ingot...). */
    record AutoSmelt(double amount) implements NodeEffect {
        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Breaks connected blocks of the same type while sneaking (amount = blocks per level). */
    record VeinMiner(double amount, Set<Material> blocks) implements NodeEffect {
        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Fells the whole tree while sneaking (amount = logs per level). */
    record TreeFeller(double amount) implements NodeEffect {
        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Chance to double the drops of killed mobs. */
    record MobDrops(double amount) implements NodeEffect {
        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Fraction of experience kept on death. */
    record KeepXp(double amount) implements NodeEffect {
        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Chance to keep the inventory on death. */
    record KeepInventory(double amount) implements NodeEffect {
        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Chance to not lose hunger. */
    record HungerSave(double amount) implements NodeEffect {
        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Active ability with a cooldown, cast with a key combination. */
    record Ability(AbilityType type, Trigger trigger, double cooldown, double cooldownPerLevel,
                   double power, double powerPerLevel) implements NodeEffect {

        public double cooldownAt(int level) {
            return Math.max(1.0, cooldown + cooldownPerLevel * (level - 1));
        }

        public double powerAt(int level) {
            return Math.max(0.0, power + powerPerLevel * (level - 1));
        }

        @Override
        public double perLevel() {
            return powerPerLevel;
        }
    }

    enum AbilityType { DASH, LEAP, HEAL, SHIELD, SPEED, NOVA }

    enum Trigger { SNEAK_SWAP, DOUBLE_SNEAK, SNEAK_JUMP }
}
