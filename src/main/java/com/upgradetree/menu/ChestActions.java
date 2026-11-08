package com.upgradetree.menu;

import com.upgradetree.chest.ChestManager;
import com.upgradetree.chest.ChestType;
import com.upgradetree.currency.Currency;
import com.upgradetree.util.Text;

/**
 * Buttons of the Chests section. A bought (or owned) chest is opened in front of the player, so the
 * menu closes first and the chest appears a few ticks later.
 */
final class ChestActions {

    private final DisplayMenu m;
    private final ChestSection s;

    ChestActions(DisplayMenu menu, ChestSection section) {
        this.m = menu;
        this.s = section;
    }

    private ChestManager chests() {
        return m.plugin().chests();
    }

    /** BUY button: confirms expensive chests with a second click like upgrades do. */
    void buy(ChestType c, String button) {
        if (c == null) {
            return;
        }
        boolean wantsConfirm = m.settings().confirmCost > 0 && c.price >= m.settings().confirmCost
                && chests().affordable(m.player(), c);
        if (wantsConfirm && !(c.id.equals(s.confirmId) && m.tick <= s.confirmUntil)) {
            s.confirmId = c.id;
            s.confirmUntil = m.tick + 60;
            m.sound("minecraft:block.note_block.pling", 0.6f, 1.2f);
            m.player().sendActionBar(m.lang().plain("chest-confirm", "chest", c.name(m.lang()),
                    "cost", Text.escape(m.plugin().money(m.lang(), c.price))));
            m.refreshInfo();
            return;
        }
        s.confirmId = null;
        Currency cur = m.plugin().currency();
        switch (chests().buy(m.player(), c)) {
            case SUCCESS -> {
                m.info.feedback(button, true);
                m.sound("minecraft:entity.player.levelup", 0.55f, 1.6f);
                m.sound("minecraft:block.chest.open", 0.7f, 1.2f);
                if (chests().busy(m.player())) {
                    m.player().sendActionBar(m.lang().plain("chest-bought-stash", "chest", c.name(m.lang())));
                    m.updateStatus();
                    m.refreshInfo();
                    return;
                }
                m.player().sendMessage(m.lang().msg("chest-bought", "chest", c.name(m.lang())));
                summon(c);
            }
            case NOT_ENOUGH -> deny(button, m.lang().plain("purchase-not-enough",
                    "currency", Text.escape(m.plugin().currencyName(m.lang())), "cost", Text.escape(m.plugin().money(m.lang(), c.price)),
                    "balance", Text.escape(m.plugin().money(m.lang(), cur.available() ? cur.balance(m.player()) : 0))));
            case UNAVAILABLE -> deny(button, m.lang().plain("currency-unavailable"));
            case DISABLED -> deny(button, m.lang().plain("chests-disabled"));
        }
    }

    /** OPEN button: one of the player's chests appears in front of them. */
    void open(ChestType c) {
        if (c == null) {
            return;
        }
        if (m.data().chests(c.id) <= 0) {
            deny("chestopen", m.lang().plain("chest-none", "chest", c.name(m.lang())));
            return;
        }
        if (chests().busy(m.player())) {
            deny("chestopen", m.lang().plain("chest-busy"));
            return;
        }
        m.info.feedback("chestopen", true);
        m.sound("minecraft:block.chest.open", 0.7f, 1.2f);
        summon(c);
    }

    private void summon(ChestType c) {
        m.plugin().menus().close(m.player(), false);
        chests().openLater(m.player(), c, 6L);
    }

    private void deny(String button, net.kyori.adventure.text.Component message) {
        m.info.feedback(button, false);
        m.sound("minecraft:block.note_block.bass", 0.8f, 0.7f);
        m.player().sendActionBar(message);
        m.updateStatus();
        m.refreshInfo();
    }
}
