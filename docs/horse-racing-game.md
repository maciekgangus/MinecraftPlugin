# Horse Racing Game Documentation

## Overview

`HorseRacingGame` is a chest-GUI casino game registered in the main `/casino` menu.
Players pick one horse, pay a gold cost, watch a short race animation, and receive a payout if their horse wins.

## Player Flow

1. Run `/casino`.
2. Click `Virtual Derby`.
3. Pick a horse in the betting GUI.
4. Watch the race.
5. After finish, the betting GUI opens again.
6. Click `Back to all games` to return to the root casino menu.

## Betting Options

The game currently has three horses with different risk profiles:

- `Diamond Flash`: cost `3` gold, payout `7` gold, move chance per tick `75%`
- `Golden Horseshoe`: cost `2` gold, payout `6` gold, move chance per tick `55%`
- `Iron Hoof`: cost `1` gold, payout `5` gold, move chance per tick `35%`

Higher cost horses are configured with higher chance to advance each race tick.

## Race Mechanics

- The race runs in a 3-row, 9-column inventory.
- Each horse starts in column 1 of its row.
- On each timer tick, each horse advances by one slot based on its configured move chance.
- First horse that reaches column 9 wins.
- If the player selected the winner, payout is deposited through `EconomyManager`.

## Economy Integration

- All costs and payouts use `EconomyManager`.
- Cost is withdrawn before race start.
- Payout is deposited only when the selected horse wins.

## Main Classes

- Game implementation: `src/main/java/dev/casino/games/derby/HorseRacingGame.java`
- Registration: `src/main/java/dev/casino/games/GameRegistrar.java`
- Main menu return helper: `PluginContext#openMainCasinoMenu(Player)`
- Tests: `src/test/java/dev/casino/games/derby/HorseRacingGameTest.java`

