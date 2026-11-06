package com.upgradetree.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Called before a player prestiges. Cancel it to stop the prestige. */
public final class PrestigeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int newPrestige;
    private boolean cancelled;

    public PrestigeEvent(Player player, int newPrestige) {
        super(player);
        this.newPrestige = newPrestige;
    }

    public int getNewPrestige() {
        return newPrestige;
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
