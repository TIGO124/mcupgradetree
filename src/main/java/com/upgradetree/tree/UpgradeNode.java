package com.upgradetree.tree;

import com.upgradetree.config.Lang;
import com.upgradetree.config.Localized;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One node of an upgrade tree, loaded from a file in trees/. */
public final class UpgradeNode {

    public final String id;
    /** Lower-case id that is safe to use inside a NamespacedKey. */
    public final String key;
    public final String treeId;
    /** Name, description and bonus line, each possibly written per language (see {@link Localized}). */
    public final Localized<String> names;
    public final Material icon;
    /** Optional item model (resource packs), e.g. "myserver:sword". */
    public final String iconModel;
    /** Optional base64 player head texture. */
    public final String headTexture;
    /** Position on the hex grid: side neighbours at x±1, diagonal neighbours at x±0.5 / y±1. */
    public final double x;
    public final double y;
    public final int maxLevel;
    public final double cost;
    public final double costIncrease;
    public final double costMultiplier;
    public final List<Double> costs;
    /** All of these need at least one level. */
    public final List<String> requires;
    /** At least one of these needs a level (empty = no rule). */
    public final List<String> requiresAny;
    /** These need the given level. */
    public final Map<String, Integer> requiresLevel;
    /** Cannot be bought while any of these is owned. */
    public final List<String> exclusiveWith;
    public final String permission;
    public final int minPrestige;
    public final boolean alwaysVisible;
    public final int color;
    public final Localized<List<String>> descriptions;
    public final Localized<String> bonuses;
    public final double bonusMultiplier;
    public final Double bonusPerLevel;
    public final List<NodeEffect> effects;
    /** Every node mentioned by a requirement. */
    public final List<String> parents;
    /**
     * The structural parents: requires + requires-any (or the requires-level nodes when those are
     * empty). Lines are drawn to them and the node is revealed when one of them is owned.
     */
    public final List<String> links;

    private UpgradeNode(Builder b) {
        this.id = b.id;
        this.key = b.key;
        this.treeId = b.treeId;
        this.names = b.name == null || b.name.isEmpty() ? Localized.of(b.id) : b.name;
        this.icon = b.icon;
        this.iconModel = blankToNull(b.iconModel);
        this.headTexture = blankToNull(b.headTexture);
        this.x = b.x;
        this.y = b.y;
        this.maxLevel = Math.max(1, b.maxLevel);
        this.cost = b.cost;
        this.costIncrease = b.costIncrease;
        this.costMultiplier = b.costMultiplier;
        this.costs = List.copyOf(b.costs);
        this.requires = List.copyOf(b.requires);
        this.requiresAny = List.copyOf(b.requiresAny);
        this.requiresLevel = Map.copyOf(b.requiresLevel);
        this.exclusiveWith = List.copyOf(b.exclusiveWith);
        this.permission = blankToNull(b.permission);
        this.minPrestige = Math.max(0, b.minPrestige);
        this.alwaysVisible = b.alwaysVisible;
        this.color = b.color;
        this.descriptions = b.description == null ? Localized.of(List.of()) : b.description;
        this.bonuses = b.bonus == null ? Localized.of("") : b.bonus;
        this.bonusMultiplier = b.bonusMultiplier;
        this.bonusPerLevel = b.bonusPerLevel;
        this.effects = List.copyOf(b.effects);
        Set<String> p = new LinkedHashSet<>(requires);
        p.addAll(requiresAny);
        p.addAll(new ArrayList<>(b.requiresLevel.keySet()));
        this.parents = List.copyOf(p);
        Set<String> l = new LinkedHashSet<>(requires);
        l.addAll(requiresAny);
        if (l.isEmpty()) {
            l.addAll(b.requiresLevel.keySet());
        }
        this.links = List.copyOf(l);
    }

    /** Name in the reader's language (MiniMessage). */
    public String name(Lang lang) {
        String n = names.get(lang);
        return n == null ? id : n;
    }

    public Component nameComponent(Lang lang) {
        return Text.mm(name(lang));
    }

    /** Name for the console and checks (no reader). */
    public String baseName() {
        return name(null);
    }

    public List<String> description(Lang lang) {
        List<String> d = descriptions.get(lang);
        return d == null ? List.of() : d;
    }

    /** Bonus line ("+{value} damage") in the reader's language, "" if none. */
    public String bonus(Lang lang) {
        String b = bonuses.get(lang);
        return b == null ? "" : b;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Base cost of buying the next level when the player currently has {@code currentLevel}. */
    public double costFor(int currentLevel) {
        return formula(cost, costIncrease, costMultiplier, costs, currentLevel);
    }

    /** cost * multiplier^level + increase * level, or the explicit list entry. */
    public static double formula(double cost, double increase, double multiplier, List<Double> costs, int level) {
        if (!costs.isEmpty()) {
            return Math.max(0, costs.get(Math.max(0, Math.min(level, costs.size() - 1))));
        }
        double c = cost * Math.pow(multiplier, level) + increase * level;
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

    public Builder toBuilder() {
        Builder b = new Builder(id, treeId);
        b.key = key;
        b.name = names;
        b.icon = icon;
        b.iconModel = iconModel;
        b.headTexture = headTexture;
        b.x = x;
        b.y = y;
        b.maxLevel = maxLevel;
        b.cost = cost;
        b.costIncrease = costIncrease;
        b.costMultiplier = costMultiplier;
        b.costs = new ArrayList<>(costs);
        b.requires = new ArrayList<>(requires);
        b.requiresAny = new ArrayList<>(requiresAny);
        b.requiresLevel = new LinkedHashMap<>(requiresLevel);
        b.exclusiveWith = new ArrayList<>(exclusiveWith);
        b.permission = permission;
        b.minPrestige = minPrestige;
        b.alwaysVisible = alwaysVisible;
        b.color = color;
        b.description = descriptions;
        b.bonus = bonuses;
        b.bonusMultiplier = bonusMultiplier;
        b.bonusPerLevel = bonusPerLevel;
        b.effects = new ArrayList<>(effects);
        return b;
    }

    /** Mutable form used while loading. */
    public static final class Builder {
        public final String id;
        public final String treeId;
        public String key;
        public Localized<String> name;
        public Material icon = Material.PAPER;
        public String iconModel;
        public String headTexture;
        public double x;
        public double y;
        public int maxLevel = 1;
        public double cost = 1;
        public double costIncrease;
        public double costMultiplier = 1;
        public List<Double> costs = new ArrayList<>();
        public List<String> requires = new ArrayList<>();
        public List<String> requiresAny = new ArrayList<>();
        public Map<String, Integer> requiresLevel = new LinkedHashMap<>();
        public List<String> exclusiveWith = new ArrayList<>();
        public String permission;
        public int minPrestige;
        public boolean alwaysVisible;
        public int color = UpgradeTree.DEFAULT_COLOR;
        public Localized<List<String>> description = Localized.of(List.of());
        public Localized<String> bonus = Localized.of("");
        public double bonusMultiplier = 1;
        public Double bonusPerLevel;
        public List<NodeEffect> effects = new ArrayList<>();

        public Builder(String id, String treeId) {
            this.id = id;
            this.treeId = treeId;
            this.key = id;
            this.name = Localized.of(id);
        }

        public UpgradeNode build() {
            return new UpgradeNode(this);
        }
    }
}
