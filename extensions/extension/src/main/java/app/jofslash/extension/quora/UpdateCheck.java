package app.jofslash.extension.quora;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Says when a newer version of these patches is out. A patched app doesn't update from Google
 * Play, so without this nobody inside the app learns about new patches or fixes. Once a day it
 * downloads patches-bundle.json from the repository (the file Morphe Manager reads too): from the
 * main branch for a stable version, from dev for a pre-release. A newer version is shown once in a
 * dialog, and in Patch settings until the app has it.
 */
public final class UpdateCheck {

    private static final String TAG = "JoFSlashPatches";
    private static final String PREFS = "jofslash_patch_settings";

    /** The switch, registered by the Update notice patch. */
    static final String KEY = "updateNotice";

    private static final String BUNDLE = "https://raw.githubusercontent.com/JoFSlash/quora-patches/%s/patches-bundle.json";
    static final String RELEASES = "https://github.com/JoFSlash/quora-patches/releases";

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final int MAX_BYTES = 64 * 1024;

    /** The version of the patches this app was patched with, set by the Update notice patch. */
    private static volatile String version;
    private static volatile boolean checking;

    private UpdateCheck() {
    }

    /** Injection point: start of QTabBar.setupTabs. */
    public static void setVersion(String patched) {
        version = patched;
    }

    /** The patches' version, or null when the Update notice patch isn't in the app. */
    static String version() {
        return version;
    }

    /** Whether a version is a pre-release from the dev branch, such as 1.10.0-dev.2. */
    static boolean preRelease(String v) {
        return v != null && v.contains("-");
    }

    /**
     * Compares two versions as semantic versions: 1.10.0 > 1.9.0, 1.10.0 > 1.10.0-dev.3, and
     * 1.10.0-dev.10 > 1.10.0-dev.9. Parts that aren't numbers compare as text.
     */
    static int compare(String a, String b) {
        String[] ap = a.trim().replaceFirst("^v", "").split("-", 2);
        String[] bp = b.trim().replaceFirst("^v", "").split("-", 2);
        int core = compareParts(ap[0], bp[0]);
        if (core != 0) return core;
        if (ap.length == 1 || bp.length == 1) return Integer.compare(bp.length, ap.length);
        return compareParts(ap[1], bp[1]);
    }

    private static int compareParts(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            if (i >= x.length) return -1;
            if (i >= y.length) return 1;
            boolean xn = x[i].matches("\\d{1,9}");
            boolean yn = y[i].matches("\\d{1,9}");
            int c = xn && yn ? Integer.compare(Integer.parseInt(x[i]), Integer.parseInt(y[i]))
                    : xn ? -1 : yn ? 1 : x[i].compareTo(y[i]);
            if (c != 0) return c;
        }
        return 0;
    }

    private static boolean enabled() {
        return version != null && PatchSettings.isRegistered(KEY) && PatchSettings.isOn(KEY);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** The newest version known, when it is newer than this app's, else null. */
    static String newer(SharedPreferences prefs) {
        String latest = prefs.getString("update_latest", null);
        if (version == null || latest == null) return null;
        try {
            return compare(latest, version) > 0 ? latest : null;
        } catch (Exception ex) {
            return null;
        }
    }

    /** Called when the tab bar is set up: checks when the last try is a day old. */
    static void check(View home) {
        Context context = home == null ? null : home.getContext();
        if (context == null || !enabled() || checking) return;
        SharedPreferences prefs = prefs(context);
        if (System.currentTimeMillis() - prefs.getLong("update_tried", 0) >= DAY) {
            checking = true;
            new Thread(() -> {
                try {
                    download(prefs);
                } finally {
                    checking = false;
                }
                home.post(() -> offer(home, prefs));
            }, "JoFSlashUpdateCheck").start();
        } else {
            offer(home, prefs);
        }
    }

    private static void download(SharedPreferences prefs) {
        HttpURLConnection connection = null;
        try {
            prefs.edit().putLong("update_tried", System.currentTimeMillis()).apply();
            String branch = preRelease(version) ? "dev" : "main";
            connection = (HttpURLConnection) java.net.URI.create(String.format(BUNDLE, branch)).toURL().openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(15_000);
            connection.setUseCaches(false);
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Update notice: HTTP " + code);
                return;
            }
            String json = read(connection.getInputStream());
            if (json == null) return;
            String latest = new JSONObject(json).optString("version", "");
            if (!latest.matches("\\d{1,4}\\.\\d{1,4}\\.\\d{1,4}(-[0-9A-Za-z.]{1,20})?")) return;
            prefs.edit().putString("update_latest", latest).putLong("update_time", System.currentTimeMillis()).apply();
        } catch (Exception ex) {
            Log.e(TAG, "Update notice: check failed: " + ex);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String read(InputStream in) throws java.io.IOException {
        try (InputStream input = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = input.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                if (out.size() > MAX_BYTES) return null;
            }
            return out.toString("UTF-8");
        }
    }

    /**
     * Shows a newer version once. Not on the first start after patching, which has the presets
     * dialog, and a few seconds after the start, when the feed is there.
     */
    private static void offer(View home, SharedPreferences prefs) {
        String latest = newer(prefs);
        if (latest == null || latest.equals(prefs.getString("update_shown", null))) return;
        if (!prefs.getBoolean("hint_shown", false)) return;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Activity activity = PatchSettings.activity(home.getContext());
            if (activity == null || activity.isFinishing() || activity.isDestroyed() || !home.isAttachedToWindow()) return;
            if (latest.equals(prefs.getString("update_shown", null))) return;
            prefs.edit().putString("update_shown", latest).apply();
            try {
                show(activity, latest);
            } catch (Exception ex) {
                Log.e(TAG, "Update notice: failed to show", ex);
            }
        }, 4000);
    }

    static void show(Activity activity, String latest) {
        boolean dark = (activity.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        new AlertDialog.Builder(activity, dark
                ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle(Texts.t("New Quora patches: ") + latest)
                .setMessage(Texts.t("This app has ") + version + ". "
                        + Texts.t("Patch Quora again in Morphe Manager to get the new version, and install it over this app: you stay signed in and keep your settings."))
                .setPositiveButton(Texts.t("What's new"), (d, w) -> openRelease(activity, latest))
                .setNegativeButton(Texts.t("Later"), null)
                .show();
    }

    static void openRelease(Context context, String latest) {
        try {
            android.content.Intent open = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(RELEASES + "/tag/v" + latest));
            open.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(open);
        } catch (Exception ex) {
            Log.e(TAG, "Update notice: failed to open the release", ex);
        }
    }

    /** One line for the settings page, or null when the patch isn't in the app. */
    static String status(Context context) {
        if (version == null || !PatchSettings.isRegistered(KEY)) return null;
        if (!PatchSettings.isOn(KEY)) return Texts.t("Patches ") + version + ".";
        String latest = newer(prefs(context));
        if (latest != null) return Texts.t("Patches ") + version + ". " + Texts.t("A newer version is out: ") + latest + ".";
        return prefs(context).getLong("update_time", 0) == 0 ? Texts.t("Patches ") + version + "."
                : Texts.t("Patches ") + version + ", " + Texts.t("the newest.");
    }
}
