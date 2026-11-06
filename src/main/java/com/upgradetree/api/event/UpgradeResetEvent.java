package com.upgradetree.api.event;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Called before a player's upgrades are reset. Cancel it to keep them (not for ADMIN resets of offline players). */
public final class UpgradeResetEvent extends Event implements Cancellable {

    public enum Cause { RESPEC, ADMIN, PRESTIGE }

    private static final HandlerList HANDLERS = new HandlerList();

    private final OfflinePlayer player;
    private final Cause cause;
    private final double refund;
    private boolean cancelled;

    public UpgradeResetEvent(OfflinePlayer player, Cause cause, double refund) {
        this.player = player;
        this.cause = cause;
        this.refund = refund;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public Cause getCause() {
        return cause;
    }

    /** Currency returned to the player (0 for admin resets and prestige). */
    public double getRefund() {
        return refund;
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
