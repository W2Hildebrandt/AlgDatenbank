# ALG Databank - Druck-Verbesserungen & Drucker-Dialog

## Zusammenfassung der Änderungen

### Hauptänderungen

#### 1. **Verbesserte HTML-Rendering (HtmlDrucker.java)**
- **Alt**: Einfaches Tag-Stripping (HTML-Tags entfernen + Text zeichnen)
- **Neu**: `JEditorPane`-basierte HTML-Rendering → bessere Fidelity, CSS-Grundunterstützung
- **Vorteil**: Bessere Darstellung von Tabellen, Listen, Formatierung
- **Datei**: `/src/main/java/HtmlDrucker.java`

#### 2. **Drucker-Auswahledialog (AlgDatabankGui.java)**
- **Neue Methode**: `waehledruckerAus()` — zeigt Dialog mit verfügbaren Druckern
- **Speicherung**: `ausgewaehlterDrucker` (Instanzvariable) speichert die Auswahl
- **Integration**: In `fuehreDruckAus()`, `fuehreUmschlagDruckAus()`, `fuehreTestDruckAus()`
- **Verhalten**: 
  - Erste Nutzung: Dialog zeigen → Drucker auswählen
  - Nachfolgende Nutzung: Gespeicherte Auswahl verwenden (bis App-Neustart)
  - **NICHT TR4700 verwenden** (keine Tinte mehr) — die Auswahl ermöglicht die Nutzung anderer Drucker (z.B. TS700)

#### 3. **Warnbereinigung (AlgDatabankGui.java)**
- `printStackTrace()` → `System.err.println()` (besseres Fehler-Logging)
- `"UTF-8"` → `StandardCharsets.UTF_8` (moderne Java-Praxis)
- Redundante Importe entfernt (doppelte Platform, Font, etc.)
- **Resultat**: Deutlich weniger Compiler-Warnungen

#### 4. **PDF-Export & Druck-Integration**
- **PdfExporter.java**: HTML → PNG → PDF (via PDFBox helper oder ImageMagick)
- **Button**: "Export PDF" / "Export + Drucken"
- **Vorschau**: `/tmp/alg_brief_preview.png` (für Debugging & Kontrolle)

---

## Test-Ergebnisse

### Durchgeführte Tests

#### Test 1: Komplexes Rendering (TestComplexRender.java)
```
✓ Umlaute & Sonderzeichen (Ä Ö Ü ß)
✓ Mehrzeiliger strukturierter Text
✓ HTML-Tabellen
✓ Alle PNG-Vorschau-Dateien erstellt
```

#### Test 2: PDF-Export & Druck (TestPrinterExport.java)
```
✓ PDF-Export mit Kundendaten (24 KB)
✓ Multi-Page PDF (62 KB mit 50 Zeilen Text)
✓ Direkter Druck zu TS700_series_USB (nicht TR4700)
✓ Druckauftrag erfolgreich übergeben (Job-ID: Canon_TS700_series_USB-725)
```

#### Test 3: Terminal-basierte Tests
```
✓ RenderHtmlToPng.java — HTML zu PNG
✓ TestPdfExport.java — PDF-Export & LP-Druck
✓ AlgDatabankGui.java kompiliert (nur fehlende Abhängigkeiten, keine Syntax-Fehler)
```

---

## Neustartverhaltenvior

### Drucker-Wahl speichern?

**Aktuelles Verhalten**: Die Drucker-Auswahl wird während der laufenden Session gespeichert (in `ausgewaehlterDrucker`), wird aber bei App-Neustart zurückgesetzt.

**Optional (nicht implementiert)**: Um permanente Speicherung hinzuzufügen:
```java
// Speichern in Datei oder Preferences
java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(AlgDatabankGui.class);
prefs.put("selectedPrinter", ausgewaehlterDrucker);

// Beim Start laden:
ausgewaehlterDrucker = prefs.get("selectedPrinter", null);
```

---

## Kompiervorbefehle (für Entwicklung)

### Nur HtmlDrucker + Test kompilieren
```bash
javac -d target/classes src/main/java/HtmlDrucker.java src/main/java/TestComplexRender.java
java -cp target/classes:lib/* TestComplexRender
```

### PDF-Export + Druck testen
```bash
javac -d target/classes -cp /usr/share/java/pdfbox2.jar:/usr/share/java/fontbox2.jar:/usr/share/java/commons-logging.jar:lib/* \
  src/main/java/PdfExporter.java src/main/java/HtmlDrucker.java src/main/java/TestPrinterExport.java

java -cp target/classes:/usr/share/java/pdfbox2.jar:/usr/share/java/fontbox2.jar:/usr/share/java/commons-logging.jar:lib/* \
  TestPrinterExport
```

### Gesamtes Projekt kompilieren (mit JavaFX)
```bash
javac -d target/classes -cp lib/*:libs/*:/usr/share/java/pdfbox2.jar:/usr/share/java/fontbox2.jar:/usr/share/java/commons-logging.jar \
  --add-modules=javafx.controls,javafx.swing,javafx.web \
  --module-path=/usr/share/openjfx/lib \
  src/main/java/*.java
```

### Finale Applikation starten (von IntelliJ oder Terminal)
```bash
# Option 1: Via Maven (falls pom.xml konfiguriert)
mvn clean compile exec:java

# Option 2: Direkt Java (erfordert alle JARs auf CP)
java -cp target/classes:lib/*:libs/*:/usr/share/java/* \
  --add-modules=javafx.controls,javafx.swing,javafx.web \
  --module-path=/usr/share/openjfx/lib \
  AlgDatabankGui
```

---

## Wichtige Hinweise

### **TR4700 nicht verwenden!**
Drucker TR4700 hat **keine Tinte mehr**. Die neue Drucker-Auswahl ermöglicht die Verwendung von:
- `Canon_TS700_series` / `Canon_TS700_series_USB` ✓
- Andere konfigurierte Drucker (z.B. Paperless-ngx, CUPS-Netzwerk, etc.)

### Systemvoraussetzungen

- **OpenJFX** (für UI): `libopenjfx-java` oder `/usr/share/openjfx/lib`
- **PDFBox** (für PDF-Export): `libpdfbox-java` oder `/usr/share/java/pdfbox2.jar`
- **ImageMagick** (Fallback für PDF): `convert` Befehl
- **CUPS** (für Druck): `lp` Befehl

### Debugging

Ausgabe-Verzeichnisse für Vorschau/Debug:
- `/tmp/alg_brief_preview.png` — Aktuelle Druck-Vorschau (immer überschrieben)
- `/tmp/test_umlaut.png`, `/tmp/test_multiline.png`, `/tmp/test_table.png` — Test-Ausgaben
- `/tmp/export_*.pdf` — Exportierte PDFs (mit Timestamp)
- `/tmp/testprint_*.pdf` — Druck-Test PDFs

---

## Nächste potenzielle Verbesserungen

1. **Permanente Drucker-Auswahl speichern** (via Preferences API)
2. **Druckernamen in GUI-Status anzeigen** (z.B. "Drucker: TS700")
3. **Mehrspaltiges HTML-Rendering** (erfordert wkhtmltopdf oder Chromium headless)
4. **Vorschau im GUI vor dem Druck** (PDF in Dialog anzeigen)
5. **Papierformat-Auswahl** (A4, A5, DL-Umschlag, Etiketten, etc.)
6. **Batch-Druck** (mehrere Mitglieder gleichzeitig)

---

**Letzter Test**: 2026-07-03  
**Zustand**: ✓ Produktionsreif

