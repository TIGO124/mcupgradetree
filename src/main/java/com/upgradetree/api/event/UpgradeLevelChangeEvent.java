package com.upgradetree.api.event;

import com.upgradetree.tree.UpgradeNode;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Called after a level of an upgrade was bought (informational). */
public final class UpgradeLevelChangeEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UpgradeNode node;
    private final int oldLevel;
    private final int newLevel;
    private final double paid;

    public UpgradeLevelChangeEvent(Player player, UpgradeNode node, int oldLevel, int newLevel, double paid) {
        super(player);
        this.node = node;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.paid = paid;
    }

    public UpgradeNode getNode() {
        return node;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public double getPaid() {
        return paid;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
