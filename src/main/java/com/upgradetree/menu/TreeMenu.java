package com.upgradetree.menu;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Settings;
import com.upgradetree.currency.Currency;
import com.upgradetree.data.PlayerData;
import com.upgradetree.service.UpgradeService;
import com.upgradetree.service.UpgradeService.Result;
import com.upgradetree.service.UpgradeService.State;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.tree.UpgradeTree;
import com.upgradetree.util.Palette;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * The Roblox-style upgrade tree, drawn with display entities that only its viewer can see.
 * The panel floats in front of the player; the crosshair works as the mouse cursor.
 */
public final class TreeMenu {

    // ------------------------------------------------------------------ layout (units)
    private static final double PANEL_CX = 0.0;
    private static final double PANEL_CY = 0.02;
    private static final double PANEL_W = 1.90;
    private static final double PANEL_H = 0.88;
    private static final double PANEL_STROKE = 0.014;
    private static final double[][] PANEL_EXTENT = {{-0.97, 0.97}, {-0.44, 0.48}};

    private static final double HEADER_CY = 0.405;
    private static final double HEADER_W = 1.87;
    private static final double HEADER_H = 0.076;

    private static final double CANVAS_X0 = -0.93;
    private static final double CANVAS_X1 = 0.35;
    private static final double AREA_Y0 = -0.35;
    private static final double AREA_Y1 = 0.35;
    private static final double SIDE_X0 = 0.37;
    private static final double SIDE_X1 = 0.93;
    private static final double SIDE_CX = (SIDE_X0 + SIDE_X1) / 2.0;

    private static final double TITLE_X = (CANVAS_X0 + CANVAS_X1) / 2.0;
    private static final double PILL_X = 0.60;
    private static final double PILL_W = 0.40;
    private static final double PILL_H = 0.056;
    private static final double CLOSE_X = 0.885;
    private static final double CLOSE_SIZE = 0.056;
    private static final double FOOTER_Y = -0.387;

    private static final double INFO_ICON_Y = 0.255;
    private static final double INFO_ICON_BOX = 0.12;
    private static final double INFO_NAME_Y = 0.158;
    private static final double INFO_LEVEL_Y = 0.113;
    private static final double INFO_BAR_Y = 0.083;
    private static final double INFO_BAR_W = 0.44;
    private static final double INFO_BAR_H = 0.016;
    private static final double INFO_DESC_TOP = 0.055;
    private static final double INFO_DESC_LH = 0.027;
    private static final int INFO_DESC_MAX_LINES = 4;
    private static final int INFO_DESC_WRAP_PX = 168;
    private static final double INFO_BONUS_Y = -0.09;
    private static final double INFO_COST_Y = -0.135;
    private static final double BTN_Y = -0.262;
    private static final double BTN_W = 0.44;
    private static final double BTN_H = 0.07;

    private static final double NODE_MAX = 0.095;
    private static final double NODE_STROKE = 0.011;
    private static final double NODE_DROP = 0.007;
    private static final double BADGE_W = 0.074;
    private static final double BADGE_H = 0.030;
    private static final double BADGE_LH = 0.024;
    private static final double LINE_W = 0.012;
    private static final double LINE_STROKE_W = 0.026;
    private static final double HOVER_SCALE = 1.12;

    // ------------------------------------------------------------------ layers (fraction of distance)
    private static final double Z0 = 0.000;
    private static final double Z1 = 0.002;
    private static final double Z2 = 0.004;
    private static final double Z3 = 0.006;
    private static final double Z4 = 0.008;
    private static final double Z5 = 0.010;
    private static final double Z6 = 0.012;
    private static final double Z7 = 0.014;
    private static final double Z8 = 0.016;
    private static final double Z9 = 0.020;
    private static final double Z10 = 0.060;

    private static final int CAN_BUY_STROKE = 0xFF7CF29A;

    // ------------------------------------------------------------------ state
    private final UpgradeTreePlugin plugin;
    private final MenuManager manager;
    private final Player player;
    private final Settings settings;
    private final UpgradeService service;
    private final Frame frame;
    private final boolean animate;

    private final List<Display> entities = new ArrayList<>();
    private final Group chrome = new Group(PANEL_CX, PANEL_CY);
    private final Group lineGroup = new Group(0, 0);
    private final Map<String, NodeView> nodes = new LinkedHashMap<>();
    private final List<LineView> lines = new ArrayList<>();
    private final List<Timed> timeline = new ArrayList<>();

    private Group closeGroup;
    private final List<Part> closeFill = new ArrayList<>();
    private Group buyGroup;
    private final List<Part> buyFill = new ArrayList<>();
    private Part buyLabel;
    private boolean buyVisible;
    private Part currencyLabel;
    private Component currencyCache;

    private final List<Part> infoBoxFill = new ArrayList<>();
    private Part infoIcon;
    private Part infoName;
    private Part infoLevel;
    private Part infoDesc;
    private Part infoBonus;
    private Part infoCost;
    private Part barFill;
    private Material infoIconType;
    private double barFrac = -1;

    private UpgradeNode selected;
    private String hovered;
    private int tick;
    private int lastClick = -100;
    private int ignoreClicksUntil;
    private boolean closing;
    private boolean done;
    private boolean handHidden;
    private boolean rehide;
    private boolean breathUp;

