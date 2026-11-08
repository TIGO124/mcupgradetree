package com.upgradetree.menu;

import com.upgradetree.chest.ChestReward;
import com.upgradetree.chest.ChestType;
import com.upgradetree.config.Settings;
import com.upgradetree.currency.Currency;
import com.upgradetree.data.PlayerData;
import com.upgradetree.util.Icons;
import com.upgradetree.util.Palette;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.upgradetree.menu.Layout.*;

/**
 * The Chests section of the 3D menu (in place of a hexagon tree): one card per chest on top and,
 * below, the contents of the hovered/selected chest with CS2-style rarity colours.
 */
final class ChestLayer {

    static final String TAB = "@chests";
    private static final double AVAIL = (CANVAS_X1 - CANVAS_X0) - 0.04;
    private static final int PER_ROW = 5;
    private static final double CARD_MAX_W = 0.25;
    private static final double CARD_GAP = 0.03;
    private static final double CELL_W = 0.105;
    private static final double CELL_H = 0.088;
    private static final double CELL_GAP = 0.012;
    private static final int DARK = 0xFF10131D;

    static final class CardView {
        final ChestType chest;
        final Group group;
        final double cx;
        final double cy;
        final double w;
        final double h;
        List<Part> border = List.of();
        Part price;
        Part badge;
        boolean hover;

        CardView(ChestType chest, Group group, double cx, double cy, double w, double h) {
            this.chest = chest;
            this.group = group;
            this.cx = cx;
            this.cy = cy;
            this.w = w;
            this.h = h;
        }
    }

    static final class CellView {
        final Group group;
        final double cx;
        final double cy;
        Part bg;
        Part bar;
        Part icon;
        boolean visible = true;

        CellView(Group group, double cx, double cy) {
            this.group = group;
            this.cx = cx;
            this.cy = cy;
        }
    }

    private final DisplayMenu menu;
    final List<CardView> cards = new ArrayList<>();
    final List<CellView> cells = new ArrayList<>();
    private Group texts;
    private Part label;
    private ChestType shown;
    private List<ChestReward> shownRewards = List.of();

    ChestLayer(DisplayMenu menu) {
        this.menu = menu;
    }

    private Settings.Theme theme() {
        return menu.settings().theme;
    }

    private List<ChestType> chests() {
        List<ChestType> all = menu.plugin().chestTypes().chests();
        return all.size() > 2 * PER_ROW ? all.subList(0, 2 * PER_ROW) : all;
    }

    // ------------------------------------------------------------------ building

    void build(boolean pop) {
        Painter pt = menu.painter();
        List<ChestType> list = chests();
        int rows = list.size() > PER_ROW ? 2 : 1;
        double cardH = rows == 1 ? 0.27 : 0.19;
        double[] rowY = rows == 1 ? new double[]{0.145} : new double[]{0.2, -0.005};
        double labelY = rows == 1 ? -0.03 : -0.118;
        double cellTop = rows == 1 ? -0.075 : -0.16;
        int cellRows = rows == 1 ? 3 : 2;

        texts = pt.group(CANVAS_CX, 0);
        pt.text(texts, Part.text(CANVAS_CX, 0.335, 0.022, 1, false, Z_TEXT), menu.lang().plain("chests-hint"), 4000);
        label = pt.text(texts, Part.text(CANVAS_CX, labelY, 0.022, 1, false, Z_TEXT), Component.empty(), 4000);

        for (int row = 0; row < rows; row++) {
            int from = row * PER_ROW;
            int count = Math.min(PER_ROW, list.size() - from);
            double w = Math.min(CARD_MAX_W, (AVAIL - (count - 1) * CARD_GAP) / Math.max(1, count));
            double x = CANVAS_CX - (count * w + (count - 1) * CARD_GAP) / 2.0 + w / 2.0;
            for (int i = 0; i < count; i++) {
                cards.add(spawnCard(list.get(from + i), x, rowY[row], w, cardH, pop));
                x += w + CARD_GAP;
            }
        }

        int perRow = (int) Math.floor((AVAIL + CELL_GAP) / (CELL_W + CELL_GAP));
        int needed = Math.min(perRow * cellRows, menu.plugin().chestTypes().maxRewards());
        for (int i = 0; i < needed; i++) {
            int row = i / perRow;
            int col = i % perRow;
            int inRow = Math.min(perRow, needed - row * perRow);
            double rowW = inRow * CELL_W + (inRow - 1) * CELL_GAP;
            double cx = CANVAS_CX - rowW / 2.0 + CELL_W / 2.0 + col * (CELL_W + CELL_GAP);
            double cy = cellTop - CELL_H / 2.0 - row * (CELL_H + CELL_GAP);
            cells.add(spawnCell(cx, cy));
        }
        if (pop && menu.animate()) {
            for (int i = 0; i < cards.size(); i++) {
                popIn(cards.get(i).group, 3 + i * 2);
            }
        }
        refresh();
    }

