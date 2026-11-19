package de.fischfang.app;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Zeigt die Fischfang-Tracker-Webseite als eigene App an und gibt ihr Zugriff auf
 * GPS, die Spracherkennung von Android und das Speichern von Sicherungsdateien.
 */
public class MainActivity extends Activity {
    static final String APP_URL = "https://marchens1977.github.io/Fisch-Fang-Tracker/";
    private static final String APP_HOST = "marchens1977.github.io";

    private static final int REQ_LOCATION = 1;
    private static final int REQ_OPEN_FILE = 2;
    private static final int REQ_SAVE_FILE = 3;
    private static final int REQ_SPEECH = 4;
    private static final int REQ_MIC = 5;

    private WebView webView;
    private UpdateChecker updates;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingSaveContent;
    private String pendingGeoOrigin;
    private GeolocationPermissions.Callback pendingGeoCallback;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean triedOfflineCopy = false;

    // Dauer-Diktat (wie in der App „Smartphone-Audio-Aufnahme“ des Nutzers)
    private SpeechRecognizer recognizer;
    private boolean dictating = false;
    private final StringBuilder dictationText = new StringBuilder();
    private String lastPartial = "";
    // Text, der in der laufenden Runde schon gesichert wurde (weil die Erkennung nach einer
    // Pause intern neu angefangen hat). Wird gebraucht, damit das Endergebnis nichts doppelt.
    private final StringBuilder sessionCommitted = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#002D72"));
        getWindow().setNavigationBarColor(Color.parseColor("#002D72"));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#001433"));
        // Ab Android 15 zeichnet die App bis unter die Leisten (Uhrzeit oben, Navigation unten).
        // Damit nichts verdeckt wird (z. B. der Start-Knopf), bekommt die Seite Abstand zu den Leisten.
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#002D72"));
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        root.setOnApplyWindowInsetsListener(this::applyBarInsets);
        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true); // localStorage = Speicher der Fänge
        settings.setGeolocationEnabled(true);
        settings.setCacheMode(isOnline() ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);

        webView.addJavascriptInterface(new AppBridge(), "AndroidApp");
        webView.setWebViewClient(new AppWebViewClient());
        webView.setWebChromeClient(new AppChromeClient());

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(APP_URL);
        }

        updates = new UpdateChecker(this);
        updates.autoCheck(); // von selbst nach Update suchen
    }

    /** Abstand zu Status-, Navigationsleiste und Tastatur, damit die Seite nicht darunter liegt. */
    private WindowInsets applyBarInsets(View v, WindowInsets insets) {
        int top, bottom, left, right;
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
            top = bars.top; left = bars.left; right = bars.right;
            bottom = Math.max(bars.bottom, ime.bottom);
        } else {
            top = insets.getSystemWindowInsetTop();
            bottom = insets.getSystemWindowInsetBottom();
            left = insets.getSystemWindowInsetLeft();
            right = insets.getSystemWindowInsetRight();
        }
        v.setPadding(left, top, right, bottom);
        return insets;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (updates != null) {
            updates.autoCheck();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // ---------------------------------------------------------------- Standort (GPS)

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC) {
            if (hasMicPermission()) {
                beginDictation();
            } else {
                dictationStatus("Mikrofon nicht erlaubt. Bitte in den App-Einstellungen das Mikrofon erlauben.");
            }
            return;
        }
        if (requestCode != REQ_LOCATION) return;
        boolean granted = hasLocationPermission();
        if (pendingGeoCallback != null) {
            pendingGeoCallback.invoke(pendingGeoOrigin, granted, false);
            pendingGeoCallback = null;
            pendingGeoOrigin = null;
        }
        if (!granted) {
            Toast.makeText(this, "Ohne Standort-Erlaubnis kann der Fangort nicht automatisch gesetzt werden.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------- Sprache

    private void startSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Sag z. B. „Zander 65 Zentimeter“");
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
        try {
            startActivityForResult(intent, REQ_SPEECH);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Keine Spracherkennung auf dem Handy gefunden (z. B. Google-App fehlt).",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------- Dauer-Diktat
    // Hört so lange zu, bis auf der Seite „Aufnahme beenden & Speichern“ gedrückt wird.
    // Nach jeder Sprechpause startet die Erkennung sofort neu; der Text wird gesammelt.

    private boolean hasMicPermission() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void startDictation() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            // Keine Dauer-Erkennung möglich: das einfache Android-Sprachfenster verwenden
            dictationStatus("Dauer-Diktat nicht verfügbar – nutze das einfache Sprachfenster.");
            startSpeech();
            return;
        }
        dictationText.setLength(0);
        lastPartial = "";
        if (hasMicPermission()) {
            beginDictation();
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        }
    }

    private void beginDictation() {
        dictating = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        listen();
    }

    private void listen() {
        if (!dictating) return;
        sessionCommitted.setLength(0);
        lastPartial = "";
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(listener);
        }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);
        try {
            recognizer.startListening(i);
        } catch (Exception e) {
            dictationStatus("Spracherkennung ließ sich nicht starten. Du kannst trotzdem speichern.");
        }
    }

    /** Beendet das Zuhören und gibt den gesammelten Text an die Seite (dort wird der Fang gespeichert). */
    private void stopDictation() {
        boolean wasActive = dictating || dictationText.length() > 0 || !lastPartial.isEmpty();
        dictating = false;
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!lastPartial.isEmpty()) appendDictation(lastPartial); // zuletzt Gesagtes nicht verlieren
        lastPartial = "";
        String text = dictationText.toString().trim();
        dictationText.setLength(0);
        if (wasActive) sendSpeechToPage(text);
    }

    private void appendDictation(String part) {
        String t = part == null ? "" : part.trim();
        if (t.isEmpty()) return;
        if (dictationText.length() > 0) dictationText.append(' ');
        dictationText.append(t);
    }

    private void showDictation() {
        String shown = (dictationText + " " + lastPartial).trim();
        webView.evaluateJavascript("window.FischNativeApp && window.FischNativeApp.onDictation && "
                + "window.FischNativeApp.onDictation(" + JSONObject.quote(shown) + ");", null);
    }

    private void dictationStatus(String message) {
        webView.evaluateJavascript("window.FischNativeApp && window.FischNativeApp.onDictationStatus && "
                + "window.FischNativeApp.onDictationStatus(" + JSONObject.quote(message) + ");", null);
    }

    private final RecognitionListener listener = new RecognitionListener() {
        @Override public void onReadyForSpeech(Bundle b) { }
        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float db) { }
        @Override public void onBufferReceived(byte[] b) { }
        @Override public void onEndOfSpeech() { }
        @Override public void onEvent(int type, Bundle b) { }

        @Override public void onPartialResults(Bundle b) {
            ArrayList<String> t = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (t != null && !t.isEmpty() && t.get(0) != null && !t.get(0).trim().isEmpty()) {
                String neu = t.get(0).trim();
                // Manche Handys (z. B. Samsung) fangen nach einer Sprechpause innerhalb derselben
                // Runde wieder bei null an. Dann das bisher Gesagte sichern, statt es zu überschreiben.
                if (DictationMerge.isRestart(lastPartial, neu)) {
                    appendDictation(lastPartial);
                    if (sessionCommitted.length() > 0) sessionCommitted.append(' ');
                    sessionCommitted.append(lastPartial.trim());
                }
                lastPartial = neu;
                showDictation();
            }
        }

        @Override public void onResults(Bundle b) {
            ArrayList<String> t = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String fin = (t != null && !t.isEmpty() && t.get(0) != null) ? t.get(0).trim() : "";
            // Endergebnis enthält oft nur den letzten Abschnitt; falls es doch alles enthält,
            // den schon gesicherten Anfang abschneiden (sonst doppelt).
            fin = DictationMerge.stripCommitted(sessionCommitted.toString(), fin);
            if (fin.isEmpty()) fin = lastPartial; // nichts verlieren
            lastPartial = "";
            appendDictation(fin);
            showDictation();
            listen(); // gleich weiter zuhören
        }

        @Override public void onError(int error) {
            if (!dictating) return;
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                if (!lastPartial.isEmpty()) {
                    appendDictation(lastPartial);
                    lastPartial = "";
                }
                listen(); // nur eine Sprechpause – weiter zuhören
            } else if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                if (recognizer != null) {
                    recognizer.destroy();
                    recognizer = null;
                }
                handler.postDelayed(MainActivity.this::listen, 500);
            } else {
                dictating = false;
                if (recognizer != null) {
                    recognizer.destroy();
                    recognizer = null;
                }
                String reason = error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                        ? "Keine Internet-Verbindung für die Spracherkennung."
                        : error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                        ? "Mikrofon nicht erlaubt."
                        : error == SpeechRecognizer.ERROR_AUDIO
                        ? "Mikrofon wird gerade von einer anderen App benutzt."
                        : "Spracherkennung unterbrochen (Fehler " + error + ").";
                dictationStatus(reason + " Mit „Beenden & Speichern“ wird das bisher Gesagte gespeichert.");
            }
        }
    };

    @Override
    protected void onDestroy() {
        dictating = false;
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        super.onDestroy();
    }

    private void sendSpeechToPage(String text) {
        String js = "window.FischNativeApp && window.FischNativeApp.onSpeech(" + JSONObject.quote(text) + ");";
        webView.evaluateJavascript(js, null);
    }

    // ---------------------------------------------------------------- Ergebnisse (Datei, Sprache)

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OPEN_FILE) {
            if (fileCallback != null) {
                fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                fileCallback = null;
            }
        } else if (requestCode == REQ_SAVE_FILE) {
            String content = pendingSaveContent;
            pendingSaveContent = null;
            if (resultCode != RESULT_OK || data == null || data.getData() == null || content == null) return;
            try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                if (out == null) throw new IllegalStateException("kein Ziel");
                out.write(content.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, "Sicherung gespeichert.", Toast.LENGTH_SHORT).show();
                webView.evaluateJavascript("window.FischNativeApp && window.FischNativeApp.onBackupSaved();", null);
            } catch (Exception e) {
                Toast.makeText(this, "Sicherung konnte nicht gespeichert werden.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQ_SPEECH) {
            if (resultCode != RESULT_OK || data == null) return;
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty() && results.get(0) != null) {
                sendSpeechToPage(results.get(0));
            }
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        if (cm == null || cm.getActiveNetwork() == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    // ---------------------------------------------------------------- Webseite

    private class AppWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (APP_HOST.equals(uri.getHost())) return false;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri)); // fremde Links im Browser öffnen
            } catch (ActivityNotFoundException ignored) {
            }
            return true;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            // Kein Internet: gespeicherte Kopie der Seite verwenden
            if (request.isForMainFrame() && !triedOfflineCopy) {
                triedOfflineCopy = true;
                view.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                view.loadUrl(APP_URL);
            }
        }
    }

    private class AppChromeClient extends WebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            if (hasLocationPermission()) {
                callback.invoke(origin, true, false);
                return;
            }
            if (pendingGeoCallback != null) pendingGeoCallback.invoke(pendingGeoOrigin, false, false);
            pendingGeoOrigin = origin;
            pendingGeoCallback = callback;
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }

        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = callback;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            try {
                startActivityForResult(Intent.createChooser(intent, "Sicherungsdatei wählen"), REQ_OPEN_FILE);
                return true;
            } catch (ActivityNotFoundException e) {
                fileCallback = null;
                return false;
            }
        }
    }

    /** Wird von der Webseite als window.AndroidApp aufgerufen. */
    private class AppBridge {
        @JavascriptInterface
        public void saveFile(String fileName, String content) {
            handler.post(() -> {
                pendingSaveContent = content;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, fileName);
                try {
                    startActivityForResult(intent, REQ_SAVE_FILE);
                } catch (ActivityNotFoundException e) {
                    pendingSaveContent = null;
                    Toast.makeText(MainActivity.this, "Kein Speicherort verfügbar.", Toast.LENGTH_LONG).show();
                }
            });
        }

        @JavascriptInterface
        public void startSpeech() {
            handler.post(MainActivity.this::startSpeech);
        }

        @JavascriptInterface
        public void startDictation() {
            handler.post(MainActivity.this::startDictation);
        }

        @JavascriptInterface
        public void stopDictation() {
            handler.post(MainActivity.this::stopDictation);
        }

        @JavascriptInterface
        public String getVersion() {
            return updates.currentVersionName();
        }

        @JavascriptInterface
        public void checkUpdate() {
            handler.post(() -> updates.check(true));
        }

        @JavascriptInterface
        public boolean isApp() {
            return true;
        }
    }
}
