package dev.casino.games;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Map-based {@link GameRegistry} that preserves registration order. */
public final class DefaultGameRegistry implements GameRegistry {

    private final Map<String, CasinoGame> games = new LinkedHashMap<>();

    @Override
    public void register(CasinoGame game) {
        games.put(game.getName(), game);
    }

    @Override
    public Collection<CasinoGame> getAll() {
        return Collections.unmodifiableCollection(games.values());
    }

    @Override
    public Optional<CasinoGame> findByName(String name) {
        return Optional.ofNullable(games.get(name));
    }
}
