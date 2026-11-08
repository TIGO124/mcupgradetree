package com.upgradetree.menu;

import com.upgradetree.chest.ChestReward;
import com.upgradetree.chest.ChestType;
import com.upgradetree.chest.Rarity;
import com.upgradetree.config.Lang;
import com.upgradetree.currency.Currency;
import com.upgradetree.data.PlayerData;
import com.upgradetree.service.UpgradeService;
import com.upgradetree.service.UpgradeService.State;
import com.upgradetree.tree.HexGrid;
import com.upgradetree.tree.NodeEffect;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.tree.UpgradeTree;
import com.upgradetree.util.Icons;
import com.upgradetree.util.Palette;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.upgradetree.menu.Layout.*;

/** The card on the right: details of the hovered/selected upgrade and the buy button. */
final class InfoCard {

    private final DisplayMenu menu;
    private Group group;
    private List<Part> hexBorder;
    private List<Part> hexFill;
    private Part hexShine;
    private Part icon;
    private String iconFor = "";
    private Part name;
    private Part level;
    private Part desc;
    private Part extra;
    private Part bonus;
    private Part cost;
    private Part barFill;
    private double barFrac = -1;
    private CardButton buy;
    private CardButton minus;
    /** Chests section: "open" and "buy" side by side when the player owns the shown chest. */
    private CardButton chestOpen;
    private CardButton chestBuy;

    InfoCard(DisplayMenu menu) {
        this.menu = menu;
    }

    void build(double startScale) {
        Painter pt = menu.painter();
        int card = menu.settings().theme.card();
        int stroke = menu.settings().theme.panelBorder();
        group = pt.group(CARD_CX, (CARD_Y0 + CARD_Y1) / 2.0);
        group.scale = startScale;
        double cy = (CARD_Y0 + CARD_Y1) / 2.0;
        double h = CARD_Y1 - CARD_Y0;
        pt.roundRect(group, CARD_CX, cy, CARD_W + 0.014, h + 0.014, 0.03, Z_BOX, stroke, 3);
        pt.roundRect(group, CARD_CX, cy, CARD_W, h, 0.025, Z_BOX_FILL, card, 3);

        double r = INFO_HEX_R;
        hexBorder = pt.hex(group, CARD_CX, INFO_HEX_Y, r, Z_BORDER, stroke);
        double inner = HexGrid.inset(r, r * 0.13);
        hexFill = pt.hex(group, CARD_CX, INFO_HEX_Y, inner, Z_FILL, card);
        hexShine = pt.rect(group, Part.rect(CARD_CX, INFO_HEX_Y + inner * 0.47, Math.sqrt(3) * inner * 0.6, inner * 0.2, Z_SHINE, card));
        icon = pt.item(group, Part.item(CARD_CX, INFO_HEX_Y, Math.sqrt(3) * r * 0.58, Z_ICON), Icons.of(Material.NETHER_STAR));

        pt.roundRect(group, CARD_CX, INFO_BAR_Y, INFO_BAR_W, INFO_BAR_H, INFO_BAR_H / 2.0, Z_BORDER, menu.settings().theme.background(), 2);
        barFill = pt.rect(group, Part.rect(CARD_CX - (INFO_BAR_W - 0.006) / 2.0, INFO_BAR_Y, 0, INFO_BAR_H - 0.006, Z_FILL,
                menu.settings().theme.gold()));
        barFrac = 0;

        name = pt.text(group, Part.text(CARD_CX, INFO_NAME_Y, 0.038, 1, false, Z_TEXT), Component.empty(), 2000);
        level = pt.text(group, Part.text(CARD_CX, INFO_LEVEL_Y, 0.026, 1, false, Z_TEXT), Component.empty(), 2000);
        desc = pt.text(group, Part.text(CARD_CX, INFO_DESC_TOP, INFO_DESC_LH, 1, true, Z_TEXT), Component.empty(), 2000);
        extra = pt.text(group, Part.text(CARD_CX, INFO_EXTRA_Y, 0.024, 1, false, Z_TEXT), Component.empty(), 2000);
        bonus = pt.text(group, Part.text(CARD_CX, INFO_BONUS_Y, 0.026, 1, false, Z_TEXT), Component.empty(), 2000);
        // wraps on the client; extra lines grow upwards into the (then empty) bonus row
        cost = pt.text(group, Part.text(CARD_CX, INFO_COST_Y, 0.027, 1, false, Z_TEXT), Component.empty(), 168);

        if (menu.admin()) {
            double half = (BTN_W - 0.03) / 2.0;
            minus = CardButton.spawn(pt, CARD_CX - half / 2.0 - 0.015, BTN_Y, half, BTN_H, stroke, menu.settings().theme.deny());
            buy = CardButton.spawn(pt, CARD_CX + half / 2.0 + 0.015, BTN_Y, half, BTN_H, stroke, menu.settings().theme.buy());
        } else {
            buy = CardButton.spawn(pt, CARD_CX, BTN_Y, BTN_W, BTN_H, stroke, menu.settings().theme.buy());
        }
    }

