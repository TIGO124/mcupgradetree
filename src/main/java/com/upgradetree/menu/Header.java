package com.upgradetree.menu;

import com.upgradetree.config.Lang;
import com.upgradetree.config.Settings;
import com.upgradetree.currency.Currency;
import com.upgradetree.tree.UpgradeTree;
import com.upgradetree.util.Icons;
import com.upgradetree.util.Palette;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

import static com.upgradetree.menu.Layout.*;

/** Top bar: tabs (or the title), currency, reset and prestige buttons, close button. */
final class Header {

    private static final int ACCENT = 0xFF7DD3FC;

    private final DisplayMenu menu;
    private final List<Tab> tabs = new ArrayList<>();
    private Group group;
    private Group close;
    private List<Part> closeFill;
    private Group respec;
    private List<Part> respecFill;
    private Part respecLabel;
    private Group prestige;
    private List<Part> prestigeFill;
    private Part prestigeLabel;
    private boolean prestigeVisible;
    private Part currency;
    private Component currencyCache;
    private String active = "";

    private record Tab(UpgradeTree tree, Group group, List<Part> fill, double cx, double w) {
    }

    Header(DisplayMenu menu) {
        this.menu = menu;
    }

    private Settings.Theme theme() {
        return menu.settings().theme;
    }

    void build(List<UpgradeTree> trees, UpgradeTree current, double startScale) {
        Painter pt = menu.painter();
        Lang lang = menu.lang();
        group = pt.group(0, HEADER_CY);
        group.scale = startScale;
        pt.roundRect(group, 0, HEADER_CY, HEADER_W, HEADER_H, 0.026, Z_BOX, theme().header(), 3);

        if (trees.size() >= 2) {
            double avail = TABS_X1 - TABS_X0;
            double w = Math.min(TAB_MAX_W, (avail - (trees.size() - 1) * TAB_GAP) / trees.size());
            double x = TABS_X0 + w / 2.0;
            for (UpgradeTree t : trees) {
                Group g = pt.group(x, HEADER_CY);
                g.scale = startScale;
                List<Part> fill = pt.roundRect(g, x, HEADER_CY, w, TAB_H, 0.018, Z_BOX_FILL, theme().header(), 3);
                double iconX = x - w / 2.0 + 0.032;
                pt.item(g, Part.item(iconX, HEADER_CY, 0.034, Z_ICON), Icons.of(t.tabIcon));
                pt.text(g, Part.text(x + 0.016, HEADER_CY, 0.026, 1, false, Z_TEXT), t.tabName, 2000);
                tabs.add(new Tab(t, g, fill, x, w));
                x += w + TAB_GAP;
            }
        } else {
            Component title = current == null ? Text.mm(UpgradeTree.DEFAULT_TITLE) : current.title;
            pt.text(group, Part.text((TABS_X0 + TABS_X1) / 2.0, HEADER_CY, 0.044, 1, false, Z_TEXT), title, 2000);
        }

        pt.roundRect(group, PILL_X, HEADER_CY, PILL_W, PILL_H, 0.02, Z_BOX_FILL, Palette.darken(theme().header(), 0.4), 3);
        currencyCache = currencyText();
        currency = pt.text(group, Part.text(PILL_X, HEADER_CY, 0.028, 1, false, Z_TEXT), currencyCache, 2000);

        if (menu.settings().respecEnabled) {
            respec = pt.group(RESPEC_X, HEADER_CY);
            respec.scale = startScale;
            respecFill = pt.roundRect(respec, RESPEC_X, HEADER_CY, RESPEC_W, BUTTON_H, 0.018, Z_BOX_FILL,
                    Palette.lighten(theme().header(), 0.08), 3);
            respecLabel = pt.text(respec, Part.text(RESPEC_X, HEADER_CY, 0.025, 1, false, Z_TEXT), lang.plain("button-respec"), 2000);
        }

        prestige = pt.group(PRESTIGE_X, HEADER_CY);
        prestige.scale = 0.001;
        prestigeFill = pt.roundRect(prestige, PRESTIGE_X, HEADER_CY, PRESTIGE_W, BUTTON_H, 0.018, Z_BOX_FILL,
                Palette.mix(theme().gold(), theme().header(), 0.55), 3);
        prestigeLabel = pt.text(prestige, Part.text(PRESTIGE_X, HEADER_CY, 0.025, 1, false, Z_TEXT), lang.plain("button-prestige"), 2000);
        prestigeVisible = false;

        close = pt.group(CLOSE_X, HEADER_CY);
        close.scale = startScale;
        pt.roundRect(close, CLOSE_X, HEADER_CY - 0.003, CLOSE_SIZE + 0.014, CLOSE_SIZE + 0.02, 0.016, Z_BOX_FILL, theme().panelBorder(), 3);
        closeFill = pt.roundRect(close, CLOSE_X, HEADER_CY, CLOSE_SIZE, CLOSE_SIZE, 0.012, Z_BORDER, theme().deny(), 3);
        pt.text(close, Part.text(CLOSE_X, HEADER_CY, 0.034, 1, false, Z_TEXT), Text.mm("<white><b>X"), 200);

        setActive(current == null ? "" : current.id);
    }

