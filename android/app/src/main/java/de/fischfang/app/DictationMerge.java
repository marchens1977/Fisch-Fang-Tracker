package de.fischfang.app;

import java.util.Locale;

/**
 * Hilfsregeln für das Dauer-Diktat (ohne Android-Teile, damit sie sich leicht testen lassen).
 * Geprüft von test/DiktatTest.java (läuft in test/pruefung.py mit).
 */
final class DictationMerge {
    private DictationMerge() { }

    /** true, wenn das neue Zwischenergebnis nicht die Fortsetzung des alten ist, sondern neu beginnt. */
    static boolean isRestart(String oldText, String newText) {
        String[] a = words(oldText), b = words(newText);
        if (a.length == 0 || b.length == 0) return false;
        if (!similar(a[0], b[0])) return true; // ganz anderer Anfang = neuer Abschnitt
        // Gleicher Anfang, aber plötzlich viel kürzer (z. B. „Zander“ noch einmal gesagt)
        return a.length >= 3 && b.length * 2 < a.length;
    }

    /** Schneidet den schon gesicherten Text vorne vom Endergebnis ab, falls er dort steht. */
    static String stripCommitted(String committed, String fin) {
        String c = norm(committed), f = norm(fin);
        if (c.isEmpty() || f.isEmpty()) return fin == null ? "" : fin.trim();
        if (f.equals(c)) return "";
        if (f.startsWith(c + " ")) {
            String[] all = fin.trim().split("\\s+");
            int skip = words(committed).length;
            StringBuilder r = new StringBuilder();
            for (int i = skip; i < all.length; i++) { if (r.length() > 0) r.append(' '); r.append(all[i]); }
            return r.toString();
        }
        return fin.trim();
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.GERMAN).replaceAll("\\s+", " ");
    }

    private static String[] words(String s) {
        String n = norm(s);
        return n.isEmpty() ? new String[0] : n.split(" ");
    }

    /** Ähnliche Wörter (z. B. „sander“/„zander“) gelten als gleich – das ist nur eine Korrektur. */
    static boolean similar(String x, String y) {
        if (x.equals(y)) return true;
        int d = distance(x, y);
        return d * 2 < Math.max(x.length(), y.length());
    }

    private static int distance(String x, String y) {
        int[] prev = new int[y.length() + 1], cur = new int[y.length() + 1];
        for (int j = 0; j <= y.length(); j++) prev[j] = j;
        for (int i = 1; i <= x.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= y.length(); j++) {
                int c = x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + c);
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[y.length()];
    }
}
