import os
import random
import time
from typing import Tuple

# Konfiguracja
# SYMBOLS = ["DIAMOND", "GOLD_INGOT", "IRON_INGOT", "DIRT"]
SYMBOLS = ["DIAMOND", "GOLD_INGOT"]
TRIGGER_FILE = "spin.txt"
RESULT_FILE = "result.txt"

def spin_slot_machine() -> Tuple[str, str, str, str]:
    """
    Symuluje pociągnięcie dźwigni w automacie.
    Zwraca krotkę z trzema symbolami oraz ostatecznym wynikiem (WIN/LOSE).
    """
    slots = [random.choice(SYMBOLS) for _ in range(3)]
    
    if slots[0] == slots[1] and slots[1] == slots[2]:
        outcome = "WIN"
    else:
        outcome = "LOSE"
        
    return slots[0], slots[1], slots[2], outcome

def main() -> None:
    print("Backend kasyna uruchomiony pomyślnie. Nasłuchuję poleceń z Javy...")
    
    # Pętla główna
    while True:
        if os.path.exists(TRIGGER_FILE):
            print("\n[EVENT] Wykryto żądanie gry (spin.txt). Uruchamiam losowanie...")
            
            try:
                os.remove(TRIGGER_FILE)
            except OSError as e:
                print(f"[ERROR] Nie można usunąć pliku triggera: {e}")
                continue

            # Losowanie
            s1, s2, s3, outcome = spin_slot_machine()

            # Zapisanie wyniku dla Javy
            with open(RESULT_FILE, "w") as file:
                file.write(f"{s1},{s2},{s3},{outcome}")
                
            print(f"[RESULT] {s1} | {s2} | {s3} -> {outcome}")
            print("Zapisano do result.txt. Oczekuję na kolejną grę...")
            
        time.sleep(0.5)

if __name__ == "__main__":
    main()