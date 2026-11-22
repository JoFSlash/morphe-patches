package app.jofslash.extension.booking;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The "Price history" tab. Everything comes from prices saved on this phone: Booking.com keeps no
 * price history and no site publishes past hotel prices, so the history starts when the patch was
 * installed.
 *
 * <p>Two charts, both over the time the prices were seen, limited by the range buttons:
 * <ol>
 * <li>the prices of this exact stay (dates, adults, rooms);</li>
 * <li>every price saved for this hotel over the past year, for any dates, per night, the lowest
 * of each day, to show the hotel's general trend and seasons.</li>
 * </ol>
 */
final class HistoryTab {
    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final String[] RANGE_LABELS = {Strings.RANGE_1M, Strings.RANGE_6M, Strings.RANGE_1Y, Strings.RANGE_ALL};
    private static final long[] RANGE_DAYS = {30, 182, 365, -1};
    /** The hotel chart never looks back further than this. */
    private static final long HOTEL_CHART_DAYS = 365;
    /** Most points one chart draws; older ones are left out so the line stays readable. */
    private static final int MAX_POINTS = 90;

    private final Activity activity;
    private final Panel panel;
    private final Panel.Colors colors;
    private final HotelInfo hotel;
    private final Stay stay;

    private LinearLayout content;
    private final TextView[] rangeButtons = new TextView[RANGE_LABELS.length];
    private List<PriceDb.Row> all;
    private List<PriceDb.Row> others;
    private int range = RANGE_LABELS.length - 1;
    /** The Price watch patch is on. */
    private final boolean watchOn;
    private LinearLayout watchBox;
    private List<PriceDb.Watch> watches;

    HistoryTab(Activity activity, Panel panel, Panel.Colors colors, HotelInfo hotel, Stay stay) {
        this.activity = activity;
        this.panel = panel;
        this.colors = colors;
        this.hotel = hotel;
        this.stay = stay;
        this.watchOn = PriceWatch.isAvailable(activity);
    }

