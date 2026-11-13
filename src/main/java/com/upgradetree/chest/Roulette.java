package com.upgradetree.chest;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.util.Palette;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The CS2-style item strip above an opened chest: cards slide right to left under a gold marker,
 * slow down and stop on the prize. A fixed pool of card slots is recycled as the strip moves; cards
 * shrink away at the window edges, so a recycled card never visibly jumps.
 */
final class Roulette {

    private static final int SLOTS = 12;
    private static final double PITCH = 0.31;
    private static final double CARD_W = 0.28;
    private static final double CARD_H = 0.37;
    private static final double ICON = 0.19;
    private static final double BAR_H = 0.028;
    /** Cards are fully shown up to this distance (in cards) from the marker, then shrink until FADE_END. */
    private static final double FULL = 3.0;
    private static final double FADE_END = 4.45;
    private static final double FRAME_W = 2 * FADE_END * PITCH + 0.06;
    private static final double FRAME_H = CARD_H + 0.08;

    private final class Slot {
        TextDisplay bg;
        TextDisplay bar;
        ItemDisplay icon;
        TextDisplay badge;
        int card = Integer.MIN_VALUE;
        double fade = -1;
    }

    private final UpgradeTreePlugin plugin;
    private final Player viewer;
    private final Location at;
    private final ChestType chest;
    private final Strip strip;
    private final Map<Integer, ItemStack> icons = new HashMap<>();
    private final List<Slot> slots = new ArrayList<>();
    private final List<TextDisplay> frame = new ArrayList<>();
    private TextDisplay marker;
    private TextDisplay title;
    private TextDisplay result;
    private double scale = 0.001;
    private double position;

