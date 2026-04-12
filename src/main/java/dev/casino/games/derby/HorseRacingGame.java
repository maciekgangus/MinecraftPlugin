package dev.casino.games.derby;

import dev.casino.core.PluginContext;
import dev.casino.economy.EconomyManager;
import dev.casino.games.CasinoGame;
import dev.casino.gui.GuiBuilder;
import dev.casino.gui.GuiItem;
import dev.casino.gui.GuiMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.Inventory;

import java.util.Random;

public final class HorseRacingGame implements CasinoGame {

    private final PluginContext context;
    private final Random random = new Random();

    public HorseRacingGame(PluginContext context) {
        this.context = context;
    }

    private GuiMenu buildBettingMenu(Player player) {
        EconomyManager eco = context.economy();
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(Component.text("Back to all games"));
            back.setItemMeta(backMeta);
        }
        return new GuiBuilder()
            .title(Component.text("Pick a winner! (Cost: 1 ingot)"))
            .size(3)
            .item(11, new GuiItem(new ItemStack(Material.DIAMOND_HORSE_ARMOR),
                e -> placeBet(player, eco, 1)))
            .item(13, new GuiItem(new ItemStack(Material.GOLDEN_HORSE_ARMOR),
                e -> placeBet(player, eco, 2)))
            .item(15, new GuiItem(new ItemStack(Material.IRON_HORSE_ARMOR),
                e -> placeBet(player, eco, 3)))
            .item(22, new GuiItem(back, e -> context.openMainCasinoMenu(player)))
            .build();
    }

    @Override
    public void start(Player player) {
        context.gui().open(player, buildBettingMenu(player));
    }

    private void placeBet(Player player, EconomyManager eco, int horseNumber) {
        if (!eco.hasFunds(player, 1)) {
            player.sendMessage(Component.text("You don't have enough gold!"));
            return;
        }
        eco.withdraw(player, 1);
        player.sendMessage(Component.text("The race begins!"));
        Inventory inv = player.getOpenInventory().getTopInventory();
        inv.clear();

        ItemStack horse1 = new ItemStack(Material.DIAMOND_HORSE_ARMOR);
        ItemStack horse2 = new ItemStack(Material.GOLDEN_HORSE_ARMOR);
        ItemStack horse3 = new ItemStack(Material.IRON_HORSE_ARMOR);
        int[] positions = {0, 0, 0};

        inv.setItem(0, horse1);
        inv.setItem(9, horse2);
        inv.setItem(18, horse3);

        new BukkitRunnable() {
            @Override
            public void run() {
                inv.setItem(positions[0], null);
                inv.setItem(positions[1] + 9, null);
                inv.setItem(positions[2] + 18, null);

                for (int i = 0; i < 3; i++) {
                    if (random.nextBoolean()) {
                        positions[i]++;
                    }
                }

                inv.setItem(positions[0], horse1);
                inv.setItem(positions[1] + 9, horse2);
                inv.setItem(positions[2] + 18, horse3);

                int winner = 0;
                if (positions[0] >= 8) {
                    winner = 1;
                } else if (positions[1] >= 8) {
                    winner = 2;
                } else if (positions[2] >= 8) {
                    winner = 3;
                }

                if (winner != 0) {
                    cancel();
                    if (winner == horseNumber) {
                        player.sendMessage(Component.text("Your horse won! You receive 3 gold."));
                        eco.deposit(player, 3);
                    } else {
                        player.sendMessage(Component.text("Horse #" + winner + " won. Better luck next time."));
                    }
                    context.gui().open(player, buildBettingMenu(player));
                }
            }
        }.runTaskTimer(context.plugin(), 10L, 10L);
    }

    @Override
    public void stop(Player player) {
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.SADDLE);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.displayName(getDisplayName());
            icon.setItemMeta(meta);
        }
        return icon;
    }

    @Override
    public String getName() {
        return "horse_racing";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Virtual Derby");
    }
}