    private record Timed(int at, Runnable task) {
    }

    private static final class NodeView {
        final UpgradeNode node;
        final double cx;
        final double cy;
        final double size;
        final Group group;
        final List<Part> stroke = new ArrayList<>();
        final List<Part> fill = new ArrayList<>();
        Part shine;
        Part badge;
        Part badgeText;
        Part icon;
        State state;
        int level = -1;
        boolean hover;
        boolean popping;

        NodeView(UpgradeNode node, double cx, double cy, double size) {
            this.node = node;
            this.cx = cx;
            this.cy = cy;
            this.size = size;
            this.group = new Group(cx, cy);
        }
    }

    private static final class LineView {
        final NodeView from;
        final NodeView to;
        Part stroke;
        Part fill;

        LineView(NodeView from, NodeView to) {
            this.from = from;
            this.to = to;
        }
    }

    TreeMenu(UpgradeTreePlugin plugin, MenuManager manager, Player player) {
        this.plugin = plugin;
        this.manager = manager;
        this.player = player;
        this.settings = plugin.settings();
        this.service = plugin.service();
        this.animate = settings.animations;
        this.frame = new Frame(player, settings.menuDistance, settings.menuScale, PANEL_EXTENT);
    }

    Player player() {
        return player;
    }

    boolean isClosing() {
        return closing;
    }

    boolean isDone() {
        return done;
    }

    // ------------------------------------------------------------------ open / close

    void open() {
        Freeze.apply(plugin, player);
        if (settings.hideHand) {
            hideHand();
        }
        try {
            build();
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Menü oluşturulamadı", ex);
            closing = true;
            release();
            removeAll();
            throw ex;
        }
        sound("minecraft:ui.toast.in", 0.8f, 1.3f);
        sound("minecraft:block.amethyst_block.chime", 0.6f, 1.5f);
        player.sendActionBar(settings.plain("opened-hint"));
    }

    void close(boolean instant) {
        if (closing) {
            return;
        }
        closing = true;
        timeline.clear();
        release();
        if (instant || !animate) {
            removeAll();
            return;
        }
        sound("minecraft:ui.toast.out", 0.7f, 1.3f);
        chrome.scale = 0.88;
        chrome.apply(frame, 3);
        for (NodeView nv : nodes.values()) {
            nv.group.scale = 0.001;
            nv.group.apply(frame, 3);
        }
        closeGroup.scale = 0.001;
        closeGroup.apply(frame, 3);
        buyGroup.scale = 0.001;
        buyGroup.apply(frame, 3);
        for (LineView lv : lines) {
            lv.stroke.progress(0);
            lv.fill.progress(0);
            lineGroup.apply(frame, lv.stroke, 3);
            lineGroup.apply(frame, lv.fill, 3);
        }
        later(4, this::removeAll);
    }

    private void release() {
        if (!player.isOnline()) {
            return;
        }
        Freeze.release(plugin, player);
        if (handHidden) {
            handHidden = false;
            player.updateInventory();
        }
    }

    /** Removes everything immediately (plugin disable). */
    void forceRemove() {
        timeline.clear();
        if (!closing) {
            closing = true;
            release();
        }
        removeAll();
    }

    /** The server re-sent the inventory (e.g. after a cancelled drop): hide the hand again. */
    void rehideHand() {
        rehide = true;
    }

    private void removeAll() {
        for (Display e : entities) {
            if (e.isValid()) {
                e.remove();
            }
        }
        entities.clear();
        done = true;
    }

    /** Shows an invisible item in the player's hands (client side only) so no arm covers the UI. */
    private void hideHand() {
        ItemStack fake = new ItemStack(Material.PAPER);
        fake.editMeta(m -> {
            m.setItemModel(NamespacedKey.minecraft("air"));
            m.customName(Component.empty());
            m.setHideTooltip(true);
        });
        Map<EquipmentSlot, ItemStack> map = new EnumMap<>(EquipmentSlot.class);
        map.put(EquipmentSlot.HAND, fake);
        if (!player.getInventory().getItemInOffHand().getType().isAir()) {
            map.put(EquipmentSlot.OFF_HAND, fake.clone());
        }
        player.sendEquipmentChange(player, map);
        handHidden = true;
    }

    // ------------------------------------------------------------------ building

