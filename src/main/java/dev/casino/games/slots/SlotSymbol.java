package dev.casino.games.slots;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public enum SlotSymbol {
    DIAMOND(Material.DIAMOND,    5,  20, 3),
    GOLD   (Material.GOLD_INGOT, 15,  8, 2),
    EMERALD(Material.EMERALD,    20,  4, 1),
    IRON   (Material.IRON_INGOT, 25,  2, 0),
    COAL   (Material.COAL,       35,  1, 0);

    private final Material material;
    private final int weight;
    private final int threeMultiplier;
    private final int twoMultiplier;

    private static final List<SlotSymbol> POOL;

    static {
        List<SlotSymbol> pool = new ArrayList<>();
        for (SlotSymbol s : values()) {
            for (int i = 0; i < s.weight; i++) pool.add(s);
        }
        POOL = List.copyOf(pool);
    }

    SlotSymbol(Material material, int weight, int threeMultiplier, int twoMultiplier) {
        this.material        = material;
        this.weight          = weight;
        this.threeMultiplier = threeMultiplier;
        this.twoMultiplier   = twoMultiplier;
    }

    public Material getMaterial() { return material; }

    public static SlotSymbol draw(Random rng) {
        return POOL.get(rng.nextInt(POOL.size()));
    }

    /** Returns total payout gold for given 3-reel result and bet amount. 0 = loss. */
    public static int evaluate(SlotSymbol[] reels, int bet) {
        if (reels[0] == reels[1] && reels[1] == reels[2]) {
            return bet * reels[0].threeMultiplier;
        }
        SlotSymbol pair = findPair(reels);
        if (pair != null && pair.twoMultiplier > 0) {
            return bet * pair.twoMultiplier;
        }
        return 0;
    }

    private static SlotSymbol findPair(SlotSymbol[] reels) {
        if (reels[0] == reels[1]) return reels[0];
        if (reels[1] == reels[2]) return reels[1];
        if (reels[0] == reels[2]) return reels[0];
        return null;
    }
}
