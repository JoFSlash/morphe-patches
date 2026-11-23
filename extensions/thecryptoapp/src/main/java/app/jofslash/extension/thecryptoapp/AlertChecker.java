package app.jofslash.extension.thecryptoapp;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates all enabled rules against freshly fetched prices and posts notifications for the ones
 * whose condition is now met. One network request per distinct symbol. Blocking; always called off
 * the main thread. Never throws to its caller.
 */
public final class AlertChecker {

    static final String TAG = CustomAlertPoc.TAG;

    /** After firing, a rule won't fire again for this long unless the price crosses back first. */
    private static final long COOLDOWN_MS = 30L * 60L * 1000L;

    private AlertChecker() {
    }

    public static void checkNow(Context ctx) {
        try {
            Context app = ctx.getApplicationContext();
            List<AlertRule> rules = AlertStore.load(app);
            int enabled = 0;
            for (AlertRule r : rules) if (r.enabled) enabled++;
            Log.i(TAG, "check: start (" + rules.size() + " rules, " + enabled + " enabled)");
            if (enabled == 0) {
                Log.i(TAG, "check: nothing enabled, done");
                return;
            }

            // One fetch per distinct symbol.
            Map<String, Double> priceBySymbol = new HashMap<>();
            for (AlertRule r : rules) {
                if (!r.enabled || r.symbol == null || r.symbol.isEmpty()) continue;
                if (priceBySymbol.containsKey(r.symbol)) continue;
                Double price = PriceFetcher.fetchPrice(r.symbol);
                priceBySymbol.put(r.symbol, price);
                Log.i(TAG, "check: " + r.symbol + " = " + (price == null ? "unavailable" : ("$" + price)));
            }

            long now = System.currentTimeMillis();
            boolean changed = false;
            for (AlertRule r : rules) {
                if (!r.enabled) continue;
                Double price = priceBySymbol.get(r.symbol);
                if (price == null) continue;

                if (r.conditionMet(price)) {
                    boolean cooled = r.lastTriggeredAt == 0L || (now - r.lastTriggeredAt) > COOLDOWN_MS;
                    if (cooled) {
                        AlertNotifier.notifyRule(app, r, price);
                        r.lastTriggeredAt = now;
                        changed = true;
                    } else {
                        Log.i(TAG, "check: " + r.displayName + " met but in cooldown");
                    }
                } else if (r.lastTriggeredAt != 0L) {
                    // Price crossed back; re-arm so it can fire again next time.
                    r.lastTriggeredAt = 0L;
                    changed = true;
                }
            }

            if (changed) AlertStore.save(app, rules);
            Log.i(TAG, "check: done");
        } catch (Throwable t) {
            Log.e(TAG, "check: unexpected failure", t);
        }
    }
}
