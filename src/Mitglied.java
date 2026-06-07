public class Mitglied {
    private int mNr;
    private String eintritt;
    private String austritt;
    private String anrede;
    private String titel;
    private String nachname;
    private String vorname;
    private String strasse;
    private String postleitzahl;
    private String ort;
    private String telefon;
    private String email;
    private String hinweise;

    // Konstruktor für die Erstellung eines Objekts
    public Mitglied(int mNr, String eintritt, String austritt, String anrede, String titel,
                    String nachname, String vorname, String strasse, String postleitzahl,
                    String ort, String telefon, String email, String hinweise) {
        this.mNr = mNr;
        this.eintritt = eintritt;
        this.austritt = austritt;
        this.anrede = anrede;
        this.titel = titel;
        this.nachname = nachname;
        this.vorname = vorname;
        this.strasse = strasse;
        this.postleitzahl = postleitzahl;
        this.ort = ort;
        this.telefon = telefon;
        this.email = email;
        this.hinweise = hinweise;
    }

    // 2. DIESEN HIER NEU HINZUFÜGEN:
    public Mitglied() {
        // Bleibt leer. Erlaubt "new Mitglied()"
    }

    // Getter und Setter für alle Felder
    public int getMNr() { return mNr; }
    public void setMNr(int mNr) { this.mNr = mNr; }

    public String getEintritt() { return eintritt; }
    public void setEintritt(String eintritt) { this.eintritt = eintritt; }

    public String getAustritt() { return austritt; }
    public void setAustritt(String austritt) { this.austritt = austritt; }

    public String getAnrede() { return anrede; }
    public void setAnrede(String anrede) { this.anrede = anrede; }

    public String getTitel() { return titel; }
    public void setTitel(String titel) { this.titel = titel; }

    public String getNachname() { return nachname; }
    public void setNachname(String nachname) { this.nachname = nachname; }

    public String getVorname() { return vorname; }
    public void setVorname(String vorname) { this.vorname = vorname; }

    public String getStrasse() { return strasse; }
    public void setStrasse(String strasse) { this.strasse = strasse; }

    public String getPostleitzahl() { return postleitzahl; }
    public void setPostleitzahl(String postleitzahl) { this.postleitzahl = postleitzahl; }

    public String getOrt() { return ort; }
    public void setOrt(String ort) { this.ort = ort; }

    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHinweise() { return hinweise; }
    public void setHinweise(String hinweise) { this.hinweise = hinweise; }
}