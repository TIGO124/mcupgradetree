package com.upgradetree.menu;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.config.Settings;
import com.upgradetree.currency.Currency;
import com.upgradetree.data.PlayerData;
import com.upgradetree.service.ProgressService;
import com.upgradetree.service.UpgradeService.Result;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.tree.UpgradeTree;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

import static com.upgradetree.menu.Layout.*;

/**
 * The Roblox-style hexagon upgrade tree, drawn with display entities that only its viewer can see.
 * The panel floats in front of the player; the crosshair works as the mouse cursor.
 */
public final class DisplayMenu {

    private final UpgradeTreePlugin plugin;
    private final MenuManager manager;
    private final Player player;
    private final Settings settings;
    private final Painter painter;
    private final boolean animate;
    private final List<UpgradeTree> trees;
    private final Timeline timeline;
    final Header header = new Header(this);
    final InfoCard info = new InfoCard(this);
    private final MenuActions actions = new MenuActions(this);

    private Group chrome;
    private Part progress;
    private UpgradeTree current;
    NodeLayer layer;
    private UpgradeNode selected;
    private String hovered;
    int tick;
    private int lastClick = -100;
    private int ignoreClicksUntil;
    String confirmNode;
    int confirmUntil;
    int respecUntil = -1;
    int prestigeUntil = -1;
    private boolean closing;
    private boolean done;
    private boolean handHidden;
    private boolean rehide;
    private boolean breathUp;

    DisplayMenu(UpgradeTreePlugin plugin, MenuManager manager, Player player, List<UpgradeTree> trees, UpgradeTree start) {
        this.plugin = plugin;
        this.manager = manager;
        this.player = player;
        this.settings = plugin.settings();
        this.animate = settings.animations;
        this.trees = trees;
        this.current = start;
        this.painter = new Painter(plugin, player, new Frame(player, settings.menuDistance, settings.menuScale, EXTENT));
        this.timeline = new Timeline(plugin.getLogger());
    }

    // ------------------------------------------------------------------ accessors for the parts

    UpgradeTreePlugin plugin() {
        return plugin;
    }

    Player player() {
        return player;
    }

    Painter painter() {
        return painter;
    }

    Frame frame() {
        return painter.frame;
    }

    Settings settings() {
        return settings;
    }

    Lang lang() {
        return plugin.lang();
    }

    PlayerData data() {
        return plugin.data().get(player);
    }

    boolean animate() {
        return animate;
    }

    UpgradeTree tree() {
        return current;
    }

    NodeLayer layer() {
        return layer;
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
        player.sendActionBar(lang().plain("opened-hint"));
    }

