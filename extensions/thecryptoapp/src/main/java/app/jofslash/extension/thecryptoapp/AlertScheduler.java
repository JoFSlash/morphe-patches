package app.jofslash.extension.thecryptoapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

/**
 * Schedules the periodic alert check with AlarmManager. Uses inexact repeating alarms (~15 min) so
 * it needs no exact-alarm permission and is battery friendly.
 */
public final class AlertScheduler {

    static final String TAG = CustomAlertPoc.TAG;

    public static final String ACTION_CHECK = "app.jofslash.thecryptoapp.CHECK_ALERTS";
    private static final int REQUEST_CODE = 0x4A4F41; // "JOA"

    private AlertScheduler() {
    }

    private static PendingIntent checkIntent(Context ctx) {
        Intent i = new Intent(ctx, AlertReceiver.class).setAction(ACTION_CHECK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx.getApplicationContext(), REQUEST_CODE, i, flags);
    }

    /** (Re)schedules the repeating check. Safe to call repeatedly. */
    public static void schedule(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getApplicationContext()
                    .getSystemService(Context.ALARM_SERVICE);
            if (am == null) {
                Log.e(TAG, "schedule: no AlarmManager");
                return;
            }
            long interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
            long first = SystemClock.elapsedRealtime() + 60_000L; // first check a minute after start
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, first, interval,
                    checkIntent(ctx));
            Log.i(TAG, "schedule: inexact repeating check every ~15 min scheduled");
        } catch (Throwable t) {
            Log.e(TAG, "schedule: failed", t);
        }
    }

    public static void cancel(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getApplicationContext()
                    .getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(checkIntent(ctx));
        } catch (Throwable t) {
            Log.e(TAG, "schedule: cancel failed", t);
        }
    }
}
