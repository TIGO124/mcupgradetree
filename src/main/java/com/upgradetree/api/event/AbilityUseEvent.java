package com.upgradetree.api.event;

import com.upgradetree.tree.NodeEffect;
import com.upgradetree.tree.UpgradeNode;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Called before an active ability is cast. Cancel it to block the cast (no cooldown is used). */
public final class AbilityUseEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UpgradeNode node;
    private final NodeEffect.Ability ability;
    private final int level;
    private boolean cancelled;

    public AbilityUseEvent(Player player, UpgradeNode node, NodeEffect.Ability ability, int level) {
        super(player);
        this.node = node;
        this.ability = ability;
        this.level = level;
    }

    public UpgradeNode getNode() {
        return node;
    }

    public NodeEffect.Ability getAbility() {
        return ability;
    }

    public int getLevel() {
        return level;
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
