package dev.casino.games.slots;

import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

final class SlotSession {
    int bet = 1;
    boolean spinning = false;
    BukkitTask animationTask = null;
    Inventory openInventory = null;
    final SlotSymbol[] result = new SlotSymbol[3];
}
