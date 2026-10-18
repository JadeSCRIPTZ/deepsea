# DeepSea

Mod Fabric pentru **Hypixel SkyBlock** care îți arată un **pătrat colorat + numele creaturii** în colțul ecranului atunci când prinzi un **Sea Creature**.

Făcut pentru **Minecraft 26.1.2** (Fabric, Java 25).

## Ce face

- detectează **exact** ce sea creature ai prins, citind mesajul din chat (aceleași mesaje pe care le folosește și SkyHanni);
- afișează un **pătrat colorat** în colțul ales de tine, plus numele creaturii;
- **selectezi** pentru care creaturi apare (89 de creaturi: apă, lavă, hotspot-uri, insule noi);
- **culoare proprie** pentru fiecare creatură;
- **durată proprie** (2s – 15s), cu fade out;
- **muți pătratul cu mouse-ul**, oriunde pe ecran;
- totul se salvează în `config/deepsea.json`.

## Instalare

1. Fabric Loader pentru Minecraft **26.1.2** + **Java 25**.
2. În `mods/` pune:
   - `deepsea-1.0.0.jar`
   - [Fabric API](https://modrinth.com/mod/fabric-api) pentru 26.1.2
3. Pornește jocul. În chat: `/dpsea`.

## Comenzi

| Comandă | Ce face |
|---|---|
| `/dpsea` | deschide GUI-ul (listă căutabilă, ON/OFF, culoare, durată, nume) |
| `/dpsea edit` | modul de mutare a HUD-ului cu mouse-ul |
| `/dpsea test` | afișează 3 alerte demo (ca să vezi cum arată) |
| `/dpsea toggle <nume>` | pornește/oprește o creatură (ex. `/dpsea toggle Lord Jawbus`) |
| `/dpsea color <#RRGGBB> <nume>` | setează culoarea exactă (ex. `/dpsea color #FFAA00 Thunder`) |

## Cum se folosește (3 pași)

1. `/dpsea`
2. Caută creatura, dă click pe nume până apare `§a[ON]`, alege culoarea și durata.
3. Butonul **Muta HUD** → trage pătratul unde vrei, apoi **Gata**.

## Config

`config/deepsea.json`:

```json
{
  "x": 10,
  "y": 10,
  "boxSize": 22,
  "gap": 6,
  "maxAlerts": 5,
  "showLabel": true,
  "textShadow": true,
  "creatures": {
    "Thunder": {
      "enabled": true,
      "color": -43691,
      "durationMs": 8000,
      "showLabel": true
    }
  }
}
```

> `color` este ARGB în format int (alpha în byte-ul cel mai semnificativ). Cel mai simplu îl schimbi din GUI sau cu `/dpsea color`.

## Build

Ai nevoie de **JDK 25**.

### Varianta 1 — IntelliJ IDEA (recomandat)

1. `File → Open` pe folderul proiectului.
2. IDEA detectează Gradle și descarcă tot (inclusiv Minecraft 26.1.2).
3. Rulează configurația `runClient` sau `Tasks → build`.

### Varianta 2 — din terminal

```bash
gradle build
```

(proiectul nu are wrapper inclus; folosește Gradle **9.7.1**)

Jar-ul apare în `build/libs/`.

> Dacă Loom se plânge că nu găsește `net.minecraft` / jar-ul e gol, schimbă în
> `gradle.properties` `loom_version=1.18-SNAPSHOT` în `1.17-SNAPSHOT` și rulează din nou.

## Cum funcționează (pe scurt)

1. `ClientReceiveMessageEvents.ALLOW_GAME` prinde fiecare linie de chat.
2. Textul e curățat de coduri `§` și comparat **exact** cu mesajele din `assets/deepsea/sea_creatures.json`.
3. Dacă mesajul există în bază și creatura e pornită în config → se creează o alertă.
4. `HudElementRegistry` desenează pătratul + numele, la poziția din config.

Fără mixin-uri, fără scanare de entități: modul e mic și stabil.

## Status

- ✅ Compilat cu succes împotriva **Minecraft 26.1.2** (client jar real) și **Fabric API 0.155.3+26.1.2**.
- ⚠️ Nu a fost rulat încă în joc (mediul de dezvoltare nu are GPU/sesiune Minecraft). Dacă apare o eroare la lansare, trimite `latest.log`.

## Credit

- Baza de date de mesaje / rarități / XP este extrasă din [SkyHanni](https://github.com/hannibal002/SkyHanni) (`constants/SeaCreatures.json`) — mulțumiri autorilor.
- Modul în sine: MIT.

## Licență

MIT — vezi [LICENSE](LICENSE).
