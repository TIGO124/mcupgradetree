package com.upgradetree.service;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.api.event.UpgradeLevelChangeEvent;
import com.upgradetree.api.event.UpgradePurchaseEvent;
import com.upgradetree.config.Lang;
import com.upgradetree.currency.Currency;
import com.upgradetree.data.PlayerData;
import com.upgradetree.tree.TreeRegistry;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.tree.UpgradeTree;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Purchase rules: requirements, visibility, costs and node states. */
public final class UpgradeService {

    public enum State { LOCKED, BLOCKED, AVAILABLE, OWNED, MAXED }

    public enum Result { SUCCESS, MAXED, LOCKED, BLOCKED, NOT_ENOUGH, UNAVAILABLE, CANCELLED }

    private final UpgradeTreePlugin plugin;

    public UpgradeService(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    private TreeRegistry trees() {
        return plugin.trees();
    }

    // ------------------------------------------------------------------ requirements

    /** Every rule except "exclusive-with". {@code p} may be null (offline): permissions are then ignored. */
    public boolean requirementsMet(PlayerData d, Player p, UpgradeNode n) {
        for (String r : n.requires) {
            if (d.level(r) <= 0) {
                return false;
            }
        }
        if (!n.requiresAny.isEmpty()) {
            boolean any = false;
            for (String r : n.requiresAny) {
                if (d.level(r) > 0) {
                    any = true;
                    break;
                }
            }
            if (!any) {
                return false;
            }
        }
        for (Map.Entry<String, Integer> e : n.requiresLevel.entrySet()) {
            if (d.level(e.getKey()) < e.getValue()) {
                return false;
            }
        }
        if (d.prestige() < n.minPrestige) {
            return false;
        }
        return n.permission == null || p == null || p.hasPermission(n.permission);
    }

    /** Owned upgrades that this one cannot be combined with. */
    public List<UpgradeNode> blockers(PlayerData d, UpgradeNode n) {
        List<UpgradeNode> list = new ArrayList<>();
        for (String id : n.exclusiveWith) {
            if (d.level(id) > 0) {
                UpgradeNode b = trees().node(id);
                if (b != null) {
                    list.add(b);
                }
            }
        }
        return list;
    }

    public State state(PlayerData d, Player p, UpgradeNode n) {
        int lvl = d.level(n.id);
        if (lvl >= n.maxLevel) {
            return State.MAXED;
        }
        if (lvl > 0) {
            return State.OWNED;
        }
        if (!blockers(d, n).isEmpty()) {
            return State.BLOCKED;
        }
        return requirementsMet(d, p, n) ? State.AVAILABLE : State.LOCKED;
    }

    /** Progressive reveal: a node shows up once one of the nodes it mentions is owned. */
    public boolean visible(PlayerData d, UpgradeNode n) {
        if (!plugin.settings().progressiveReveal || n.alwaysVisible || n.links.isEmpty() || d.level(n.id) > 0) {
            return true;
        }
        for (String parent : n.links) {
            if (d.level(parent) > 0) {
                return true;
            }
        }
        return false;
    }

    /** Unmet requirements of {@code p} (permissions), e.g. "Sharp Sword", "X or Y (one of)", "Prestige 1". */
    public List<String> missing(PlayerData d, Player p, UpgradeNode n) {
        Lang lang = plugin.lang();
        List<String> out = new ArrayList<>();
        for (String r : n.requires) {
            if (d.level(r) <= 0) {
                out.add(name(r, lang));
            }
        }
        if (!n.requiresAny.isEmpty()) {
            boolean any = false;
            List<String> names = new ArrayList<>();
            for (String r : n.requiresAny) {
                any |= d.level(r) > 0;
                names.add(name(r, lang));
            }
            if (!any) {
                out.add(lang.format("req-any", "nodes", String.join(lang.raw("req-or"), names)));
            }
        }
        for (Map.Entry<String, Integer> e : n.requiresLevel.entrySet()) {
            if (d.level(e.getKey()) < e.getValue()) {
                out.add(lang.format("req-level", "node", name(e.getKey(), lang), "level", String.valueOf(e.getValue())));
            }
        }
        if (d.prestige() < n.minPrestige) {
            out.add(lang.format("req-prestige", "level", String.valueOf(n.minPrestige)));
        }
        if (n.permission != null && p != null && !p.hasPermission(n.permission)) {
            out.add(lang.raw("req-permission"));
        }
        return out;
    }

    private String name(String id, Lang lang) {
        UpgradeNode n = trees().node(id);
        return n == null ? id : n.name();
    }

    // ------------------------------------------------------------------ cost & purchase

    /** Cost of the next level, including the prestige increase. */
    public double cost(PlayerData d, UpgradeNode n) {
        double base = n.costFor(d.level(n.id));
        double factor = 1.0 + plugin.settings().prestigeCostIncrease * d.prestige();
        return Math.round(base * factor * 100.0) / 100.0;
    }

    public Result check(Player p, UpgradeNode n) {
        PlayerData d = plugin.data().get(p);
        State st = state(d, p, n);
        if (st == State.MAXED) {
            return Result.MAXED;
        }
        if (st == State.BLOCKED) {
            return Result.BLOCKED;
        }
        if (st == State.LOCKED) {
            return Result.LOCKED;
        }
        double cost = cost(d, n);
        if (cost > 0) {
            Currency c = plugin.currency();
            if (!c.available()) {
                return Result.UNAVAILABLE;
            }
            if (c.balance(p) + 1e-9 < cost) {
                return Result.NOT_ENOUGH;
            }
        }
        return Result.SUCCESS;
    }

    public Result purchase(Player p, UpgradeNode n) {
        Result pre = check(p, n);
        if (pre != Result.SUCCESS) {
            return pre;
        }
        PlayerData d = plugin.data().get(p);
        int lvl = d.level(n.id);
        UpgradePurchaseEvent event = new UpgradePurchaseEvent(p, n, lvl + 1, cost(d, n));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return Result.CANCELLED;
        }
        double cost = event.getCost();
        Currency c = plugin.currency();
        if (cost > 0) {
            if (!c.available()) {
                return Result.UNAVAILABLE;
            }
            if (!c.withdraw(p, cost)) {
                return Result.NOT_ENOUGH;
            }
        }
        d.setLevel(n.id, lvl + 1);
        d.addSpent(n.id, cost);
        plugin.data().save(d);
        plugin.effects().apply(p);
        plugin.effects().runCommands(p, n, lvl + 1);
        Bukkit.getPluginManager().callEvent(new UpgradeLevelChangeEvent(p, n, lvl, lvl + 1, cost));
        return Result.SUCCESS;
    }

