package app.jofslash.extension.thecryptoapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Locale;

/** Creates the alert channel and posts per-rule local notifications. */
public final class AlertNotifier {

    static final String TAG = CustomAlertPoc.TAG;

    public static final String CHANNEL_ID = "custom_price_alerts";
    public static final String CHANNEL_NAME = "Price alerts";

    private AlertNotifier() {
    }

    public static void ensureChannel(Context ctx) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager nm = ctx.getSystemService(NotificationManager.class);
                if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                    channel.setDescription("Custom injected crypto price alerts");
                    nm.createNotificationChannel(channel);
                    Log.i(TAG, "notify: channel '" + CHANNEL_ID + "' created");
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "notify: could not create channel", t);
        }
    }

    /** Posts the alert for a rule. Notification id is derived from the rule id, so rules don't stack. */
    public static void notifyRule(Context ctx, AlertRule rule, double price) {
        try {
            ensureChannel(ctx);
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm == null) {
                Log.e(TAG, "notify: no NotificationManager");
                return;
            }

            String dir = rule.isAbove() ? "above" : "below";
            String title = rule.displayName + " alert";
            String text = String.format(Locale.US, "%s is %s (target %s %s)",
                    rule.displayName, PriceFormat.money(price), dir, PriceFormat.money(rule.targetPrice));

            // Tapping opens our alerts screen.
            Intent open = new Intent(ctx, AlertsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) piFlags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent contentIntent = PendingIntent.getActivity(
                    ctx, rule.id.hashCode(), open, piFlags);

            Notification notification;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notification = new Notification.Builder(ctx, CHANNEL_ID)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true)
                        .build();
            } else {
                notification = new Notification.Builder(ctx)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true)
                        .build();
            }

            nm.notify(rule.id.hashCode(), notification);
            Log.i(TAG, "notify: posted for rule " + rule.displayName + " -> " + text);
        } catch (Throwable t) {
            Log.e(TAG, "notify: could not post for rule " + (rule != null ? rule.displayName : "?"), t);
        }
    }
}
