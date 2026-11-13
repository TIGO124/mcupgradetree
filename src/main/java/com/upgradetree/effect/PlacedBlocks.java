package com.upgradetree.effect;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

/**
 * Remembers blocks that players placed (stored in the chunk), so placing an ore and mining it
 * again gives no points, no double drops and no smelting bonus.
 * <p>
 * A chunk keeps at most {@link #MAX} marks. A chunk whose list is full counts as placed everywhere:
 * otherwise blocks placed after the list filled up would pass as natural, and placing and breaking
 * them over and over would pay out the bonuses without end.
 */
public final class PlacedBlocks {

    /** Most marks kept per chunk (64 KB of chunk data). */
    static final int MAX = 16384;
    private static final long PRUNE_EVERY_MS = 1000;

    private final NamespacedKey key;
    private long lastPrune;

    public PlacedBlocks(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "placed");
    }

    static int pack(Block b) {
        return pack(b.getX(), b.getY(), b.getZ());
    }

    /** Position inside the chunk as one number: x and z in the chunk (0-15), then the height. */
    static int pack(int x, int y, int z) {
        return (x & 15) | ((z & 15) << 4) | ((y + 4096) << 8);
    }

    /** The {x in chunk, y, z in chunk} of a packed position. */
    static int[] unpack(int v) {
        return new int[]{v & 15, (v >> 8) - 4096, (v >> 4) & 15};
    }

    public boolean isPlaced(Block b) {
        int[] arr = b.getChunk().getPersistentDataContainer().get(key, PersistentDataType.INTEGER_ARRAY);
        if (arr == null) {
            return false;
        }
        if (arr.length >= MAX) {
            return true; // full list: nothing in this chunk can be told apart from a placed block
        }
        int v = pack(b);
        for (int x : arr) {
            if (x == v) {
                return true;
            }
        }
        return false;
    }

    public void mark(Block b) {
        Chunk c = b.getChunk();
        PersistentDataContainer pdc = c.getPersistentDataContainer();
        int[] arr = pdc.get(key, PersistentDataType.INTEGER_ARRAY);
        int v = pack(b);
        if (arr == null) {
            pdc.set(key, PersistentDataType.INTEGER_ARRAY, new int[]{v});
            return;
        }
        for (int x : arr) {
            if (x == v) {
                return;
            }
        }
        if (arr.length >= MAX) {
            // make room: marks of blocks that are gone (burnt, blown up) are of no use any more.
            // Looking up a full list takes a few milliseconds, so at most once a second.
            long now = System.currentTimeMillis();
            int[] kept = now - lastPrune >= PRUNE_EVERY_MS ? prune(c, arr) : arr;
            if (kept.length >= MAX) {
                return; // still full: isPlaced treats the whole chunk as placed
            }
            arr = kept;
        }
        int[] next = Arrays.copyOf(arr, arr.length + 1);
        next[arr.length] = v;
        pdc.set(key, PersistentDataType.INTEGER_ARRAY, next);
    }

    /** The marks whose block is still there. */
    private int[] prune(Chunk c, int[] arr) {
        lastPrune = System.currentTimeMillis();
        int min = c.getWorld().getMinHeight();
        int max = c.getWorld().getMaxHeight();
        int[] kept = new int[arr.length];
        int n = 0;
        for (int v : arr) {
            int[] at = unpack(v);
            if (at[1] >= min && at[1] < max && !c.getBlock(at[0], at[1], at[2]).getType().isAir()) {
                kept[n++] = v;
            }
        }
        return n == arr.length ? arr : Arrays.copyOf(kept, n);
    }

    public void unmark(Block b) {
        PersistentDataContainer pdc = b.getChunk().getPersistentDataContainer();
        int[] arr = pdc.get(key, PersistentDataType.INTEGER_ARRAY);
        if (arr == null) {
            return;
        }
        int v = pack(b);
        int idx = -1;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == v) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return;
        }
        if (arr.length == 1) {
            pdc.remove(key);
            return;
        }
        int[] next = new int[arr.length - 1];
        System.arraycopy(arr, 0, next, 0, idx);
        System.arraycopy(arr, idx + 1, next, idx, arr.length - idx - 1);
        pdc.set(key, PersistentDataType.INTEGER_ARRAY, next);
    }
}
