package com.upgradetree.menu;

import com.upgradetree.currency.Currency;
import com.upgradetree.data.PlayerData;
import com.upgradetree.service.ProgressService;
import com.upgradetree.service.UpgradeService.Result;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

/** What the menu buttons do: buying (with confirmation), resetting and prestige. */
final class MenuActions {

    private final DisplayMenu m;

    MenuActions(DisplayMenu menu) {
        this.m = menu;
    }

    void tryPurchase(UpgradeNode n, boolean viaButton) {
        if (n == null) {
            return;
        }
        double cost = m.plugin().service().cost(m.data(), n);
        boolean wantsConfirm = m.settings().confirmCost > 0 && cost >= m.settings().confirmCost
                && m.plugin().service().check(m.player(), n) == Result.SUCCESS;
        if (wantsConfirm && !(n.id.equals(m.confirmNode) && m.tick <= m.confirmUntil)) {
            m.confirmNode = n.id;
            m.confirmUntil = m.tick + 60;
            m.sound("minecraft:block.note_block.pling", 0.6f, 1.2f);
            m.player().sendActionBar(m.lang().plain("purchase-confirm", "node", n.nameRaw,
                    "cost", Text.escape(m.plugin().currency().format(cost))));
            m.refreshInfo();
            return;
        }
        m.confirmNode = null;
        purchase(n, viaButton);
    }

    private void purchase(UpgradeNode n, boolean viaButton) {
        PlayerData d = m.data();
        Currency cur = m.plugin().currency();
        double cost = m.plugin().service().cost(d, n);
        double balance = cur.available() ? cur.balance(m.player()) : 0;
        Result r = m.plugin().service().purchase(m.player(), n);
        switch (r) {
            case SUCCESS -> {
                int lvl = d.level(n.id);
                boolean max = lvl >= n.maxLevel;
                if (m.layer != null) {
                    m.layer.paintAll(6);
                    m.layer.pop(n.id);
                    m.layer.burst(n.id, max);
                    if (m.layer.reveal(n) > 0) {
                        m.later(6, () -> m.sound("minecraft:block.amethyst_block.resonate", 0.8f, 1.4f));
                    }
                }
                if (viaButton) {
                    m.info.pulseBuy();
                }
                m.sound("minecraft:entity.player.levelup", 0.55f, max ? 2.0f : 1.5f);
                m.sound("minecraft:block.amethyst_block.chime", 1.0f, 1.2f);
                m.player().sendActionBar(m.lang().plain(max ? "purchase-max" : "purchase-success",
                        "node", n.nameRaw, "level", String.valueOf(lvl)));
                m.plugin().leaderboard().touch(d);
            }
            case MAXED -> deny(n, viaButton, m.lang().plain("purchase-maxed"));
            case LOCKED -> deny(n, viaButton, m.lang().plain("purchase-locked", "requires",
                    String.join("<gray>, </gray>", m.plugin().service().missing(d, m.player(), n))));
            case BLOCKED -> {
                List<String> names = new ArrayList<>();
                for (UpgradeNode b : m.plugin().service().blockers(d, n)) {
                    names.add(b.nameRaw);
                }
                deny(n, viaButton, m.lang().plain("purchase-blocked", "nodes", String.join("<gray>, </gray>", names)));
            }
            case NOT_ENOUGH -> deny(n, viaButton, m.lang().plain("purchase-not-enough",
                    "currency", Text.escape(m.settings().currencyName),
                    "cost", Text.escape(cur.format(cost)),
                    "balance", Text.escape(cur.format(balance))));
            case UNAVAILABLE -> deny(n, viaButton, m.lang().plain("currency-unavailable"));
            case CANCELLED -> deny(n, viaButton, m.lang().plain("purchase-cancelled"));
        }
        m.updateStatus();
        m.refreshInfo();
    }

    private void deny(UpgradeNode n, boolean viaButton, Component message) {
        if (viaButton) {
            m.info.shakeBuy();
        } else if (m.layer != null) {
            m.layer.shake(n.id);
        }
        m.sound("minecraft:block.note_block.bass", 0.8f, 0.7f);
        m.player().sendActionBar(message);
    }

    void respec() {
        PlayerData d = m.data();
        ProgressService ps = m.plugin().progress();
        if (m.respecUntil < 0 || m.tick > m.respecUntil) {
            if (!m.settings().respecEnabled) {
                m.player().sendActionBar(m.lang().plain("respec-disabled"));
                return;
            }
            if (d.levels().isEmpty()) {
                m.player().sendActionBar(m.lang().plain("respec-nothing"));
                m.sound("minecraft:block.note_block.bass", 0.8f, 0.7f);
                return;
            }
            long left = ps.respecCooldownLeft(d);
            if (left > 0) {
                m.player().sendActionBar(m.lang().plain("respec-cooldown", "time", Text.duration(left)));
                m.sound("minecraft:block.note_block.bass", 0.8f, 0.7f);
                return;
            }
            m.respecUntil = m.tick + 60;
            m.header.setRespecConfirm(true);
            m.player().sendActionBar(m.lang().plain("respec-confirm", "refund", Text.escape(m.plugin().currency().format(ps.refund(d)))));
            m.sound("minecraft:block.note_block.pling", 0.6f, 0.9f);
            return;
        }
        m.respecUntil = -1;
        m.header.setRespecConfirm(false);
        double refund = ps.refund(d);
        ProgressService.RespecResult r = ps.respec(m.player());
        switch (r) {
            case DONE -> {
                m.player().sendActionBar(m.lang().plain("respec-done", "refund", Text.escape(m.plugin().currency().format(refund))));
                m.sound("minecraft:block.beacon.deactivate", 0.8f, 1.2f);
                m.rebuildLayer();
            }
            case COOLDOWN -> m.player().sendActionBar(m.lang().plain("respec-cooldown", "time", Text.duration(ps.respecCooldownLeft(d))));
            case NOTHING -> m.player().sendActionBar(m.lang().plain("respec-nothing"));
            case DISABLED -> m.player().sendActionBar(m.lang().plain("respec-disabled"));
            case CANCELLED -> m.player().sendActionBar(m.lang().plain("purchase-cancelled"));
        }
    }

    void prestige() {
        if (m.prestigeUntil < 0 || m.tick > m.prestigeUntil) {
            m.prestigeUntil = m.tick + 60;
            m.header.setPrestigeConfirm(true);
            m.player().sendActionBar(m.lang().plain("prestige-confirm"));
            m.sound("minecraft:block.note_block.pling", 0.6f, 1.5f);
            return;
        }
        m.prestigeUntil = -1;
        m.header.setPrestigeConfirm(false);
        ProgressService.PrestigeResult r = m.plugin().progress().prestige(m.player());
        PlayerData d = m.data();
        switch (r) {
            case DONE -> {
                m.player().sendActionBar(m.lang().plain("prestige-done", "level", String.valueOf(d.prestige())));
                m.rebuildLayer();
            }
            case MAX -> m.player().sendActionBar(m.lang().plain("prestige-max"));
            case DISABLED -> m.player().sendActionBar(m.lang().plain("prestige-disabled"));
            case NOT_READY -> m.player().sendActionBar(m.lang().plain("prestige-not-ready",
                    "percent", String.valueOf(Math.round(m.plugin().service().progress(d, m.player()) * 100)),
                    "required", Text.num(m.settings().prestigeRequiredPercent)));
            case CANCELLED -> m.player().sendActionBar(m.lang().plain("purchase-cancelled"));
        }
        m.updateStatus();
    }
}
