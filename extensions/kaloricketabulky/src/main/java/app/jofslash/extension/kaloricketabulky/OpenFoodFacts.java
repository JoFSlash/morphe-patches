package app.jofslash.extension.kaloricketabulky;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Used by the Salt and sugar from Open Food Facts patch. A food's detail page gets a small button
 * when the food has a barcode; tapping it looks the barcode up in Open Food Facts, a free and open
 * food database, and shows the salt and sugars it lists per 100 g. Nothing is read from the app's
 * own Premium values: the numbers come from Open Food Facts only, and only the barcode is sent,
 * only when the button is tapped.
 */
@SuppressWarnings("unused")
public final class OpenFoodFacts {
    private static final String TAG = "JoFSlashPatches";
    private static final String DETAIL_FRAGMENT = "FoodDetailComposableFragment";
    private static final String API = "https://world.openfoodfacts.org/api/v2/product/";
    private static final String PAGE = "https://world.openfoodfacts.org/product/";
    // Open Food Facts asks apps to name themselves; no contact details are sent.
    private static final String USER_AGENT = "MorphePatches-KaloTab/1.0 (open-source Morphe patch)";

    /** Barcodes of the foods whose detail was loaded, by food id. */
    private static final Map<String, List<String>> EANS = new LinkedHashMap<String, List<String>>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest) {
            return size() > 100;
        }
    };

    private OpenFoodFacts() {
    }

    /** Injection point: start of the mapping of a food's detail from the API to the app's Food. */
    public static void remember(Object detailDto) {
        try {
            if (detailDto == null) return;
            Object id = detailDto.getClass().getMethod("getId").invoke(detailDto);
            Object eans = detailDto.getClass().getMethod("getEans").invoke(detailDto);
            if (!(id instanceof String) || !(eans instanceof List)) return;
            List<String> codes = new ArrayList<>();
            for (Object code : (List<?>) eans) {
                if (code instanceof String && ((String) code).matches("\\d{6,14}")) codes.add((String) code);
            }
            synchronized (EANS) {
                EANS.put((String) id, codes);
            }
        } catch (Throwable ex) {
            Log.e(TAG, "Open Food Facts: couldn't read the food's barcodes", ex);
        }
    }

    /** The fragment whose page is being made (pages are made on the main thread, one at a time). */
    private static Object creating;

    /** Injection point: start of ComposableFragment.onCreateView. */
    public static void creating(Object fragment) {
        creating = fragment;
    }

    /**
     * Injection point: end of ComposableFragment.onCreateView. On a food's detail page, the button
     * comes with the page; every page is returned as it is.
     */
    public static View wrap(View page) {
        Object fragment = creating;
        creating = null;
        try {
            if (fragment == null || page == null || !fragment.getClass().getName().endsWith(DETAIL_FRAGMENT)) return page;
            Bundle args = (Bundle) fragment.getClass().getMethod("getArguments").invoke(fragment);
            String foodId = args == null ? null : args.getString("foodID");
            String scanned = args == null ? null : args.getString("ean");
            Context context = page.getContext();
            TextView button = button(context);
            button.setVisibility(View.GONE);
            Handler main = new Handler(Looper.getMainLooper());
            // The button floats over the window, bottom right, while the page is shown; the page
            // itself stays as the app made it.
            page.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    try {
                        ViewGroup content = v.getRootView().findViewById(android.R.id.content);
                        if (!(content instanceof FrameLayout) || button.getParent() != null) return;
                        int bottom = 0;
                        android.view.WindowInsets insets = v.getRootWindowInsets();
                        if (insets != null) bottom = insets.getSystemWindowInsetBottom();
                        FrameLayout.LayoutParams where = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END);
                        int margin = dp(context, 16);
                        where.setMargins(margin, margin, margin, margin + bottom);
                        content.addView(button, where);
                    } catch (Throwable ex) {
                        Log.e(TAG, "Open Food Facts: couldn't show the button", ex);
                    }
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    ViewGroup parent = (ViewGroup) button.getParent();
                    if (parent != null) parent.removeView(button);
                }
            });
            // The detail loads after the page is made: show the button once its barcode is known.
            Runnable[] check = new Runnable[1];
            long until = System.currentTimeMillis() + 30_000;
            check[0] = () -> {
                List<String> codes = codes(foodId, scanned);
                if (!codes.isEmpty()) {
                    button.setVisibility(View.VISIBLE);
                    button.setOnClickListener(v -> lookUp(v.getContext(), codes));
                } else if (System.currentTimeMillis() < until) {
                    main.postDelayed(check[0], 400);
                }
            };
            main.post(check[0]);
            return page;
        } catch (Throwable ex) {
            Log.e(TAG, "Open Food Facts: couldn't add the button", ex);
            return page;
        }
    }

    /** The barcodes to try: the one scanned to open the page first, then the food's own. */
    private static List<String> codes(String foodId, String scanned) {
        List<String> out = new ArrayList<>();
        if (scanned != null && scanned.matches("\\d{6,14}")) out.add(scanned);
        if (foodId != null) {
            List<String> known;
            synchronized (EANS) {
                known = EANS.get(foodId);
            }
            if (known != null) for (String code : known) if (!out.contains(code)) out.add(code);
        }
        return out;
    }

    private static TextView button(Context context) {
        TextView button = new TextView(context);
        button.setText(t("Salt and sugar"));
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setTextColor(Color.WHITE);
        button.setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10));
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xE0466A1E);
        background.setCornerRadius(dp(context, 22));
        button.setBackground(background);
        button.setElevation(dp(context, 6));
        button.setContentDescription(t("Salt and sugar from Open Food Facts"));
        return button;
    }

    private static void lookUp(Context context, List<String> codes) {
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            Product found = null;
            String failure = null;
            for (String code : codes) {
                try {
                    found = fetch(code);
                    if (found != null) break;
                } catch (Exception ex) {
                    Log.e(TAG, "Open Food Facts: lookup failed", ex);
                    failure = t("Open Food Facts couldn't be reached. Try again later.");
                }
            }
            Product product = found;
            String error = failure;
            main.post(() -> show(context, codes.get(0), product, error));
        }, "jofslash-off").start();
    }

    /** What Open Food Facts lists for one product, or null when it doesn't know the barcode. */
    static final class Product {
        String name;
        String code;
        Double salt, sugars;
    }

    private static Product fetch(String code) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(API + code
                + ".json?fields=code,product_name,nutriments").openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "application/json");
        try {
            int status = connection.getResponseCode();
            if (status == 404) return null;
            if (status != 200) throw new IllegalStateException("HTTP " + status);
            return parse(read(connection.getInputStream()), code);
        } finally {
            connection.disconnect();
        }
    }

    /** Reads Open Food Facts' answer; null when it says the product isn't there. */
    static Product parse(String json, String code) throws Exception {
        JSONObject root = new JSONObject(json);
        if (root.optInt("status", 0) != 1) return null;
        JSONObject product = root.optJSONObject("product");
        if (product == null) return null;
        Product out = new Product();
        out.code = product.optString("code", code);
        out.name = product.optString("product_name", "").trim();
        JSONObject n = product.optJSONObject("nutriments");
        if (n != null) {
            out.salt = number(n, "salt_100g");
            // Without salt, salt is sodium times 2.5 (the EU rule for the label).
            if (out.salt == null) {
                Double sodium = number(n, "sodium_100g");
                if (sodium != null) out.salt = sodium * 2.5;
            }
            out.sugars = number(n, "sugars_100g");
        }
        return out;
    }

    private static Double number(JSONObject object, String key) {
        if (!object.has(key) || object.isNull(key)) return null;
        Object value = object.opt(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value).replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String read(InputStream in) throws Exception {
        try (InputStream stream = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = stream.read(buffer)) > 0) {
                out.write(buffer, 0, n);
                if (out.size() > 2_000_000) throw new IllegalStateException("Answer too long");
            }
            return out.toString("UTF-8");
        }
    }

    private static void show(Context context, String code, Product product, String error) {
        try {
            String message;
            String page = PAGE + (product != null && product.code != null ? product.code : code);
            if (product != null) {
                StringBuilder text = new StringBuilder();
                if (!product.name.isEmpty()) text.append(product.name).append("\n\n");
                text.append(t("Per 100 g:")).append('\n');
                text.append(t("Salt")).append(": ").append(grams(product.salt)).append('\n');
                text.append(t("Sugars")).append(": ").append(grams(product.sugars)).append("\n\n");
                text.append(t("From Open Food Facts (open data, ODbL). It can differ from the label: the label on the package counts."));
                message = text.toString();
            } else if (error != null) {
                message = error;
            } else {
                message = t("Open Food Facts doesn't know this product yet. You can add it there.");
            }
            new AlertDialog.Builder(context)
                    .setTitle(t("Salt and sugar"))
                    .setMessage(message)
                    .setPositiveButton(t("Close"), null)
                    .setNeutralButton(t("Open Food Facts"), (d, w) -> {
                        try {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(page))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        } catch (Exception ex) {
                            Log.e(TAG, "Open Food Facts: couldn't open the page", ex);
                        }
                    })
                    .show();
        } catch (Exception ex) {
            Log.e(TAG, "Open Food Facts: couldn't show the values", ex);
        }
    }

    private static String grams(Double value) {
        if (value == null) return t("not listed");
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(value < 1 ? 2 : 1);
        format.setMinimumFractionDigits(0);
        return format.format(value) + " g";
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    // ---------------------------------------------------------------- texts

    private static final Map<String, Map<String, String>> TEXTS = new HashMap<>();

    static {
        Map<String, String> cs = new HashMap<>();
        cs.put("Salt and sugar", "Sůl a cukr");
        cs.put("Salt and sugar from Open Food Facts", "Sůl a cukr z Open Food Facts");
        cs.put("Per 100 g:", "Na 100 g:");
        cs.put("Salt", "Sůl");
        cs.put("Sugars", "Cukry");
        cs.put("not listed", "neuvedeno");
        cs.put("Close", "Zavřít");
        cs.put("Open Food Facts", "Open Food Facts");
        cs.put("From Open Food Facts (open data, ODbL). It can differ from the label: the label on the package counts.",
                "Z Open Food Facts (otevřená data, ODbL). Může se lišit od etikety: platí údaje na obalu.");
        cs.put("Open Food Facts doesn't know this product yet. You can add it there.",
                "Open Food Facts tento výrobek zatím nezná. Můžeš ho tam přidat.");
        cs.put("Open Food Facts couldn't be reached. Try again later.",
                "Open Food Facts se nepodařilo spojit. Zkus to později.");
        TEXTS.put("cs", cs);
        Map<String, String> sk = new HashMap<>();
        sk.put("Salt and sugar", "Soľ a cukor");
        sk.put("Salt and sugar from Open Food Facts", "Soľ a cukor z Open Food Facts");
        sk.put("Per 100 g:", "Na 100 g:");
        sk.put("Salt", "Soľ");
        sk.put("Sugars", "Cukry");
        sk.put("not listed", "neuvedené");
        sk.put("Close", "Zavrieť");
        sk.put("Open Food Facts", "Open Food Facts");
        sk.put("From Open Food Facts (open data, ODbL). It can differ from the label: the label on the package counts.",
                "Z Open Food Facts (otvorené dáta, ODbL). Môže sa líšiť od etikety: platia údaje na obale.");
        sk.put("Open Food Facts doesn't know this product yet. You can add it there.",
                "Open Food Facts tento výrobok zatiaľ nepozná. Môžeš ho tam pridať.");
        sk.put("Open Food Facts couldn't be reached. Try again later.",
                "Open Food Facts sa nepodarilo spojiť. Skús to neskôr.");
        TEXTS.put("sk", sk);
        Map<String, String> pl = new HashMap<>();
        pl.put("Salt and sugar", "Sól i cukier");
        pl.put("Salt and sugar from Open Food Facts", "Sól i cukier z Open Food Facts");
        pl.put("Per 100 g:", "Na 100 g:");
        pl.put("Salt", "Sól");
        pl.put("Sugars", "Cukry");
        pl.put("not listed", "brak danych");
        pl.put("Close", "Zamknij");
        pl.put("Open Food Facts", "Open Food Facts");
        pl.put("From Open Food Facts (open data, ODbL). It can differ from the label: the label on the package counts.",
                "Z Open Food Facts (otwarte dane, ODbL). Może się różnić od etykiety: liczą się dane na opakowaniu.");
        pl.put("Open Food Facts doesn't know this product yet. You can add it there.",
                "Open Food Facts nie zna jeszcze tego produktu. Możesz go tam dodać.");
        pl.put("Open Food Facts couldn't be reached. Try again later.",
                "Nie udało się połączyć z Open Food Facts. Spróbuj później.");
        TEXTS.put("pl", pl);
    }

    private static String t(String english) {
        Map<String, String> table = TEXTS.get(Locale.getDefault().getLanguage());
        String text = table == null ? null : table.get(english);
        return text != null ? text : english;
    }
}
