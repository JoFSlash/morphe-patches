package app.jofslash.extension.thecryptoapp;

import android.app.Activity;
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
 * in code (no XML, no Compose), so it has no dependency on the host app's resources or runtime.
 * Registered in the host manifest with its own LAUNCHER intent-filter ("Crypto Alerts").
 */
public final class AlertsActivity extends Activity {

    static final String TAG = CustomAlertPoc.TAG;

    private LinearLayout listContainer;
    private EditText symbolInput;
    private EditText targetInput;
    private ToggleButton directionToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            AlertNotifier.ensureChannel(this);
            setContentView(buildRoot());
            render();
        } catch (Throwable t) {
            Log.e(TAG, "ui: onCreate failed", t);
        }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private View buildRoot() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#FFFFFF"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(heading("Crypto Alerts"));
        root.addView(hint("Custom price alerts (injected). Free public API. No account, no Pro."));

        // --- Add-rule form ---
        root.addView(subheading("Add a rule"));

        symbolInput = new EditText(this);
        symbolInput.setHint("Symbol, e.g. BTCUSDT");
        symbolInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
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

        Button add = new Button(this);
        add.setText("Add rule");
        add.setOnClickListener(v -> onAdd());
        root.addView(add);

        // --- Test helper ---
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
        t.setTextColor(Color.parseColor("#111111"));
        t.setPadding(0, 0, 0, dp(4));
        return t;
    }

    private TextView subheading(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(16);
        t.setTextColor(Color.parseColor("#00A36C"));
        t.setPadding(0, dp(16), 0, dp(6));
        return t;
    }

    private TextView hint(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setTextColor(Color.parseColor("#777777"));
        return t;
    }

    private void onAdd() {
        try {
            String symbol = symbolInput.getText().toString().trim().toUpperCase(Locale.US);
            String targetStr = targetInput.getText().toString().trim();
            if (symbol.isEmpty() || targetStr.isEmpty()) {
                toast("Enter a symbol and a target price");
                return;
            }
            double target;
            try {
                target = Double.parseDouble(targetStr);
            } catch (NumberFormatException e) {
                toast("Target price isn't a number");
                return;
            }
            String base = symbol.replaceAll("(USDT|BUSD|USDC|USD)$", "");
            String display = base.isEmpty() ? symbol : base;
            String direction = directionToggle.isChecked() ? AlertRule.ABOVE : AlertRule.BELOW;
            AlertStore.add(this, symbol, display, target, direction);
            symbolInput.setText("");
            targetInput.setText("");
            render();
            toast("Rule added");
        } catch (Throwable t) {
            Log.e(TAG, "ui: add failed", t);
        }
    }

    private void onCheckNow() {
        toast("Checking prices...");
        Thread t = new Thread(() -> AlertChecker.checkNow(getApplicationContext()), "CryptoAlerts-manual");
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
        row.setPadding(0, dp(8), 0, dp(8));

        TextView label = new TextView(this);
        label.setText(String.format(Locale.US, "%s %s $%s",
                r.displayName, r.isAbove() ? "↑ above" : "↓ below",
                AlertNotifier.trim(r.targetPrice)));
        label.setTextColor(Color.parseColor("#111111"));
        label.setTextSize(15);
        row.addView(label, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch enable = new Switch(this);
        enable.setChecked(r.enabled);
        enable.setOnCheckedChangeListener((b, checked) -> {
            AlertStore.setEnabled(AlertsActivity.this, r.id, checked);
        });
        row.addView(enable);

        Button del = new Button(this);
        del.setText("Delete");
        del.setOnClickListener(v -> {
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