    /** The two half-width buttons of the Chests section (only menus that show that section). */
    void buildChestButtons() {
        if (chestOpen != null) {
            return;
        }
        Painter pt = menu.painter();
        int stroke = menu.settings().theme.panelBorder();
        double half = (BTN_W - 0.03) / 2.0;
        chestOpen = CardButton.spawn(pt, CARD_CX - half / 2.0 - 0.015, BTN_Y, half, BTN_H, stroke, menu.settings().theme.gold());
        chestBuy = CardButton.spawn(pt, CARD_CX + half / 2.0 + 0.015, BTN_Y, half, BTN_H, stroke, menu.settings().theme.buy());
    }

    boolean hitChestOpen(double u, double v) {
        return chestOpen != null && chestOpen.hit(u, v);
    }

    boolean hitChestBuy(double u, double v) {
        return chestBuy != null && chestBuy.hit(u, v);
    }

    double[] chestOpenCenter() {
        return chestOpen == null ? null : new double[]{chestOpen.cx, chestOpen.cy};
    }

    double[] chestBuyCenter() {
        return chestBuy == null ? null : new double[]{chestBuy.cx, chestBuy.cy};
    }

    private void hideChestButtons() {
        if (chestOpen != null) {
            chestOpen.hide(menu);
            chestBuy.hide(menu);
        }
    }

    /** Details of a chest in the Chests section, with the buy (and open) buttons. */
    void showChest(ChestType c, boolean confirmPending) {
        if (name == null || c == null) {
            return;
        }
        Lang lang = menu.lang();
        PlayerData d = menu.data();
        var theme = menu.settings().theme;
        ItemStack icon = Icons.of(c.icon);
        if (c.glint) {
            icon.editMeta(m -> m.setEnchantmentGlintOverride(true));
        }
        setIconStack(icon, "chest:" + c.id);
        colorHex(Palette.mix(c.color, theme.card(), 0.55), Palette.lighten(c.color, 0.25));
        Painter.setText(name, Text.mm("<b>" + c.name(lang)));
        int owned = d.chests(c.id);
        Painter.setText(level, lang.plain("chest-owned", "count", String.valueOf(owned)));
        setBar(0, c.color);
        setDesc(String.join("\n", c.description(lang)));
        StringBuilder odds = new StringBuilder();
        for (Rarity r : c.rarities()) {
            if (!odds.isEmpty()) {
                odds.append(' ');
            }
            odds.append(r.tag()).append("■</color><gray>")
                    .append(lang.format("chest-percent", "chance", Text.num(c.chance(r) * 100))).append("</gray>");
        }
        Painter.setText(extra, Text.mm(odds.toString()));
        Painter.setText(bonus, Component.empty());
        Currency cur = menu.plugin().currency();
        boolean afford = menu.plugin().chests().affordable(menu.player(), c);
        Painter.setText(cost, c.price <= 0 ? lang.plain("info-free")
                : lang.plain(afford ? "info-cost" : "info-cost-poor", "cost", Text.escape(menu.plugin().money(lang, c.price))));
        Component buyLabel = lang.plain(!afford ? "button-poor" : confirmPending ? "button-confirm" : "button-buy");
        int buyColor = !afford ? theme.deny() : confirmPending ? Palette.mix(theme.buy(), theme.gold(), 0.5) : theme.buy();
        if (owned > 0 && chestOpen != null) {
            buy.hide(menu);
            chestOpen.show(menu, lang.plain("button-open", "count", String.valueOf(owned)), Palette.mix(theme.gold(), theme.card(), 0.2));
            chestBuy.show(menu, buyLabel, buyColor);
        } else {
            hideChestButtons();
            buy.show(menu, buyLabel, buyColor);
        }
    }

