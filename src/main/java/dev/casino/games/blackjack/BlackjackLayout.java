package dev.casino.games.blackjack;

public final class BlackjackLayout {

    // Betting Screen Slots
    public static final int BET_DECREASE = 20;
    public static final int BET_DISPLAY = 22;
    public static final int BET_INCREASE = 24;
    public static final int BET_DEAL = 40;
    public static final int RULE_BOOK = 48;

    // Game Screen Slots
    public static final int GAME_HIT = 39;
    public static final int GAME_STAND = 41;
    public static final int GAME_BET_DISPLAY = 49;
    public static final int GAME_PLAY_AGAIN = 50;
    public static final int GAME_QUIT = 53;

    // Hand Row Starts
    public static final int DEALER_HAND_START = 0;
    public static final int PLAYER_HAND_START = 18;

    private static final int[][] SLOT_PATTERNS = {
            {},
            {4},
            {3, 5},
            {2, 4, 6},
            {1, 3, 5, 7},
            {0, 2, 4, 6, 8},
            {0, 1, 3, 5, 7, 8},
            {0, 1, 3, 4, 5, 7, 8},
            {0, 1, 2, 3, 5, 6, 7, 8},
            {0, 1, 2, 3, 4, 5, 6, 7, 8}
    };

    public static int getCardSlot(int rowStart, int totalInRow, int indexInRow) {
        if (totalInRow <= 0 || totalInRow >= SLOT_PATTERNS.length) {
            return rowStart + indexInRow;
        }
        return rowStart + SLOT_PATTERNS[totalInRow][indexInRow];
    }

    private BlackjackLayout() { }
}
