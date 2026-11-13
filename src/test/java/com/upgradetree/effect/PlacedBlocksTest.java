package com.upgradetree.effect;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedBlocksTest {

    /** A mark must lead back to its block: full lists are cleaned by looking the blocks up again. */
    @Test
    void aPackedPositionUnpacksToTheSameBlock() {
        int[][] blocks = {{0, 0, 0}, {15, 319, 15}, {7, -64, 3}, {-1, 64, -1}, {-16, -2032, 16}, {1234, 2031, -5678}};
        for (int[] b : blocks) {
            int[] at = PlacedBlocks.unpack(PlacedBlocks.pack(b[0], b[1], b[2]));
            assertArrayEquals(new int[]{b[0] & 15, b[1], b[2] & 15}, at, b[0] + " " + b[1] + " " + b[2]);
        }
    }

    @Test
    void everyBlockOfAChunkColumnGetsItsOwnMark() {
        Set<Integer> seen = new HashSet<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = -64; y < 320; y += 37) {
                    assertTrue(seen.add(PlacedBlocks.pack(x, y, z)));
                }
            }
        }
        assertEquals(16 * 16 * 11, seen.size());
    }
}
