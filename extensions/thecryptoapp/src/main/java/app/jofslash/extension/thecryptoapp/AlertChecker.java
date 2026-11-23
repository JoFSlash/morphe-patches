package app.jofslash.extension.thecryptoapp;

import android.content.Context;
import android.util.Log;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Evaluates all enabled rules against freshly fetched prices and posts notifications for the ones
 * whose condition is now met. One batched fetch per source (see {@link PriceFetcher}). Blocking;
 * always called off the main thread. Never throws to its caller.
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
            Set<String> symbols = new LinkedHashSet<>();
            int enabled = 0;
            for (AlertRule r : rules) {
                if (r.enabled && r.symbol != null && !r.symbol.isEmpty()) {
                    enabled++;
                    symbols.add(r.symbol);
                }
            }
            Log.i(TAG, "check: start (" + rules.size() + " rules, " + enabled + " enabled)");
            if (enabled == 0) {
                Log.i(TAG, "check: nothing enabled, done");
                return;
            }

            PriceFetcher.PriceMap pm = PriceFetcher.fetchAll(app, symbols);
            for (String s : symbols) {
                Double p = pm.prices.get(s);
                Log.i(TAG, "check: " + s + " = "
                        + (p == null ? "unavailable" : ("$" + p + (pm.stale.contains(s) ? " (stale)" : ""))));
            }

            long now = System.currentTimeMillis();
            boolean changed = false;
            for (AlertRule r : rules) {
                if (!r.enabled) continue;
                Double price = pm.prices.get(r.symbol);
                if (price == null) continue;

                // Record the latest reading for the UI. Only bump the checked-time when fresh, so a
                // stale (rate-limited) reading keeps the old timestamp.
                r.lastPrice = price;
                r.hasLastPrice = true;
                if (!pm.stale.contains(r.symbol)) r.lastCheckedAt = now;
                changed = true;

                if (r.conditionMet(price)) {
                    boolean cooled = r.lastTriggeredAt == 0L || (now - r.lastTriggeredAt) > COOLDOWN_MS;
                    if (cooled) {
                        AlertNotifier.notifyRule(app, r, price);
                        r.lastTriggeredAt = now;
                    } else {
                        Log.i(TAG, "check: " + r.displayName + " met but in cooldown");
                    }
                } else if (r.lastTriggeredAt != 0L) {
                    // Price crossed back; re-arm so it can fire again next time.
                    r.lastTriggeredAt = 0L;
                }
            }

            if (changed) AlertStore.save(app, rules);
            Log.i(TAG, "check: done");
        } catch (Throwable t) {
            Log.e(TAG, "check: unexpected failure", t);
        }
    }
}