    private CardView spawnCard(ChestType c, double cx, double cy, double w, double h, boolean pop) {
        Painter pt = menu.painter();
        Group g = pt.group(cx, cy);
        g.scale = pop && menu.animate() ? 0.001 : 1.0;
        CardView v = new CardView(c, g, cx, cy, w, h);
        v.border = pt.roundRect(g, cx, cy, w + 0.014, h + 0.014, 0.024, Z_BORDER, border(v), 3);
        pt.roundRect(g, cx, cy, w, h, 0.02, Z_FILL, Palette.mix(theme().card(), c.color, 0.1), 3);
        ItemStack icon = Icons.of(c.icon);
        if (c.glint) {
            icon.editMeta(m -> m.setEnchantmentGlintOverride(true));
        }
        pt.item(g, Part.item(cx, cy + h * 0.15, Math.min(0.11, w * 0.45), Z_ICON), icon);
        pt.text(g, Part.text(cx, cy - h * 0.19, 0.024, 1, false, Z_TEXT), Text.mm("<b>" + c.name(menu.lang())), 4000);
        v.price = pt.text(g, Part.text(cx, cy - h * 0.35, 0.021, 1, false, Z_TEXT), Component.empty(), 4000);
        v.badge = pt.text(g, Part.text(cx + w / 2 - 0.032, cy + h / 2 - 0.03, 0.024, 1, false, Z_TEXT), Component.empty(), 400);
        return v;
    }

    private CellView spawnCell(double cx, double cy) {
        Painter pt = menu.painter();
        Group g = pt.group(cx, cy);
        CellView v = new CellView(g, cx, cy);
        v.bg = pt.rect(g, Part.rect(cx, cy, CELL_W, CELL_H, Z_BOX_FILL, DARK));
        v.bar = pt.rect(g, Part.rect(cx, cy - CELL_H / 2 + 0.006, CELL_W, 0.012, Z_BORDER, DARK));
        v.icon = pt.item(g, Part.item(cx, cy + 0.006, 0.058, Z_ICON), Icons.of(org.bukkit.Material.CHEST));
        return v;
    }

    private void popIn(Group g, int at) {
        menu.later(at, () -> {
            g.scale = 1.12;
            g.apply(menu.frame(), 3);
        });
        menu.later(at + 3, () -> {
            g.scale = 1.0;
            g.apply(menu.frame(), 3);
        });
    }

    void shrink(int duration) {
        for (CardView v : cards) {
            v.group.scale = 0.001;
            v.group.apply(menu.frame(), duration);
        }
        for (CellView c : cells) {
            c.group.scale = 0.001;
            c.group.apply(menu.frame(), duration);
        }
        texts.scale = 0.001;
        texts.apply(menu.frame(), duration);
    }

    void remove() {
        for (CardView v : cards) {
            menu.painter().forget(v.group);
        }
        for (CellView c : cells) {
            menu.painter().forget(c.group);
        }
        if (texts != null) {
            menu.painter().forget(texts);
        }
        cards.clear();
        cells.clear();
    }

    // ------------------------------------------------------------------ state

    private int border(CardView v) {
        if (v.hover) {
            return 0xFFFFFFFF;
        }
        ChestType sel = menu.chests.selected();
        if (sel != null && sel.id.equals(v.chest.id)) {
            return theme().gold();
        }
        return Palette.mix(v.chest.color, theme().panelBorder(), 0.35);
    }

