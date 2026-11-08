package com.upgradetree.chest;

import com.upgradetree.config.Lang;
import com.upgradetree.config.Localized;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * A chest that can be bought in the Chests section. Odds work like CS2 cases: first a rarity is
 * picked by its weight, then one reward of that rarity (by the rewards' own weights).
 */
public final class ChestType {

    /** Materials of the chest that appears in front of the player. */
    public record Look(Material body, Material trim, Material latch) {
    }

    public final String id;
    public final Localized<String> names;
    public final Material icon;
    public final boolean glint;
    public final double price;
    public final int order;
    public final int color;
    public final Localized<List<String>> descriptions;
    public final Look look;
    public final List<ChestReward> rewards;
    private final double[] chances;

    ChestType(String id, Localized<String> names, Material icon, boolean glint, double price, int order, int color,
              Localized<List<String>> descriptions, Look look, List<ChestReward> rewards, Map<String, Double> weightOverride) {
        this.id = id;
        this.names = names.isEmpty() ? Localized.of(id) : names;
        this.icon = icon;
        this.glint = glint;
        this.price = Math.max(0, price);
        this.order = order;
        this.color = color;
        this.descriptions = descriptions;
        this.look = look;
        this.rewards = List.copyOf(rewards);
        this.chances = chancesOf(this.rewards, weightOverride);
    }

    /**
     * Chance (0..1) of every reward: rarity weight / sum of the weights of the rarities present,
     * shared among that rarity's rewards by their weights.
     */
    static double[] chancesOf(List<ChestReward> rewards, Map<String, Double> override) {
        Map<String, Double> rarityWeight = new LinkedHashMap<>();
        Map<String, Double> inside = new LinkedHashMap<>();
        for (ChestReward r : rewards) {
            double w = override.getOrDefault(r.rarity.id(), r.rarity.weight());
            if (w > 0 && r.weight > 0) {
                rarityWeight.put(r.rarity.id(), w);
                inside.merge(r.rarity.id(), r.weight, Double::sum);
            }
        }
        double total = 0;
        for (double w : rarityWeight.values()) {
            total += w;
        }
        double[] out = new double[rewards.size()];
        for (int i = 0; i < rewards.size(); i++) {
            ChestReward r = rewards.get(i);
            Double w = rarityWeight.get(r.rarity.id());
            if (w == null || total <= 0 || r.weight <= 0) {
                continue;
            }
            out[i] = (w / total) * (r.weight / inside.get(r.rarity.id()));
        }
        return out;
    }

    /** Name in the reader's language (MiniMessage). */
    public String name(Lang lang) {
        String n = names.get(lang);
        return n == null ? id : n;
    }

    public Component nameComponent(Lang lang) {
        return Text.mm(name(lang));
    }

    public List<String> description(Lang lang) {
        List<String> d = descriptions.get(lang);
        return d == null ? List.of() : d;
    }

    /** Chance (0..1) of every reward, by reward index (a copy). */
    public double[] chances() {
        return chances.clone();
    }

    /** Chance (0..1) of one reward. */
    public double chance(ChestReward r) {
        return r.index >= 0 && r.index < chances.length ? chances[r.index] : 0;
    }

    /** Chance (0..1) of landing on a rarity. */
    public double chance(Rarity rarity) {
        double c = 0;
        for (ChestReward r : rewards) {
            if (r.rarity.id().equals(rarity.id())) {
                c += chance(r);
            }
        }
        return c;
    }

    /** The reward for a uniform random number {@code roll} in [0, 1). */
    public ChestReward pick(double roll) {
        double acc = 0;
        ChestReward last = null;
        for (ChestReward r : rewards) {
            double c = chance(r);
            if (c <= 0) {
                continue;
            }
            acc += c;
            last = r;
            if (roll < acc) {
                return r;
            }
        }
        return last;
    }

    public ChestReward roll(RandomGenerator random) {
        return pick(random.nextDouble());
    }

    /** Rarities that can drop from this chest, most common first. */
    public List<Rarity> rarities() {
        List<Rarity> out = new ArrayList<>();
        for (ChestReward r : rewards) {
            if (chance(r) > 0 && out.stream().noneMatch(x -> x.id().equals(r.rarity.id()))) {
                out.add(r.rarity);
            }
        }
        out.sort(Comparator.comparingInt(Rarity::rank));
        return out;
    }

    /** Rewards that can drop, sorted like a CS2 case: common to rare, then file order. */
    public List<ChestReward> sortedRewards() {
        List<ChestReward> out = new ArrayList<>();
        for (ChestReward r : rewards) {
            if (chance(r) > 0) {
                out.add(r);
            }
        }
        out.sort(Comparator.comparingInt((ChestReward r) -> r.rarity.rank()).thenComparingInt(r -> r.index));
        return out;
    }

    /** True when at least one reward can drop. */
    public boolean openable() {
        for (double c : chances) {
            if (c > 0) {
                return true;
            }
        }
        return false;
    }
}