    private void build() {
        double startScale = animate ? 0.86 : 1.0;
        Settings.Theme t = settings.theme;
        chrome = painter.group(PANEL_CX, PANEL_CY);
        chrome.scale = startScale;
        painter.roundRect(chrome, PANEL_CX, PANEL_CY, PANEL_W + 2 * PANEL_STROKE, PANEL_H + 2 * PANEL_STROKE, 0.046,
                Z_PANEL_STROKE, t.panelBorder(), 3);
        painter.roundRect(chrome, PANEL_CX, PANEL_CY, PANEL_W, PANEL_H, 0.04, Z_PANEL, t.background(), 3);
        if (settings.stars) {
            Stars.spawn(painter, chrome, player.getUniqueId().hashCode());
        }
        header.build(trees, current, startScale);
        info.build(startScale);
        progress = painter.text(chrome, Part.text(CANVAS_CX, PROGRESS_Y, 0.032, 1, false, Z_TEXT), Component.empty(), 2000);
        painter.text(chrome, Part.text(CARD_CX, FOOTER_Y, 0.021, 1, false, Z_TEXT),
                lang().plain(settings.clickToBuy ? "menu-footer" : "menu-footer-select"), 4000);
        if (current != null) {
            layer = new NodeLayer(this, current);
            layer.build(animate);
        }
        updateStatus();
        info.show(null, false);
        if (animate) {
            later(2, () -> {
                chrome.scale = 1.0;
                chrome.apply(frame(), 5);
                header.setScale(1.0, 5);
                info.open(5);
            });
        }
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
        chrome.apply(frame(), 3);
        header.setScale(0.001, 3);
        info.shrink(3);
        if (layer != null) {
            layer.shrink(3);
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

    void rehideHand() {
        rehide = true;
    }

    private void removeAll() {
        painter.removeAll();
        done = true;
    }

    private void hideHand() {
        Timeline.hideHand(player);
        handHidden = true;
    }

    // ------------------------------------------------------------------ ticking

    void tick() {
        tick++;
        timeline.run(tick);
        if (done || closing) {
            return;
        }
        if (!player.isOnline() || player.isDead() || !player.getWorld().equals(frame().world)) {
            manager.close(player, true);
            return;
        }
        if (player.getLocation().toVector().distanceSquared(frame().feet) > 0.36) {
            manager.close(player, false);
            return;
        }
        if (tick >= 3) {
            updateHover();
        }
        if (respecUntil >= 0 && tick > respecUntil) {
            respecUntil = -1;
            header.setRespecConfirm(false);
        }
        if (prestigeUntil >= 0 && tick > prestigeUntil) {
            prestigeUntil = -1;
            header.setPrestigeConfirm(false);
        }
        if (confirmNode != null && tick > confirmUntil) {
            confirmNode = null;
            refreshInfo();
        }
        if (tick % 10 == 0) {
            updateStatus();
            refreshInfo();
        }
        if (settings.hideHand && (rehide || tick % 40 == 0)) {
            rehide = false;
            hideHand();
        }
        if (tick % 20 == 0 && layer != null && animate) {
            breathUp = !breathUp;
            Currency cur = plugin.currency();
            layer.breathe(breathUp, cur.available() ? cur.balance(player) : 0, cur.available());
        }
    }

    void later(int delay, Runnable task) {
        timeline.add(tick, delay, task);
    }

    void updateStatus() {
        header.update(plugin.progress().prestigeReady(data(), player));
        if (current != null) {
            int doneCount = plugin.service().maxed(data(), current);
            int total = current.nodes().size();
            Painter.setText(progress, lang().plain(doneCount >= total && total > 0 ? "menu-progress-done" : "menu-progress",
                    "done", String.valueOf(doneCount), "total", String.valueOf(total)));
        }
    }

    void refreshInfo() {
        UpgradeNode show = null;
        if (hovered != null && hovered.startsWith("node:")) {
            show = plugin.trees().node(hovered.substring(5));
        } else if (selected != null) {
            show = selected;
        }
        info.show(show, show != null && show.id.equals(confirmNode));
    }

    // ------------------------------------------------------------------ hover & click

    private String hitTest() {
        double[] uv = frame().project(player.getEyeLocation());
        if (uv == null) {
            return null;
        }
        String h = header.hit(uv[0], uv[1]);
        if (h != null) {
            return h;
        }
        if (info.hitBuy(uv[0], uv[1])) {
            return "buy";
        }
        if (layer != null) {
            String id = layer.hit(uv[0], uv[1]);
            if (id != null) {
                return "node:" + id;
            }
        }
        return null;
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
        refreshInfo();
    }

    private void setHover(String id, boolean on) {
        if (id.equals("buy")) {
            info.setBuyHover(on);
        } else if (id.startsWith("node:")) {
            if (layer != null) {
                layer.setHover(id.substring(5), on);
            }
        } else {
            header.hover(id, on);
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
        switch (target) {
            case "close" -> {
                sound("minecraft:ui.button.click", 0.5f, 1.4f);
                manager.close(player, false);
            }
            case "buy" -> {
                if (selected != null) {
                    actions.tryPurchase(selected, true);
                } else if (hovered != null && hovered.startsWith("node:")) {
                    actions.tryPurchase(plugin.trees().node(hovered.substring(5)), true);
                }
            }
            case "respec" -> actions.respec();
            case "prestige" -> actions.prestige();
            default -> {
                if (target.startsWith("tab:")) {
                    switchTab(target.substring(4));
                } else if (target.startsWith("node:")) {
                    UpgradeNode n = plugin.trees().node(target.substring(5));
                    if (n == null) {
                        return;
                    }
                    selected = n;
                    if (settings.clickToBuy) {
                        actions.tryPurchase(n, false);
                    } else {
                        sound("minecraft:ui.button.click", 0.4f, 1.6f);
                        refreshInfo();
                    }
                }
            }
        }
    }

    void ignoreClicks(int ticks) {
        ignoreClicksUntil = tick + ticks;
    }

    private void switchTab(String id) {
        UpgradeTree t = plugin.trees().tree(id);
        if (t == null || t == current) {
            return;
        }
        if (hovered != null) {
            setHover(hovered, false);
            hovered = null;
        }
        if (layer != null) {
            layer.remove();
        }
        current = t;
        selected = null;
        confirmNode = null;
        layer = new NodeLayer(this, t);
        layer.build(animate);
        header.setActive(t.id);
        updateStatus();
        refreshInfo();
        sound("minecraft:item.book.page_turn", 0.8f, 1.2f);
    }

    void rebuildLayer() {
        if (hovered != null) {
            setHover(hovered, false);
            hovered = null;
        }
        selected = null;
        if (layer != null) {
            layer.remove();
        }
        if (current != null) {
            layer = new NodeLayer(this, current);
            layer.build(animate);
        }
        updateStatus();
        refreshInfo();
    }

    // ------------------------------------------------------------------ small animations

    void pulse(Group g) {
        double base = g.scale;
        g.scale = base * 0.92;
        g.apply(frame(), 1);
        later(1, () -> {
            g.scale = base;
            g.apply(frame(), 3);
        });
    }

    void shake(Group g) {
        double[] seq = {0.012, -0.012, 0.007, -0.004, 0.0};
        g.dx = seq[0];
        g.apply(frame(), 1);
        for (int i = 1; i < seq.length; i++) {
            double v = seq[i];
            boolean last = i == seq.length - 1;
            later(i, () -> {
                g.dx = v;
                g.apply(frame(), last ? 2 : 1);
            });
        }
    }

    void sound(String key, float volume, float pitch) {
        if (settings.sounds && player.isOnline()) {
            player.playSound(player.getLocation(), key, SoundCategory.MASTER, volume, pitch);
        }
    }

    // ------------------------------------------------------------------ debug

    int entityCount() {
        return painter.count();
    }

    /** What the crosshair points at right now (debug). */
    String target() {
        return String.valueOf(hitTest());
    }

    List<Group> groups() {
        return painter.groups;
    }

    /** Layout position of a node (units), or null if it is not shown. */
    double[] nodePosition(String id) {
        return layer == null || !layer.views.containsKey(id) ? null : layer.position(id);
    }
}
