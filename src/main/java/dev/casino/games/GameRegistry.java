package dev.casino.games;

import java.util.Collection;
import java.util.Optional;

/** Registry of all casino games available in this session. */
public interface GameRegistry {

    /** Registers a game. Duplicate names overwrite the previous entry. */
    void register(CasinoGame game);

    /** Returns all registered games in insertion order. */
    Collection<CasinoGame> getAll();

    /** Finds a game by its {@link CasinoGame#getName()} key. */
    Optional<CasinoGame> findByName(String name);
}
