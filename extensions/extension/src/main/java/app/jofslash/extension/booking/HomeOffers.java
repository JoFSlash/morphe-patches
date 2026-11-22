package app.jofslash.extension.booking;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Used by the Hide upsells patch, through SabaJsonFilter. The home screen is a server-driven
 * ("Saba") page, /mobile.saba?route_path=homescreen. Its blocks are one list: the app's own blocks
 * are abu.homescreen.LegacyHomeComponent items named by componentId ("recent_searches",
 * "wishlist", "upcoming_bookings", "ai_trip_planner", ...), the rest are drawn from the JSON
 * itself, such as campaign banners and offer carousels. Every block carries the ids Booking's
 * home screen tracking uses (component_id, region_id, unique_id). The blocks whose ids name an
 * offer, a deal, a campaign or an attraction are taken out of the list; the search box, recent
 * searches, Genius, saved properties and your trips stay. On any doubt a block stays.
 */
final class HomeOffers {
    private static final String TAG = "JoFSlash";

    /** Marks the home screen page. */
    static final String HOME_MARKER = "abu.homescreen.";
    private static final String LEGACY_COMPONENT = "abu.homescreen.LegacyHomeComponent";

    /** The keys that hold a block's tracking ids. */
    private static final String[] ID_KEYS = {
            "componentId", "component_id", "regionId", "region_id", "uniqueId", "unique_id",
    };

    /** The app's own blocks that are left out ("Plan your trip with AI"). */
    private static final String[] HIDDEN_NATIVE = {"ai_trip_planner"};

    /** Ids of blocks that always stay: your searches, saved properties, trips, Genius, weekend stays. */
    private static final String[] KEEP = {
            "search", "recent", "wishlist", "saved", "genius", "loyalty", "reservation",
            "trip", "sign_in", "signin", "review", "weekend", "loading",
    };

    /** Ids of offer blocks: campaign banners, "Offers" rows, attraction and other cross-sell carousels. */
    private static final String[] HIDE = {
            "offer", "deal", "campaign", "promo", "black_friday", "blackfriday", "seasonal", "marketing",
            "discount", "coupon", "voucher", "attraction", "cross_sell", "crosssell", "esim",
    };

    /** Links that lead to an offer page, for a block without ids. */
    private static final String[] HIDE_LINKS = {
            "/deals", "dealspage", "campaign", "black-friday", "blackfriday", "/attractions", "attractions/",
    };

    private HomeOffers() {
    }

    /** Takes the offer blocks out of the home screen page [root]. Returns how many were taken out. */
    static int remove(Object root) {
        JSONArray blocks = findBlockList(root);
        if (blocks == null) {
            Log.d(TAG, "Home screen: no block list found");
            return 0;
        }
        int removed = 0;
        for (int i = blocks.length() - 1; i >= 0; i--) {
            Object block = blocks.opt(i);
            List<String> ids = new ArrayList<>();
            collectIds(block, ids, 0);
            boolean hide = shouldHide(block, ids);
            Log.d(TAG, "Home screen block " + i + (hide ? " removed" : " kept") + ": ids=" + ids
                    + " text=" + firstText(block, 0));
            if (hide) {
                blocks.remove(i);
                removed++;
            }
        }
        return removed;
    }

    private static boolean shouldHide(Object block, List<String> ids) {
        String legacyId = legacyComponentId(block, 0);
        if (legacyId != null) {
            // The app's own blocks: only the ones named, so nothing native disappears by accident.
            for (String hidden : HIDDEN_NATIVE) {
                if (hidden.equals(legacyId)) return true;
            }
            return false;
        }
        String joined = String.join(" ", ids).toLowerCase(Locale.ROOT);
        if (containsAny(joined, KEEP)) return false;
        if (!joined.isEmpty()) return containsAny(joined, HIDE);
        // No tracking ids: go by where the block's links lead.
        List<String> links = new ArrayList<>();
        collectLinks(block, links, 0);
        String allLinks = String.join(" ", links).toLowerCase(Locale.ROOT);
        return !containsAny(allLinks, KEEP) && containsAny(allLinks, HIDE_LINKS);
    }

    /**
     * The list of home screen blocks: the array with the most items that are (or hold) one of the
     * app's own blocks. Null when no array holds two of them.
     */
    private static JSONArray findBlockList(Object root) {
        JSONArray[] best = {null};
        int[] bestCount = {1};
        findBlockList(root, best, bestCount, 0);
        return best[0];
    }

    private static void findBlockList(Object node, JSONArray[] best, int[] bestCount, int depth) {
        if (depth > 60) return;
        if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            int count = 0;
            for (int i = 0; i < array.length(); i++) {
                if (legacyComponentId(array.opt(i), 0) != null) count++;
            }
            if (count > bestCount[0]) {
                bestCount[0] = count;
                best[0] = array;
            }
            for (int i = 0; i < array.length(); i++) findBlockList(array.opt(i), best, bestCount, depth + 1);
        } else if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) findBlockList(object.opt(keys.next()), best, bestCount, depth + 1);
        }
    }

    /** The componentId of the app's own block in [node] (at any depth), or null. */
    private static String legacyComponentId(Object node, int depth) {
        if (depth > 40) return null;
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            if (LEGACY_COMPONENT.equals(object.optString("type"))) {
                JSONObject props = object.optJSONObject("props");
                String id = props != null ? props.optString("componentId", null) : null;
                if (id == null) id = object.optString("componentId", null);
                return id != null ? id : "";
            }
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String found = legacyComponentId(object.opt(keys.next()), depth + 1);
                if (found != null) return found;
            }
        } else if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.length(); i++) {
                String found = legacyComponentId(array.opt(i), depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void collectIds(Object node, List<String> ids, int depth) {
        if (depth > 40 || ids.size() > 20) return;
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = object.opt(key);
                if (value instanceof String && isIdKey(key) && !((String) value).isEmpty()) {
                    if (!ids.contains(value)) ids.add((String) value);
                } else {
                    collectIds(value, ids, depth + 1);
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.length(); i++) collectIds(array.opt(i), ids, depth + 1);
        }
    }

    private static boolean isIdKey(String key) {
        for (String idKey : ID_KEYS) {
            if (idKey.equals(key)) return true;
        }
        return false;
    }

    private static void collectLinks(Object node, List<String> links, int depth) {
        if (depth > 40 || links.size() > 20) return;
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) collectLinks(object.opt(keys.next()), links, depth + 1);
        } else if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.length(); i++) collectLinks(array.opt(i), links, depth + 1);
        } else if (node instanceof String) {
            String value = (String) node;
            if (value.startsWith("http") || value.startsWith("booking:") || value.startsWith("/")) links.add(value);
        }
    }

    /** The first text of a block, for the log. */
    private static String firstText(Object node, int depth) {
        if (depth > 40) return null;
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            Object text = object.opt("text");
            if (text instanceof String && !((String) text).isEmpty()) return (String) text;
            Object title = object.opt("title");
            if (title instanceof String && !((String) title).isEmpty()) return (String) title;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String found = firstText(object.opt(keys.next()), depth + 1);
                if (found != null) return found;
            }
        } else if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.length(); i++) {
                String found = firstText(array.opt(i), depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean containsAny(String text, String[] parts) {
        for (String part : parts) {
            if (text.contains(part)) return true;
        }
        return false;
    }
}
