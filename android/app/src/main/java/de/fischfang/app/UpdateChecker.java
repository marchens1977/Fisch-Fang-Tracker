package de.fischfang.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Sucht bei GitHub nach einer neueren Version der App und installiert sie auf Wunsch.
 *
 * Ablauf: GitHub-„Release“ abfragen → Versionsnummer aus dem Namen (app-v1.0.N) mit der eigenen
 * vergleichen → Fenster „Update verfügbar“ → APK herunterladen → Android-Installationsfenster.
 * Wiederverwendbar für andere Apps: nur REPO und APK_NAME anpassen.
 */
public class UpdateChecker {
    static final String REPO = "marchens1977/Fisch-Fang-Tracker";
    static final String APK_NAME = "FischfangTracker.apk";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final long AUTO_CHECK_INTERVAL_MS = 60L * 60L * 1000L; // höchstens 1x pro Stunde von selbst

    private final Activity activity;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean busy = false;
    private boolean installAfterPermission = false;
    private String pendingDownloadUrl;
    private AlertDialog progressDialog;

    UpdateChecker(Activity activity) {
        this.activity = activity;
    }

    /** Eigene Versionsnummer (fortlaufende Bau-Nummer von GitHub). */
    long currentVersion() {
        try {
            PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            return info.getLongVersionCode();
        } catch (Exception e) {
            return 0;
        }
    }

    String currentVersionName() {
        try {
            return activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    /** Beim Start: von selbst nachsehen (nicht öfter als einmal pro Stunde). */
    void autoCheck() {
        SharedPreferences prefs = activity.getSharedPreferences("update", Context.MODE_PRIVATE);
        long last = prefs.getLong("lastCheck", 0);
        if (System.currentTimeMillis() - last < AUTO_CHECK_INTERVAL_MS) return;
        check(false);
    }

    /** Wird nach der Rückkehr aus den Einstellungen („Apps installieren erlauben“) aufgerufen. */
    void onResume() {
        if (installAfterPermission && canInstall()) {
            installAfterPermission = false;
            download(pendingDownloadUrl);
        }
    }

    /** manual = true: auch „Du hast die neueste Version“ bzw. Fehler anzeigen. */
    void check(boolean manual) {
        if (busy) return;
        busy = true;
        if (manual) toast("Suche nach Update …");
        new Thread(() -> {
            try {
                JSONObject release = new JSONObject(readText(API_URL));
                long latest = versionFromTag(release.optString("tag_name", ""));
                String url = findApkUrl(release);
                activity.getSharedPreferences("update", Context.MODE_PRIVATE)
                        .edit().putLong("lastCheck", System.currentTimeMillis()).apply();
                ui.post(() -> {
                    busy = false;
                    if (latest > currentVersion() && url != null) {
                        showUpdateDialog("1.0." + latest, url);
                    } else if (manual) {
                        toast("Du hast schon die neueste Version (" + currentVersionName() + ").");
                    }
                });
            } catch (Exception e) {
                ui.post(() -> {
                    busy = false;
                    if (manual) toast("Update-Suche nicht möglich. Bitte Internet prüfen.");
                });
            }
        }).start();
    }

    static long versionFromTag(String tag) {
        // "app-v1.0.7" -> 7
        int dot = tag.lastIndexOf('.');
        if (dot < 0) return 0;
        try {
            return Long.parseLong(tag.substring(dot + 1).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String findApkUrl(JSONObject release) {
        JSONArray assets = release.optJSONArray("assets");
        if (assets == null) return null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.optJSONObject(i);
            if (a != null && APK_NAME.equals(a.optString("name"))) return a.optString("browser_download_url", null);
        }
        return null;
    }

    private void showUpdateDialog(String newVersion, String url) {
        if (activity.isFinishing()) return;
        new AlertDialog.Builder(activity)
                .setTitle("Update verfügbar")
                .setMessage("Eine neue Version der App ist da (" + newVersion + ", du hast "
                        + currentVersionName() + ").\n\nDeine Fänge bleiben beim Update erhalten.")
                .setPositiveButton("Jetzt installieren", (d, w) -> startInstall(url))
                .setNegativeButton("Später", null)
                .show();
    }

    private boolean canInstall() {
        return activity.getPackageManager().canRequestPackageInstalls();
    }

    private void startInstall(String url) {
        if (canInstall()) {
            download(url);
            return;
        }
        // Android fragt einmalig: Darf diese App andere Apps (= ihre Updates) installieren?
        pendingDownloadUrl = url;
        new AlertDialog.Builder(activity)
                .setTitle("Einmalige Erlaubnis")
                .setMessage("Damit die App sich selbst aktualisieren kann, muss Android das einmal erlauben.\n\n"
                        + "Auf der nächsten Seite den Schalter „Aus dieser Quelle zulassen“ einschalten "
                        + "und dann mit „Zurück“ zur App wechseln.")
                .setPositiveButton("Weiter", (d, w) -> {
                    installAfterPermission = true;
                    activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + activity.getPackageName())));
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void download(String url) {
        if (url == null || busy) return;
        busy = true;
        progressDialog = new AlertDialog.Builder(activity)
                .setTitle("Update wird geladen")
                .setMessage("Bitte kurz warten …")
                .setCancelable(false)
                .show();
        new Thread(() -> {
            try {
                PackageInstaller installer = activity.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params =
                        new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(activity.getPackageName());
                int sessionId = installer.createSession(params);
                try (PackageInstaller.Session session = installer.openSession(sessionId)) {
                    HttpURLConnection conn = open(url);
                    long total = conn.getContentLengthLong();
                    try (InputStream in = conn.getInputStream();
                         OutputStream out = session.openWrite("update.apk", 0, total > 0 ? total : -1)) {
                        byte[] buf = new byte[16384];
                        long done = 0;
                        int lastPercent = -1;
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            out.write(buf, 0, n);
                            done += n;
                            if (total > 0) {
                                int percent = (int) (done * 100 / total);
                                if (percent != lastPercent) {
                                    lastPercent = percent;
                                    ui.post(() -> {
                                        if (progressDialog != null) progressDialog.setMessage("Geladen: " + percent + " %");
                                    });
                                }
                            }
                        }
                        session.fsync(out);
                    } finally {
                        conn.disconnect();
                    }
                    Intent callback = new Intent(activity, UpdateReceiver.class);
                    PendingIntent pending = PendingIntent.getBroadcast(activity, sessionId, callback,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                    session.commit(pending.getIntentSender());
                }
                ui.post(this::closeProgress);
            } catch (Exception e) {
                ui.post(() -> {
                    closeProgress();
                    toast("Update konnte nicht geladen werden. Bitte später noch einmal versuchen.");
                });
            }
        }).start();
    }

    private void closeProgress() {
        busy = false;
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream");
        conn.setRequestProperty("User-Agent", "FischfangApp");
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
        return conn;
    }

    private static String readText(String url) throws Exception {
        HttpURLConnection conn = open(url);
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    private void toast(String text) {
        Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
    }
}
