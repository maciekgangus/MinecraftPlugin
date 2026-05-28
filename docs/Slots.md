# Slot Machine Game Documentation

## Overview

`SlotsGame` is a chest-GUI casino game registered in the main `/casino` menu.
It features a distributed architecture: the Minecraft server (Java) handles the user interface and economy, while the core business logic, RNG (Random Number Generation), and outcome evaluation are processed by an external Python backend script.

## Player Flow

1. Run `/casino`.
2. Click `Slot Machine`.
3. **Spinning:** Click the `Lever` icon to pay the entry cost and start the machine.
4. **Animation:** Watch the three reels (slots 11, 13, and 15) rapidly change symbols.
5. **Resolution:** The reels stop sequentially. If all three symbols match, the player wins a jackpot.
6. Click the `Barrier` icon to return to the root casino menu.

## Game Mechanics & Architecture

- **The Interface:** The game runs in a 3-row, 9-column inventory. The reels are positioned in the center row.
- **Asynchronous Python Bridge:** - When a player spins, Java creates a `spin.txt` file in the server's root directory.
    - The Python script (`casino_backend.py`) continuously monitors the folder, detects the file, deletes it, calculates the outcome, and writes the result to `result.txt`.
    - Java reads `result.txt`, applies the visual stopping animation, and processes the outcome.
- **Concurrency Lock:** A global lock (`globalSpinLock`) is implemented to prevent players from triggering multiple spins at the exact same millisecond, ensuring the file-based communication remains stable.

## Economy Integration

- All costs and payouts use the shared `EconomyManager`.
- **Cost:** `1` Gold Ingot (withdrawn immediately upon clicking the Lever, before the animation starts).
- **Payout:** `5` Gold Ingots (deposited only if the `result.txt` yields a `WIN` outcome).
- The machine automatically blocks spins if the player has insufficient funds.

## Statistics & Persistence

- **Tracking:** Every slot spin and jackpot is automatically recorded.
- **Persistence:** Stats are saved asynchronously to `stats.yml` immediately after each spin.
- **Leaderboard:** Use `/leaderboard` to see who the Slot Machine champions are.

## Deployment & Requirements

Because of the distributed architecture, this game requires the Python backend to be running on the host machine alongside the Minecraft server.

1. **Python Script Placement:** Place `casino_backend.py` in the server's root directory (the same folder as `server.jar`).
2. **Running the Backend:** Use a terminal multiplexer (like `screen` or `tmux`) to keep the script running in the background.
    ```bash
    # Create a background session
    screen -S casino_python
    
    # Run the script
    python3 casino_backend.py
    
    # Detach from the session (Leave it running): Ctrl + A, then D
    ```

## Main Classes & Files

- **Game Implementation:** `src/main/java/dev/casino/games/slots/SlotsGame.java`
- **Backend Logic:** `casino_backend.py` (Located in server root)
- **Registration:** `src/main/java/dev/casino/games/GameRegistrar.java`
- **Main menu return helper:** `PluginContext#openMainCasinoMenu(Player)`