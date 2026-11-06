package com.upgradetree.menu;

import com.upgradetree.config.Settings;
import com.upgradetree.data.PlayerData;
import com.upgradetree.service.UpgradeService;
import com.upgradetree.service.UpgradeService.State;
import com.upgradetree.tree.HexGrid;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.tree.UpgradeTree;
import com.upgradetree.util.Icons;
import com.upgradetree.util.Palette;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The hexagon tree of one tab: layout, reveal, colours, hover and hit testing. */
final class NodeLayer {

    private static final double HOVER_SCALE = 1.1;

    private final DisplayMenu menu;
    final UpgradeTree tree;
    final Map<String, NodeView> views = new LinkedHashMap<>();
    private final Map<String, double[]> pos = new HashMap<>();
    private final List<LineView> lines = new ArrayList<>();
    private final Set<String> highlighted = new HashSet<>();
    private String highlightTarget;
    final double step;
    final double radius;

    static final class NodeView {
        final UpgradeNode node;
        final double cx;
        final double cy;
        final Group group;
        List<Part> halo = List.of();
        List<Part> border = List.of();
        List<Part> fill = List.of();
        Part shine;
        Part icon;
        Part badge;
        State state;
        int level = -1;
        boolean hover;
        boolean popping;

        NodeView(UpgradeNode node, double cx, double cy, Group group) {
            this.node = node;
            this.cx = cx;
            this.cy = cy;
            this.group = group;
        }
    }

    private static final class LineView {
        final UpgradeNode from;
        final UpgradeNode to;
        final Group group;
        Part part;

        LineView(UpgradeNode from, UpgradeNode to, Group group) {
            this.from = from;
            this.to = to;
            this.group = group;
        }
    }

    NodeLayer(DisplayMenu menu, UpgradeTree tree) {
        this.menu = menu;
        this.tree = tree;
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (UpgradeNode n : tree.nodes()) {
            minX = Math.min(minX, n.x);
            maxX = Math.max(maxX, n.x);
            minY = Math.min(minY, n.y);
            maxY = Math.max(maxY, n.y);
        }
        if (tree.nodes().isEmpty()) {
            minX = maxX = minY = maxY = 0;
        }
        double margin = 0.02;
        double cw = Layout.CANVAS_X1 - Layout.CANVAS_X0 - 2 * margin;
        double ch = Layout.CANVAS_Y1 - Layout.CANVAS_Y0 - 2 * margin;
        double byW = cw / (maxX - minX + 1.0);
        double byH = ch / ((maxY - minY) * HexGrid.ROW_STEP + 2 * HexGrid.RADIUS);
        double s = Math.max(Layout.MIN_STEP, Math.min(Layout.MAX_STEP, Math.min(byW, byH)));
        this.step = s;
        this.radius = HexGrid.RADIUS * s * 0.94;
        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double cy0 = (Layout.CANVAS_Y0 + Layout.CANVAS_Y1) / 2.0;
        for (UpgradeNode n : tree.nodes()) {
            pos.put(n.id, new double[]{Layout.CANVAS_CX + (n.x - midX) * s, cy0 - (n.y - midY) * HexGrid.ROW_STEP * s});
        }
    }

    private PlayerData data() {
        return menu.data();
    }

    private Settings.Theme theme() {
        return menu.settings().theme;
    }

    // ------------------------------------------------------------------ building & reveal

    /** Spawns every visible node. {@code pop} = start tiny for the opening animation. */
    void build(boolean pop) {
        PlayerData d = data();
        for (UpgradeNode n : tree.nodes()) {
            if ((menu.admin() || menu.plugin().service().visible(d, n))) {
                spawnNode(n, pop);
            }
        }
        for (UpgradeNode[] e : tree.edges()) {
            if (views.containsKey(e[0].id) && views.containsKey(e[1].id)) {
                spawnLine(e[0], e[1], pop ? 0 : 1);
            }
        }
        paintAll(0);
        if (pop) {
            for (NodeView v : views.values()) {
                int at = 4 + Math.min(tree.depth(v.node), 12) * 2;
                popIn(v, at);
            }
            for (LineView l : lines) {
                growLine(l, 5 + Math.min(tree.depth(l.from), 12) * 2);
            }
        }
    }

