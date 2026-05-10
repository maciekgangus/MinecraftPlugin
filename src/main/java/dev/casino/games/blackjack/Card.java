package dev.casino.games.blackjack;

import org.bukkit.Material;

public enum Card {
    TWO(2, Material.COAL_BLOCK, "2"),
    THREE(3, Material.COPPER_BLOCK, "3"),
    FOUR(4, Material.IRON_BLOCK, "4"),
    FIVE(5, Material.LAPIS_BLOCK, "5"),
    SIX(6, Material.REDSTONE_BLOCK, "6"),
    SEVEN(7, Material.NETHER_QUARTZ_ORE, "7"),
    EIGHT(8, Material.GOLD_BLOCK, "8"),
    NINE(9, Material.EMERALD_BLOCK, "9"),
    TEN(10, Material.DIAMOND, "10"),
    JACK(10, Material.DEEPSLATE_DIAMOND_ORE, "Jack (10)"),
    QUEEN(10, Material.DIAMOND_ORE, "Queen (10)"),
    KING(10, Material.DIAMOND_BLOCK, "King (10)"),
    ACE(11, Material.NETHERITE_BLOCK, "Ace (11 or 1)");

    private final int value;
    private final Material material;
    private final String displayName;

    Card(int value, Material material, String displayName) {
        this.value = value;
        this.material = material;
        this.displayName = displayName;
    }

    public int getValue() {
        return value;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }
}
