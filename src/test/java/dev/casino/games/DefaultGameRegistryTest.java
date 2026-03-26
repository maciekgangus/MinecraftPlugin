package dev.casino.games;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultGameRegistryTest {

    private DefaultGameRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultGameRegistry();
    }

    @Test
    void emptyRegistryHasNoGames() {
        assertTrue(registry.getAll().isEmpty());
    }

    @Test
    void registerAddsGameToRegistry() {
        registry.register(stub("slots"));
        assertEquals(1, registry.getAll().size());
    }

    @Test
    void findByNameReturnsPresentForRegisteredGame() {
        CasinoGame game = stub("blackjack");
        registry.register(game);
        assertTrue(registry.findByName("blackjack").isPresent());
        assertSame(game, registry.findByName("blackjack").get());
    }

    @Test
    void findByNameReturnsEmptyForUnknownGame() {
        assertTrue(registry.findByName("roulette").isEmpty());
    }

    @Test
    void getAllPreservesInsertionOrder() {
        CasinoGame a = stub("aaa");
        CasinoGame b = stub("bbb");
        CasinoGame c = stub("ccc");
        registry.register(a);
        registry.register(b);
        registry.register(c);
        List<CasinoGame> all = new ArrayList<>(registry.getAll());
        assertSame(a, all.get(0));
        assertSame(b, all.get(1));
        assertSame(c, all.get(2));
    }

    @Test
    void duplicateNameOverwritesPreviousEntry() {
        CasinoGame first  = stub("dupe");
        CasinoGame second = stub("dupe");
        registry.register(first);
        registry.register(second);
        assertEquals(1, registry.getAll().size());
        assertSame(second, registry.findByName("dupe").get());
    }

    @Test
    void getAllIsUnmodifiable() {
        registry.register(stub("x"));
        assertThrows(UnsupportedOperationException.class,
                () -> registry.getAll().clear());
    }

    // --- Helper ---

    private CasinoGame stub(String name) {
        return new CasinoGame() {
            @Override public void start(Player p)           {}
            @Override public void stop(Player p)            {}
            @Override public ItemStack getIcon()            { return null; }
            @Override public String getName()               { return name; }
            @Override public Component getDisplayName()     { return Component.text(name); }
        };
    }
}
