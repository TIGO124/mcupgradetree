package com.upgradetree.tree;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/** One effect granted by an upgrade node. Amounts are per purchased level. */
public abstract class NodeEffect {

    /** Per-level amount used for the "bonus" preview line (0 if not numeric). */
    public double perLevel() {
        return 0;
    }

    /** Attribute modifier, e.g. max_health +2 per level. */
    public static final class AttributeBonus extends NodeEffect {
        public final Attribute attribute;
        public final String attributeKey;
        public final double amount;
        public final AttributeModifier.Operation operation;

        public AttributeBonus(Attribute attribute, String attributeKey, double amount, AttributeModifier.Operation operation) {
            this.attribute = attribute;
            this.attributeKey = attributeKey;
            this.amount = amount;
            this.operation = operation;
        }

        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Permanent potion effect. Amplifier = base + (level - 1) * perLevel. */
    public static final class PotionBonus extends NodeEffect {
        public final PotionEffectType type;
        public final int amplifier;
        public final int amplifierPerLevel;

        public PotionBonus(PotionEffectType type, int amplifier, int amplifierPerLevel) {
            this.type = type;
            this.amplifier = amplifier;
            this.amplifierPerLevel = amplifierPerLevel;
        }

        public int amplifierAt(int level) {
            return Math.max(0, Math.min(255, amplifier + (level - 1) * amplifierPerLevel));
        }

        @Override
        public double perLevel() {
            return 1;
        }
    }

    /** Extra experience from orbs, e.g. 0.1 = +10% per level. */
    public static final class XpBoost extends NodeEffect {
        public final double amount;

        public XpBoost(double amount) {
            this.amount = amount;
        }

        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Chance per level to double the drops of configured ore blocks. */
    public static final class DoubleDrop extends NodeEffect {
        public final double amount;

        public DoubleDrop(double amount) {
            this.amount = amount;
        }

        @Override
        public double perLevel() {
            return amount;
        }
    }

    /** Console commands run every time a level is purchased. */
    public static final class Commands extends NodeEffect {
        public final List<String> commands;

        public Commands(List<String> commands) {
            this.commands = List.copyOf(commands);
        }
    }
}