    private void build() {
        UpgradeTree tree = plugin.tree();
        double chromeInit = animate ? 0.86 : 1.0;
        double popInit = animate ? 0.001 : 1.0;
        chrome.scale = chromeInit;

        // panel background
        roundRect(chrome, PANEL_CX, PANEL_CY, PANEL_W + 2 * PANEL_STROKE, PANEL_H + 2 * PANEL_STROKE, 0.046, Z0, Palette.PANEL_STROKE, 3);
        roundRect(chrome, PANEL_CX, PANEL_CY, PANEL_W, PANEL_H, 0.038, Z1, Palette.PANEL, 3);
        roundRect(chrome, 0, HEADER_CY, HEADER_W, HEADER_H, 0.026, Z2, Palette.HEADER, 3);

        double areaCy = (AREA_Y0 + AREA_Y1) / 2.0;
        double areaH = AREA_Y1 - AREA_Y0;
        double canvasCx = (CANVAS_X0 + CANVAS_X1) / 2.0;
        roundRect(chrome, canvasCx, areaCy, CANVAS_X1 - CANVAS_X0 + 0.016, areaH + 0.016, 0.032, Z2, Palette.STROKE, 3);
        roundRect(chrome, canvasCx, areaCy, CANVAS_X1 - CANVAS_X0, areaH, 0.026, Z3, Palette.CANVAS, 3);
        roundRect(chrome, SIDE_CX, areaCy, SIDE_X1 - SIDE_X0 + 0.016, areaH + 0.016, 0.032, Z2, Palette.STROKE, 3);
        roundRect(chrome, SIDE_CX, areaCy, SIDE_X1 - SIDE_X0, areaH, 0.026, Z3, Palette.SIDEBAR, 3);
        roundRect(chrome, PILL_X, HEADER_CY, PILL_W, PILL_H, 0.02, Z3, Palette.PILL, 3);

        // close button
        closeGroup = new Group(CLOSE_X, HEADER_CY);
        closeGroup.scale = popInit;
        roundRect(closeGroup, CLOSE_X, HEADER_CY - 0.003, CLOSE_SIZE + 0.016, CLOSE_SIZE + 0.022, 0.016, Z3, Palette.STROKE, 3);
        closeFill.addAll(roundRect(closeGroup, CLOSE_X, HEADER_CY, CLOSE_SIZE, CLOSE_SIZE, 0.012, Z4, Palette.DENY, 3));

        // tree layout and connection lines
        layoutNodes(tree.nodes());
        for (UpgradeNode[] e : tree.edges()) {
            NodeView a = nodes.get(e[0].id);
            NodeView b = nodes.get(e[1].id);
            if (a != null && b != null) {
                lines.add(new LineView(a, b));
            }
        }
        double lineInit = animate ? 0.0 : 1.0;
        for (LineView lv : lines) {
            lv.stroke = spawnRect(lineGroup, Part.line(lv.from.cx, lv.from.cy, lv.to.cx, lv.to.cy, LINE_STROKE_W, Z4, Palette.STROKE, lineInit));
        }
        for (LineView lv : lines) {
            lv.fill = spawnRect(lineGroup, Part.line(lv.from.cx, lv.from.cy, lv.to.cx, lv.to.cy, LINE_W, Z5, Palette.LINE_LOCKED, lineInit));
        }

        // nodes (back to front: strokes, fills, shine and badges)
        for (NodeView nv : nodes.values()) {
            nv.group.scale = popInit;
            nv.stroke.addAll(roundRect(nv.group, nv.cx, nv.cy - NODE_DROP / 2.0, nv.size + 2 * NODE_STROKE,
                    nv.size + 2 * NODE_STROKE + NODE_DROP, nv.size * 0.30, Z6, Palette.STROKE, 3));
        }
        for (NodeView nv : nodes.values()) {
            nv.fill.addAll(roundRect(nv.group, nv.cx, nv.cy, nv.size, nv.size, nv.size * 0.24, Z7, Palette.LOCKED, 3));
        }
        for (NodeView nv : nodes.values()) {
            double sh = nv.size * 0.2;
            nv.shine = spawnRect(nv.group, Part.rect(nv.cx, nv.cy + nv.size / 2.0 - nv.size * 0.09 - sh / 2.0,
                    nv.size * 0.6, sh, Z8, Palette.lighten(Palette.LOCKED, 0.08)));
            nv.badge = spawnRect(nv.group, Part.rect(nv.cx, nv.cy - nv.size / 2.0 - 0.003, BADGE_W, BADGE_H, Z8, Palette.STROKE));
        }

        // info panel boxes
        roundRect(chrome, SIDE_CX, INFO_ICON_Y - 0.004, INFO_ICON_BOX + 0.022, INFO_ICON_BOX + 0.03, INFO_ICON_BOX * 0.3, Z4, Palette.STROKE, 3);
        infoBoxFill.addAll(roundRect(chrome, SIDE_CX, INFO_ICON_Y, INFO_ICON_BOX, INFO_ICON_BOX, INFO_ICON_BOX * 0.24, Z5, Palette.AVAILABLE, 3));
        roundRect(chrome, SIDE_CX, INFO_BAR_Y, INFO_BAR_W, INFO_BAR_H, INFO_BAR_H / 2.0, Z4, Palette.BAR_BG, 2);
        barFill = spawnRect(chrome, Part.rect(SIDE_CX - (INFO_BAR_W - 0.006) / 2.0, INFO_BAR_Y, 0, INFO_BAR_H - 0.007, Z5, Palette.GOLD));
        barFrac = 0;

        buyGroup = new Group(SIDE_CX, BTN_Y);
        buyGroup.scale = 0.001;
        roundRect(buyGroup, SIDE_CX, BTN_Y - 0.004, BTN_W + 0.018, BTN_H + 0.026, 0.03, Z4, Palette.STROKE, 3);
        buyFill.addAll(roundRect(buyGroup, SIDE_CX, BTN_Y, BTN_W, BTN_H, 0.026, Z5, Palette.BUY, 3));
        buyVisible = false;

        // texts
        spawnText(chrome, Part.text(TITLE_X, HEADER_CY, 0.046, 1, false, Z9), tree.title, 2000);
        currencyCache = currencyText();
        currencyLabel = spawnText(chrome, Part.text(PILL_X, HEADER_CY, 0.03, 1, false, Z9), currencyCache, 2000);
        spawnText(closeGroup, Part.text(CLOSE_X, HEADER_CY, 0.036, 1, false, Z9), Text.mm("<white><b>X"), 200);
        spawnText(chrome, Part.text(0, FOOTER_Y, 0.024, 1, false, Z9), settings.plain("menu-footer"), 4000);
        for (NodeView nv : nodes.values()) {
            nv.badgeText = spawnText(nv.group, Part.text(nv.cx, nv.cy - nv.size / 2.0 - 0.003, BADGE_LH, 1, false, Z9),
                    Component.empty(), 200);
        }
        infoName = spawnText(chrome, Part.text(SIDE_CX, INFO_NAME_Y, 0.04, 1, false, Z9), Component.empty(), 2000);
        infoLevel = spawnText(chrome, Part.text(SIDE_CX, INFO_LEVEL_Y, 0.027, 1, false, Z9), Component.empty(), 2000);
        infoDesc = spawnText(chrome, Part.text(SIDE_CX, INFO_DESC_TOP, INFO_DESC_LH, 1, true, Z9), Component.empty(), 2000);
        infoBonus = spawnText(chrome, Part.text(SIDE_CX, INFO_BONUS_Y, 0.027, 1, false, Z9), Component.empty(), 2000);
        // wraps on the client; extra lines grow upwards into the (then empty) bonus row
        infoCost = spawnText(chrome, Part.text(SIDE_CX, INFO_COST_Y, 0.029, 1, false, Z9), Component.empty(), 172);
        buyLabel = spawnText(buyGroup, Part.text(SIDE_CX, BTN_Y, 0.034, 1, false, Z9), settings.plain("button-buy"), 2000);

        // icons last (front-most)
        for (NodeView nv : nodes.values()) {
            nv.icon = spawnItem(nv.group, Part.item(nv.cx, nv.cy, nv.size * 0.64, Z10), iconStack(nv.node.icon));
        }
        infoIconType = Material.NETHER_STAR;
        infoIcon = spawnItem(chrome, Part.item(SIDE_CX, INFO_ICON_Y, 0.08, Z10), iconStack(infoIconType));

        refreshAll(0);
        showInfo(null);
        if (animate) {
            scheduleOpenAnimation();
        }
    }