    // ------------------------------------------------------------------ counting

    public int owned(PlayerData d, UpgradeTree t) {
        int c = 0;
        for (UpgradeNode n : t.nodes()) {
            if (d.level(n.id) > 0) {
                c++;
            }
        }
        return c;
    }

    public int maxed(PlayerData d, UpgradeTree t) {
        int c = 0;
        for (UpgradeNode n : t.nodes()) {
            if (d.level(n.id) >= n.maxLevel) {
                c++;
            }
        }
        return c;
    }

    public int ownedAll(PlayerData d) {
        int c = 0;
        for (UpgradeNode n : trees().nodes()) {
            if (d.level(n.id) > 0) {
                c++;
            }
        }
        return c;
    }

    public int totalLevels(PlayerData d) {
        int c = 0;
        for (UpgradeNode n : trees().nodes()) {
            c += Math.min(n.maxLevel, d.level(n.id));
        }
        return c;
    }

    /** Visible upgrades the player could buy right now. */
    public int affordable(Player p) {
        PlayerData d = plugin.data().get(p);
        Currency c = plugin.currency();
        double balance = c.available() ? c.balance(p) : 0;
        int count = 0;
        for (UpgradeNode n : trees().nodes()) {
            State st = state(d, p, n);
            if ((st == State.AVAILABLE || st == State.OWNED) && visible(d, n)) {
                double cost = cost(d, n);
                if (cost <= 0 || (c.available() && balance + 1e-9 >= cost)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Share of all levels owned (0..1), counting only upgrades this player can actually get. */
    public double progress(PlayerData d, Player p) {
        double total = 0;
        double have = 0;
        for (UpgradeNode n : trees().nodes()) {
            if (trees().unreachable(n.id) || n.minPrestige > d.prestige()) {
                continue;
            }
            if (n.permission != null && p != null && !p.hasPermission(n.permission)) {
                continue;
            }
            if (d.level(n.id) <= 0 && !blockers(d, n).isEmpty()) {
                continue;
            }
            total += n.maxLevel;
            have += Math.min(n.maxLevel, d.level(n.id));
        }
        return total <= 0 ? 0 : have / total;
    }
}
