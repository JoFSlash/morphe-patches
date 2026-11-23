package app.jofslash.extension.thecryptoapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Fetches a spot price from a free public API: Binance first, CoinGecko as a fallback. No API key,
 * no account. Self-contained. The CoinGecko fallback resolves a ticker to a CoinGecko coin id
 * using a small hardcoded map of majors (authoritative) plus a daily-cached copy of CoinGecko's
 * /coins/list for the long tail.
 */
public final class PriceFetcher {

    static final String TAG = CustomAlertPoc.TAG;

    private static final String BINANCE = "https://api.binance.com/api/v3/ticker/price?symbol=";
    private static final String COINGECKO_PRICE =
            "https://api.coingecko.com/api/v3/simple/price?vs_currencies=usd&ids=";
    private static final String COINGECKO_LIST = "https://api.coingecko.com/api/v3/coins/list";

    private static final String PREFS = "jofslash_crypto_alerts";
    private static final String KEY_CG_MAP = "coingecko_map";
    private static final String KEY_CG_MAP_AT = "coingecko_map_at";
    private static final long CG_MAP_TTL_MS = 24L * 60L * 60L * 1000L;

    /** Authoritative ticker-base -> CoinGecko id for the majors (avoids /coins/list ambiguity). */
    private static final Map<String, String> MAJORS = new HashMap<>();

    static {
        MAJORS.put("BTC", "bitcoin");
        MAJORS.put("ETH", "ethereum");
        MAJORS.put("BNB", "binancecoin");
        MAJORS.put("SOL", "solana");
        MAJORS.put("XRP", "ripple");
        MAJORS.put("ADA", "cardano");
        MAJORS.put("DOGE", "dogecoin");
        MAJORS.put("AVAX", "avalanche-2");
        MAJORS.put("DOT", "polkadot");
        MAJORS.put("MATIC", "matic-network");
        MAJORS.put("LTC", "litecoin");
        MAJORS.put("LINK", "chainlink");
        MAJORS.put("TRX", "tron");
        MAJORS.put("USDT", "tether");
        MAJORS.put("USDC", "usd-coin");
    }

    private PriceFetcher() {
    }

    /** USD-ish price for a Binance symbol such as "BTCUSDT", or null if both sources fail. */
    public static Double fetchPrice(Context ctx, String symbol) {
        Double price = fetchBinance(symbol);
        if (price == null) {
            Log.w(TAG, "fetch: Binance unavailable for " + symbol + ", trying CoinGecko fallback");
            price = fetchCoinGecko(ctx, symbol);
        }
        return price;
    }

    private static Double fetchBinance(String symbol) {
        try {
            String body = httpGet(BINANCE + symbol.toUpperCase(Locale.US));
            if (body == null) return null;
            String p = new JSONObject(body).optString("price", null);
            return p == null ? null : Double.parseDouble(p);
        } catch (Throwable t) {
            Log.w(TAG, "fetch: Binance fetch failed for " + symbol + ": " + t);
            return null;
        }
    }

    private static Double fetchCoinGecko(Context ctx, String symbol) {
        try {
            String id = coinGeckoId(ctx, symbol);
            if (id == null) {
                Log.w(TAG, "fetch: no CoinGecko id for " + symbol);
                return null;
            }
            String body = httpGet(COINGECKO_PRICE + URLEncoder.encode(id, "UTF-8"));
            if (body == null) return null;
            JSONObject coin = new JSONObject(body).optJSONObject(id);
            if (coin == null || !coin.has("usd")) return null;
            return coin.getDouble("usd");
        } catch (Throwable t) {
            Log.w(TAG, "fetch: CoinGecko fetch failed for " + symbol + ": " + t);
            return null;
        }
    }

    /** Resolves a Binance ticker to a CoinGecko coin id. Majors first, then the cached list. */
    private static String coinGeckoId(Context ctx, String symbol) {
        String base = symbol.toUpperCase(Locale.US).replaceAll("(USDT|BUSD|USDC|USD)$", "");
        if (base.isEmpty()) base = symbol.toUpperCase(Locale.US);
        String major = MAJORS.get(base);
        if (major != null) return major;
        try {
            Map<String, String> map = coinGeckoMap(ctx);
            return map.get(base.toLowerCase(Locale.US));
        } catch (Throwable t) {
            Log.w(TAG, "fetch: CoinGecko map lookup failed: " + t);
            return null;
        }
    }

    /** Lowercase-symbol -> coin id, cached in prefs and refreshed at most daily. */
    private static Map<String, String> coinGeckoMap(Context ctx) {
        Map<String, String> map = new HashMap<>();
        if (ctx == null) return map;
        SharedPreferences prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long at = prefs.getLong(KEY_CG_MAP_AT, 0L);
        String cached = prefs.getString(KEY_CG_MAP, null);
        boolean fresh = cached != null && (System.currentTimeMillis() - at) < CG_MAP_TTL_MS;

        if (!fresh) {
            try {
                String body = httpGet(COINGECKO_LIST);
                if (body != null) {
                    JSONArray arr = new JSONArray(body);
                    JSONObject built = new JSONObject();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject c = arr.optJSONObject(i);
                        if (c == null) continue;
                        String sym = c.optString("symbol", "").toLowerCase(Locale.US);
                        String id = c.optString("id", null);
                        // First id for a symbol wins; keep existing to stay stable.
                        if (!sym.isEmpty() && id != null && !built.has(sym)) built.put(sym, id);
                    }
                    cached = built.toString();
                    prefs.edit().putString(KEY_CG_MAP, cached)
                            .putLong(KEY_CG_MAP_AT, System.currentTimeMillis()).apply();
                    Log.i(TAG, "fetch: refreshed CoinGecko id map (" + built.length() + " symbols)");
                }
            } catch (Throwable t) {
                Log.w(TAG, "fetch: could not refresh CoinGecko list: " + t);
            }
        }

        if (cached != null) {
            try {
                JSONObject o = new JSONObject(cached);
                for (java.util.Iterator<String> it = o.keys(); it.hasNext(); ) {
                    String k = it.next();
                    map.put(k, o.optString(k, null));
                }
            } catch (Throwable ignored) {
            }
        }
        return map;
    }

    private static String httpGet(String urlString) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            if (!(conn instanceof HttpsURLConnection)) {
                Log.w(TAG, "fetch: refusing non-HTTPS URL " + urlString);
                return null;
            }
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code != 200) {
                Log.w(TAG, "fetch: HTTP " + code + " from " + urlString);
                return null;
            }
            InputStream in = conn.getInputStream();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            return sb.toString();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
