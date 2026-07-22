Änderungsübersicht — Vorschau/Editor WebView Verbesserungen

Datum: 2026-07-16

Betroffene Datei:
- src/main/java/AlgDatabankGui.java

Kurzbeschreibung der Änderungen:

1) Trennung der WebView-Instanzen
- Einführung einer separaten WebView für den Template-Editor: `editorWebView`.
- Die Vorschau bleibt in `vorschauWebView`.

2) Vorschau-WebView: A4-Basis und initialer Inhalt
- `vorschauWebView.setPrefSize(595, 842)` und `setMinSize(595,842)` gesetzt.
- Initiales Laden eines vorbereiteten leeren A4-HTML (mittels `prepareHtmlForPreview`) statt Debug-HTML.

3) Post-Load-Injektion und Zoom-Anpassung (Preview)
- Nach dem Laden einer Seite injiziert die Vorschau-Engine ein CSS-Element (mit `!important`), das `body` auf 595×842 und `overflow:hidden` setzt.
- Nach dem Laden wird außerdem der Zoom des WebView so gesetzt, dass die Seite in das verfügbare JFXPanel passt.

4) Template-Editor: eigener WebView + designMode nach Ladeende
- `editorWebView` wird initialisiert und per `webEngine.getLoadWorker().stateProperty()` wird nach `SUCCEEDED` `document.designMode='on'` gesetzt.

5) Robustere HTML-Wrapping-Logik
- `prepareHtmlForPreview(String html)` injiziert CSS in vorhandene `<head>`-Elemente oder ergänzt fehlende `<head>`/`<html>`-Struktur.
- Ziel: Reduzierung innerer Scrollbars durch durchsetzbare Stile.

6) Kleinere Lint/Cleanup-Änderungen
- Entfernen einiger ungenutzter Imports und eines unbenutzten Members (`rootNode`), Anpassung einer Regex für Dateinamen-Sanitize.

Anleitung zum Testen

1) In der IDE: Datei `AlgDatabankGui` ausführen (main) und die GUI starten.
2) In Tab 1 eine oder mehrere Mitglieder auswählen.
3) Tab "Brief- & Druckzentrum" öffnen: die Vorschau sollte die komplette A4-Seite (visuell) ohne inneren Scrollbalken zeigen.
4) Fenstergröße ändern und "Ganze Seite" bzw. Zoom-Slider testen.

Hinweise / verbleibende Warnings
- Es existieren noch mehrere Compiler-Warnungen (unused imports, ungenutzte Felder, printStackTrace-Aufrufe). Sie sind keine Kompilationsfehler, können aber in einem separaten Schritt entfernt werden.

Wenn du möchtest, kann ich im nächsten Schritt die verbliebenen Warnings (printStackTrace → Logger, unnötige Felder/Imports) entfernen und einen sauberen Commit-ready Patch (git diff) erzeugen.

---
Ende der Zusammenfassung

