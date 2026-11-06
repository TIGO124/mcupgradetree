package com.upgradetree.api.event;

import com.upgradetree.tree.UpgradeNode;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called before a player buys a level of an upgrade, after every rule passed and before the
 * currency is taken. Cancel it to block the purchase; the cost can be changed.
 */
public final class UpgradePurchaseEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UpgradeNode node;
    private final int newLevel;
    private double cost;
    private boolean cancelled;

    public UpgradePurchaseEvent(Player player, UpgradeNode node, int newLevel, double cost) {
        super(player);
        this.node = node;
        this.newLevel = newLevel;
        this.cost = cost;
    }

    public UpgradeNode getNode() {
        return node;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = Math.max(0, cost);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