    /** Spawns nodes that just became visible, growing out of {@code origin}. Returns how many appeared. */
    int reveal(UpgradeNode origin) {
        PlayerData d = data();
        List<NodeView> fresh = new ArrayList<>();
        for (UpgradeNode n : tree.nodes()) {
            if (!views.containsKey(n.id) && (menu.admin() || menu.plugin().service().visible(d, n))) {
                fresh.add(spawnNode(n, true));
            }
        }
        if (fresh.isEmpty()) {
            return 0;
        }
        for (UpgradeNode[] e : tree.edges()) {
            if (views.containsKey(e[0].id) && views.containsKey(e[1].id) && !hasLine(e[0], e[1])) {
                LineView l = spawnLine(e[0], e[1], 0);
                growLine(l, 3);
            }
        }
        paintAll(0);
        double[] o = origin == null ? null : pos.get(origin.id);
        for (NodeView v : fresh) {
            int delay = 6;
            if (o != null) {
                delay += (int) Math.round(Math.hypot(v.cx - o[0], v.cy - o[1]) / step * 3);
            }
            popIn(v, delay);
            int at = delay;
            menu.later(at + 1, () -> sparkle(v));
        }
        return fresh.size();
    }

    private boolean hasLine(UpgradeNode a, UpgradeNode b) {
        for (LineView l : lines) {
            if (l.from == a && l.to == b) {
                return true;
            }
        }
        return false;
    }

    private NodeView spawnNode(UpgradeNode n, boolean tiny) {
        double[] p = pos.get(n.id);
        Painter pt = menu.painter();
        Group g = pt.group(p[0], p[1]);
        g.scale = tiny && menu.animate() ? 0.001 : 1.0;
        NodeView v = new NodeView(n, p[0], p[1], g);
        double r = radius;
        double inner = HexGrid.inset(r, r * 0.13);
        v.border = pt.hex(g, v.cx, v.cy, r, Layout.Z_BORDER, theme().locked());
        v.fill = pt.hex(g, v.cx, v.cy, inner, Layout.Z_FILL, theme().locked());
        v.shine = pt.rect(g, Part.rect(v.cx, v.cy + inner * 0.47, Math.sqrt(3) * inner * 0.6, inner * 0.2, Layout.Z_SHINE,
                theme().locked()));
        boolean badge = n.maxLevel > 1;
        double iconY = badge ? v.cy + r * 0.08 : v.cy;
        v.icon = pt.item(g, Part.item(v.cx, iconY, Math.sqrt(3) * r * 0.56, Layout.Z_ICON), Icons.of(n));
        if (badge) {
            v.badge = pt.text(g, Part.text(v.cx, v.cy - r * 0.6, step * 0.16, 1, false, Layout.Z_TEXT), Component.empty(), 200);
        }
        views.put(n.id, v);
        return v;
    }

    private LineView spawnLine(UpgradeNode a, UpgradeNode b, double progress) {
        double[] pa = pos.get(a.id);
        double[] pb = pos.get(b.id);
        LineView l = new LineView(a, b, menu.painter().group(0, 0));
        l.part = menu.painter().rect(l.group, Part.line(pa[0], pa[1], pb[0], pb[1], Math.max(0.006, step * 0.075),
                Layout.Z_LINE, theme().lineLocked(), progress));
        lines.add(l);
        return l;
    }

    private void popIn(NodeView v, int at) {
        if (!menu.animate()) {
            return;
        }
        v.popping = true;
        menu.later(at, () -> {
            v.group.scale = 1.18;
            v.group.apply(menu.frame(), 3);
        });
        menu.later(at + 3, () -> {
            v.popping = false;
            v.group.scale = v.hover ? HOVER_SCALE : 1.0;
            v.group.apply(menu.frame(), 3);
        });
    }

