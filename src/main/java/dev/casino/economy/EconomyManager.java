package dev.casino.economy;

import org.bukkit.entity.Player;

/**
 * Service contract for the gold economy.
 * All amounts are expressed in gold ingot units
 * (1 block = 9 ingots, 9 nuggets = 1 ingot, floor division).
 */
public interface EconomyManager {

    /** Returns total gold balance in ingots: inventory + Enderchest combined. */
    int getBalance(Player player);

    /**
     * Withdraws {@code amount} ingots from the player.
     * Draws from inventory first, then Enderchest.
     * Consumes smallest denominations first (nuggets → ingots → blocks).
     *
     * @return {@code false} if the player has insufficient funds; no items removed.
     */
    boolean withdraw(Player player, int amount);

    /**
     * Deposits {@code amount} ingots into the player's inventory (as gold ingots).
     * Falls back to Enderchest if inventory is full; drops at player's feet if both full.
     */
    void deposit(Player player, int amount);

    /** Returns {@code true} if {@link #getBalance(Player)} >= {@code amount}. */
    boolean hasFunds(Player player, int amount);
}