    Roulette(UpgradeTreePlugin plugin, Player viewer, Location at, ChestType chest, Strip strip) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.at = at;
        this.chest = chest;
        this.strip = strip;
        this.position = Strip.START;
    }

    private ChestReward reward(int card) {
        int r = strip.reward(card);
        return r < 0 || r >= chest.rewards.size() ? null : chest.rewards.get(r);
    }

    private ItemStack icon(ChestReward r) {
        return icons.computeIfAbsent(r.index, i -> r.icon());
    }

    /** Card background: the node base colour of the menu style, a little darker. */
    private int dark() {
        return Palette.darken(Palette.alpha(plugin.settings().theme.locked(), 255), 0.25);
    }

    private int cardColor(ChestReward r) {
        return Palette.mix(r.rarity.color(), dark(), 0.74);
    }

    // ------------------------------------------------------------------ spawning

    /** Spawns everything at (almost) zero size; {@link #setScale} grows it. */
    void spawn(Lang lang) {
        var theme = plugin.settings().theme;
        frame.add(Displays.rect(plugin, viewer, at, Palette.alpha(theme.panelBorder(), 255), Displays.rect(0, 0, -0.012, 0.001, 0.001)));
        frame.add(Displays.rect(plugin, viewer, at, Palette.alpha(theme.background(), 0xF0), Displays.rect(0, 0, -0.006, 0.001, 0.001)));
        for (int i = 0; i < SLOTS; i++) {
            Slot s = new Slot();
            s.bg = Displays.rect(plugin, viewer, at, dark(), Displays.rect(0, 0, 0, 0.001, 0.001));
            s.bar = Displays.rect(plugin, viewer, at, dark(), Displays.rect(0, 0, 0.004, 0.001, 0.001));
            s.icon = Displays.item(plugin, viewer, at, new ItemStack(org.bukkit.Material.CHEST), Displays.item(0, 0, 0.07, 0.001));
            s.badge = Displays.text(plugin, viewer, at, Component.empty(), Displays.text(0, 0, 0.012, 0.001, 1));
            slots.add(s);
        }
        marker = Displays.rect(plugin, viewer, at, theme.maxed(), Displays.rect(0, 0, 0.16, 0.001, 0.001));
        title = Displays.text(plugin, viewer, at, Text.mm("<b>" + chest.name()), Displays.text(0, 0.37, 0, 0.001, 1));
        result = Displays.text(plugin, viewer, at, Component.empty(), Displays.text(0, -0.37, 0, 0.001, 1));
        frame.add(Displays.rect(plugin, viewer, at, theme.maxed(), Displays.rect(0, 0, 0.16, 0.001, 0.001)));
        frame.add(Displays.rect(plugin, viewer, at, theme.maxed(), Displays.rect(0, 0, 0.16, 0.001, 0.001)));
        scale = 0.001;
        update(position, 0, true);
    }

    /** Grows (or shrinks) the whole strip around its centre. */
    void setScale(double s, int duration) {
        scale = Math.max(0.001, s);
        pushFrame(duration);
        update(position, duration, true);
    }

    private void pushFrame(int duration) {
        double k = scale;
        Displays.push(frame.get(0), Displays.rect(0, 0, -0.012, (FRAME_W + 0.03) * k, (FRAME_H + 0.03) * k), duration);
        Displays.push(frame.get(1), Displays.rect(0, 0, -0.006, FRAME_W * k, FRAME_H * k), duration);
        Displays.push(marker, Displays.rect(0, 0, 0.16, 0.014 * k, (CARD_H + 0.07) * k), duration);
        double tip = (CARD_H / 2 + 0.035) * k;
        Displays.push(frame.get(2), Displays.rect(0, tip, 0.16, 0.05 * k, 0.05 * k, Math.PI / 4), duration);
        Displays.push(frame.get(3), Displays.rect(0, -tip, 0.16, 0.05 * k, 0.05 * k, Math.PI / 4), duration);
        Displays.push(title, Displays.text(0, 0.37 * k, 0, 0.075 * k, 1), duration);
        Displays.push(result, Displays.text(0, -0.37 * k, 0, 0.068 * k, 1), duration);
    }

    // ------------------------------------------------------------------ moving

    /** Card size factor at a distance (in cards) from the marker: 1 inside, shrinking to 0 at the edges. */
    static double fade(double distance) {
        double d = Math.abs(distance);
        if (d <= FULL) {
            return 1;
        }
        return Math.max(0, Math.min(1, (FADE_END - d) / (FADE_END - FULL)));
    }

    /**
     * Moves the strip to {@code p} (the card under the marker). Slots that leave the window are
     * recycled for cards entering it and jump there at once (they are invisible at that moment).
     */
    void update(double p, int duration, boolean all) {
        position = p;
        int first = (int) Math.floor(p - FADE_END - 1);
        for (int j = 0; j < SLOTS; j++) {
            Slot s = slots.get(j);
            int card = first + Math.floorMod(j - first, SLOTS);
            boolean recycled = s.card != card;
            if (recycled) {
                s.card = card;
                ChestReward r = reward(card);
                if (r != null) {
                    s.icon.setItemStack(icon(r));
                    Displays.recolor(s.bg, cardColor(r), 0);
                    Displays.recolor(s.bar, r.rarity.color(), 0);
                    s.badge.text(Text.mm("<hi><b>" + r.badge()));
                }
            }
            double f = reward(card) == null ? 0 : fade(card - p);
            if (!all && !recycled && f == 0 && s.fade == 0) {
                continue;
            }
            s.fade = f;
            place(s, (card - p) * PITCH, f * scale, recycled ? 0 : duration, 1.0);
        }
    }

    private void place(Slot s, double x, double k, int duration, double pop) {
        double kx = x * scale;
        double w = CARD_W * k * pop;
        double h = CARD_H * k * pop;
        Displays.push(s.bg, Displays.rect(kx, 0, 0, w, h), duration);
        Displays.push(s.bar, Displays.rect(kx, -h / 2 + BAR_H * k * pop / 2, 0.004, w, BAR_H * k * pop), duration);
        Displays.push(s.icon, Displays.item(kx, 0.025 * k * pop, 0.07, ICON * k * pop), duration);
        Displays.push(s.badge, Displays.text(kx + w / 2 - 0.055 * k * pop, -h / 2 + 0.065 * k * pop, 0.012, 0.045 * k * pop, 1),
                duration);
    }

    // ------------------------------------------------------------------ reveal

    private Slot winnerSlot() {
        for (Slot s : slots) {
            if (s.card == strip.winner) {
                return s;
            }
        }
        return null;
    }

    /** The prize card grows and glows, the others darken, the name of the prize shows below. */
    void reveal(ChestReward prize, Lang lang) {
        int color = prize.rarity.color();
        for (Slot s : slots) {
            if (s.card == strip.winner) {
                continue;
            }
            Displays.recolor(s.bg, Palette.darken(dark(), 0.4), 8);
            Displays.recolor(s.bar, Palette.darken(dark(), 0.2), 8);
            place(s, (s.card - position) * PITCH, s.fade * scale * 0.9, 6, 1.0);
        }
        Slot w = winnerSlot();
        if (w != null) {
            Displays.recolor(w.bg, Palette.mix(color, dark(), 0.45), 6);
            place(w, (w.card - position) * PITCH, scale, 5, 1.18);
            w.icon.setGlowColorOverride(Color.fromRGB(Palette.rgb(color)));
            w.icon.setGlowing(true);
        }
        Displays.recolor(marker, color, 6);
        result.text(Text.mm("<b>" + prize.coloredLabel(lang) + "</b>\n" + prize.rarity.name()));
        Displays.push(result, Displays.text(0, -0.40 * scale, 0, 0.068 * scale, 2), 0);
    }

    /** The prize icon flies to the viewer (local +z) and shrinks away. */
    void flyPrize(double distance, double dropY) {
        Slot w = winnerSlot();
        if (w == null) {
            return;
        }
        Displays.push(w.icon, Displays.item(0, dropY, Math.max(0.3, distance - 0.55), 0.06), 10);
        Displays.push(w.bg, Displays.rect((w.card - position) * PITCH * scale, 0, 0, CARD_W * scale, CARD_H * scale), 6);
    }

    /** The flown prize icon reached the viewer: it disappears. */
    void hidePrize() {
        Slot w = winnerSlot();
        if (w != null) {
            Displays.remove(w.icon);
        }
    }

    void remove() {
        for (Slot s : slots) {
            Displays.remove(s.bg);
            Displays.remove(s.bar);
            Displays.remove(s.icon);
            Displays.remove(s.badge);
        }
        slots.clear();
        for (TextDisplay t : frame) {
            Displays.remove(t);
        }
        frame.clear();
        Displays.remove(marker);
        Displays.remove(title);
        Displays.remove(result);
    }
}
