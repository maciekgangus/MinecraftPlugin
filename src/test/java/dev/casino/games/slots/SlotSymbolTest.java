package dev.casino.games.slots;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static dev.casino.games.slots.SlotSymbol.*;
import static org.junit.jupiter.api.Assertions.*;

class SlotSymbolTest {

    @Test void threeOfAKindDiamond() {
        assertEquals(100, evaluate(new SlotSymbol[]{DIAMOND, DIAMOND, DIAMOND}, 5));
    }

    @Test void threeOfAKindCoalRefund() {
        assertEquals(3, evaluate(new SlotSymbol[]{COAL, COAL, COAL}, 3));
    }

    @Test void threeOfAKindIronDoubles() {
        assertEquals(10, evaluate(new SlotSymbol[]{IRON, IRON, IRON}, 5));
    }

    @Test void twoOfAKindGoldPays() {
        assertEquals(4, evaluate(new SlotSymbol[]{GOLD, GOLD, COAL}, 2));
    }

    @Test void twoOfAKindEmeraldRefund() {
        assertEquals(1, evaluate(new SlotSymbol[]{EMERALD, COAL, EMERALD}, 1));
    }

    @Test void twoOfAKindDiamondOnPositions1And3() {
        assertEquals(15, evaluate(new SlotSymbol[]{DIAMOND, GOLD, DIAMOND}, 5));
    }

    @Test void twoOfAKindIronNoPay() {
        assertEquals(0, evaluate(new SlotSymbol[]{IRON, IRON, COAL}, 5));
    }

    @Test void twoOfAKindCoalNoPay() {
        assertEquals(0, evaluate(new SlotSymbol[]{COAL, EMERALD, COAL}, 10));
    }

    @Test void noMatch() {
        assertEquals(0, evaluate(new SlotSymbol[]{DIAMOND, GOLD, EMERALD}, 10));
    }

    @Test void drawReturnsValidSymbol() {
        assertNotNull(draw(new Random(42)));
    }

    @Test void drawDistributionFavoursCheaperSymbols() {
        Random rng = new Random(0);
        int diamonds = 0, coal = 0;
        for (int i = 0; i < 10_000; i++) {
            SlotSymbol s = draw(rng);
            if (s == DIAMOND) diamonds++;
            if (s == COAL) coal++;
        }
        assertTrue(coal > diamonds * 3, "Coal should appear far more often than diamond");
    }
}