    List<Group> groups() {
        List<Group> list = new ArrayList<>();
        list.add(group);
        for (Tab t : tabs) {
            list.add(t.group());
        }
        if (respec != null) {
            list.add(respec);
        }
        list.add(close);
        return list;
    }

    void setScale(double scale, int duration) {
        for (Group g : groups()) {
            g.scale = scale;
            g.apply(menu.frame(), duration);
        }
        if (prestigeVisible || scale < 0.5) {
            prestige.scale = prestigeVisible && scale >= 0.5 ? 1.0 : 0.001;
            prestige.apply(menu.frame(), duration);
        }
    }

    void setActive(String treeId) {
        active = treeId;
        for (Tab t : tabs) {
            boolean on = t.tree().id.equals(treeId);
            int c = on ? Palette.mix(theme().header(), ACCENT, 0.32) : Palette.darken(theme().header(), 0.35);
            for (Part p : t.fill()) {
                p.recolor(c, 4);
            }
        }
    }

    String hit(double u, double v) {
        if (Math.abs(u - CLOSE_X) <= CLOSE_SIZE * 0.7 && Math.abs(v - HEADER_CY) <= CLOSE_SIZE * 0.7) {
            return "close";
        }
        double half = BUTTON_H / 2.0 + 0.006;
        if (respec != null && Math.abs(u - RESPEC_X) <= RESPEC_W / 2.0 && Math.abs(v - HEADER_CY) <= half) {
            return "respec";
        }
        if (prestigeVisible && Math.abs(u - PRESTIGE_X) <= PRESTIGE_W / 2.0 && Math.abs(v - HEADER_CY) <= half) {
            return "prestige";
        }
        for (Tab t : tabs) {
            if (Math.abs(u - t.cx()) <= t.w() / 2.0 && Math.abs(v - HEADER_CY) <= TAB_H / 2.0 + 0.006) {
                return "tab:" + t.tree().id;
            }
        }
        return null;
    }

    void hover(String target, boolean on) {
        switch (target) {
            case "close" -> {
                close.scale = on ? 1.15 : 1.0;
                close.apply(menu.frame(), 3);
                for (Part p : closeFill) {
                    p.recolor(on ? Palette.lighten(theme().deny(), 0.2) : theme().deny(), 3);
                }
            }
            case "respec" -> {
                respec.scale = on ? 1.08 : 1.0;
                respec.apply(menu.frame(), 3);
            }
            case "prestige" -> {
                prestige.scale = on ? 1.08 : 1.0;
                prestige.apply(menu.frame(), 3);
            }
            default -> {
                for (Tab t : tabs) {
                    if (target.equals("tab:" + t.tree().id)) {
                        t.group().scale = on ? 1.06 : 1.0;
                        t.group().apply(menu.frame(), 3);
                    }
                }
            }
        }
    }

    void setRespecConfirm(boolean on) {
        if (respec == null) {
            return;
        }
        Painter.setText(respecLabel, menu.lang().plain(on ? "button-respec-confirm" : "button-respec"));
        for (Part p : respecFill) {
            p.recolor(on ? Palette.mix(theme().deny(), theme().header(), 0.3) : Palette.lighten(theme().header(), 0.08), 3);
        }
    }

    void setPrestigeConfirm(boolean on) {
        Painter.setText(prestigeLabel, menu.lang().plain(on ? "button-prestige-confirm" : "button-prestige"));
        for (Part p : prestigeFill) {
            p.recolor(on ? theme().gold() : Palette.mix(theme().gold(), theme().header(), 0.55), 3);
        }
    }

    /** Refreshes the currency and whether the prestige button is shown. */
    void update(boolean prestigeReady) {
        Component c = currencyText();
        if (!c.equals(currencyCache)) {
            currencyCache = c;
            Painter.setText(currency, c);
        }
        if (prestigeReady != prestigeVisible) {
            prestigeVisible = prestigeReady;
            prestige.scale = prestigeReady ? 1.0 : 0.001;
            prestige.apply(menu.frame(), 4);
        }
    }

    private Component currencyText() {
        Currency c = menu.plugin().currency();
        Settings s = menu.settings();
        String amount = c.available() ? c.formatShort(c.balance(menu.player())) : "-";
        return menu.lang().plain("menu-currency", "symbol", Text.escape(s.currencySymbol),
                "amount", Text.escape(amount), "currency", Text.escape(s.currencyName));
    }

    String active() {
        return active;
    }
}
