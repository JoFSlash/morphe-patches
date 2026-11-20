package app.jofslash.extension.quora;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reading history: the answers, posts and questions you opened, newest first.
 * Kept in the app's own storage, apart from the settings so backups don't include it.
 */
final class ReadingHistory {

    private static final String TAG = "JoFSlashPatches";
    private static final String PREFS = "jofslash_history";
    private static final String KEY = "items";
    private static final int MAX = 300;

    // Only Quora pages: quora.com and Spaces on their own subdomains.
    private static final Pattern QUORA_URL = Pattern.compile("^https://([a-z0-9-]+\\.)?quora\\.com/[^\\s]*$");

    static final class Item {
        final String url, title, author;
        final long time;

        Item(String url, String title, String author, long time) {
            this.url = url;
            this.title = title;
            this.author = author;
            this.time = time;
        }
    }

    private ReadingHistory() {
    }

    static boolean isQuoraUrl(String url) {
        return url != null && url.length() < 2000 && QUORA_URL.matcher(url).matches();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Adds an item, or moves it to the top when it is already there. */
    static synchronized void add(Context context, String url, String title, String author) {
        if (context == null || !isQuoraUrl(url) || title == null || title.trim().isEmpty()) return;
        try {
            List<Item> items = load(context);
            for (int i = items.size() - 1; i >= 0; i--) {
                if (items.get(i).url.equals(url)) items.remove(i);
            }
            items.add(0, new Item(url, clip(title.trim(), 300), clip(author == null ? "" : author.trim(), 100),
                    System.currentTimeMillis()));
            while (items.size() > MAX) items.remove(items.size() - 1);
            save(context, items);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to save the reading history", ex);
        }
    }

    static synchronized List<Item> load(Context context) {
        List<Item> items = new ArrayList<>();
        if (context == null) return items;
        try {
            JSONArray array = new JSONArray(prefs(context).getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o == null || !isQuoraUrl(o.optString("u"))) continue;
                items.add(new Item(o.optString("u"), o.optString("t"), o.optString("a"), o.optLong("d")));
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to read the reading history", ex);
        }
        return items;
    }

    static synchronized void clear(Context context) {
        if (context != null) prefs(context).edit().remove(KEY).apply();
    }

    private static void save(Context context, List<Item> items) throws Exception {
        JSONArray array = new JSONArray();
        for (Item item : items) {
            JSONObject o = new JSONObject();
            o.put("u", item.url);
            o.put("t", item.title);
            o.put("a", item.author);
            o.put("d", item.time);
            array.put(o);
        }
        prefs(context).edit().putString(KEY, array.toString()).apply();
    }

    private static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
