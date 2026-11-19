package de.fischfang.app;

/**
 * Prüft die Regeln des Dauer-Diktats (DictationMerge) mit nachgespielten Spracherkennungs-Abläufen.
 * Start: javac -d /tmp/diktat android/app/src/main/java/de/fischfang/app/DictationMerge.java test/DiktatTest.java
 *        && java -cp /tmp/diktat de.fischfang.app.DiktatTest
 */
public class DiktatTest {
    static int fehler = 0;

    /** Spielt eine Runde nach: Zwischenergebnisse, dann Endergebnis – wie MainActivity. */
    static String runde(String[] partials, String fin) {
        StringBuilder text = new StringBuilder(), committed = new StringBuilder();
        String last = "";
        for (String p : partials) {
            if (DictationMerge.isRestart(last, p)) {
                if (text.length() > 0) text.append(' ');
                text.append(last);
                if (committed.length() > 0) committed.append(' ');
                committed.append(last);
            }
            last = p;
        }
        String f = DictationMerge.stripCommitted(committed.toString(), fin);
        if (f.isEmpty()) f = last;
        if (!f.isEmpty()) { if (text.length() > 0) text.append(' '); text.append(f); }
        return text.toString();
    }

    static void pruefe(String name, String ist, String soll) {
        if (ist.equals(soll)) System.out.println("  OK   " + name);
        else { fehler++; System.out.println("  FEHLER " + name + ": „" + ist + "“ statt „" + soll + "“"); }
    }

    public static void main(String[] a) {
        // Samsung-Fall: nach Pause fängt die Erkennung neu an, Endergebnis = nur letzter Abschnitt
        pruefe("Neubeginn nach Pause, Ende nur letzter Teil",
                runde(new String[]{"Zander", "Zander 65", "Zander 65 Zentimeter", "am", "am Rhein"}, "am Rhein"),
                "Zander 65 Zentimeter am Rhein");
        pruefe("Drei Abschnitte",
                runde(new String[]{"Hecht", "80", "80 Zentimeter", "gefangen", "gefangen mit Gummifisch"},
                        "gefangen mit Gummifisch"),
                "Hecht 80 Zentimeter gefangen mit Gummifisch");
        // Normales Handy: Endergebnis enthält alles, keine Neubeginne
        pruefe("Normaler Ablauf",
                runde(new String[]{"Zander", "Zander 65", "Zander 65 Zentimeter"}, "Zander 65 Zentimeter"),
                "Zander 65 Zentimeter");
        // Endergebnis enthält trotz Neubeginn alles → nichts doppelt
        pruefe("Ende enthält alles, nichts doppelt",
                runde(new String[]{"Zander", "65 Zentimeter"}, "Zander 65 Zentimeter"),
                "Zander 65 Zentimeter");
        // Korrektur des ersten Wortes ist kein Neubeginn
        pruefe("Korrektur Sander → Zander",
                runde(new String[]{"Sander", "Zander 65"}, "Zander 65"),
                "Zander 65");
        // Kein Endergebnis → letztes Zwischenergebnis wird genommen
        pruefe("Leeres Endergebnis",
                runde(new String[]{"Barsch", "Barsch 30"}, ""),
                "Barsch 30");
        pruefe("Gleicher Anfang nochmal gesagt",
                runde(new String[]{"Zander 65 Zentimeter am Rhein", "Zander"}, "Zander"),
                "Zander 65 Zentimeter am Rhein Zander");
        System.out.println(fehler == 0 ? "Diktat-Prüfung bestanden." : fehler + " Diktat-Fehler.");
        System.exit(fehler == 0 ? 0 : 1);
    }
}
