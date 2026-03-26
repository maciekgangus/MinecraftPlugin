package dev.casino.economy;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** Counts gold from player inventory and Enderchest. All amounts in ingot units. */
public final class GoldEconomyManager implements EconomyManager {

    @Override
    public int getBalance(Player player) {
        return toIngots(player.getInventory()) + toIngots(player.getEnderChest());
    }

    @Override
    public boolean hasFunds(Player player, int amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean withdraw(Player player, int amount) {
        if (!hasFunds(player, amount)) return false;
        int remaining = removeGold(player.getInventory(), amount);
        if (remaining > 0) removeGold(player.getEnderChest(), remaining);
        return true;
    }

    @Override
    public void deposit(Player player, int amount) {
        ItemStack ingots = new ItemStack(Material.GOLD_INGOT, amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(ingots);
        if (overflow.isEmpty()) return;
        Map<Integer, ItemStack> enderOverflow = player.getEnderChest().addItem(overflow.get(0));
        if (!enderOverflow.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), enderOverflow.get(0));
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /** Total ingot value of the inventory (floor division for nuggets). */
    private int toIngots(Inventory inventory) {
        return sum(inventory, Material.GOLD_NUGGET) / 9
             + sum(inventory, Material.GOLD_INGOT)
             + sum(inventory, Material.GOLD_BLOCK) * 9;
    }

    /**
     * Removes {@code ingotAmount} ingots' worth from {@code inventory},
     * consuming smallest denominations first (nuggets → ingots → blocks).
     *
     * @return ingots still unpaid (0 = fully removed)
     */
    private int removeGold(Inventory inventory, int ingotAmount) {
        int nuggetTarget = ingotAmount * 9;

        // 1. Consume nuggets
        int tN = Math.min(sum(inventory, Material.GOLD_NUGGET), nuggetTarget);
        consume(inventory, Material.GOLD_NUGGET, tN);
        nuggetTarget -= tN;
        if (nuggetTarget <= 0) return 0;

        // 2. Consume ingots (ceiling-divide to cover remaining nugget target)
        int tI = Math.min(sum(inventory, Material.GOLD_INGOT), (nuggetTarget + 8) / 9);
        consume(inventory, Material.GOLD_INGOT, tI);
        nuggetTarget -= tI * 9;
        if (nuggetTarget <= 0) { returnChange(inventory, -nuggetTarget); return 0; }

        // 3. Consume blocks
        int tB = Math.min(sum(inventory, Material.GOLD_BLOCK), (nuggetTarget + 80) / 81);
        consume(inventory, Material.GOLD_BLOCK, tB);
        nuggetTarget -= tB * 81;
        if (nuggetTarget <= 0) { returnChange(inventory, -nuggetTarget); return 0; }

        return (nuggetTarget + 8) / 9; // remaining ingots caller must take elsewhere
    }

    /** Gives back excess nugget units as ingots + nuggets. */
    private void returnChange(Inventory inventory, int excessNuggets) {
        if (excessNuggets <= 0) return;
        int ingots  = excessNuggets / 9;
        int nuggets = excessNuggets % 9;
        if (ingots  > 0) inventory.addItem(new ItemStack(Material.GOLD_INGOT,  ingots));
        if (nuggets > 0) inventory.addItem(new ItemStack(Material.GOLD_NUGGET, nuggets));
    }

    private int sum(Inventory inventory, Material material) {
        return inventory.all(material).values().stream()
                .mapToInt(ItemStack::getAmount).sum();
    }

    private void consume(Inventory inventory, Material material, int amount) {
        if (amount <= 0) return;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length && amount > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;
            int take = Math.min(item.getAmount(), amount);
            item.setAmount(item.getAmount() - take);
            if (item.getAmount() == 0) inventory.setItem(i, null);
            amount -= take;
        }
    }
}
