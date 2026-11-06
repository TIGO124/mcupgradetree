package com.upgradetree.currency;

import com.upgradetree.data.DataStore;
import com.upgradetree.data.PlayerData;
import com.upgradetree.util.Text;
import org.bukkit.entity.Player;

/** The plugin's own upgrade points, stored with the player data. */
public final class PointsCurrency implements Currency {

    private final DataStore store;
    private final String name;

    public PointsCurrency(DataStore store, String name) {
        this.store = store;
        this.name = name;
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
        store.save(d);
        return true;
    }

    @Override
    public String format(double amount) {
        return formatShort(amount) + " " + name;
    }

    @Override
    public String formatShort(double amount) {
        return Text.num(amount);
    }
}
