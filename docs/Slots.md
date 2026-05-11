# Automat do Gier (Slot Machine)

Rozszerzenie do głównego pluginu kasyna (`CasinoPlugin`), wprowadzające w pełni funkcjonalny, animowany automat do gier. Moduł ten opiera się na architekturze rozproszonej – interfejs obsługiwany jest przez serwer Minecraft (Java), natomiast główna logika biznesowa i generator liczb losowych działają jako zewnętrzny skrypt w Pythonie.

## Główne funkcje
* **Wizualna animacja bębnów:** Kręcące się symbole w ramkach na przedmioty. Zatrzymywanie odbywa się sekwencyjnie (od lewej do prawej).
* **Zewnętrzny Backend (Python):** Decyzja o wygranej zapada poza silnikiem Minecrafta - pozwala to na łatwą rozbudowę o dodatkowe funkcje, uczenie maszynowe czy bazę danych bez obciążania serwera gry.
* **Zintegrowana Ekonomia:** Automat automatycznie pobiera opłatę za grę (Złote Samorodki) i fizycznie podaje nagrodę z podajnika (Droppera).
* **Zabezpieczenie przed kradzieżą (Anti-Theft):** Zwykli gracze nie mogą otworzyć podajnika z nagrodami. Skarbiec jest zablokowany przez plugin.
* **Tryb Administratora (Bypass):** Operatorzy serwera (OP) mogą łatwo uzupełnić nagrody, klikając prawym przyciskiem myszy na podajnik, trzymając jednocześnie klawisz kucania (`Shift`).

---

## Architektura 

Komunikacja między Javą a Pythonem odbywa się asynchronicznie za pomocą systemu monitorowania plików lokalnych:
1. Gracz klika przycisk. Java pobiera opłatę i tworzy plik `spin.txt` w głównym katalogu serwera, po czym uruchamia pętlę animacji.
2. Skrypt w Pythonie wykrywa plik `spin.txt`, usuwa go, przeprowadza losowanie i zapisuje wynik do pliku `result.txt`.
3. Java cały czas nasłuchuje. Gdy wykryje plik `result.txt`, odczytuje go, zatrzymuje animację na wylosowanych symbolach i wydaje nagrodę.

---

## Wymagania Techniczne
* **Serwer:** Paper / Spigot w wersji **1.21.4**
* **Java:** JDK 21
* **Python:** Wersja 3.8 lub nowsza
* Narzędzie `screen` na serwerze Linux - do działania backendu w tle.

---

## Budowa automatu w grze

Aby automat działał, musi zostać prawidłowo zbudowany w świecie gry. Kod dynamicznie wykrywa otoczenie - nie trzeba dzięki temu na sztywno wpisywać koordynatów.

**Instrukcja budowy:**
1. Postaw ścianę z dowolnych bloków.
2. Powieś obok siebie **3 Ramki na przedmioty** (Item Frames).
3. Pod środkową ramką umieść **Kamienny Przycisk** (Stone Button).
4. W promieniu 2 bloków od przycisku postaw **Podajnik** (Dropper) skierowany "twarzą" do gracza.
5. Uzupełnij Dropper nagrodami (wymaga uprawnień OP + użycia Shift+Kliknięcia).

---

## Instrukcja Wdrożenia (Deployment)

Zanim gracze będą mogli zagrać, należy wgrać i uruchomić oba komponenty na serwerze fizycznym.

### 1. Kompilacja Pluginu (Java)
Projekt jest zintegrowany z Gradle. W celu zbudowania paczki:
1. Uruchom zadanie `shadowJar` (w IntelliJ: zakładka Gradle -> Tasks -> shadow -> shadowJar).
2. Wygenerowany plik `.jar` (z folderu `build/libs`) przerzuć do folderu `plugins/` na serwerze Minecraft.

### 2. Uruchomienie Backendu (Python)
Plik `casino_backend.py` **musi** znajdować się w głównym katalogu serwera (tam, gdzie leży plik `server.jar`).

Aby uruchomić skrypt tak, by działał po zamknięciu konsoli:
```bash
# 1. Przejdź do głównego folderu serwera
cd /sciezka/do/serwera/minecraft/

# 2. Utwórz nową wirtualną sesję za pomocą programu screen
screen -S kasyno_python

# 3. Uruchom skrypt
python3 casino_backend.py

# 4. Odepnij sesję (skrypt będzie działał w tle)
# Wciśnij skrót klawiszowy: Ctrl + A, a następnie puść i wciśnij klawisz D.
```

## Konfiguracja 

Zmiany waluty, kosztu gry lub nagrody można dokonać modyfikując zmienne na początku klasy CasinoBridge.java:
```java
// Koszt gry i waluta
private final Material currencyType = Material.GOLD_NUGGET;
private final int costPerSpin = 1;

// Zestaw symboli, które losuje maszyna (muszą pokrywać się ze skryptem w Pythonie)
private final Material[] symbols = {Material.DIAMOND, Material.GOLD_INGOT, Material.IRON_INGOT, Material.DIRT};
```

Po dodaniu nowych symboli do Javy trzeba je również dodać do listy SYMBOLS w pliku casino_backend.py.


