package com.upgradetree.currency;

import com.upgradetree.data.DataStore;
import com.upgradetree.data.PlayerData;
import com.upgradetree.util.Text;
import org.bukkit.entity.Player;

/** The plugin's own upgrade points, stored with the player data. */
public final class PointsCurrency implements Currency {

    private final DataStore store;

    public PointsCurrency(DataStore store) {
        this.store = store;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public double balance(Player player) {
        return store.get(player).points();
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        PlayerData d = store.get(player);
        if (d.points() + 1e-9 < amount) {
            return false;
        }
        d.points(d.points() - amount);
        return true;
    }

    @Override
    public boolean deposit(Player player, double amount) {
        PlayerData d = store.get(player);
        d.addPoints(Math.max(0, amount));
        return true;
    }

    @Override
    public String format(double amount, String name) {
        return formatShort(amount) + " " + name;
    }

    @Override
    public String formatShort(double amount) {
        return Text.num(amount);
    }
}