    private void layoutNodes(Collection<UpgradeNode> all) {
        if (all.isEmpty()) {
            return;
        }
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (UpgradeNode n : all) {
            minX = Math.min(minX, n.x);
            maxX = Math.max(maxX, n.x);
            minY = Math.min(minY, n.y);
            maxY = Math.max(maxY, n.y);
        }
        double ax0 = CANVAS_X0 + 0.085;
        double ax1 = CANVAS_X1 - 0.085;
        double ay0 = AREA_Y0 + 0.08;
        double ay1 = AREA_Y1 - 0.068;
        double spanX = maxX - minX;
        double spanY = maxY - minY;
        double spX = spanX > 1e-9 ? Math.min(0.34, (ax1 - ax0) / spanX) : 0;
        double spY = spanY > 1e-9 ? Math.min(0.22, (ay1 - ay0) / spanY) : 0;
        double size = NODE_MAX;
        if (spX > 0) {
            size = Math.min(size, spX * 0.62);
        }
        if (spY > 0) {
            size = Math.min(size, spY * 0.66);
        }
        size = Math.max(0.035, size);
        double midX = (ax0 + ax1) / 2.0;
        double midY = (ay0 + ay1) / 2.0;
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        for (UpgradeNode n : all) {
            double u = midX + (n.x - centerX) * spX;
            double v = midY - (n.y - centerY) * spY;
            nodes.put(n.id, new NodeView(n, u, v, size));
        }
    }

    private void scheduleOpenAnimation() {
        later(2, () -> {
            chrome.scale = 1.0;
            chrome.apply(frame, 5);
            closeGroup.scale = 1.0;
            closeGroup.apply(frame, 5);
        });
        UpgradeTree tree = plugin.tree();
        for (NodeView nv : nodes.values()) {
            int at = 4 + Math.min(tree.depth(nv.node), 12) * 2;
            nv.popping = true;
            later(at, () -> {
                nv.group.scale = 1.18;
                nv.group.apply(frame, 3);
            });
            later(at + 3, () -> {
                nv.popping = false;
                nv.group.scale = nv.hover ? HOVER_SCALE : 1.0;
                nv.group.apply(frame, 3);
            });
        }
        for (LineView lv : lines) {
            int at = 5 + Math.min(tree.depth(lv.from.node), 12) * 2;
            later(at, () -> {
                lv.stroke.progress(1);
                lv.fill.progress(1);
                lineGroup.apply(frame, lv.stroke, 4);
                lineGroup.apply(frame, lv.fill, 4);
            });
        }
    }

    // ------------------------------------------------------------------ spawning

    private <T extends Display> T spawn(Class<T> type, Group g, Part p, Consumer<T> setup) {
        Transformation t = p.build(frame, g.pivotX, g.pivotY, g.scale, g.dx, g.dy);
        T e = frame.world.spawn(frame.spawn, type, d -> {
            d.setPersistent(false);
            d.setVisibleByDefault(false);
            d.setBillboard(Display.Billboard.FIXED);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setShadowRadius(0f);
            d.setShadowStrength(0f);
            d.setInterpolationDuration(0);
            d.setTeleportDuration(0);
            d.setTransformation(t);
            setup.accept(d);
        });
        player.showEntity(plugin, e);
        entities.add(e);
        p.entity = e;
        g.add(p);
        return e;
    }