    private void growLine(LineView l, int at) {
        if (!menu.animate()) {
            l.part.progress(1);
            l.group.apply(menu.frame(), l.part, 0);
            return;
        }
        menu.later(at, () -> {
            l.part.progress(1);
            l.group.apply(menu.frame(), l.part, 4);
        });
    }

    /** Shrinks everything for the closing animation. */
    void shrink(int duration) {
        for (NodeView v : views.values()) {
            v.group.scale = 0.001;
            v.group.apply(menu.frame(), duration);
        }
        for (LineView l : lines) {
            l.part.progress(0);
            l.group.apply(menu.frame(), l.part, duration);
        }
    }

    void remove() {
        for (NodeView v : views.values()) {
            menu.painter().forget(v.group);
        }
        for (LineView l : lines) {
            menu.painter().forget(l.group);
        }
        views.clear();
        lines.clear();
    }

    // ------------------------------------------------------------------ painting

    private int fill(UpgradeNode n, State st) {
        Settings.Theme t = theme();
        return switch (st) {
            case LOCKED -> t.locked();
            case BLOCKED -> Palette.mix(t.locked(), t.deny(), 0.3);
            case AVAILABLE -> Palette.mix(n.color, t.background(), 0.6);
            default -> n.color;
        };
    }

    private int border(UpgradeNode n, State st) {
        Settings.Theme t = theme();
        return switch (st) {
            case LOCKED -> Palette.lighten(t.locked(), 0.16);
            case BLOCKED -> Palette.mix(t.locked(), t.deny(), 0.55);
            case AVAILABLE -> Palette.mix(n.color, 0xFFFFFFFF, 0.15);
            case OWNED -> Palette.lighten(n.color, 0.5);
            case MAXED -> t.gold();
        };
    }

    int fillColor(UpgradeNode n, State st) {
        return fill(n, st);
    }

    int borderColor(UpgradeNode n, State st) {
        return border(n, st);
    }

    void paintAll(int duration) {
        PlayerData d = data();
        for (NodeView v : views.values()) {
            paint(v, d, duration);
        }
        for (LineView l : lines) {
            boolean parent = d.level(l.from.id) > 0;
            boolean child = d.level(l.to.id) > 0;
            int c = parent && child ? Palette.lighten(l.to.color, 0.25) : (parent ? theme().line() : theme().lineLocked());
            if (!parent && highlighted.contains(l.from.id) && l.to.id.equals(highlightTarget)) {
                c = theme().deny();
            }
            l.part.recolor(c, duration);
        }
    }

    private void paint(NodeView v, PlayerData d, int duration) {
        UpgradeNode n = v.node;
        State st = menu.plugin().service().state(d, menu.subject(), n);
        int lvl = Math.min(d.level(n.id), n.maxLevel);
        int fill = fill(n, st);
        int border = v.hover ? 0xFFFFFFFF : (highlighted.contains(n.id) ? theme().deny() : border(n, st));
        for (Part p : v.fill) {
            p.recolor(fill, duration);
        }
        for (Part p : v.border) {
            p.recolor(border, duration);
        }
        v.shine.recolor(Palette.lighten(fill, st == State.LOCKED ? 0.06 : 0.22), duration);
        boolean glow = (st == State.OWNED || st == State.MAXED) && menu.settings().highQuality;
        if (glow && v.halo.isEmpty()) {
            v.halo = menu.painter().hex(v.group, v.cx, v.cy, radius * 1.17, Layout.Z_HALO, theme().background());
            v.group.apply(menu.frame(), 0);
        }
        int haloColor = st == State.MAXED ? Palette.mix(theme().gold(), theme().background(), 0.62)
                : Palette.mix(n.color, theme().background(), 0.55);
        for (Part p : v.halo) {
            p.recolor(glow ? haloColor : theme().background(), duration);
        }
        if (v.state != st || v.level != lvl) {
            v.state = st;
            v.level = lvl;
            if (v.badge != null) {
                Component badge = st == State.MAXED ? menu.lang().plain("badge-max")
                        : Text.mm((st == State.LOCKED ? "<gray>" : "<white>") + "<b>" + lvl + "/" + n.maxLevel);
                Painter.setText(v.badge, badge);
            }
            double iconSize = Math.sqrt(3) * radius * (st == State.LOCKED || st == State.BLOCKED ? 0.46 : 0.56);
            if (Math.abs(v.icon.size - iconSize) > 1e-9) {
                v.icon.size = iconSize;
                if (!v.popping) {
                    v.group.apply(menu.frame(), v.icon, duration);
                }
            }
        }
    }

