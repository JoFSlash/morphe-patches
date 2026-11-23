package app.jofslash.extension.thecryptoapp;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Fetches a spot price from a free public API: Binance first, CoinGecko as a fallback. No API key,
 * no account. Entirely self-contained.
 */
public final class PriceFetcher {

    static final String TAG = CustomAlertPoc.TAG;

    private static final String BINANCE = "https://api.binance.com/api/v3/ticker/price?symbol=";
    private static final String COINGECKO =
            "https://api.coingecko.com/api/v3/simple/price?vs_currencies=usd&ids=";

    /** Minimal ticker-base -> CoinGecko id map for the fallback. Best-effort. */
    private static final Map<String, String> COINGECKO_IDS = new HashMap<>();

    static {
        COINGECKO_IDS.put("BTC", "bitcoin");
        COINGECKO_IDS.put("ETH", "ethereum");
        COINGECKO_IDS.put("BNB", "binancecoin");
        COINGECKO_IDS.put("SOL", "solana");
        COINGECKO_IDS.put("XRP", "ripple");
        COINGECKO_IDS.put("ADA", "cardano");
        COINGECKO_IDS.put("DOGE", "dogecoin");
        COINGECKO_IDS.put("AVAX", "avalanche-2");
        COINGECKO_IDS.put("DOT", "polkadot");
        COINGECKO_IDS.put("MATIC", "matic-network");
        COINGECKO_IDS.put("LTC", "litecoin");
        COINGECKO_IDS.put("LINK", "chainlink");
        COINGECKO_IDS.put("TRX", "tron");
    }

    private PriceFetcher() {
    }

    /** USD-ish price for a Binance symbol such as "BTCUSDT", or null if both sources fail. */
    public static Double fetchPrice(String symbol) {
        Double price = fetchBinance(symbol);
        if (price == null) {
            Log.w(TAG, "fetch: Binance unavailable for " + symbol + ", trying CoinGecko fallback");
            price = fetchCoinGecko(symbol);
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

    private static Double fetchCoinGecko(String symbol) {
        try {
            String base = symbol.toUpperCase(Locale.US)
                    .replaceAll("(USDT|BUSD|USDC|USD)$", "");
            String id = COINGECKO_IDS.get(base);
            if (id == null) {
                Log.w(TAG, "fetch: no CoinGecko id mapping for base " + base);
                return null;
            }
            String body = httpGet(COINGECKO + id);
            if (body == null) return null;
            JSONObject coin = new JSONObject(body).optJSONObject(id);
            if (coin == null || !coin.has("usd")) return null;
            return coin.getDouble("usd");
        } catch (Throwable t) {
            Log.w(TAG, "fetch: CoinGecko fetch failed for " + symbol + ": " + t);
            return null;
        }
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
            conn.setReadTimeout(10000);
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
