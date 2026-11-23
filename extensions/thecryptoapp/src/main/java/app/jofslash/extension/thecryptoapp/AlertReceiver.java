package app.jofslash.extension.thecryptoapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receives the alarm check ticks (explicit {@link AlertScheduler#ACTION_CHECK}) and
 * BOOT_COMPLETED. Network runs on a background thread via goAsync() so the main thread is never
 * blocked. Never crashes the host.
 */
public final class AlertReceiver extends BroadcastReceiver {

    static final String TAG = CustomAlertPoc.TAG;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            final Context app = context.getApplicationContext();
            String action = intent != null ? intent.getAction() : null;
            Log.i(TAG, "receiver: onReceive action=" + action);

            if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                AlertScheduler.schedule(app);
                return;
            }

            // Alarm tick: run the check off the main thread, then re-arm the next one (the exact
            // self-rescheduling chain; harmless in the inexact-repeating fallback mode).
            final PendingResult pending = goAsync();
            Thread t = new Thread(() -> {
                try {
                    AlertChecker.checkNow(app);
                } finally {
                    try {
                        AlertScheduler.scheduleNext(app);
                    } catch (Throwable ignored) {
                    }
                    try {
                        pending.finish();
                    } catch (Throwable ignored) {
                    }
                }
            }, "CryptoAlerts-check");
            t.setDaemon(true);
            t.start();
        } catch (Throwable t) {
            Log.e(TAG, "receiver: unexpected failure", t);
        }
    }
}
