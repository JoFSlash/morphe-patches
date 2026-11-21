package app.jofslash.extension.booking;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The full-screen panel the property page button opens. It has a tab for each patch that is on:
 * "Price history" (prices saved on this phone) and "Official site" (Google Hotels in a WebView).
 */
final class Panel {
    /** Colors for the light and dark theme. */
    static final class Colors {
        int background, surface, text, secondaryText, divider, accent, onAccent, good, bad;

        static Colors of(Activity activity) {
            boolean dark = (activity.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            Colors c = new Colors();
            if (dark) {
                c.background = 0xFF121212;
                c.surface = 0xFF1E1E1E;
                c.text = 0xFFF2F2F2;
                c.secondaryText = 0xFFB0B0B0;
                c.divider = 0xFF3A3A3A;
                c.accent = 0xFF57A6FF;
                c.onAccent = 0xFF00224D;
                c.good = 0xFF6BD18A;
                c.bad = 0xFFFF8A80;
            } else {
                c.background = 0xFFFFFFFF;
                c.surface = 0xFFF2F6FC;
                c.text = 0xFF1A1A1A;
                c.secondaryText = 0xFF595959;
                c.divider = 0xFFDDDDDD;
                c.accent = 0xFF006CE4;
                c.onAccent = 0xFFFFFFFF;
                c.good = 0xFF008234;
                c.bad = 0xFFD4111E;
            }
            return c;
        }
    }

    private final Activity activity;
    private final Colors colors;
    private final HotelInfo hotel;
    private final Stay stay;
    private final boolean showHistory;
    private final boolean showOfficial;
    private final float density;
    private Dialog dialog;
    private OfficialSiteTab officialTab;

    Panel(Activity activity, HotelInfo hotel, Stay stay, boolean showHistory, boolean showOfficial) {
        this.activity = activity;
        this.colors = Colors.of(activity);
        this.hotel = hotel;
        this.stay = stay;
        this.showHistory = showHistory;
        this.showOfficial = showOfficial;
        this.density = activity.getResources().getDisplayMetrics().density;
    }

    void show() {
        dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(colors.background);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });

        root.addView(header());

        final FrameLayout content = new FrameLayout(activity);
        final View historyView = showHistory ? historyTab() : null;
        if (showOfficial) officialTab = new OfficialSiteTab(activity, this, colors, hotel, stay);
        final View officialView = officialTab != null ? officialTab.view() : null;

