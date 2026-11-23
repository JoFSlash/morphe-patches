package app.jofslash.extension.thecryptoapp;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * One user-defined price-alert rule. Fully self-contained; carries no reference to any host-app
 * type. Serialized to/from plain JSON for SharedPreferences storage.
 */
public final class AlertRule {

    public static final String ABOVE = "ABOVE";
    public static final String BELOW = "BELOW";

    /** Stable id for the notification id and for CRUD by id. */
    public String id;
    /** Binance symbol, e.g. "BTCUSDT". Also used to derive a CoinGecko id for the fallback. */
    public String symbol;
    /** Human label shown in the UI and notification, e.g. "BTC". */
    public String displayName;
    public double targetPrice;
    /** {@link #ABOVE} or {@link #BELOW}. */
    public String direction = ABOVE;
    public boolean enabled = true;
    /** Epoch millis of the last time this rule fired; 0 means armed. Used for the cooldown. */
    public long lastTriggeredAt = 0L;

    public AlertRule() {
    }

    public AlertRule(String id, String symbol, String displayName, double targetPrice,
                     String direction, boolean enabled) {
        this.id = id;
        this.symbol = symbol;
        this.displayName = displayName;
        this.targetPrice = targetPrice;
        this.direction = direction;
        this.enabled = enabled;
    }

    public boolean isAbove() {
        return !BELOW.equals(direction);
    }

    /** Whether the given price satisfies this rule's condition. */
    public boolean conditionMet(double price) {
        return isAbove() ? price >= targetPrice : price <= targetPrice;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("symbol", symbol);
        o.put("displayName", displayName);
        o.put("targetPrice", targetPrice);
        o.put("direction", direction);
        o.put("enabled", enabled);
        o.put("lastTriggeredAt", lastTriggeredAt);
        return o;
    }

    public static AlertRule fromJson(JSONObject o) {
        AlertRule r = new AlertRule();
        r.id = o.optString("id", null);
        r.symbol = o.optString("symbol", "");
        r.displayName = o.optString("displayName", r.symbol);
        r.targetPrice = o.optDouble("targetPrice", 0d);
        r.direction = o.optString("direction", ABOVE);
        r.enabled = o.optBoolean("enabled", true);
        r.lastTriggeredAt = o.optLong("lastTriggeredAt", 0L);
        return r;
    }
}
