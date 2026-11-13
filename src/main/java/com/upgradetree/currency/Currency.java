package com.upgradetree.currency;

import org.bukkit.entity.Player;

/** What upgrades are paid with. */
public interface Currency {

    /** False if the backing system (e.g. Vault economy) is missing. */
    boolean available();

    double balance(Player player);

    /** Takes the amount; returns false if the player cannot pay. */
    boolean withdraw(Player player, double amount);

    /** Gives the amount back (refunds); returns false if it failed. */
    boolean deposit(Player player, double amount);

    /**
     * Amount with the currency name, e.g. "5 Levels" or "$1,250"
     * ({@code name} comes from Settings#currencyName).
     */
    String format(double amount, String name);

    /** Just the number part for compact displays. */
    String formatShort(double amount);
}
