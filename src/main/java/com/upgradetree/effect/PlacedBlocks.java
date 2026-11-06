package com.upgradetree.effect;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Remembers blocks that players placed (stored in the chunk), so placing an ore and mining it
 * again gives no points, no double drops and no smelting bonus.
 */
public final class PlacedBlocks {

    private final NamespacedKey key;

    public PlacedBlocks(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "placed");
    }

    static int pack(Block b) {
        return (b.getX() & 15) | ((b.getZ() & 15) << 4) | ((b.getY() + 4096) << 8);
    }

    public boolean isPlaced(Block b) {
        int[] arr = b.getChunk().getPersistentDataContainer().get(key, PersistentDataType.INTEGER_ARRAY);
        if (arr == null) {
            return false;
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
        if (arr.length >= 4096) {
            return; // keep chunk data small
        }
        int[] next = java.util.Arrays.copyOf(arr, arr.length + 1);
        next[arr.length] = v;
        pdc.set(key, PersistentDataType.INTEGER_ARRAY, next);
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
