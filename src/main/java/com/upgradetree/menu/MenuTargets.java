package com.upgradetree.menu;

/**
 * What the crosshair points at on a 3D menu, as a target id: "close", "respec", "prestige",
 * "tab:&lt;id&gt;", "buy", "minus", "chestopen", "chestbuy", "chest:&lt;id&gt;", "reward:&lt;n&gt;" or
 * "node:&lt;id&gt;"; and the hover feedback of each kind.
 */
final class MenuTargets {

    private MenuTargets() {
    }

    static String at(DisplayMenu m) {
        double[] uv = m.frame().project(m.player().getEyeLocation());
        if (uv == null) {
            return null;
        }
        String h = m.header.hit(uv[0], uv[1]);
        if (h != null) {
            return h;
        }
        if (m.info.hitBuy(uv[0], uv[1])) {
            return "buy";
        }
        if (m.info.hitMinus(uv[0], uv[1])) {
            return "minus";
        }
        String c = m.chests.hitButtons(uv[0], uv[1]);
        if (c != null || m.chests.active()) {
            return c != null ? c : m.chests.hit(uv[0], uv[1]);
        }
        String id = m.layer == null ? null : m.layer.hit(uv[0], uv[1]);
        return id == null ? null : "node:" + id;
    }

    static void hover(DisplayMenu m, String id, boolean on) {
        if (m.chests.hover(id, on)) {
            return;
        }
        if (id.equals("buy") || id.equals("minus")) {
            m.info.setBuyHover(id, on);
        } else if (id.startsWith("node:")) {
            if (m.layer != null) {
                m.layer.setHover(id.substring(5), on);
            }
        } else {
            m.header.hover(id, on);
        }
    }
}