    private Part spawnRect(Group g, Part p) {
        int color = p.color;
        spawn(TextDisplay.class, g, p, d -> {
            d.text(Component.text(" "));
            d.setBackgroundColor(Color.fromARGB(color));
            d.setDefaultBackground(false);
            d.setShadowed(false);
            d.setSeeThrough(false);
            d.setLineWidth(200);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
        });
        return p;
    }

    private Part spawnText(Group g, Part p, Component text, int lineWidth) {
        spawn(TextDisplay.class, g, p, d -> {
            d.text(text);
            d.setBackgroundColor(Color.fromARGB(0));
            d.setDefaultBackground(false);
            d.setShadowed(true);
            d.setSeeThrough(false);
            d.setLineWidth(lineWidth);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
        });
        return p;
    }

    private Part spawnItem(Group g, Part p, ItemStack stack) {
        spawn(ItemDisplay.class, g, p, d -> {
            d.setItemStack(stack);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
        });
        return p;
    }

    /**
     * Rounded rectangle made of overlapping plain rectangles: one wide, one tall and
     * (with 3 steps) an inset one that fills the corner, giving a smooth pixel-rounded corner.
     */
    private List<Part> roundRect(Group g, double cx, double cy, double w, double h, double r, double z, int color, int steps) {
        List<Part> list = new ArrayList<>(3);
        r = Math.min(r, Math.min(w, h) / 2.0);
        if (r <= 1e-4 || steps <= 1) {
            list.add(spawnRect(g, Part.rect(cx, cy, w, h, z, color)));
            return list;
        }
        list.add(spawnRect(g, Part.rect(cx, cy, w, h - 2 * r, z, color)));
        list.add(spawnRect(g, Part.rect(cx, cy, w - 2 * r, h, z, color)));
        if (steps >= 3) {
            list.add(spawnRect(g, Part.rect(cx, cy, w - 0.58 * r, h - 0.58 * r, z, color)));
        }
        return list;
    }

    /** Creates icon item stacks (replaceable for offline layout previews). */
    static java.util.function.Function<Material, ItemStack> iconFactory =
            m -> new ItemStack(m != null && m.isItem() && !m.isAir() ? m : Material.PAPER);

    private static ItemStack iconStack(Material m) {
        return iconFactory.apply(m);
    }

    // ------------------------------------------------------------------ ticking

    void tick() {
        tick++;
        runTimeline();
        if (done || closing) {
            return;
        }
        if (!player.isOnline() || player.isDead() || !player.getWorld().equals(frame.world)) {
            manager.close(player, true);
            return;
        }
        if (player.getLocation().toVector().distanceSquared(frame.feet) > 0.36) {
            manager.close(player, false);
            return;
        }
        if (tick >= 3) {
            updateHover();
        }
        if (tick % 10 == 0) {
            updateCurrency();
            showInfo(selected);
        }
        if (settings.hideHand && (rehide || tick % 40 == 0)) {
            rehide = false;
            hideHand();
        }
        if (tick % 20 == 0) {
            breathe();
        }
    }

    private void later(int delay, Runnable task) {
        timeline.add(new Timed(tick + Math.max(1, delay), task));
    }

    private void runTimeline() {
        if (timeline.isEmpty()) {
            return;
        }
        List<Timed> due = null;
        for (Iterator<Timed> it = timeline.iterator(); it.hasNext(); ) {
            Timed t = it.next();
            if (t.at() <= tick) {
                if (due == null) {
                    due = new ArrayList<>();
                }
                due.add(t);
                it.remove();
            }
        }
        if (due != null) {
            for (Timed t : due) {
                try {
                    t.task().run();
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Menü animasyon hatası", ex);
                }
            }
        }
    }

    /** Pulses nodes that can be bought right now, and the shine of available nodes. */
    private void breathe() {
        breathUp = !breathUp;
        PlayerData d = plugin.data().get(player);
        Currency cur = plugin.currency();
        double balance = cur.available() ? cur.balance(player) : 0;
        for (NodeView nv : nodes.values()) {
            if (nv.hover || nv.state == null) {
                continue;
            }
            int lvl = d.level(nv.node.id);
            boolean canBuy = (nv.state == State.AVAILABLE || nv.state == State.OWNED)
                    && (nv.node.costFor(lvl) <= 0 || (cur.available() && balance + 1e-9 >= nv.node.costFor(lvl)));
            int base = nv.state == State.MAXED ? Palette.GOLD : Palette.STROKE;
            int stroke = canBuy && animate && breathUp ? CAN_BUY_STROKE : base;
            for (Part p : nv.stroke) {
                p.recolor(stroke, animate ? 20 : 0);
            }
            if (nv.state == State.AVAILABLE && animate) {
                nv.shine.recolor(Palette.lighten(Palette.AVAILABLE, breathUp ? 0.42 : 0.18), 20);
            }
        }
    }

    // ------------------------------------------------------------------ hover & click

