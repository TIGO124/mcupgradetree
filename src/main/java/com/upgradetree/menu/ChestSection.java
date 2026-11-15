package com.upgradetree.menu;

import com.upgradetree.chest.ChestReward;
import com.upgradetree.chest.ChestType;

import java.util.List;

/**
 * State of the Chests tab of one 3D menu: whether it is shown, the selected chest, and routing of
 * its hover targets and clicks.
 */
final class ChestSection {

    private final DisplayMenu m;
    private final ChestActions actions;
    private final boolean enabled;
    ChestLayer layer;
    private boolean active;
    private ChestType selected;

    ChestSection(DisplayMenu menu, boolean enabled) {
        this.m = menu;
        this.enabled = enabled;
        this.actions = new ChestActions(menu);
    }

    /** True when the header has the Chests tab (never in the admin mode). */
    boolean enabled() {
        return enabled;
    }

    /** True while the Chests section is shown instead of a tree. */
    boolean active() {
        return active;
    }

    /** The chest chosen in the section (the first one until the player clicks another). */
    ChestType selected() {
        if (selected != null) {
            ChestType now = m.plugin().chestTypes().chest(selected.id);
            if (now != null) {
                return now;
            }
        }
        List<ChestType> all = m.plugin().chestTypes().chests();
        return all.isEmpty() ? null : all.get(0);
    }

    /** Builds the section; the tree layer must already be gone. */
    void open() {
        active = true;
        layer = new ChestLayer(m);
        layer.build(m.animate());
        m.header.setActive(ChestLayer.TAB);
    }

    void leave() {
        if (!active) {
            return;
        }
        active = false;
        if (layer != null) {
            layer.remove();
            layer = null;
        }
    }

    void shrink(int duration) {
        if (layer != null) {
            layer.shrink(duration);
        }
    }

    /** The open/buy pair on the info card (shown only for chests the player owns). */
    String hitButtons(double u, double v) {
        if (m.info.hitChestOpen(u, v)) {
            return "chestopen";
        }
        return m.info.hitChestBuy(u, v) ? "chestbuy" : null;
    }

    String hit(double u, double v) {
        return layer == null ? null : layer.hit(u, v);
    }

    /** Hover of a Chests target; false if the target is not one. */
    boolean hover(String id, boolean on) {
        if (id.equals("chestopen") || id.equals("chestbuy")) {
            m.info.setBuyHover(id, on);
            return true;
        }
        if (id.startsWith("chest:") || id.startsWith("reward:")) {
            if (layer != null) {
                layer.setHover(id, on);
            }
            return true;
        }
        return false;
    }

    /** Handles a click; false if it does not belong to the Chests section. */
    boolean click(String target, String hovered) {
        switch (target) {
            case "chestbuy" -> actions.buy(selected(), "chestbuy");
            case "chestopen" -> actions.open(selected());
            case "buy" -> {
                if (!active) {
                    return false;
                }
                actions.buy(selected(), "buy");
            }
            default -> {
                if (target.startsWith("reward:")) {
                    return true;
                }
                if (!target.startsWith("chest:")) {
                    return false;
                }
                ChestType c = m.plugin().chestTypes().chest(target.substring(6));
                if (c != null) {
                    selected = c;
                    m.sound("minecraft:ui.button.click", 0.4f, 1.6f);
                    if (layer != null) {
                        layer.refresh();
                    }
                    refreshInfo(hovered);
                }
            }
        }
        return true;
    }

    void updateStatus(Part progress) {
        int owned = m.data().totalChests();
        Painter.setText(progress, owned > 0 ? m.lang().plain("chests-progress", "count", String.valueOf(owned))
                : m.lang().plain("chests-progress-none"));
        if (layer != null) {
            layer.refresh();
        }
    }

    /** The hovered prize, else the hovered chest, else the selected chest. */
    void refreshInfo(String hovered) {
        if (layer == null) {
            return;
        }
        ChestType chest = selected();
        if (hovered != null && hovered.startsWith("chest:")) {
            ChestType h = m.plugin().chestTypes().chest(hovered.substring(6));
            if (h != null) {
                chest = h;
            }
        }
        if (hovered != null && hovered.startsWith("reward:") && layer.shown() != null) {
            ChestReward r = layer.reward(hovered);
            if (r != null) {
                m.info.showReward(layer.shown(), r);
                return;
            }
        }
        layer.showContents(chest);
        m.info.showChest(chest);
    }
}
