package com.upgradetree.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.upgradetree.tree.UpgradeNode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/** Item stacks used as upgrade icons (material, resource pack model or player head texture). */
public final class Icons {

    private Icons() {
    }

    public static ItemStack of(Material m) {
        return new ItemStack(m != null && m.isItem() && !m.isAir() ? m : Material.PAPER);
    }

    public static ItemStack of(UpgradeNode n) {
        if (n.headTexture != null) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            try {
                head.editMeta(SkullMeta.class, meta -> {
                    PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(n.headTexture.getBytes()), null);
                    profile.setProperty(new ProfileProperty("textures", n.headTexture));
                    meta.setPlayerProfile(profile);
                });
                return head;
            } catch (Exception ignored) {
                // fall back to the material
            }
        }
        ItemStack it = of(n.icon);
        if (n.iconModel != null) {
            NamespacedKey key = NamespacedKey.fromString(n.iconModel);
            if (key != null) {
                it.editMeta(m -> m.setItemModel(key));
            }
        }
        return it;
    }
}