        if (historyView != null && officialView != null) {
            final TextView[] tabs = new TextView[2];
            LinearLayout tabRow = new LinearLayout(activity);
            tabRow.setOrientation(LinearLayout.HORIZONTAL);
            tabs[0] = tab(Strings.TAB_HISTORY);
            tabs[1] = tab(Strings.TAB_OFFICIAL);
            for (int i = 0; i < 2; i++) {
                final int index = i;
                tabRow.addView(tabs[i], new LinearLayout.LayoutParams(0, dp(48), 1));
                tabs[i].setOnClickListener(v -> select(index, tabs, historyView, officialView));
            }
            root.addView(tabRow);
            root.addView(divider());
            select(0, tabs, historyView, officialView);
        } else if (officialTab != null) {
            officialTab.start();
        }
        if (historyView != null) content.addView(historyView, matchParent());
        if (officialView != null) content.addView(officialView, matchParent());
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(colors.background));
            if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            boolean dark = colors.background != 0xFFFFFFFF;
            if (Build.VERSION.SDK_INT >= 30 && window.getInsetsController() != null) {
                int light = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                window.getInsetsController().setSystemBarsAppearance(dark ? 0 : light, light);
            }
        }
        dialog.setOnDismissListener(d -> {
            if (officialTab != null) officialTab.destroy();
        });
        dialog.show();
    }

    private void select(int index, TextView[] tabs, View historyView, View officialView) {
        for (int i = 0; i < tabs.length; i++) {
            boolean on = i == index;
            tabs[i].setTextColor(on ? colors.accent : colors.secondaryText);
            tabs[i].setBackground(on ? underlineDrawable() : null);
        }
        historyView.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        officialView.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        // Google is only loaded when the user opens its tab.
        if (index == 1) officialTab.start();
    }

    private android.graphics.drawable.Drawable underlineDrawable() {
        GradientDrawable line = new GradientDrawable();
        line.setColor(colors.accent);
        android.graphics.drawable.LayerDrawable layer = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[]{line});
        layer.setLayerInset(0, 0, dp(45), 0, 0);
        return layer;
    }

    private TextView tab(String label) {
        TextView t = text(label, 15, colors.secondaryText, true);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private View header() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(8), dp(4), dp(8));

        LinearLayout titles = new LinearLayout(activity);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(hotel.name != null ? hotel.name : Strings.DIALOG_TITLE, 18, colors.text, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        titles.addView(title);
        if (stay != null) titles.addView(text(stayText(stay), 13, colors.secondaryText, false));
        row.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView close = text("✕", 20, colors.text, false);
        close.setGravity(Gravity.CENTER);
        close.setContentDescription(Strings.CLOSE);
        close.setOnClickListener(v -> dialog.dismiss());
        row.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return row;
    }

    // ---------------------------------------------------------------------------------------
    // Price history tab

    private View historyTab() {
        ScrollView scroll = new ScrollView(activity);
        final LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(list);

        if (stay == null) {
            list.addView(text(Strings.HISTORY_NONE, 14, colors.secondaryText, false));
            return scroll;
        }
        final int hotelId = hotel.id;
        PriceHistory.load(activity, db -> new Object[]{db.forStay(hotelId, stay), db.otherStays(hotelId, stay, 6)},
                result -> fillHistory(list, result));
        return scroll;
    }

    @SuppressWarnings("unchecked")
    private void fillHistory(LinearLayout list, Object[] result) {
        List<PriceDb.Row> rows = result == null ? null : (List<PriceDb.Row>) result[0];
        List<PriceDb.Row> others = result == null ? null : (List<PriceDb.Row>) result[1];

        // Only chart prices in the currency shown now.
        String currency = hotel.currency;
        if (rows != null && !rows.isEmpty()) {
            if (currency == null) currency = rows.get(rows.size() - 1).currency;
            java.util.ArrayList<PriceDb.Row> same = new java.util.ArrayList<>();
            for (PriceDb.Row r : rows) if (r.currency.equals(currency)) same.add(r);
            rows = same;
        }

        list.addView(text(Strings.HISTORY_HEADER, 16, colors.text, true));
        if (rows == null || rows.isEmpty()) {
            list.addView(spaced(text(Strings.HISTORY_EMPTY, 14, colors.secondaryText, false), 8));
        } else {
            PriceDb.Row min = rows.get(0), max = rows.get(0), last = rows.get(rows.size() - 1);
            for (PriceDb.Row r : rows) {
                if (r.price < min.price) min = r;
                if (r.price > max.price) max = r;
            }
            double now = hotel.hasPrice() ? hotel.price : last.price;

            PriceChartView chart = new PriceChartView(activity, colors);
            chart.setData(rows, hotel.strike, currency);
            list.addView(spaced(chart, 12));

            LinearLayout stats = new LinearLayout(activity);
            stats.setOrientation(LinearLayout.HORIZONTAL);
            stats.addView(stat(Strings.HISTORY_NOW, money(now, currency), null), weight());
            stats.addView(stat(Strings.HISTORY_LOWEST, money(min.price, currency), day(min.time)), weight());
            stats.addView(stat(Strings.HISTORY_HIGHEST, money(max.price, currency), day(max.time)), weight());
            list.addView(spaced(stats, 8));

            list.addView(spaced(verdict(now, min.price, max.price, rows.size()), 12));
            String checks = rows.size() == 1 ? String.format(Strings.HISTORY_CHECKS_ONE, day(rows.get(0).time))
                    : String.format(Locale.getDefault(), Strings.HISTORY_CHECKS, rows.size(), day(rows.get(0).time));
            list.addView(spaced(text(checks, 13, colors.secondaryText, false), 6));
            if (rows.size() == 1) {
                list.addView(spaced(text(Strings.HISTORY_FIRST, 14, colors.secondaryText, false), 8));
            }

            if (!Double.isNaN(hotel.strike)) {
                String note;
                if (rows.size() < 3) {
                    note = String.format(Strings.STRIKE_FEW, money(hotel.strike, currency));
                } else if (hotel.strike > max.price + 0.5) {
                    note = String.format(Strings.STRIKE_HIGHER, money(hotel.strike, currency), money(max.price, currency));
                } else {
                    note = String.format(Strings.STRIKE_SEEN, money(hotel.strike, currency), day(max.time));
                }
                list.addView(spaced(card(text(note, 14, colors.text, false)), 12));
            }
        }

        if (others != null && !others.isEmpty()) {
            list.addView(spaced(text(Strings.HISTORY_OTHER_DATES, 16, colors.text, true), 20));
            for (PriceDb.Row r : others) {
                list.addView(spaced(text(String.format(Strings.HISTORY_OTHER_ROW, shortDate(r.checkIn),
                        shortDate(r.checkOut), money(r.price, r.currency), day(r.time)), 14, colors.text, false), 6));
            }
        }
    }

    private View verdict(double now, double min, double max, int count) {
        String label;
        int color;
        if (count < 3) {
            label = Strings.VERDICT_FEW;
            color = colors.secondaryText;
        } else if (max - min < 0.5) {
            label = Strings.VERDICT_SAME;
            color = colors.secondaryText;
        } else {
            double pos = (now - min) / (max - min);
            if (pos <= 0.25) {
                label = Strings.VERDICT_LOW;
                color = colors.good;
            } else if (pos >= 0.75) {
                label = Strings.VERDICT_HIGH;
                color = colors.bad;
            } else {
                label = Strings.VERDICT_TYPICAL;
                color = colors.text;
            }
        }
        return text(label, 15, color, true);
    }

    private View stat(String label, String value, String sub) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text(label, 12, colors.secondaryText, false));
        box.addView(text(value, 16, colors.text, true));
        if (sub != null) box.addView(text(sub, 12, colors.secondaryText, false));
        return box;
    }

    // ---------------------------------------------------------------------------------------
    // Helpers shared with the official site tab

    static String stayText(Stay stay) {
        String adults = stay.adults == 1 ? Strings.ADULTS_ONE : String.format(Locale.getDefault(), Strings.ADULTS_MANY, stay.adults);
        String rooms = stay.rooms == 1 ? Strings.ROOMS_ONE : String.format(Locale.getDefault(), Strings.ROOMS_MANY, stay.rooms);
        return String.format(Strings.HISTORY_STAY, shortDate(stay.checkIn), shortDate(stay.checkOut), adults, rooms);
    }

    static String shortDate(String isoDate) {
        try {
            return new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(new Date(Stay.utcMillis(isoDate)
                    + 12L * 3600 * 1000));
        } catch (RuntimeException e) {
            return isoDate;
        }
    }

    static String day(long time) {
        return new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(time));
    }

    static String money(double value, String currency) {
        try {
            NumberFormat f = NumberFormat.getCurrencyInstance(Locale.getDefault());
            if (currency != null && !currency.isEmpty()) f.setCurrency(Currency.getInstance(currency));
            f.setMaximumFractionDigits(Math.abs(value - Math.rint(value)) < 0.005 || value >= 10 ? 0 : 2);
            f.setMinimumFractionDigits(f.getMaximumFractionDigits());
            return f.format(value);
        } catch (RuntimeException e) {
            return String.format(Locale.getDefault(), "%.0f %s", value, currency == null ? "" : currency);
        }
    }

    void openInBrowser(String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, Strings.NO_BROWSER, Toast.LENGTH_SHORT).show();
        }
    }

    TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(activity);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    TextView link(String s, View.OnClickListener onClick) {
        TextView t = text(s, 14, colors.accent, true);
        t.setPadding(0, dp(10), 0, dp(10));
        t.setOnClickListener(onClick);
        return t;
    }

    TextView button(String s, View.OnClickListener onClick) {
        TextView t = text(s, 15, colors.onAccent, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colors.accent);
        bg.setCornerRadius(dp(8));
        t.setBackground(bg);
        t.setOnClickListener(onClick);
        return t;
    }

    View card(View inside) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colors.surface);
        bg.setCornerRadius(dp(8));
        box.setBackground(bg);
        box.addView(inside);
        return box;
    }

    View divider() {
        View v = new View(activity);
        v.setBackgroundColor(colors.divider);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1) / 2)));
        return v;
    }

    View spaced(View v, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        v.setLayoutParams(lp);
        return v;
    }

    private static LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
    }

    private static FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    int dp(float v) {
        return Math.round(v * density);
    }
}
