package com.upgradetree.chest;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StripTest {

    private static final double[] CHANCES = {0.6, 0.3, 0.1};

    @Test
    void theStripStopsOnTheWinner() {
        for (long seed = 0; seed < 200; seed++) {
            Strip s = Strip.build(CHANCES, 2, 120, new SplittableRandom(seed));
            assertTrue(s.winner >= 38 && s.winner <= 46, "winner " + s.winner);
            assertEquals(2, s.cards[s.winner]);
            assertEquals(s.winner + Strip.AFTER + 1, s.cards.length);
            assertTrue(Math.abs(s.end - s.winner) <= 0.35 + 1e-9);
            assertEquals(s.winner, Strip.under(s.end));
            assertEquals(s.winner, Strip.under(s.position(s.ticks)));
        }
    }

    @Test
    void positionStartsFullAndNeverGoesBack() {
        Strip s = Strip.build(CHANCES, 0, 100, new SplittableRandom(7));
        assertEquals(Strip.START, s.position(0), 1e-9);
        assertEquals(s.end, s.position(100), 1e-9);
        assertEquals(s.end, s.position(500), 1e-9);
        double last = s.position(0);
        for (int t = 1; t <= 100; t++) {
            double p = s.position(t);
            assertTrue(p >= last - 1e-12, "tick " + t);
            last = p;
        }
        // fast start, slow end: the first tenth covers far more cards than the last tenth
        double first = s.position(10) - s.position(0);
        double end = s.position(100) - s.position(90);
        assertTrue(first > 20 * end, first + " vs " + end);
    }

    @Test
    void fillerCardsUseTheOdds() {
        Strip s = Strip.build(CHANCES, 2, 120, new SplittableRandom(3));
        for (int c : s.cards) {
            assertTrue(c >= 0 && c < CHANCES.length);
        }
        assertEquals(-1, s.reward(-1));
        assertEquals(-1, s.reward(s.cards.length));
        assertEquals(1, Strip.pick(new double[]{0, 1, 0}, 0.5, 9));
        assertEquals(9, Strip.pick(new double[]{0, 0}, 0.5, 9));
    }

    @Test
    void easingAndEdgeFade() {
        assertEquals(0, Strip.ease(0), 1e-12);
        assertEquals(1, Strip.ease(1), 1e-12);
        assertEquals(1, Strip.ease(2), 1e-12);
        assertEquals(1, Roulette.fade(0), 1e-12);
        assertEquals(1, Roulette.fade(-3.0), 1e-12);
        assertEquals(0, Roulette.fade(4.45), 1e-12);
        assertEquals(0, Roulette.fade(10), 1e-12);
        double mid = Roulette.fade(3.7);
        assertTrue(mid > 0 && mid < 1);
    }
}
