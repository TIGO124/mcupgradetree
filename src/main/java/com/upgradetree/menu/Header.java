package com.upgradetree.menu;

import com.upgradetree.config.Lang;
import com.upgradetree.config.Settings;
import com.upgradetree.currency.Currency;
import com.upgradetree.tree.UpgradeTree;
import com.upgradetree.util.Icons;
import com.upgradetree.util.Palette;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.upgradetree.menu.Layout.*;

/** Top bar: tabs (or the title), currency, reset and prestige buttons, close button. */
final class Header {

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

    /** A header tab: a tree, or the Chests section ({@link ChestLayer#TAB}); {@code line} marks the active one. */
    private record Tab(String id, Group group, List<Part> fill, Part line, double cx, double w) {
    }

    Header(DisplayMenu menu) {
        this.menu = menu;
    }

    private Settings.Theme theme() {
        return menu.settings().theme;
    }

    void build(List<UpgradeTree> trees, UpgradeTree current, double startScale, boolean chests) {
        Painter pt = menu.painter();
        Lang lang = menu.lang();
        group = pt.group(0, HEADER_CY);
        group.scale = startScale;
        pt.roundRect(group, 0, HEADER_CY, HEADER_W, HEADER_H, 0.026, Z_BOX, theme().header(), 3);

        int count = trees.size() + (chests ? 1 : 0);
        if (count >= 2) {
            double avail = TABS_X1 - TABS_X0;
            double w = Math.min(TAB_MAX_W, (avail - (count - 1) * TAB_GAP) / count);
            double x = TABS_X0 + w / 2.0;
            for (UpgradeTree t : trees) {
                addTab(t.id, Icons.of(t.tabIcon), t.tabRaw(), x, w, startScale);
                x += w + TAB_GAP;
            }
            if (chests) {
                addTab(ChestLayer.TAB, Icons.of(Material.CHEST), lang.raw("chests-tab"), x, w, startScale);
            }
        } else {
            Component title = current == null ? lang.plain("tree-title-default") : current.title(lang);
            pt.text(group, Part.text((TABS_X0 + TABS_X1) / 2.0, HEADER_CY, 0.044, 1, false, Z_TEXT), title, 2000);
        }

        pt.roundRect(group, PILL_X, HEADER_CY, PILL_W, PILL_H, 0.02, Z_BOX_FILL, Palette.darken(theme().header(), 0.4), 3);
        pt.item(group, Part.item(PILL_X - PILL_W / 2.0 + 0.036, HEADER_CY, PILL_ICON, Z_ICON), Icons.of(currencyIcon()));
        currencyCache = currencyText();
        currency = pt.text(group, Part.text(PILL_X + 0.018, HEADER_CY, 0.028, 1, false, Z_TEXT), currencyCache, 2000);

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
        pt.text(close, Part.text(CLOSE_X, HEADER_CY, 0.034, 1, false, Z_TEXT), Text.mm("<hi><b>X"), 200);

        setActive(current == null ? "" : current.id);
    }

    /** Icon of the currency in the pill (a name tag in the admin mode). */
    private Material currencyIcon() {
        if (menu.admin()) {
            return Material.NAME_TAG;
        }
        return switch (menu.settings().currencyType) {
            case "VAULT" -> Material.GOLD_INGOT;
            case "POINTS" -> Material.AMETHYST_SHARD;
            default -> Material.EXPERIENCE_BOTTLE;
        };
    }

    /** Icon and name side by side, centred together in the tab; a long name gets a smaller font. */
    private void addTab(String id, ItemStack icon, String name, double x, double w, double startScale) {
        Painter pt = menu.painter();
        Group g = pt.group(x, HEADER_CY);
        g.scale = startScale;
        List<Part> fill = pt.roundRect(g, x, HEADER_CY, w, TAB_H, 0.018, Z_BOX_FILL, theme().header(), 3);
        Part line = pt.rect(g, Part.rect(x, HEADER_CY - TAB_H / 2.0 + TAB_LINE_H / 2.0 + 0.003, w - 0.07, TAB_LINE_H, Z_BORDER, 0));
        double gap = 0.012;
        double room = w - TAB_ICON - gap - 0.03;
        double perUnit = Painter.width(name, 1.0);
        double size = perUnit <= 0 ? TAB_TEXT : Math.min(TAB_TEXT, room / perUnit);
        double textW = perUnit * size;
        double left = x - (TAB_ICON + gap + textW) / 2.0;
        pt.item(g, Part.item(left + TAB_ICON / 2.0, HEADER_CY, TAB_ICON, Z_ICON), icon);
        pt.text(g, Part.text(left + TAB_ICON + gap + textW / 2.0, HEADER_CY, size, 1, false, Z_TEXT), Text.mm(name), 2000);
        tabs.add(new Tab(id, g, fill, line, x, w));
    }

    /** See-through header (glass styles): idle tabs are invisible; opaque styles get darker slabs. */
    private boolean glass() {
        return Palette.translucent(theme().header());
    }

    private int tabColor(boolean active, boolean hover) {
        Settings.Theme t = theme();
        if (active) {
            // light styles (white frames) would make the slab too bright for the white label
            return glass() ? Palette.alpha(Palette.atMost(Palette.mix(Palette.alpha(t.panelBorder(), 255), t.gold(), 0.2), 0.42), 0xC0)
                    : Palette.atMost(Palette.mix(t.header(), t.gold(), 0.16), 0.42);
        }
        if (hover) {
            return glass() ? Palette.alpha(t.panelBorder(), 0x80) : Palette.lighten(t.header(), 0.06);
        }
        return glass() ? 0x00000000 : Palette.darken(t.header(), 0.25);
    }

    private void paintTab(Tab t, boolean hover, int duration) {
        boolean on = t.id().equals(active);
        int c = tabColor(on, hover);
        for (Part p : t.fill()) {
            p.recolor(c, duration);
        }
        t.line().recolor(on ? Palette.alpha(theme().gold(), 255) : 0, duration);
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

    void setActive(String tabId) {
        active = tabId;
        for (Tab t : tabs) {
            paintTab(t, false, 4);
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
                return "tab:" + t.id();
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
                    if (target.equals("tab:" + t.id())) {
                        t.group().scale = on ? 1.05 : 1.0;
                        t.group().apply(menu.frame(), 3);
                        paintTab(t, on, 3);
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
        if (menu.admin()) {
            return menu.lang().plain("admin-editing", "player", Text.escape(menu.subjectName()));
        }
        Currency c = menu.plugin().currency();
        Settings s = menu.settings();
        String amount = c.available() ? c.formatShort(c.balance(menu.player())) : "-";
        return menu.lang().plain("menu-currency", "symbol", Text.symbol(s.currencySymbol),
                "amount", Text.escape(amount), "currency", Text.escape(menu.plugin().currencyName()));
    }

    String active() {
        return active;
    }

    double[] tabCenter(String tabId) {
        for (Tab t : tabs) {
            if (t.id().equalsIgnoreCase(tabId)) {
                return new double[]{t.cx(), HEADER_CY};
            }
        }
        return null;
    }
}
