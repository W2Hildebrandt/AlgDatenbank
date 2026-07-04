# ALG Databank – Konkreter Probedruck

## Schnellstart (sauberer Klassenpfad)

Die Klassenpfad-Aufloesung ist zentralisiert in:
- `tools/classpath.sh` (baut den Classpath)
- `tools/alg.sh` (compile/run Wrapper)

Projekt kompilieren:

```bash
cd /home/wolfram/JB-Java_Projekte/Alg_Databank
./tools/alg.sh compile
```

Einfachstarter (kompilieren + starten in einem Schritt):

```bash
cd /home/wolfram/JB-Java_Projekte/Alg_Databank
./start.sh
```

Ohne Neukompilierung starten:

```bash
cd /home/wolfram/JB-Java_Projekte/Alg_Databank
./start.sh --skip-compile AlgDatabankGui
```

GUI starten:

```bash
cd /home/wolfram/JB-Java_Projekte/Alg_Databank
./tools/alg.sh run AlgDatabankGui
```

Probedruck starten:

```bash
cd /home/wolfram/JB-Java_Projekte/Alg_Databank
./tools/alg.sh run KonkreterProbedruck
```

Nur den berechneten Klassenpfad ausgeben:

```bash
cd /home/wolfram/JB-Java_Projekte/Alg_Databank
./tools/alg.sh classpath
```

Optionale Flags:
- `INCLUDE_DISABLED_LIBS=1` nimmt auch `lib_disabled/` auf.
- `INCLUDE_SYSTEM_JARS=1` sucht zusaetzlich `log4j-api/core` in `/usr/share/java`.

Hinweis: Wenn zur Laufzeit `NoClassDefFoundError: org/apache/logging/log4j/LogManager` erscheint, fehlt `log4j-api` als JAR im Klassenpfad (aktuell nicht im Repo enthalten).

Aktuell verwendet:
- `lib/log4j-api-2.23.1.jar`
- `lib/log4j-core-2.23.1.jar`

Schnell pruefen:

```bash
cd /home/wolfram/JB-Java_Projekte/Alg_Databank
ls -l lib/log4j-api-2.23.1.jar lib/log4j-core-2.23.1.jar
```

## Zweck

Der Probedruck verwendet die Vorlage `src/main/resources/templates/algorithmus1.html` mit konkreten Beispieldaten und erzeugt entweder:
- eine PNG-Vorschau unter `/tmp/alg_konkreter_probedruck_preview.png`
- oder einen echten Druckauftrag an den gewählten Drucker.

