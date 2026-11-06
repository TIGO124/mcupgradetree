package com.upgradetree.service;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.currency.Currency;
import com.upgradetree.data.PlayerData;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.tree.UpgradeTree;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Purchase rules: requirements, costs and node states. */
public final class UpgradeService {

    public enum State { LOCKED, AVAILABLE, OWNED, MAXED }

    public enum Result { SUCCESS, MAXED, LOCKED, NOT_ENOUGH, UNAVAILABLE }

    private final UpgradeTreePlugin plugin;

    public UpgradeService(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean requirementsMet(PlayerData d, UpgradeNode n) {
        for (String r : n.requires) {
            if (d.level(r) <= 0) {
                return false;
            }
        }
        return true;
    }

    public List<UpgradeNode> missing(PlayerData d, UpgradeNode n) {
        List<UpgradeNode> list = new ArrayList<>();
        UpgradeTree tree = plugin.tree();
        for (String r : n.requires) {
            if (d.level(r) <= 0) {
                UpgradeNode rn = tree.node(r);
                if (rn != null) {
                    list.add(rn);
                }
            }
        }
        return list;
    }

    public State state(PlayerData d, UpgradeNode n) {
        int lvl = d.level(n.id);
        if (lvl >= n.maxLevel) {
            return State.MAXED;
        }
        if (lvl > 0) {
            return State.OWNED;
        }
        return requirementsMet(d, n) ? State.AVAILABLE : State.LOCKED;
    }

    public Result check(Player p, UpgradeNode n) {
        PlayerData d = plugin.data().get(p);
        int lvl = d.level(n.id);
        if (lvl >= n.maxLevel) {
            return Result.MAXED;
        }
        if (!requirementsMet(d, n)) {
            return Result.LOCKED;
        }
        Currency c = plugin.currency();
        double cost = n.costFor(lvl);
        if (cost > 0) {
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
        double cost = n.costFor(lvl);
        if (cost > 0 && !plugin.currency().withdraw(p, cost)) {
            return Result.NOT_ENOUGH;
        }
        d.setLevel(n.id, lvl + 1);
        plugin.data().save(d);
        plugin.effects().apply(p);
        plugin.effects().runCommands(p, n, lvl + 1);
        return Result.SUCCESS;
    }

    public int ownedNodes(PlayerData d) {
        int c = 0;
        for (UpgradeNode n : plugin.tree().nodes()) {
            if (d.level(n.id) > 0) {
                c++;
            }
        }
        return c;
    }

    public int totalLevels(PlayerData d) {
        int c = 0;
        for (UpgradeNode n : plugin.tree().nodes()) {
            c += Math.min(n.maxLevel, d.level(n.id));
        }
        return c;
    }
}
