package com.upgradetree.chest;

import java.util.random.RandomGenerator;

/**
 * The CS2-style item strip of one opening: which reward sits on every card, where the winner is,
 * and where the strip is at each tick (fast start, long slow stop). Pure logic, no server needed.
 */
public final class Strip {

    /** Cards left of the centre marker when the spin starts (the window is full from the first tick). */
    public static final double START = 4.0;
    /** Cards drawn after the winner, so the window is still full when the strip stops. */
    public static final int AFTER = 6;

    /** Reward index (in the chest's reward list) on every card. */
    public final int[] cards;
    /** Card index of the winner. */
    public final int winner;
    /** Final strip position: inside the winner card, a little off centre like CS2. */
    public final double end;
    public final int ticks;

    private Strip(int[] cards, int winner, double end, int ticks) {
        this.cards = cards;
        this.winner = winner;
        this.end = end;
        this.ticks = ticks;
    }

    /**
     * Builds a strip that stops on {@code winnerReward}. Filler cards are drawn with the chest's
     * real odds, so commons dominate the strip just like in CS2.
     */
    public static Strip build(double[] chances, int winnerReward, int ticks, RandomGenerator rnd) {
        int winner = 38 + rnd.nextInt(9);
        int[] cards = new int[winner + AFTER + 1];
        for (int i = 0; i < cards.length; i++) {
            cards[i] = pick(chances, rnd.nextDouble(), winnerReward);
        }
        cards[winner] = winnerReward;
        double end = winner + (rnd.nextDouble() * 0.7 - 0.35);
        return new Strip(cards, winner, end, Math.max(20, ticks));
    }

    /** Weighted pick of a reward index; falls back to {@code def} if no chance is positive. */
    static int pick(double[] chances, double roll, int def) {
        double total = 0;
        for (double c : chances) {
            total += Math.max(0, c);
        }
        if (total <= 0) {
            return def;
        }
        double target = roll * total;
        double acc = 0;
        int last = def;
        for (int i = 0; i < chances.length; i++) {
            if (chances[i] <= 0) {
                continue;
            }
            acc += chances[i];
            last = i;
            if (target < acc) {
                return i;
            }
        }
        return last;
    }

    /** Ease-out (cubic): fast at first, then a long slow creep onto the winner. */
    public static double ease(double u) {
        double t = Math.max(0, Math.min(1, u));
        double inv = 1 - t;
        return 1 - inv * inv * inv;
    }

    /** Strip position (in cards, the marker points at it) {@code tick} ticks after the spin started. */
    public double position(int tick) {
        return START + (end - START) * ease((double) tick / ticks);
    }

    public boolean finished(int tick) {
        return tick >= ticks;
    }

    /** The card under the centre marker at a strip position. */
    public static int under(double position) {
        return (int) Math.floor(position + 0.5);
    }

    /** Reward index on a card, or -1 outside the strip. */
    public int reward(int card) {
        return card < 0 || card >= cards.length ? -1 : cards[card];
    }
}