    private String hitTest() {
        double[] uv = frame.project(player.getEyeLocation());
        if (uv == null) {
            return null;
        }
        double u = uv[0];
        double v = uv[1];
        if (Math.abs(u - CLOSE_X) <= CLOSE_SIZE * 0.7 && Math.abs(v - HEADER_CY) <= CLOSE_SIZE * 0.7) {
            return "close";
        }
        if (buyVisible && Math.abs(u - SIDE_CX) <= BTN_W / 2.0 + 0.01 && Math.abs(v - BTN_Y) <= BTN_H / 2.0 + 0.01) {
            return "buy";
        }
        NodeView best = null;
        double bestD = Double.MAX_VALUE;
        for (NodeView nv : nodes.values()) {
            double r = nv.size * 0.62;
            double du = Math.abs(u - nv.cx);
            double dv = Math.abs(v - nv.cy);
            if (du <= r && dv <= r) {
                double dd = du * du + dv * dv;
                if (dd < bestD) {
                    bestD = dd;
                    best = nv;
                }
            }
        }
        return best == null ? null : "node:" + best.node.id;
    }

    private void updateHover() {
        String target = hitTest();
        if (Objects.equals(target, hovered)) {
            return;
        }
        String old = hovered;
        hovered = target;
        if (old != null) {
            setHover(old, false);
        }
        if (target != null) {
            setHover(target, true);
            sound("minecraft:block.note_block.hat", 0.25f, 1.9f);
        }
    }

    private void setHover(String id, boolean on) {
        if (id.equals("close")) {
            closeGroup.scale = on ? 1.15 : 1.0;
            closeGroup.apply(frame, 3);
            for (Part p : closeFill) {
                p.recolor(on ? Palette.lighten(Palette.DENY, 0.2) : Palette.DENY, 3);
            }
            return;
        }
        if (id.equals("buy")) {
            if (buyVisible) {
                buyGroup.scale = on ? 1.06 : 1.0;
                buyGroup.apply(frame, 3);
            }
            return;
        }
        if (!id.startsWith("node:")) {
            return;
        }
        NodeView nv = nodes.get(id.substring(5));
        if (nv == null) {
            return;
        }
        nv.hover = on;
        if (!nv.popping) {
            nv.group.scale = on ? HOVER_SCALE : 1.0;
            nv.group.apply(frame, 3);
        }
        paintNode(nv, plugin.data().get(player), 3);
        if (settings.glowOnHover && nv.icon.entity != null && nv.icon.entity.isValid()) {
            nv.icon.entity.setGlowColorOverride(Color.WHITE);
            nv.icon.entity.setGlowing(on);
        }
        if (on) {
            selected = nv.node;
            showInfo(nv.node);
        }
    }

    /** Called for every left click (and right click with an item) while the menu is open. */
    void click() {
        if (closing || tick < 4 || tick < ignoreClicksUntil || tick - lastClick < 3) {
            return;
        }
        lastClick = tick;
        String target = hitTest();
        if (target == null) {
            return;
        }
        if (target.equals("close")) {
            sound("minecraft:ui.button.click", 0.5f, 1.4f);
            manager.close(player, false);
            return;
        }
        if (target.equals("buy")) {
            if (selected != null) {
                purchase(nodes.get(selected.id), true);
            }
            return;
        }
        NodeView nv = nodes.get(target.substring(5));
        if (nv != null) {
            selected = nv.node;
            purchase(nv, false);
        }
    }

    void ignoreClicks(int ticks) {
        ignoreClicksUntil = tick + ticks;
    }

    private void purchase(NodeView nv, boolean viaButton) {
        if (nv == null) {
            return;
        }
        UpgradeNode n = nv.node;
        PlayerData d = plugin.data().get(player);
        Currency cur = plugin.currency();
        int before = d.level(n.id);
        double cost = n.costFor(before);
        double balance = cur.available() ? cur.balance(player) : 0;
        Result r = service.purchase(player, n);
        switch (r) {
            case SUCCESS -> {
                int lvl = d.level(n.id);
                boolean max = lvl >= n.maxLevel;
                refreshAll(6);
                pop(nv);
                if (viaButton) {
                    pulse(buyGroup);
                }
                burst(nv, max);
                sound("minecraft:entity.player.levelup", 0.55f, max ? 2.0f : 1.5f);
                sound("minecraft:block.amethyst_block.chime", 1.0f, 1.2f);
                player.sendActionBar(settings.plain(max ? "purchase-max" : "purchase-success",
                        "node", n.nameRaw, "level", String.valueOf(lvl)));
            }
            case MAXED -> deny(nv, viaButton, settings.plain("purchase-maxed"));
            case LOCKED -> {
                List<String> names = new ArrayList<>();
                for (UpgradeNode m : service.missing(d, n)) {
                    names.add(m.nameRaw);
                }
                deny(nv, viaButton, settings.plain("purchase-locked", "requires", String.join("<gray>, </gray>", names)));
            }
            case NOT_ENOUGH -> deny(nv, viaButton, settings.plain("purchase-not-enough",
                    "currency", Text.escape(settings.currencyName),
                    "cost", Text.escape(cur.format(cost)),
                    "balance", Text.escape(cur.format(balance))));
            case UNAVAILABLE -> deny(nv, viaButton, settings.plain("currency-unavailable"));
        }
        showInfo(n);
        updateCurrency();
    }

