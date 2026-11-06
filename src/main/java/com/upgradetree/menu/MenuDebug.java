package com.upgradetree.menu;

import java.util.List;

import static com.upgradetree.menu.Layout.*;

/** Debug and test helpers for an open 3D menu (used by /yukselt debug). */
final class MenuDebug {

    private MenuDebug() {
    }

    static int entityCount(DisplayMenu m) {
        return m.painter().count();
    }

    /** What the crosshair points at right now. */
    static String target(DisplayMenu m) {
        return String.valueOf(m.hitTest());
    }

    static List<Group> groups(DisplayMenu m) {
        return m.painter().groups;
    }

    /** Layout position (units) of a shown node, a tab ("tab:id") or a button (buy, minus, respec, prestige, close). */
    static double[] position(DisplayMenu m, String target) {
        if (target.startsWith("tab:")) {
            return m.header.tabCenter(target.substring(4));
        }
        return switch (target) {
            case "buy" -> m.info.buyCenter();
            case "minus" -> m.info.minusCenter();
            case "respec" -> new double[]{RESPEC_X, HEADER_CY};
            case "prestige" -> new double[]{PRESTIGE_X, HEADER_CY};
            case "close" -> new double[]{CLOSE_X, HEADER_CY};
            default -> m.layer == null || !m.layer.views.containsKey(target) ? null : m.layer.position(target);
        };
    }
}