    /** Prices (red when too expensive), owned counts and the selection frame. */
    void refresh() {
        PlayerData d = menu.data();
        Currency cur = menu.plugin().currency();
        for (CardView v : cards) {
            ChestType c = v.chest;
            Component price = c.price <= 0 ? menu.lang().plain("info-free")
                    : Text.mm((menu.plugin().chests().affordable(menu.player(), c) ? "<yellow>" : "<red>") + Text.escape(menu.plugin().money(menu.lang(), c.price)));
            Painter.setText(v.price, price);
            int owned = d.chests(c.id);
            Painter.setText(v.badge, owned > 0 ? menu.lang().plain("chests-owned-badge", "count", String.valueOf(owned)) : Component.empty());
            int b = border(v);
            for (Part p : v.border) {
                p.recolor(b, 3);
            }
        }
    }

    /** Puts the rewards of a chest into the contents grid (common to rare). */
    void showContents(ChestType c) {
        if (c == null || (shown != null && shown.id.equals(c.id) && shown == c)) {
            return;
        }
        shown = c;
        shownRewards = c.sortedRewards();
        Painter.setText(label, menu.lang().plain("chests-contents", "chest", c.name(menu.lang())));
        for (int i = 0; i < cells.size(); i++) {
            CellView v = cells.get(i);
            ChestReward r = i < shownRewards.size() ? shownRewards.get(i) : null;
            if (r == null) {
                if (v.visible) {
                    v.visible = false;
                    v.group.scale = 0.001;
                    v.group.apply(menu.frame(), 2);
                }
                continue;
            }
            v.bg.recolor(Palette.mix(r.rarity.color(), DARK, 0.74), 3);
            v.bar.recolor(r.rarity.color(), 3);
            if (v.icon.entity instanceof ItemDisplay id && id.isValid()) {
                id.setItemStack(r.icon());
                v.icon.material = r.icon().getType();
            }
            if (!v.visible) {
                v.visible = true;
                v.group.scale = 1.0;
                v.group.apply(menu.frame(), 3);
            }
        }
    }

    // ------------------------------------------------------------------ hover & hit

    /** "chest:&lt;id&gt;", "reward:&lt;n&gt;" or null. */
    String hit(double u, double v) {
        for (CardView c : cards) {
            if (Math.abs(u - c.cx) <= c.w / 2 + 0.006 && Math.abs(v - c.cy) <= c.h / 2 + 0.006) {
                return "chest:" + c.chest.id;
            }
        }
        for (int i = 0; i < cells.size() && i < shownRewards.size(); i++) {
            CellView c = cells.get(i);
            if (Math.abs(u - c.cx) <= CELL_W / 2 && Math.abs(v - c.cy) <= CELL_H / 2) {
                return "reward:" + i;
            }
        }
        return null;
    }

    void setHover(String target, boolean on) {
        if (target.startsWith("chest:")) {
            String id = target.substring(6);
            for (CardView c : cards) {
                if (c.chest.id.equals(id)) {
                    c.hover = on;
                    c.group.scale = on ? 1.06 : 1.0;
                    c.group.apply(menu.frame(), 3);
                    int b = border(c);
                    for (Part p : c.border) {
                        p.recolor(b, 3);
                    }
                }
            }
        } else if (target.startsWith("reward:")) {
            CellView c = cell(target);
            if (c != null) {
                c.group.scale = on ? 1.15 : 1.0;
                c.group.apply(menu.frame(), 3);
            }
        }
    }

    private CellView cell(String target) {
        try {
            int i = Integer.parseInt(target.substring(7));
            return i >= 0 && i < cells.size() && i < shownRewards.size() ? cells.get(i) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    ChestType shown() {
        return shown;
    }

    /** The reward behind "reward:&lt;n&gt;", or null. */
    ChestReward reward(String target) {
        try {
            int i = Integer.parseInt(target.substring(7));
            return i >= 0 && i < shownRewards.size() ? shownRewards.get(i) : null;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    double[] position(String target) {
        if (target.startsWith("chest:")) {
            for (CardView c : cards) {
                if (c.chest.id.equalsIgnoreCase(target.substring(6))) {
                    return new double[]{c.cx, c.cy};
                }
            }
            return null;
        }
        CellView c = target.startsWith("reward:") ? cell(target) : null;
        return c == null ? null : new double[]{c.cx, c.cy};
    }
}