    /** Border pulse for nodes the player can afford right now. */
    void breathe(boolean up, double balance, boolean currencyOk) {
        PlayerData d = data();
        UpgradeService svc = menu.plugin().service();
        for (NodeView v : views.values()) {
            if (v.hover || v.state == null || highlighted.contains(v.node.id)) {
                continue;
            }
            if (v.state != State.AVAILABLE && v.state != State.OWNED) {
                continue;
            }
            double cost = svc.cost(d, v.node);
            boolean canBuy = cost <= 0 || (currencyOk && balance + 1e-9 >= cost);
            int base = border(v.node, v.state);
            int c = canBuy && up ? Palette.lighten(base, 0.55) : base;
            for (Part p : v.border) {
                p.recolor(c, 20);
            }
        }
    }

    // ------------------------------------------------------------------ hover & hit

    String hit(double u, double v) {
        NodeView best = null;
        double bestD = Double.MAX_VALUE;
        for (NodeView nv : views.values()) {
            double du = u - nv.cx;
            double dv = v - nv.cy;
            if (HexGrid.contains(du, dv, radius * 1.03)) {
                double dd = du * du + dv * dv;
                if (dd < bestD) {
                    bestD = dd;
                    best = nv;
                }
            }
        }
        return best == null ? null : best.node.id;
    }

    void setHover(String id, boolean on) {
        NodeView v = views.get(id);
        if (v == null) {
            return;
        }
        v.hover = on;
        if (!v.popping) {
            v.group.scale = on ? HOVER_SCALE : 1.0;
            v.group.apply(menu.frame(), 3);
        }
        highlight(on && (v.state == State.LOCKED) ? v.node : null);
        paint(v, data(), 3);
        if (menu.settings().glowOnHover && v.icon.entity != null && v.icon.entity.isValid()) {
            v.icon.entity.setGlowColorOverride(Color.WHITE);
            v.icon.entity.setGlowing(on);
        }
    }

    /** Marks the missing requirements of a locked node in red (null clears). */
    private void highlight(UpgradeNode locked) {
        Set<String> next = new HashSet<>();
        if (locked != null) {
            PlayerData d = data();
            for (String p : locked.parents) {
                if (views.containsKey(p) && d.level(p) <= 0) {
                    next.add(p);
                }
            }
        }
        String target = locked == null ? null : locked.id;
        if (next.equals(highlighted) && java.util.Objects.equals(target, highlightTarget)) {
            return;
        }
        highlighted.clear();
        highlighted.addAll(next);
        highlightTarget = target;
        paintAll(4);
    }

    // ------------------------------------------------------------------ effects

    void pop(String id) {
        NodeView v = views.get(id);
        if (v == null) {
            return;
        }
        v.popping = true;
        v.group.scale = 1.3;
        v.group.apply(menu.frame(), 2);
        menu.later(2, () -> {
            v.popping = false;
            v.group.scale = v.hover ? HOVER_SCALE : 1.0;
            v.group.apply(menu.frame(), 4);
        });
    }

    void shake(String id) {
        NodeView v = views.get(id);
        if (v != null) {
            Sparks.shake(menu, v.group);
        }
    }

    void burst(String id, boolean max) {
        NodeView v = views.get(id);
        if (v != null && menu.settings().particles) {
            Sparks.burst(menu, v.cx, v.cy, radius, v.node.color, max);
        }
    }

    private void sparkle(NodeView v) {
        if (menu.settings().particles) {
            Sparks.ring(menu, v.cx, v.cy, radius, v.node.color);
        }
    }

    // ------------------------------------------------------------------ queries

    int visibleCount() {
        return views.size();
    }

    double[] position(String id) {
        return pos.get(id);
    }
}