    /** One possible prize of a chest (hovered in the contents grid): rarity and chance. */
    void showReward(ChestType c, ChestReward r) {
        if (name == null || c == null || r == null) {
            return;
        }
        Lang lang = menu.lang();
        setIconStack(r.icon(), "reward:" + c.id + ":" + r.index);
        colorHex(Palette.mix(r.rarity.color(), menu.settings().theme.card(), 0.45), r.rarity.color());
        Painter.setText(name, Text.mm("<b>" + r.coloredLabel(lang)));
        Painter.setText(level, Text.mm(r.rarity.name(lang)));
        setBar(c.chance(r), r.rarity.color());
        setDesc(lang.format("chest-reward-chance", "chance", Text.num(c.chance(r) * 100), "chest", c.name(lang)));
        Painter.setText(extra, Component.empty());
        Painter.setText(bonus, Component.empty());
        Painter.setText(cost, Component.empty());
        hideChestButtons();
        buy.hide(menu);
    }

    private void setIconStack(ItemStack stack, String key) {
        if (key.equals(iconFor) || !(icon.entity instanceof ItemDisplay id) || !id.isValid()) {
            return;
        }
        iconFor = key;
        id.setItemStack(stack);
        icon.material = stack.getType();
    }

    void open(int duration) {
        group.scale = 1.0;
        group.apply(menu.frame(), duration);
    }

    void shrink(int duration) {
        group.scale = 0.9;
        group.apply(menu.frame(), duration);
        buy.shrink(menu, duration);
        if (minus != null) {
            minus.shrink(menu, duration);
        }
        if (chestOpen != null) {
            chestOpen.shrink(menu, duration);
            chestBuy.shrink(menu, duration);
        }
    }

    boolean hitBuy(double u, double v) {
        return buy != null && buy.hit(u, v);
    }

    boolean hitMinus(double u, double v) {
        return minus != null && minus.hit(u, v);
    }

    double[] buyCenter() {
        return new double[]{buy.cx, buy.cy};
    }

    double[] minusCenter() {
        return minus == null ? null : new double[]{minus.cx, minus.cy};
    }

    private CardButton button(String id) {
        return switch (id) {
            case "minus" -> minus;
            case "chestopen" -> chestOpen;
            case "chestbuy" -> chestBuy;
            default -> buy;
        };
    }

    void setBuyHover(String id, boolean on) {
        CardButton b = button(id);
        if (b != null) {
            b.setHover(menu, on);
        }
    }

    void pulseBuy(boolean minusButton) {
        Sparks.pulse(menu, (minusButton && minus != null ? minus : buy).group);
    }

    void shakeBuy(boolean minusButton) {
        Sparks.shake(menu, (minusButton && minus != null ? minus : buy).group);
    }

    /** Squeeze (ok) or shake (refused) of a button by id ("buy", "chestbuy", "chestopen"). */
    void feedback(String id, boolean ok) {
        CardButton b = button(id);
        if (b != null) {
            if (ok) {
                Sparks.pulse(menu, b.group);
            } else {
                Sparks.shake(menu, b.group);
            }
        }
    }

