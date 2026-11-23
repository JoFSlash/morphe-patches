package app.jofslash.extension.thecryptoapp;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;

/**
 * The one place that holds The Crypto App's palette, so all our programmatic screens match the
 * host look (card-based, royal-blue accent) in both light and dark. Colors are picked at runtime
 * from {@link Configuration#UI_MODE_NIGHT_MASK}; the host ships both themes.
 */
public final class Palette {

    public final boolean night;

    public final int screenBg;
    public final int cardBg;
    public final int cardBg2;
    public final int cardBorder;
    public final int textPrimary;
    public final int textSecondary;
    public final int accent;
    public final int accentPressed;
    public final int up;
    public final int down;
    public final int gold;

    private final float density;

    public Palette(Context ctx) {
        this.density = ctx.getResources().getDisplayMetrics().density;
        this.night = (ctx.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        accent = Color.parseColor("#2662F0");
        accentPressed = Color.parseColor("#332662F0");
        up = Color.parseColor("#40B42E");
        down = Color.parseColor("#FB0006");
        gold = Color.parseColor("#F1AF05");

        if (night) {
            screenBg = Color.parseColor("#040404");
            cardBg = Color.parseColor("#191A1E");
            cardBg2 = Color.parseColor("#0F1011");
            cardBorder = Color.parseColor("#22FFFFFF");
            textPrimary = Color.parseColor("#FFFFFF");
            textSecondary = Color.parseColor("#87A6C2");
        } else {
            screenBg = Color.parseColor("#EEF1F6");
            cardBg = Color.parseColor("#FFFFFF");
            cardBg2 = Color.parseColor("#EFF1F4");
            cardBorder = Color.parseColor("#14203A5C");
            textPrimary = Color.parseColor("#0F1011");
            textSecondary = Color.parseColor("#90A1AC");
        }
    }

    public int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, density());
    }

    private android.util.DisplayMetrics density() {
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        dm.density = density;
        return dm;
    }

    /** A rounded card: surface fill + thin translucent border. */
    public GradientDrawable card() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(cardBg);
        g.setCornerRadius(dp(14));
        g.setStroke(Math.max(1, dp(1)), cardBorder);
        return g;
    }

    /** Primary filled button background (royal-blue). */
    public GradientDrawable primaryButton() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(accent);
        g.setCornerRadius(dp(12));
        return g;
    }

    /** Secondary button background (subtle surface + border). */
    public GradientDrawable secondaryButton() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(cardBg2);
        g.setCornerRadius(dp(12));
        g.setStroke(Math.max(1, dp(1)), cardBorder);
        return g;
    }

    /** Boxed input background. */
    public GradientDrawable inputBox() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(cardBg);
        g.setCornerRadius(dp(10));
        g.setStroke(Math.max(1, dp(1)), cardBorder);
        return g;
    }
}
