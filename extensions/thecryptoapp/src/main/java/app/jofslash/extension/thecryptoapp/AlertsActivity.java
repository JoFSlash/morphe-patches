package app.jofslash.extension.thecryptoapp;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.List;
import java.util.Locale;

/**
 * Standalone, self-contained config screen for the custom price alerts. Pure framework Views built
 * in code (no XML, no Compose), so it has no dependency on the host app's resources or runtime. It
 * is theme-aware (DayNight): colors come from theme attributes, so it reads in light and dark.
 * Registered in the host manifest with its own LAUNCHER intent-filter ("Crypto Alerts").
 */
public final class AlertsActivity extends Activity {

    static final String TAG = CustomAlertPoc.TAG;

    private ScrollView scroll;
    private LinearLayout listContainer;
    private EditText symbolInput;
    private EditText targetInput;
    private ToggleButton directionToggle;
    private Button addButton;

    /** Non-null when editing an existing rule; its id. */
    private String editingId = null;

    /** Throttle for the on-open auto-check, so reopening quickly doesn't re-hit the API. */
    private static long lastOpenCheckAt = 0L;
    private static final long OPEN_CHECK_MIN_GAP_MS = 30_000L;

    // Theme-resolved colors.
    private int accentColor;
    private int secondaryColor;
    private int metColor;
    private int unmetColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            AlertNotifier.ensureChannel(this);
            resolveColors();
            setContentView(buildRoot());
            render();
        } catch (Throwable t) {
            Log.e(TAG, "ui: onCreate failed", t);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Show stored values instantly.
        render();
        // Throttle the on-open network refresh.
        long now = System.currentTimeMillis();
        if (now - lastOpenCheckAt < OPEN_CHECK_MIN_GAP_MS) {
            return;
        }
        lastOpenCheckAt = now;
        Thread t = new Thread(() -> {
            try {
                AlertChecker.checkNow(getApplicationContext());
            } finally {
                runOnUiThread(this::render);
            }
        }, "CryptoAlerts-open");
        t.setDaemon(true);
        t.start();
    }

    private void resolveColors() {
        accentColor = attrColor(android.R.attr.colorAccent, Color.parseColor("#00A36C"));
        secondaryColor = attrColor(android.R.attr.textColorSecondary, Color.parseColor("#888888"));
        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        metColor = night ? Color.parseColor("#66BB6A") : Color.parseColor("#2E7D32");
        unmetColor = night ? Color.parseColor("#EF9A9A") : Color.parseColor("#C62828");
    }

    private int attrColor(int attr, int fallback) {
        try {
            TypedValue tv = new TypedValue();
            if (getTheme().resolveAttribute(attr, tv, true)) {
                if (tv.resourceId != 0) {
                    return getResources().getColor(tv.resourceId, getTheme());
                }
                return tv.data;
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private View buildRoot() {
        scroll = new ScrollView(this);
        // No explicit background: the DayNight window background shows through and adapts.

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(heading("Crypto Alerts"));
        root.addView(hint("Custom price alerts (injected). Free public API. No account, no Pro."));

        root.addView(subheading("Add a rule"));

        symbolInput = new EditText(this);
        symbolInput.setHint("Symbol, e.g. BTCUSDT");
        symbolInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        symbolInput.setAllCaps(true);
        root.addView(symbolInput);

        targetInput = new EditText(this);
        targetInput.setHint("Target price in USD, e.g. 65000");
        targetInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(targetInput);

        directionToggle = new ToggleButton(this);
        directionToggle.setTextOn("When price goes ABOVE target");
        directionToggle.setTextOff("When price goes BELOW target");
        directionToggle.setChecked(true);
        root.addView(directionToggle);

        addButton = new Button(this);
        addButton.setText("Add rule");
        addButton.setOnClickListener(v -> onAddOrSave());
        root.addView(addButton);

        Button checkNow = new Button(this);
        checkNow.setText("Check now (test)");
        checkNow.setOnClickListener(v -> onCheckNow());
        root.addView(checkNow);

        root.addView(subheading("Your rules"));

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);

        return scroll;
    }

    private TextView heading(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(22);
        t.setPadding(0, 0, 0, dp(4));
        return t; // primary text color from theme
    }

    private TextView subheading(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(16);
        t.setTextColor(accentColor);
        t.setPadding(0, dp(16), 0, dp(6));
        return t;
    }

    private TextView hint(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setTextColor(secondaryColor);
        return t;
    }

    private void onAddOrSave() {
        try {
            symbolInput.setError(null);
            targetInput.setError(null);

            final String symbol = symbolInput.getText().toString().trim().toUpperCase(Locale.US);
            String targetStr = targetInput.getText().toString().trim();
            if (symbol.isEmpty()) {
                symbolInput.setError("Enter a symbol, e.g. BTCUSDT");
                return;
            }
            if (targetStr.isEmpty()) {
                targetInput.setError("Enter a target price");
                return;
            }
            final double target;
            try {
                target = Double.parseDouble(targetStr);
            } catch (NumberFormatException e) {
                targetInput.setError("Not a number");
                return;
            }
            if (target <= 0) {
                targetInput.setError("Must be greater than 0");
                return;
            }

            final String base = symbol.replaceAll("(USDT|BUSD|USDC|USD)$", "");
            final String display = base.isEmpty() ? symbol : base;
            final String direction = directionToggle.isChecked() ? AlertRule.ABOVE : AlertRule.BELOW;
            final String editing = editingId;

            // Validate the symbol by fetching its price off the main thread (non-blocking).
            addButton.setEnabled(false);
            addButton.setText(editing != null ? "Saving..." : "Checking symbol...");
            Thread t = new Thread(() -> {
                final Double price = PriceFetcher.fetchPrice(getApplicationContext(), symbol);
                runOnUiThread(() -> {
                    addButton.setEnabled(true);
                    addButton.setText(editing != null ? "Save" : "Add rule");
                    if (price == null) {
                        symbolInput.setError("Unknown or unavailable symbol");
                        return;
                    }
                    if (editing != null) {
                        AlertStore.update(this, editing, symbol, display, target, direction);
                        exitEditMode();
                        toast("Rule saved");
                    } else {
                        AlertStore.add(this, symbol, display, target, direction);
                        toast("Rule added");
                    }
                    symbolInput.setText("");
                    targetInput.setText("");
                    directionToggle.setChecked(true);
                    render();
                });
            }, "CryptoAlerts-validate");
            t.setDaemon(true);
            t.start();
        } catch (Throwable t) {
            Log.e(TAG, "ui: add/save failed", t);
        }
    }

    private void beginEdit(AlertRule r) {
        editingId = r.id;
        symbolInput.setError(null);
        targetInput.setError(null);
        symbolInput.setText(r.symbol);
        targetInput.setText(PriceFormat.editable(r.targetPrice));
        directionToggle.setChecked(r.isAbove());
        addButton.setText("Save");
        scroll.smoothScrollTo(0, 0);
        toast("Editing " + r.displayName + " - change and Save");
    }

    private void exitEditMode() {
        editingId = null;
        addButton.setText("Add rule");
    }

    private void onCheckNow() {
        toast("Checking prices...");
        Thread t = new Thread(() -> {
            try {
                AlertChecker.checkNow(getApplicationContext());
            } finally {
                runOnUiThread(this::render);
            }
        }, "CryptoAlerts-manual");
        t.setDaemon(true);
        t.start();
    }

    private void render() {
        try {
            listContainer.removeAllViews();
            List<AlertRule> rules = AlertStore.load(this);
            if (rules.isEmpty()) {
                listContainer.addView(hint("No rules yet. Add one above."));
                return;
            }
            for (final AlertRule r : rules) {
                listContainer.addView(ruleRow(r));
            }
        } catch (Throwable t) {
            Log.e(TAG, "ui: render failed", t);
        }
    }

    private View ruleRow(final AlertRule r) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        row.addView(textCol, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText(String.format(Locale.US, "%s %s %s",
                r.displayName, r.isAbove() ? "↑ above" : "↓ below",
                PriceFormat.money(r.targetPrice)));
        title.setTextSize(15);
        textCol.addView(title);

        TextView status = new TextView(this);
        status.setTextSize(13);
        if (r.hasLastPrice) {
            boolean met = r.conditionMet(r.lastPrice);
            status.setText(String.format(Locale.US, "now %s  %s",
                    PriceFormat.money(r.lastPrice), met ? "✓ met" : "✗ not met"));
            status.setTextColor(met ? metColor : unmetColor);
        } else {
            status.setText("not checked yet");
            status.setTextColor(secondaryColor);
        }
        textCol.addView(status);

        // Tap the text to edit this rule.
        textCol.setClickable(true);
        textCol.setOnClickListener(v -> beginEdit(r));

        Switch enable = new Switch(this);
        enable.setChecked(r.enabled);
        enable.setOnCheckedChangeListener((b, checked) ->
                AlertStore.setEnabled(AlertsActivity.this, r.id, checked));
        row.addView(enable);

        Button del = new Button(this);
        del.setText("Delete");
        del.setOnClickListener(v -> {
            if (r.id.equals(editingId)) exitEditMode();
            AlertStore.delete(AlertsActivity.this, r.id);
            render();
        });
        row.addView(del);

        return row;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