    /** Shows the upgrade (or the welcome text when null). */
    void show(UpgradeNode n, boolean confirmPending) {
        if (name == null) {
            return;
        }
        hideChestButtons();
        Lang lang = menu.lang();
        PlayerData d = menu.data();
        UpgradeService svc = menu.plugin().service();
        var theme = menu.settings().theme;
        if (n == null) {
            UpgradeTree t = menu.tree();
            setIcon(t == null ? Material.NETHER_STAR : t.tabIcon, "welcome:" + (t == null ? "" : t.id));
            colorHex(Palette.mix(0xFF7DD3FC, theme.background(), 0.45), Palette.lighten(0xFF7DD3FC, 0.3));
            Painter.setText(name, lang.plain("menu-welcome-title"));
            int done = t == null ? 0 : svc.maxed(d, t);
            int total = t == null ? 0 : t.nodes().size();
            Painter.setText(level, lang.plain("info-level", "level", String.valueOf(done), "max", String.valueOf(total)));
            setBar(t == null ? 0 : (double) done / Math.max(1, total), theme.gold());
            setDesc(String.join("\n", lang.rawList("menu-welcome-desc")));
            Painter.setText(extra, d.prestige() > 0
                    ? lang.plain("info-prestige", "level", String.valueOf(d.prestige())) : Component.empty());
            Painter.setText(bonus, Component.empty());
            Painter.setText(cost, Component.empty());
            setButton(null, 0);
            return;
        }
        State st = svc.state(d, menu.subject(), n);
        int lvl = Math.min(d.level(n.id), n.maxLevel);
        setIcon(null, n.id);
        NodeLayer layer = menu.layer();
        int fill = layer == null ? n.color : layer.fillColor(n, st);
        int border = layer == null ? n.color : layer.borderColor(n, st);
        colorHex(fill, border);
        Painter.setText(name, Text.mm("<b>" + n.name(lang)));
        Painter.setText(level, lang.plain("info-level", "level", String.valueOf(lvl), "max", String.valueOf(n.maxLevel)));
        setBar((double) lvl / n.maxLevel, st == State.MAXED ? theme.gold() : n.color);
        setDesc(String.join("\n", n.description(lang)));
        Painter.setText(extra, extraLine(n, d, lvl));

        String bonusText = n.bonus(lang);
        if (st != State.LOCKED && st != State.BLOCKED && !bonusText.isBlank()) {
            String hex = String.format("#%06X", Palette.rgb(Palette.lighten(n.color, 0.3)));
            String now = "<color:" + hex + ">" + Text.apply(bonusText, "value", Text.num(n.bonusValue(lvl))) + "</color>";
            String next = lvl < n.maxLevel
                    ? " <dark_gray>→</dark_gray> <color:" + hex + ">" + Text.apply(bonusText, "value", Text.num(n.bonusValue(lvl + 1))) + "</color>"
                    : "";
            Painter.setText(bonus, Text.mm(now + next));
        } else {
            Painter.setText(bonus, Component.empty());
        }

        if (menu.admin()) {
            Painter.setText(cost, lang.plain("admin-card-hint"));
            buy.show(menu, lang.plain(lvl >= n.maxLevel ? "button-max" : "button-admin-plus"),
                    lvl >= n.maxLevel ? Palette.mix(theme.gold(), theme.card(), 0.25) : theme.buy());
            minus.show(menu, lang.plain("button-admin-minus"), lvl <= 0 ? Palette.lighten(theme.locked(), 0.12) : theme.deny());
            return;
        }
        Currency cur = menu.plugin().currency();
        Player who = menu.subject();
        switch (st) {
            case MAXED -> {
                Painter.setText(cost, lang.plain("info-completed"));
                setButton(lang.plain("button-max"), Palette.mix(theme.gold(), theme.card(), 0.25));
            }
            case LOCKED -> {
                Painter.setText(cost, lang.plain("info-requires", "requires",
                        String.join("<gray>, </gray>", svc.missing(d, who, n, lang))));
                setButton(lang.plain("button-locked"), Palette.lighten(theme.locked(), 0.12));
            }
            case BLOCKED -> {
                List<String> names = new ArrayList<>();
                for (UpgradeNode b : svc.blockers(d, n)) {
                    names.add(b.name(lang));
                }
                Painter.setText(cost, lang.plain("info-blocked", "nodes", String.join("<gray>, </gray>", names)));
                setButton(lang.plain("button-blocked"), Palette.mix(theme.deny(), theme.locked(), 0.5));
            }
            default -> {
                double c = svc.cost(d, n);
                boolean afford = c <= 0 || (who != null && cur.available() && cur.balance(who) + 1e-9 >= c);
                if (c <= 0) {
                    Painter.setText(cost, lang.plain("info-free"));
                } else {
                    Painter.setText(cost, lang.plain(afford ? "info-cost" : "info-cost-poor", "cost", Text.escape(menu.plugin().money(lang, c))));
                }
                if (!afford) {
                    setButton(lang.plain("button-poor"), theme.deny());
                } else {
                    setButton(lang.plain(confirmPending ? "button-confirm" : "button-buy"),
                            confirmPending ? Palette.mix(theme.buy(), theme.gold(), 0.5) : theme.buy());
                }
            }
        }
    }

