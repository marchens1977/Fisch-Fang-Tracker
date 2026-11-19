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
import android.speech.RecognizerIntent;
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

    private WebView webView;
    private UpdateChecker updates;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingSaveContent;
    private String pendingGeoOrigin;
    private GeolocationPermissions.Callback pendingGeoCallback;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean triedOfflineCopy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#002D72"));
        getWindow().setNavigationBarColor(Color.parseColor("#002D72"));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#001433"));
        setContentView(webView);

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

    @Override
    protected void onResume() {
        super.onResume();
        if (updates != null) {
            updates.onResume();
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