    View view() {
        ScrollView scroll = new ScrollView(activity);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(panel.dp(16), panel.dp(12), panel.dp(16), panel.dp(24));
        scroll.addView(list);

        if (watchOn) {
            watchBox = new LinearLayout(activity);
            watchBox.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = panel.dp(16);
            list.addView(watchBox, lp);
            // Back from Android's notification prompt or settings: show whether they're on now.
            watchBox.getViewTreeObserver().addOnWindowFocusChangeListener(focused -> {
                if (focused && watches != null) renderWatch();
            });
        }

        LinearLayout ranges = new LinearLayout(activity);
        ranges.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < RANGE_LABELS.length; i++) {
            final int index = i;
            TextView b = panel.text(RANGE_LABELS[i], 14, colors.text, true);
            b.setGravity(Gravity.CENTER);
            b.setPadding(panel.dp(12), panel.dp(6), panel.dp(12), panel.dp(6));
            b.setOnClickListener(v -> {
                range = index;
                render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = panel.dp(8);
            ranges.addView(b, lp);
            rangeButtons[i] = b;
        }
        list.addView(ranges);

        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        list.addView(content);

        final int hotelId = hotel.id;
        final boolean loadWatches = watchOn;
        PriceHistory.load(activity, db -> new Object[]{db.forHotel(hotelId),
                stay == null ? null : db.otherStays(hotelId, stay, 6),
                loadWatches ? db.watches() : null}, result -> {
            all = result == null ? new ArrayList<>() : cast(result[0]);
            others = result == null ? null : cast(result[1]);
            watches = result == null || result[2] == null ? new ArrayList<>() : castWatches(result[2]);
            render();
        });
        return scroll;
    }

    @SuppressWarnings("unchecked")
    private static List<PriceDb.Watch> castWatches(Object o) {
        return (List<PriceDb.Watch>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<PriceDb.Row> cast(Object o) {
        return o == null ? new ArrayList<>() : (List<PriceDb.Row>) o;
    }

    private void render() {
        for (int i = 0; i < rangeButtons.length; i++) {
            boolean on = i == range;
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(panel.dp(16));
            bg.setColor(on ? colors.accent : colors.surface);
            rangeButtons[i].setBackground(bg);
            rangeButtons[i].setTextColor(on ? colors.onAccent : colors.text);
        }
        content.removeAllViews();
        if (all == null) return;

        String currency = hotel.currency;
        if (currency == null && !all.isEmpty()) currency = all.get(all.size() - 1).currency;
        long now = System.currentTimeMillis();
        long since = RANGE_DAYS[range] < 0 ? 0 : now - RANGE_DAYS[range] * DAY;

        stayChart(currency, since);
        hotelChart(currency, Math.max(since, now - HOTEL_CHART_DAYS * DAY));

        if (others != null && !others.isEmpty()) {
            content.addView(panel.spaced(panel.text(Strings.HISTORY_OTHER_DATES, 16, colors.text, true), 24));
            for (PriceDb.Row r : others) {
                content.addView(panel.spaced(panel.text(String.format(Strings.HISTORY_OTHER_ROW,
                        Panel.shortDate(r.checkIn), Panel.shortDate(r.checkOut), Panel.money(r.price, r.currency),
                        Panel.day(r.time)), 14, colors.text, false), 6));
            }
        }
        if (watchOn) {
            renderWatch();
            watchList();
        }
    }

    // ---------------------------------------------------------------------------------------
    // Price watch

    private PriceDb.Watch thisWatch() {
        if (stay == null || watches == null) return null;
        for (PriceDb.Watch w : watches) {
            if (w.hotelId == hotel.id && w.stay.sameAs(stay)) return w;
        }
        return null;
    }

    /**
     * The hotel with the price to start the watch from: the page's price, or else the newest
     * price saved for this stay (a page opened from a link can come without one).
     */
    private HotelInfo watchable() {
        if (hotel.hasPrice()) return hotel;
        if (stay == null || all == null) return null;
        for (int i = all.size() - 1; i >= 0; i--) {
            PriceDb.Row r = all.get(i);
            if (r.checkIn.equals(stay.checkIn) && r.checkOut.equals(stay.checkOut) && r.adults == stay.adults
                    && r.rooms == stay.rooms) {
                return HotelInfo.ofPrice(hotel.id, hotel.name != null ? hotel.name : r.name, r.price, Double.NaN,
                        r.currency);
            }
        }
        return null;
    }

    /** The card with the "Watch this price" button, at the top of the tab. */
    private void renderWatch() {
        watchBox.removeAllViews();
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.addView(panel.text(Strings.WATCH_HEADER, 16, colors.text, true));
        final PriceDb.Watch watch = thisWatch();
        if (stay == null || (watch == null && watchable() == null)) {
            card.addView(panel.spaced(panel.text(stay == null ? Strings.HISTORY_NONE : Strings.WATCH_NEEDS_PRICE, 14,
                    colors.secondaryText, false), 6));
            watchBox.addView(panel.card(card));
            return;
        }
        if (watch == null) {
            card.addView(panel.spaced(panel.text(Strings.WATCH_EXPLAIN, 14, colors.secondaryText, false), 6));
            card.addView(panel.spaced(panel.button(Strings.WATCH_START, v -> startWatching()), 12));
        } else {
            String state = watch.lastCheck == 0 ? Strings.WATCH_FIRST
                    : String.format(Strings.WATCH_LAST, when(watch.lastCheck), status(watch));
            card.addView(panel.spaced(panel.text(state, 14, colors.text, false), 6));
            if (!PriceWatch.notificationsAllowed(activity)) {
                card.addView(panel.spaced(panel.text(Strings.WATCH_NOTIFICATIONS_OFF, 14, colors.bad, false), 8));
                card.addView(panel.link(Strings.WATCH_NOTIFICATION_SETTINGS, v -> openNotificationSettings()));
            }
            TextView stop = panel.text(Strings.WATCH_STOP, 15, colors.accent, true);
            stop.setGravity(Gravity.CENTER);
            stop.setPadding(panel.dp(16), panel.dp(12), panel.dp(16), panel.dp(12));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(panel.dp(8));
            bg.setStroke(Math.max(1, panel.dp(1)), colors.accent);
            stop.setBackground(bg);
            stop.setOnClickListener(v -> stopWatching(watch.hotelId, watch.stay));
            card.addView(panel.spaced(stop, 12));
        }
        watchBox.addView(panel.card(card));
    }

    private void startWatching() {
        final Context context = activity.getApplicationContext();
        final HotelInfo h = watchable();
        final Stay s = stay;
        if (h == null) return;
        PriceHistory.load(activity, db -> new Object[]{PriceWatch.watch(context, h, s), db.watches()}, result -> {
            if (result == null) return;
            if ((Integer) result[0] == PriceWatch.WATCH_FULL) {
                Toast.makeText(activity, String.format(Locale.getDefault(), Strings.WATCH_FULL, PriceWatch.MAX_WATCHES),
                        Toast.LENGTH_LONG).show();
            }
            watches = castWatches(result[1]);
            render();
            askForNotifications();
        });
    }

    private void stopWatching(final int hotelId, final Stay s) {
        final Context context = activity.getApplicationContext();
        PriceHistory.load(activity, db -> {
            PriceWatch.unwatch(context, hotelId, s);
            return db.watches();
        }, result -> {
            if (result == null) return;
            watches = result;
            render();
        });
    }

    /** Android 13 and later: the app asks for the notification permission itself; ask again if needed. */
    private void askForNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 0x4A0F);
        }
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
            activity.startActivity(intent);
        } catch (RuntimeException e) {
            Toast.makeText(activity, Strings.NO_BROWSER, Toast.LENGTH_SHORT).show();
        }
    }

    /** Every watched stay, at the bottom of the tab. */
    private void watchList() {
        if (watches == null || watches.isEmpty()) return;
        content.addView(panel.spaced(panel.text(String.format(Locale.getDefault(), Strings.WATCH_LIST_HEADER,
                watches.size(), PriceWatch.MAX_WATCHES), 16, colors.text, true), 28));
        content.addView(panel.spaced(panel.text(Strings.WATCH_LIST_EXPLAIN, 13, colors.secondaryText, false), 4));
        for (final PriceDb.Watch w : watches) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout texts = new LinearLayout(activity);
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView name = panel.text(w.name != null ? w.name : Strings.WATCH_UNNAMED, 14, colors.text, true);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(name);
            texts.addView(panel.text(Panel.stayText(w.stay), 13, colors.secondaryText, false));
            texts.addView(panel.text(w.lastCheck == 0 ? Strings.WATCH_STATUS_NEW
                    : when(w.lastCheck) + ": " + status(w), 13, colors.secondaryText, false));
            row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            texts.setOnClickListener(v -> {
                try {
                    activity.startActivity(PriceWatch.propertyPageIntent(activity, w.hotelId, w.stay));
                } catch (RuntimeException e) {
                    Toast.makeText(activity, Strings.NO_BROWSER, Toast.LENGTH_SHORT).show();
                }
            });
            TextView remove = panel.text("\u2715", 18, colors.secondaryText, false);
            remove.setGravity(Gravity.CENTER);
            remove.setContentDescription(Strings.WATCH_STOP);
            remove.setOnClickListener(v -> stopWatching(w.hotelId, w.stay));
            row.addView(remove, new LinearLayout.LayoutParams(panel.dp(48), panel.dp(48)));
            content.addView(panel.spaced(panel.card(row), 8));
        }
    }

    private static String status(PriceDb.Watch w) {
        if (PriceWatch.STATUS_OK.equals(w.status) && !Double.isNaN(w.lastPrice)) {
            return Panel.money(w.lastPrice, w.currency);
        }
        if (PriceWatch.STATUS_SOLD_OUT.equals(w.status)) return Strings.WATCH_STATUS_SOLD_OUT;
        if (PriceWatch.STATUS_NO_PRICE.equals(w.status)) return Strings.WATCH_STATUS_NO_PRICE;
        if (PriceWatch.STATUS_ERROR.equals(w.status)) return Strings.WATCH_STATUS_ERROR;
        return Strings.WATCH_STATUS_NEW;
    }

    /** Day and time, "22 Nov 14:05". */
    private static String when(long time) {
        return Panel.day(time) + " " + java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
                .format(new java.util.Date(time));
    }

    // ---------------------------------------------------------------------------------------
    // This stay

    private void stayChart(String currency, long since) {
        content.addView(panel.spaced(panel.text(Strings.HISTORY_HEADER, 16, colors.text, true), 16));
        if (stay == null) {
            content.addView(panel.spaced(panel.text(Strings.HISTORY_NONE, 14, colors.secondaryText, false), 8));
            return;
        }
        List<PriceDb.Row> rows = new ArrayList<>();
        boolean any = false;
        for (PriceDb.Row r : all) {
            if (!r.checkIn.equals(stay.checkIn) || !r.checkOut.equals(stay.checkOut) || r.adults != stay.adults
                    || r.rooms != stay.rooms || !r.currency.equals(currency)) continue;
            any = true;
            if (r.time >= since) rows.add(r);
        }
        if (rows.isEmpty()) {
            content.addView(panel.spaced(panel.text(any ? Strings.HISTORY_NONE_IN_RANGE : Strings.HISTORY_EMPTY, 14,
                    colors.secondaryText, false), 8));
            return;
        }
        rows = latest(rows);
        PriceDb.Row min = rows.get(0), max = rows.get(0), last = rows.get(rows.size() - 1);
        for (PriceDb.Row r : rows) {
            if (r.price < min.price) min = r;
            if (r.price > max.price) max = r;
        }
        double now = hotel.hasPrice() ? hotel.price : last.price;

        PriceChartView chart = new PriceChartView(activity, colors);
        chart.setData(rows, hotel.strike, currency);
        content.addView(panel.spaced(chart, 12));

        LinearLayout stats = new LinearLayout(activity);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(stat(Strings.HISTORY_NOW, Panel.money(now, currency), null), weight());
        stats.addView(stat(Strings.HISTORY_LOWEST, Panel.money(min.price, currency), Panel.day(min.time)), weight());
        stats.addView(stat(Strings.HISTORY_HIGHEST, Panel.money(max.price, currency), Panel.day(max.time)), weight());
        content.addView(panel.spaced(stats, 8));

        content.addView(panel.spaced(verdict(now, min.price, max.price, rows.size()), 12));
        String checks = rows.size() == 1 ? String.format(Strings.HISTORY_CHECKS_ONE, Panel.day(rows.get(0).time))
                : String.format(Locale.getDefault(), Strings.HISTORY_CHECKS, rows.size(), Panel.day(rows.get(0).time));
        content.addView(panel.spaced(panel.text(checks, 13, colors.secondaryText, false), 6));
        if (rows.size() == 1) {
            content.addView(panel.spaced(panel.text(Strings.HISTORY_FIRST, 14, colors.secondaryText, false), 8));
        }

        // The same check as the line under the price on the property page.
        hotel.memberDiscount = HotelInfo.memberDiscount(DiscountCheck.shownBadges(), hotel.memberDiscount);
        StrikeCheck.Verdict strike = StrikeCheck.check(hotel, stay, all, System.currentTimeMillis());
        if (strike != null) {
            content.addView(panel.spaced(panel.card(panel.text(strike.text, 14,
                    StrikeCheck.color(colors, strike.tone), false)), 12));
        }
    }

    // ---------------------------------------------------------------------------------------
    // This hotel, any dates

    private void hotelChart(String currency, long since) {
        content.addView(panel.spaced(panel.text(Strings.HOTEL_HEADER, 16, colors.text, true), 28));
        content.addView(panel.spaced(panel.text(Strings.HOTEL_EXPLAIN, 13, colors.secondaryText, false), 4));

        // Lowest price per night seen on each day, over all stays. Stays differ in length, so the
        // totals aren't comparable; per night they are.
        Map<Long, PriceDb.Row> byDay = new LinkedHashMap<>();
        int stays = 0;
        java.util.HashSet<String> stayKeys = new java.util.HashSet<>();
        for (PriceDb.Row r : all) {
            if (r.time < since || !r.currency.equals(currency)) continue;
            int nights = new Stay(r.checkIn, r.checkOut, r.adults, r.rooms).nights();
            double perNight = r.price / nights;
            long day = startOfDay(r.time);
            PriceDb.Row d = byDay.get(day);
            if (d == null || perNight < d.price) {
                PriceDb.Row point = new PriceDb.Row();
                point.price = perNight;
                point.time = day + DAY / 2;
                point.currency = r.currency;
                byDay.put(day, point);
            }
            if (stayKeys.add(r.checkIn + r.checkOut + r.adults + "/" + r.rooms)) stays++;
        }
        List<PriceDb.Row> points = latest(new ArrayList<>(byDay.values()));
        if (points.isEmpty()) {
            content.addView(panel.spaced(panel.text(Strings.HOTEL_EMPTY, 14, colors.secondaryText, false), 8));
            return;
        }
        PriceDb.Row min = points.get(0), max = points.get(0);
        for (PriceDb.Row r : points) {
            if (r.price < min.price) min = r;
            if (r.price > max.price) max = r;
        }
        PriceChartView chart = new PriceChartView(activity, colors);
        chart.setData(points, Double.NaN, currency);
        content.addView(panel.spaced(chart, 12));

        LinearLayout stats = new LinearLayout(activity);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(stat(Strings.HOTEL_LOWEST, Panel.money(min.price, currency), Panel.day(min.time)), weight());
        stats.addView(stat(Strings.HOTEL_HIGHEST, Panel.money(max.price, currency), Panel.day(max.time)), weight());
        content.addView(panel.spaced(stats, 8));
        content.addView(panel.spaced(panel.text(String.format(Locale.getDefault(), Strings.HOTEL_COUNT, points.size(),
                stays), 13, colors.secondaryText, false), 6));
    }

    private static long startOfDay(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** The newest {@link #MAX_POINTS} rows. */
    private static List<PriceDb.Row> latest(List<PriceDb.Row> rows) {
        return rows.size() <= MAX_POINTS ? rows : new ArrayList<>(rows.subList(rows.size() - MAX_POINTS, rows.size()));
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
        return panel.text(label, 15, color, true);
    }

    private View stat(String label, String value, String sub) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(panel.text(label, 12, colors.secondaryText, false));
        box.addView(panel.text(value, 16, colors.text, true));
        if (sub != null) box.addView(panel.text(sub, 12, colors.secondaryText, false));
        return box;
    }

    private static LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
    }
}