    private Component extraLine(UpgradeNode n, PlayerData d, int lvl) {
        Lang lang = menu.lang();
        for (NodeEffect e : n.effects) {
            if (e instanceof NodeEffect.Ability a) {
                return lang.plain("info-ability", "trigger", lang.raw("trigger-" + a.trigger().name()),
                        "cooldown", Text.num(a.cooldownAt(Math.max(1, lvl))));
            }
        }
        int hidden = 0;
        UpgradeService svc = menu.plugin().service();
        for (UpgradeNode child : menu.plugin().trees().children(n.id)) {
            if (!svc.visible(d, child)) {
                hidden++;
            }
        }
        return hidden > 0 ? lang.plain("info-unlocks", "count", String.valueOf(hidden)) : Component.empty();
    }

    private void colorHex(int fill, int border) {
        for (Part p : hexFill) {
            p.recolor(fill, 4);
        }
        for (Part p : hexBorder) {
            p.recolor(border, 4);
        }
        hexShine.recolor(Palette.lighten(fill, 0.22), 4);
    }

    private void setIcon(Material m, String key) {
        if (key.equals(iconFor) || !(icon.entity instanceof ItemDisplay id) || !id.isValid()) {
            return;
        }
        iconFor = key;
        if (m != null) {
            id.setItemStack(Icons.of(m));
            icon.material = m;
        } else {
            UpgradeNode n = menu.plugin().trees().node(key);
            if (n != null) {
                id.setItemStack(Icons.of(n));
                icon.material = n.icon;
            }
        }
    }

    private void setBar(double frac, int color) {
        frac = Math.max(0, Math.min(1, frac));
        if (Math.abs(frac - barFrac) > 1e-6) {
            barFrac = frac;
            double inner = INFO_BAR_W - 0.006;
            double w = inner * frac;
            barFill.w = w;
            barFill.cx = CARD_CX - inner / 2.0 + w / 2.0;
            group.apply(menu.frame(), barFill, 5);
        }
        barFill.recolor(color, 5);
    }

    private void setDesc(String mini) {
        List<String> wrapped = Text.wrap(mini, INFO_DESC_WRAP_PX);
        if (wrapped.size() > INFO_DESC_MAX_LINES) {
            wrapped = wrapped.subList(0, INFO_DESC_MAX_LINES);
        }
        int count = Math.max(1, wrapped.size());
        Painter.setText(desc, Text.mm(String.join("\n", wrapped)));
        if (count != desc.lines) {
            desc.lines = count;
            group.apply(menu.frame(), desc, 0);
        }
    }

    private void setButton(Component label, int color) {
        if (label == null) {
            buy.hide(menu);
            if (minus != null) {
                minus.hide(menu);
            }
        } else {
            buy.show(menu, label, color);
        }
    }
}
