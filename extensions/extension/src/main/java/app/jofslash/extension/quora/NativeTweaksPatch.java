package app.jofslash.extension.quora;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

@SuppressWarnings("unused")
public final class NativeTweaksPatch {

    private static final String TAG = "JoFSlashPatches";

    /**
     * Injection point: start of the handler Quora's pages call to ask for the notification permission.
     *
     * @return True to skip the system prompt. False when the permission is already granted,
     * so Quora's own handler reports it as granted.
     */
    public static boolean skipNotificationPrompt(Context context) {
        if (Build.VERSION.SDK_INT < 33 || context == null) return false;
        try {
            return context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to check notification permission", ex);
            return false;
        }
    }

    /**
     * The answer Quora's page gets when the prompt is skipped: the permission was not granted.
     */
    public static JSONObject notificationPromptResult() {
        JSONObject result = new JSONObject();
        try {
            result.put("success", false);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to build notification result", ex);
        }
        return result;
    }

    /**
     * Injection point: end of QTabBar.createTabList.
     */
    public static void removeSpacesTab(List<?> tabs) {
        if (tabs == null) return;
        try {
            Iterator<?> it = tabs.iterator();
            while (it.hasNext()) {
                Object tab = it.next();
                if (tab instanceof Enum && "SPACES".equals(((Enum<?>) tab).name())) it.remove();
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to remove Spaces tab", ex);
        }
    }
}
