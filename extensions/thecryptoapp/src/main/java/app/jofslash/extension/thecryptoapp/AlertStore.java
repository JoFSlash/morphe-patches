package app.jofslash.extension.thecryptoapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SharedPreferences-backed CRUD store for {@link AlertRule}s. Its own prefs file, kept entirely
 * separate from the host app's preferences. Thread-safe enough for our use (synchronized on the
 * class); writes are small JSON blobs.
 */
public final class AlertStore {

    static final String TAG = CustomAlertPoc.TAG;

    private static final String PREFS = "jofslash_crypto_alerts";
    private static final String KEY_RULES = "rules";

    private AlertStore() {
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized List<AlertRule> load(Context ctx) {
        List<AlertRule> rules = new ArrayList<>();
        try {
            String raw = prefs(ctx).getString(KEY_RULES, "[]");
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) rules.add(AlertRule.fromJson(o));
            }
        } catch (Throwable t) {
            Log.e(TAG, "store: load failed", t);
        }
        return rules;
    }

    public static synchronized void save(Context ctx, List<AlertRule> rules) {
        try {
            JSONArray arr = new JSONArray();
            for (AlertRule r : rules) arr.put(r.toJson());
            prefs(ctx).edit().putString(KEY_RULES, arr.toString()).apply();
        } catch (Throwable t) {
            Log.e(TAG, "store: save failed", t);
        }
    }

    /** Adds a new rule (assigns an id) and returns it. */
    public static synchronized AlertRule add(Context ctx, String symbol, String displayName,
                                             double targetPrice, String direction,
                                             String note, boolean repeat) {
        List<AlertRule> rules = load(ctx);
        AlertRule r = new AlertRule(UUID.randomUUID().toString(), symbol, displayName,
                targetPrice, direction, true);
        r.note = note == null ? "" : note;
        r.repeat = repeat;
        rules.add(r);
        save(ctx, rules);
        Log.i(TAG, "store: added rule " + displayName + " " + direction + " " + targetPrice);
        return r;
    }

    /** Edits an existing rule's symbol/name/target/direction, keeping its id and re-arming it. */
    public static synchronized void update(Context ctx, String id, String symbol, String displayName,
                                           double targetPrice, String direction,
                                           String note, boolean repeat) {
        List<AlertRule> rules = load(ctx);
        for (AlertRule r : rules) {
            if (r.id.equals(id)) {
                r.symbol = symbol;
                r.displayName = displayName;
                r.targetPrice = targetPrice;
                r.direction = direction;
                r.note = note == null ? "" : note;
                r.repeat = repeat;
                // Changing the rule invalidates the previous reading and re-arms it.
                r.lastTriggeredAt = 0L;
                r.hasLastPrice = false;
                r.hasLastPct = false;
                r.lastCheckedAt = 0L;
            }
        }
        save(ctx, rules);
        Log.i(TAG, "store: updated rule " + displayName + " " + direction + " " + targetPrice);
    }

    public static synchronized void delete(Context ctx, String id) {
        List<AlertRule> rules = load(ctx);
        List<AlertRule> kept = new ArrayList<>();
        for (AlertRule r : rules) if (!r.id.equals(id)) kept.add(r);
        save(ctx, kept);
    }

    public static synchronized void setEnabled(Context ctx, String id, boolean enabled) {
        List<AlertRule> rules = load(ctx);
        for (AlertRule r : rules) if (r.id.equals(id)) r.enabled = enabled;
        save(ctx, rules);
    }

    public static synchronized void setLastTriggered(Context ctx, String id, long when) {
        List<AlertRule> rules = load(ctx);
        for (AlertRule r : rules) if (r.id.equals(id)) r.lastTriggeredAt = when;
        save(ctx, rules);
    }
}
