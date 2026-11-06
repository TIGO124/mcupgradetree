package com.upgradetree.tree;

import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

/** One node of the upgrade tree, loaded from tree.yml. */
public final class UpgradeNode {

    public final String id;
    /** Lower-case id that is safe to use inside a NamespacedKey. */
    public final String key;
    public final String nameRaw;
    public final Component name;
    public final String namePlain;
    public final Material icon;
    public final double x;
    public final double y;
    public final int maxLevel;
    public final double cost;
    public final double costIncrease;
    public final double costMultiplier;
    public final List<Double> costs;
    public final List<String> requires;
    public final int color;
    public final List<String> description;
    public final String bonus;
    public final double bonusMultiplier;
    public final Double bonusPerLevel;
    public final List<NodeEffect> effects;

    public UpgradeNode(String id, String key, String nameRaw, Material icon, double x, double y, int maxLevel,
                       double cost, double costIncrease, double costMultiplier, List<Double> costs,
                       List<String> requires, int color, List<String> description, String bonus,
                       double bonusMultiplier, Double bonusPerLevel, List<NodeEffect> effects) {
        this.id = id;
        this.key = key;
        this.nameRaw = nameRaw;
        this.name = Text.mm(nameRaw);
        this.namePlain = Text.plain(nameRaw);
        this.icon = icon;
        this.x = x;
        this.y = y;
        this.maxLevel = Math.max(1, maxLevel);
        this.cost = cost;
        this.costIncrease = costIncrease;
        this.costMultiplier = costMultiplier;
        this.costs = List.copyOf(costs);
        this.requires = List.copyOf(requires);
        this.color = color;
        this.description = List.copyOf(description);
        this.bonus = bonus;
        this.bonusMultiplier = bonusMultiplier;
        this.bonusPerLevel = bonusPerLevel;
        this.effects = List.copyOf(effects);
    }

    /** Cost of buying the next level when the player currently has {@code currentLevel}. */
    public double costFor(int currentLevel) {
        if (!costs.isEmpty()) {
            return costs.get(Math.min(currentLevel, costs.size() - 1));
        }
        double c = cost * Math.pow(costMultiplier, currentLevel) + costIncrease * currentLevel;
        return Math.round(Math.max(0, c) * 100.0) / 100.0;
    }

    /** Value shown in the bonus preview for a given level. */
    public double bonusValue(int level) {
        double per;
        if (bonusPerLevel != null) {
            per = bonusPerLevel;
        } else {
            per = 0;
            for (NodeEffect e : effects) {
                if (e.perLevel() != 0) {
                    per = e.perLevel();
                    break;
                }
            }
        }
        return per * level * bonusMultiplier;
    }
}
