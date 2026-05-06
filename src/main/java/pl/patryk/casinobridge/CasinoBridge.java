package pl.patryk.casinobridge;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dropper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class CasinoBridge extends JavaPlugin implements Listener {

    private final String triggerFileName = "spin.txt";
    private final String resultFileName = "result.txt";
    private final Material[] symbols = {Material.DIAMOND, Material.GOLD_INGOT, Material.IRON_INGOT, Material.DIRT};
    private boolean isSpinning = false;

    // Waluta i koszt gry
    private final Material currencyType = Material.GOLD_NUGGET;
    private final int costPerSpin = 1;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Most Kasyna v2 (Zabezpieczony) aktywowany!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null) {

            // ==========================================
            // 1. ZABEZPIECZENIE PRZED KRADZIEŻĄ
            // ==========================================
            if (block.getType() == Material.DROPPER) {
                if (isCasinoDropper(block)) {

                    if (player.isOp() && player.isSneaking()) {
                        BlockState state = block.getState();
                        if (state instanceof Dropper) {
                            Dropper dropper = (Dropper) state;

                            player.openInventory(dropper.getInventory());

                            player.sendMessage(ChatColor.GREEN + "🔓 Tryb Admina: Otwierasz skarbiec kasyna.");

                            event.setCancelled(true);
                            return;
                        }
                    }

                    event.setCancelled(true);
                    player.sendMessage(ChatColor.DARK_RED + "Błąd: Nie możesz otwierać skarbca kasyna!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
            }

            // ==========================================
            // 2. LOGIKA GRY (KLIKNIĘCIE PRZYCISKU)
            // ==========================================
            if (block.getType() == Material.STONE_BUTTON) {

                if (isSpinning) return;

                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getType() != currencyType || itemInHand.getAmount() < costPerSpin) {
                    player.sendMessage(ChatColor.RED + "Potrzebujesz " + costPerSpin + "x " + currencyType.name() + " w głównej ręce, aby zagrać!");
                    return;
                }

                List<ItemFrame> frames = new ArrayList<>();
                for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 2, 2, 2)) {
                    if (entity instanceof ItemFrame) {
                        frames.add((ItemFrame) entity);
                    }
                }

                if (frames.size() != 3) {
                    player.sendMessage(ChatColor.RED + "Błąd: Automat musi mieć 3 ramki!");
                    return;
                }

                Dropper dropper = findNearbyDropper(block);
                if (dropper == null) {
                    player.sendMessage(ChatColor.RED + "Błąd: Brak podajnika (Droppera) w pobliżu!");
                    return;
                }

                itemInHand.setAmount(itemInHand.getAmount() - costPerSpin);
                player.sendMessage(ChatColor.GRAY + "Pobrano opłatę: " + costPerSpin + "x " + currencyType.name());

                frames.sort(Comparator.comparingDouble(f -> f.getLocation().getX() + f.getLocation().getZ()));

                startCasinoGame(player, frames, dropper);
            }
        }
    }

    private boolean isCasinoDropper(Block startBlock) {
        int radius = 2;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (startBlock.getRelative(x, y, z).getType() == Material.STONE_BUTTON) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Dropper findNearbyDropper(Block startBlock) {
        int radius = 2;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block checkBlock = startBlock.getRelative(x, y, z);
                    if (checkBlock.getType() == Material.DROPPER) {
                        BlockState state = checkBlock.getState();
                        if (state instanceof Dropper) {
                            return (Dropper) state;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void startCasinoGame(Player player, List<ItemFrame> frames, Dropper dropper) {
        isSpinning = true;
        File triggerFile = new File(getDataFolder().getParentFile().getParentFile(), triggerFileName);

        try {
            triggerFile.createNewFile();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);

            new BukkitRunnable() {
                int tick = 0;
                Random random = new Random();

                @Override
                public void run() {
                    tick++;
                    for (ItemFrame frame : frames) {
                        frame.setItem(new ItemStack(symbols[random.nextInt(symbols.length)]));
                    }

                    if (tick % 10 == 0) {
                        File resultFile = new File(getDataFolder().getParentFile().getParentFile(), resultFileName);
                        if (resultFile.exists()) {
                            stopAnimationAndShowResult(player, frames, resultFile, dropper);
                            this.cancel();
                        }
                    }

                    if (tick > 200) {
                        isSpinning = false;
                        this.cancel();
                    }
                }
            }.runTaskTimer(this, 0L, 3L);

        } catch (IOException e) {
            e.printStackTrace();
            isSpinning = false;
        }
    }

    private void stopAnimationAndShowResult(Player player, List<ItemFrame> frames, File resultFile, Dropper dropper) {
        try {
            String content = Files.readString(resultFile.toPath()).trim();
            String[] parts = content.split(",");

            for (int i = 0; i < 3; i++) {
                final int index = i;
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    Material resultMat = Material.valueOf(parts[index]);
                    frames.get(index).setItem(new ItemStack(resultMat));
                    player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1f, 1.2f);

                    if (index == 2) {
                        finalizeGame(player, parts[3], dropper);
                    }
                }, i * 10L);
            }

            resultFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void finalizeGame(Player player, String outcome, Dropper dropper) {
        isSpinning = false;
        if (outcome.equals("WIN")) {
            player.sendTitle(ChatColor.GOLD + "★ WYGRANA ★", "Wypłata z automatu!", 5, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            dispenseReward(dropper, 5);
        } else {
            player.sendMessage(ChatColor.GRAY + "Spróbuj ponownie");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 0.5f);
        }
    }

    private void dispenseReward(Dropper dropper, int amount) {
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= amount) {
                    this.cancel();
                    return;
                }
                dropper.drop();
                count++;
            }
        }.runTaskTimer(this, 0L, 5L);
    }
}