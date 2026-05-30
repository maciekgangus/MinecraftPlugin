# Integration Tests

## Overview

The integration tests in `AllGamesIntegrationTest` verify that all casino games work together through the same registration path used at plugin startup. The tests exersise the shared main menu flow. 

## What Is Covered

| Test | Purpose |
|------|---------|
| `gameRegistrarRegistersAllGames` | Confirms every game from `GameRegistrar` is present and in the expected order. |
| `everyRegisteredGameIsFindableByName` | Ensures each game can be looked up in the registry by its snake_case key. |
| `mainMenuShowsAllGameIconsInRegistrationOrder` | Checks that the main casino menu displays the correct icon for each game. |
| `openMainCasinoMenuViaContextShowsAllGames` | Verifies `PluginContext.openMainCasinoMenu()` opens the menu with all games visible. |
| `clickingHorseRacingOpensDerbyGui` | Clicks slot 0 and confirms the Virtual Derby GUI opens. |
| `clickingBlackjackOpensBettingGui` | Clicks slot 1 and confirms the Blackjack betting screen opens. |
| `clickingSlotsOpensSlotMachineGui` | Clicks slot 2 and confirms the Slot Machine GUI opens. |

## Running the Tests

From the project root:sfs

```bash
./gradlew test --tests dev.casino.games.AllGamesIntegrationTest
```

To run the full test suite:

```bash
./gradlew test
```

## Adding a New Game

When a new game is registered in `GameRegistrar`, update `AllGamesIntegrationTest` as well:

1. Add the game's registry key to `EXPECTED_GAME_NAMES`.
2. Add an assertion for its icon in the main-menu tests.
3. Add a click test that opens the new game's GUI from the main menu.

This keeps the integration test in sync with the single source of truth for game registration.
