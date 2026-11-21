package app.jofslash.extension.booking;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Used by the Hide sponsored listings patch. Search results are a server-driven ("Saba") page that
 * the app gets as JSON. Before the app reads that JSON, the list items that are ads are taken out.
 *
 * An item is an ad when it carries one of Booking's native ad actions: the impression that tells
 * Booking an ad was seen, or the click that tells Booking an ad was opened. Only items of a list
 * ("core.VerticalList", which the search results and their next pages are) are taken out, so a
 * container that only holds an ad somewhere deep inside stays.
 */
@SuppressWarnings("unused")
public final class HideSponsoredListingsPatch {
    private static final String TAG = "JoFSlash";

    /** Native ad actions, such as "abu.nativead.NativeAdImpression" and "abu.nativead.SponsoredListingClick". */
    private static final String NATIVE_AD_PREFIX = "abu.nativead.";
    /** Sponsored brand carousel actions, such as "sponsoredlistings.SponsoredBrandsCarouselImpression". */
    private static final String SPONSORED_PREFIX = "sponsoredlistings.";
    private static final String VERTICAL_LIST = "core.VerticalList";

    private HideSponsoredListingsPatch() {
    }

    /** Returns [json] without sponsored list items, or [json] itself when there are none or it can't be read. */
    public static String filter(String json) {
        if (json == null || (!json.contains(NATIVE_AD_PREFIX) && !json.contains(SPONSORED_PREFIX))
                || !json.contains(VERTICAL_LIST)) {
            return json;
        }
        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("{")) {
                JSONObject root = new JSONObject(trimmed);
                int removed = visit(root);
                if (removed > 0) {
                    Log.d(TAG, "Removed " + removed + " sponsored search results");
                    return root.toString();
                }
            } else if (trimmed.startsWith("[")) {
                JSONArray root = new JSONArray(trimmed);
                int removed = visit(root);
                if (removed > 0) {
                    Log.d(TAG, "Removed " + removed + " sponsored search results");
                    return root.toString();
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Couldn't filter sponsored search results", t);
        }
        return json;
    }

    private static int visit(Object node) {
        int removed = 0;
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            if (VERTICAL_LIST.equals(object.optString("type"))) {
                JSONObject props = object.optJSONObject("props");
                JSONArray items = props != null ? props.optJSONArray("items") : null;
                if (items != null) removed += removeAds(items);
            }
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                removed += visit(object.opt(keys.next()));
            }
        } else if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.length(); i++) {
                removed += visit(array.opt(i));
            }
        }
        return removed;
    }

    /** Takes out the items that are ads. Keeps the list as it is when every item looks like one. */
    private static int removeAds(JSONArray items) {
        int ads = 0;
        boolean[] isAd = new boolean[items.length()];
        for (int i = 0; i < items.length(); i++) {
            isAd[i] = hasAdAction(items.opt(i));
            if (isAd[i]) ads++;
        }
        if (ads == 0 || ads == items.length()) return 0;
        for (int i = items.length() - 1; i >= 0; i--) {
            if (isAd[i]) items.remove(i);
        }
        return ads;
    }

    private static boolean hasAdAction(Object node) {
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            String type = object.optString("type");
            if (type.startsWith(NATIVE_AD_PREFIX) || type.startsWith(SPONSORED_PREFIX)) return true;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                if (hasAdAction(object.opt(keys.next()))) return true;
            }
        } else if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.length(); i++) {
                if (hasAdAction(array.opt(i))) return true;
            }
        }
        return false;
    }
}
