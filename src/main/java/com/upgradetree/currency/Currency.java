package com.upgradetree.currency;

import org.bukkit.entity.Player;

/** What upgrades are paid with. */
public interface Currency {

    /** False if the backing system (e.g. Vault economy) is missing. */
    boolean available();

    double balance(Player player);

    /** Takes the amount; returns false if the player cannot pay. */
    boolean withdraw(Player player, double amount);

    /** Amount with the currency name, e.g. "5 Seviye" or "$1,250". */
    String format(double amount);

    /** Just the number part for compact displays. */
    String formatShort(double amount);
}
