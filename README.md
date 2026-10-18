# DeepSea

Mod Fabric pentru **Hypixel SkyBlock** care îți arată un **pătrat colorat + numele creaturii** în colțul ecranului atunci când prinzi un **Sea Creature**, și îl ține pe ecran **până omori tot ce a ieșit din apă** (inclusiv la Double Hook).

Făcut pentru **Minecraft 26.1.2** (Fabric, Java 25).

## Ce face

- detectează **exact** ce sea creature ai prins, citind mesajul din chat (aceleași mesaje pe care le folosește și SkyHanni);
- **89 de creaturi** (apă, lavă, hotspot-uri, insule noi);
- **culoare proprie** pentru fiecare creatură;
- **pătrat + nume**, ambele mutate cu mouse-ul oriunde pe ecran;
- **Double Hook**: dacă prinzi 2 creaturi, pătratul rămâne până mor amândouă;
- **mod per creatură**:
  - `MOB` — pătratul stă cât timp creatura e în viață, apoi încă `+X secunde` după ce moare;
  - `TIM` — pătratul stă fix `X secunde`, indiferent de mob;
- **după moarte** (`+Xs`) configurabil per creatură — util la monștrii care se spawnează în mai multe bucăți (ex. Magma Slug → Baby Magma Slug);
- totul se salvează în `config/deepsea.json`.

## Instalare

1. Fabric Loader pentru Minecraft **26.1.2** + **Java 25**.
2. În `mods/` pune:
   - `deepsea-1.1.0.jar`
   - [Fabric API](https://modrinth.com/mod/fabric-api) pentru 26.1.2
3. Pornește jocul. În chat ar trebui să vezi `[DeepSea] activat`.
4. Scrie `/dpsea`.

## Comenzi

| Comandă | Ce face |
|---|---|
| `/dpsea` (sau `/deepsea`) | deschide GUI-ul |
| `/dpsea edit` | mută HUD-ul cu mouse-ul |
| `/dpsea test` | 3 alerte demo, ca să vezi cum arată |
| `/dpsea debug` | câte creaturi/alerte/entități urmărește + unde e configul |
| `/dpsea toggle <nume>` | pornește/oprește o creatură |
| `/dpsea color <#RRGGBB> <nume>` | culoare exactă |

## GUI (`/dpsea`)

Fiecare rând = o creatură. Butoane, de la stânga la dreapta:

| Buton | Înseamnă |
|---|---|
| `[ON] Nume` | click = pornește/oprește alerta |
| `#FFAA00` | click = trece la următoarea culoare din paletă |
| `6.0s` | durata (mod `TIM`, sau fallback dacă mobul nu e găsit) |
| `+1.5s` | cât rămâne pe ecran **după** ce moare mobul |
| `MOB` / `TIM` | modul: până moare mobul / doar pe timp |
| `N` / `-` | arată sau nu numele creaturii |

Sus: căutare + paginare. Jos: `Toate ON`, `Toate OFF`, `Muta HUD`, `Test`, `Gata`.

## Cum știe când a murit mobul

După mesajul de catch, modul caută în ~24 de blocuri în jurul tău entități cu nametag-ul creaturii (Hypixel afișează numele deasupra mobului). Le ține minte după id și verifică la fiecare 3 tick-uri:
- cât timp există cel puțin una → pătratul stă;
- când ultima dispare → începe numărătoarea `+Xs` și apoi dispare;
- dacă nu găsește nicio entitate în 4 secunde (mobul a murit instant / e prea departe) → folosește durata normală.

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
      "showLabel": true,
      "mode": "UNTIL_DEAD",
      "durationMs": 6000,
      "afterDeathMs": 2000
    }
  }
}
```

> `color` este ARGB în format int. Cel mai simplu îl schimbi din GUI sau cu `/dpsea color`.

## Dacă `/dpsea` nu deschide nimic (troubleshooting)

Modul îți scrie acum în chat exact unde se blochează:

1. **Nu apare `[DeepSea] activat` la intrarea pe server** → modul nu e încărcat: verifică
   că `deepsea-1.1.0.jar` e în folderul `mods/` al **instanței Fabric 26.1.2** (nu al altei instanțe) și că ai Fabric API.
2. **Nu apare `[DeepSea] Deschid GUI...` după comandă** → comanda nu e înregistrată;
   încearcă `/deepsea` (alias) și trimite `logs/latest.log`.
3. **Apare `Eroare la GUI: ...`** → trimite-mi textul exact, e o excepție pe care o repar imediat.

## Build

Ai nevoie de **JDK 25**.

### IntelliJ IDEA (recomandat)

1. `File → Open` pe folderul proiectului.
2. IDEA detectează Gradle și descarcă tot (inclusiv Minecraft 26.1.2).
3. Rulează `runClient` sau `Tasks → build`.

### Terminal

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
3. Dacă mesajul există în bază și creatura e pornită → se creează o alertă (2 mobi dacă a fost Double Hook).
4. La fiecare 3 tick-uri se verifică entitățile din jur; alerta trăiește cât trăiesc mobii.
5. `HudElementRegistry` desenează pătratul + numele.

Fără mixin-uri.

## Changelog

### 1.1.1
- deschiderea GUI-ului este amânată pe tick-ul următor (posibil fix pentru „nu se deschide GUI")
- log detaliat (added/init/extractRenderState/removed) ca să putem depana rapid dacă mai apare

### 1.1.0
- pătratul rămâne până mor toți mobii prinși (inclusiv Double Hook)
- mod per creatură: `MOB` (până moare) / `TIM` (pe timp)
- `+Xs` după moarte, per creatură
- lanț de spawn (Magma Slug → Baby Magma Slug)
- GUI extins (culoare, durată, după moarte, mod, nume)
- `[DeepSea] activat` la intrarea pe server + mesaje de eroare în chat
- alias `/deepsea`, comandă `/dpsea debug`

### 1.0.0
- prima versiune

## Credit

- Baza de date de mesaje / rarități / XP este extrasă din [SkyHanni](https://github.com/hannibal002/SkyHanni) (`constants/SeaCreatures.json`) — mulțumiri autorilor.
- Modul în sine: MIT.

## Licență

MIT — vezi [LICENSE](LICENSE).