    private void pop(NodeView nv) {
        nv.popping = true;
        nv.group.scale = 1.32;
        nv.group.apply(frame, 2);
        later(2, () -> {
            nv.popping = false;
            nv.group.scale = nv.hover ? HOVER_SCALE : 1.0;
            nv.group.apply(frame, 4);
        });
    }

    private void pulse(Group g) {
        double base = g.scale;
        g.scale = base * 0.92;
        g.apply(frame, 1);
        later(1, () -> {
            g.scale = base;
            g.apply(frame, 3);
        });
    }

    private void deny(NodeView nv, boolean viaButton, Component message) {
        shake(viaButton ? buyGroup : nv.group);
        sound("minecraft:block.note_block.bass", 0.8f, 0.7f);
        player.sendActionBar(message);
    }

    private void shake(Group g) {
        double[] seq = {0.012, -0.012, 0.007, -0.004, 0.0};
        g.dx = seq[0];
        g.apply(frame, 1);
        for (int i = 1; i < seq.length; i++) {
            double v = seq[i];
            boolean last = i == seq.length - 1;
            later(i, () -> {
                g.dx = v;
                g.apply(frame, last ? 2 : 1);
            });
        }
    }

    private void burst(NodeView nv, boolean max) {
        if (!settings.particles) {
            return;
        }
        float size = (float) Math.max(0.25, Math.min(1.0, 0.6 * frame.dist / 1.6));
        Particle.DustOptions main = new Particle.DustOptions(Color.fromRGB(Palette.rgb(Palette.lighten(nv.node.color, 0.15))), size);
        Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, size * 0.8f);
        int count = 14;
        double r = nv.size * 0.8;
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            Location loc = frame.point(nv.cx + Math.cos(a) * r, nv.cy + Math.sin(a) * r, 0.1);
            player.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, (i % 2 == 0) ? main : white);
        }
        if (max) {
            Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(Palette.rgb(Palette.GOLD)), size);
            for (int i = 0; i < 18; i++) {
                double a = Math.PI * 2 * i / 18 + 0.17;
                Location loc = frame.point(nv.cx + Math.cos(a) * r * 1.4, nv.cy + Math.sin(a) * r * 1.4, 0.1);
                player.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, gold);
            }
        }
    }

    private void sound(String key, float volume, float pitch) {
        if (settings.sounds && player.isOnline()) {
            player.playSound(player.getLocation(), key, SoundCategory.MASTER, volume, pitch);
        }
    }

    // ------------------------------------------------------------------ painting

    private int fillColor(UpgradeNode n, State st) {
        return switch (st) {
            case LOCKED -> Palette.LOCKED;
            case AVAILABLE -> Palette.AVAILABLE;
            default -> n.color;
        };
    }

    private void refreshAll(int duration) {
        PlayerData d = plugin.data().get(player);
        for (NodeView nv : nodes.values()) {
            paintNode(nv, d, duration);
        }
        for (LineView lv : lines) {
            boolean parent = d.level(lv.from.node.id) > 0;
            boolean child = d.level(lv.to.node.id) > 0;
            int c = parent && child ? lv.to.node.color : (parent ? Palette.LINE_AVAILABLE : Palette.LINE_LOCKED);
            lv.fill.recolor(c, duration);
        }
        updateCurrency();
    }

    private void paintNode(NodeView nv, PlayerData d, int duration) {
        UpgradeNode n = nv.node;
        State st = service.state(d, n);
        int lvl = Math.min(d.level(n.id), n.maxLevel);
        int fill = fillColor(n, st);
        int stroke = nv.hover ? Palette.WHITE : (st == State.MAXED ? Palette.GOLD : Palette.STROKE);
        for (Part p : nv.fill) {
            p.recolor(fill, duration);
        }
        for (Part p : nv.stroke) {
            p.recolor(stroke, duration);
        }
        nv.shine.recolor(Palette.lighten(fill, st == State.LOCKED ? 0.08 : 0.24), duration);
        nv.badge.recolor(st == State.MAXED ? Palette.darken(Palette.GOLD, 0.55) : Palette.STROKE, duration);
        if (nv.state != st || nv.level != lvl) {
            nv.state = st;
            nv.level = lvl;
            Component badge = st == State.MAXED
                    ? settings.plain("badge-max")
                    : Text.mm((st == State.LOCKED ? "<gray>" : "<white>") + "<b>" + lvl + "/" + n.maxLevel);
            setText(nv.badgeText, badge);
            double iconSize = nv.size * (st == State.LOCKED ? 0.52 : 0.64);
            if (Math.abs(nv.icon.size - iconSize) > 1e-9) {
                nv.icon.size = iconSize;
                if (!nv.popping) {
                    nv.group.apply(frame, nv.icon, duration);
                }
            }
        }
    }

    private Component currencyText() {
        Currency c = plugin.currency();
        String amount = c.available() ? c.formatShort(c.balance(player)) : "-";
        return settings.plain("menu-currency",
                "symbol", Text.escape(settings.currencySymbol),
                "amount", Text.escape(amount),
                "currency", Text.escape(settings.currencyName));
    }

    private void updateCurrency() {
        if (currencyLabel == null) {
            return;
        }
        Component c = currencyText();
        if (!c.equals(currencyCache)) {
            currencyCache = c;
            setText(currencyLabel, c);
        }
    }

    private static void setText(Part p, Component c) {
        if (p != null && p.entity instanceof TextDisplay td && td.isValid() && !c.equals(td.text())) {
            td.text(c);
        }
    }

    private void showInfo(UpgradeNode n) {
        if (infoName == null) {
            return;
        }
        PlayerData d = plugin.data().get(player);
        Currency cur = plugin.currency();
        if (n == null) {
            setInfoIcon(Material.NETHER_STAR);
            for (Part p : infoBoxFill) {
                p.recolor(Palette.AVAILABLE, 4);
            }
            setText(infoName, settings.plain("menu-welcome-title"));
            setText(infoLevel, settings.plain("menu-progress",
                    "owned", String.valueOf(service.ownedNodes(d)),
                    "total", String.valueOf(plugin.tree().nodes().size())));
            setBar((double) service.totalLevels(d) / Math.max(1, plugin.tree().totalMaxLevels()), Palette.GOLD);
            setDesc(String.join("\n", settings.rawList("menu-welcome-desc")));
            setText(infoBonus, Component.empty());
            setText(infoCost, Component.empty());
            setButton(null, 0);
            return;
        }
        State st = service.state(d, n);
        int lvl = Math.min(d.level(n.id), n.maxLevel);
        setInfoIcon(n.icon);
        for (Part p : infoBoxFill) {
            p.recolor(fillColor(n, st), 4);
        }
        setText(infoName, Text.mm("<b>" + n.nameRaw));
        setText(infoLevel, settings.plain("info-level", "level", String.valueOf(lvl), "max", String.valueOf(n.maxLevel)));
        setBar((double) lvl / n.maxLevel, st == State.MAXED ? Palette.GOLD : n.color);
        setDesc(String.join("\n", n.description));

        if (st != State.LOCKED && n.bonus != null && !n.bonus.isBlank()) {
            String hex = String.format("#%06X", Palette.rgb(Palette.lighten(n.color, 0.25)));
            String now = "<color:" + hex + ">" + Text.apply(n.bonus, "value", Text.num(n.bonusValue(lvl))) + "</color>";
            String next = lvl < n.maxLevel
                    ? " <dark_gray>→</dark_gray> <color:" + hex + ">" + Text.apply(n.bonus, "value", Text.num(n.bonusValue(lvl + 1))) + "</color>"
                    : "";
            setText(infoBonus, Text.mm(now + next));
        } else {
            setText(infoBonus, Component.empty());
        }

        if (st == State.MAXED) {
            setText(infoCost, settings.plain("info-completed"));
            setButton(settings.plain("button-max"), Palette.MAXED_BUTTON);
        } else if (st == State.LOCKED) {
            List<String> names = new ArrayList<>();
            for (UpgradeNode m : service.missing(d, n)) {
                names.add(m.nameRaw);
            }
            setText(infoCost, settings.plain("info-requires", "requires", String.join("<gray>, </gray>", names)));
            setButton(settings.plain("button-locked"), Palette.LOCKED_BUTTON);
        } else {
            double cost = n.costFor(lvl);
            boolean afford = cost <= 0 || (cur.available() && cur.balance(player) + 1e-9 >= cost);
            if (cost <= 0) {
                setText(infoCost, settings.plain("info-free"));
            } else {
                setText(infoCost, settings.plain(afford ? "info-cost" : "info-cost-poor", "cost", Text.escape(cur.format(cost))));
            }
            setButton(settings.plain(afford ? "button-buy" : "button-poor"), afford ? Palette.BUY : Palette.DENY);
        }
    }

    private void setInfoIcon(Material m) {
        if (m != infoIconType && infoIcon != null && infoIcon.entity instanceof ItemDisplay id && id.isValid()) {
            infoIconType = m;
            id.setItemStack(iconStack(m));
        }
    }

    private void setBar(double frac, int color) {
        frac = Math.max(0, Math.min(1, frac));
        if (Math.abs(frac - barFrac) > 1e-6) {
            barFrac = frac;
            double inner = INFO_BAR_W - 0.006;
            double w = inner * frac;
            barFill.w = w;
            barFill.cx = SIDE_CX - inner / 2.0 + w / 2.0;
            chrome.apply(frame, barFill, 5);
        }
        barFill.recolor(color, 5);
    }

    private void setDesc(String mini) {
        List<String> wrapped = Text.wrap(mini, INFO_DESC_WRAP_PX);
        if (wrapped.size() > INFO_DESC_MAX_LINES) {
            wrapped = wrapped.subList(0, INFO_DESC_MAX_LINES);
        }
        int count = Math.max(1, wrapped.size());
        setText(infoDesc, Text.mm(String.join("\n", wrapped)));
        if (count != infoDesc.lines) {
            infoDesc.lines = count;
            chrome.apply(frame, infoDesc, 0);
        }
    }

    private void setButton(Component label, int color) {
        boolean show = label != null;
        if (show != buyVisible) {
            buyVisible = show;
            buyGroup.scale = show ? ("buy".equals(hovered) ? 1.06 : 1.0) : 0.001;
            buyGroup.apply(frame, 3);
        }
        if (show) {
            setText(buyLabel, label);
            for (Part p : buyFill) {
                p.recolor(color, 4);
            }
        }
    }
}
