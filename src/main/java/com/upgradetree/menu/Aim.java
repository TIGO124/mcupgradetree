package com.upgradetree.menu;

import org.bukkit.entity.Player;

/**
 * Tracks whether the viewer's crosshair is on the panel. The panel stays where it was opened and
 * the player walks freely: while the crosshair is on the panel, clicks and keys belong to the menu
 * (block/entity reach is switched off and the hand is hidden); otherwise the game works as usual.
 */
final class Aim {

    private final DisplayMenu menu;
    private boolean onPanel;
    private boolean handHidden;
    private boolean rehide;

    Aim(DisplayMenu menu) {
        this.menu = menu;
    }

    /** True when clicks and keys belong to the menu (always, if the player is locked in place). */
    boolean captures() {
        return menu.settings().lockPlayer || onPanel;
    }

    boolean onPanel() {
        return onPanel;
    }

    boolean handHidden() {
        return handHidden;
    }

    void rehide() {
        rehide = true;
    }

    /** At opening the player looks straight at the panel centre. */
    void open() {
        onPanel = true;
        Freeze.blockReach(menu.plugin(), menu.player(), true);
        if (menu.settings().hideHand) {
            hide();
        }
    }

    void update(int tick) {
        Player p = menu.player();
        boolean now = inside(menu.frame().project(p.getEyeLocation()));
        if (now != onPanel) {
            onPanel = now;
            boolean capture = captures();
            Freeze.blockReach(menu.plugin(), p, capture);
            if (!capture && handHidden) {
                show();
            } else if (capture) {
                rehide = true;
            }
        }
        if (menu.settings().hideHand && captures() && (rehide || tick % 40 == 0)) {
            rehide = false;
            hide();
        }
    }

    void release() {
        Player p = menu.player();
        Freeze.blockReach(menu.plugin(), p, false);
        if (handHidden) {
            show();
        }
    }

    private void hide() {
        Timeline.hideHand(menu.player());
        handHidden = true;
    }

    private void show() {
        handHidden = false;
        menu.player().updateInventory();
    }

    private static boolean inside(double[] uv) {
        return uv != null && uv[0] >= Layout.EXTENT[0][0] && uv[0] <= Layout.EXTENT[0][1]
                && uv[1] >= Layout.EXTENT[1][0] && uv[1] <= Layout.EXTENT[1][1];
    }
}
