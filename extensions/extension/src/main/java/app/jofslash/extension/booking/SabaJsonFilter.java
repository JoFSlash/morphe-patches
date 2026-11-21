package app.jofslash.extension.booking;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Used by the Hide sponsored listings and Hide urgency messages patches. Search results are a
 * server-driven ("Saba") page that the app gets as JSON. Before the app reads that JSON, the parts
 * the enabled patches hide are taken out. On any problem the JSON is left as it is.
 */
@SuppressWarnings("unused")
public final class SabaJsonFilter {
    private static final String TAG = "JoFSlash";

    // Sponsored listings.

    /** Native ad actions, such as "abu.nativead.NativeAdImpression" and "abu.nativead.SponsoredListingClick". */
    private static final String NATIVE_AD_PREFIX = "abu.nativead.";
    /** Sponsored brand carousel actions, such as "sponsoredlistings.SponsoredBrandsCarouselImpression". */
    private static final String SPONSORED_PREFIX = "sponsoredlistings.";
    /** Lists whose items are search results; their next pages are lists too. */
    private static final String VERTICAL_LIST = "core.VerticalList";
    private static final String TEMPLATED_VERTICAL_LIST = "core.TemplatedVerticalList";

    // Urgency messages.

    /**
     * Text fields that only carry urgency, such as "We have 1 left at this price" and "Last booked
     * 2 hours ago" ("abu.search.sr.PropertyCardPrice" has the first two; both are optional there).
     */
    private static final String[] URGENCY_KEYS = {
            "urgencyMessage", "lastReservationMessage",
            "urgency_message", "last_reservation_text", "only_x_left_message", "onlyXLeftMessage",
    };
    /** Test and component ids of urgency parts of a card, such as "urgency_message" or "rooms-left". */
    private static final Pattern URGENCY_ID = Pattern.compile(
            "urgency|scarcity|only[-_]?x[-_]?left|rooms?[-_]left|last[-_]?reservation|last[-_]?booked"
                    + "|high[-_]?demand|rare[-_]?find|booked[-_]?times|people[-_]?(looking|viewing)",
            Pattern.CASE_INSENSITIVE);
    /** Sold out messages are useful, even when their id says "urgency" ("clear urgency"). */
    private static final Pattern KEEP_ID = Pattern.compile("sold[-_]?out|soldout", Pattern.CASE_INSENSITIVE);

    private SabaJsonFilter() {
    }

    /** Changed by the Hide sponsored listings patch to return true. */
    private static boolean hideSponsored() {
        return false;
    }

    /** Changed by the Hide urgency messages patch to return true. */
    private static boolean hideUrgency() {
        return false;
    }

    /** Returns [json] without the hidden parts, or [json] itself when nothing is hidden or it can't be read. */
    public static String filter(String json) {
        try {
            if (json == null) return null;
            boolean sponsored = hideSponsored() && (json.contains(NATIVE_AD_PREFIX) || json.contains(SPONSORED_PREFIX));
            boolean urgency = hideUrgency() && mayHaveUrgency(json);
            if (!sponsored && !urgency) return json;

            String trimmed = json.trim();
            Object root;
            if (trimmed.startsWith("{")) root = new JSONObject(trimmed);
            else if (trimmed.startsWith("[")) root = new JSONArray(trimmed);
            else return json;

            int[] counts = new int[2];
            visit(root, sponsored, urgency, counts);
            if (counts[0] + counts[1] == 0) return json;
            Log.d(TAG, "Removed " + counts[0] + " sponsored results and " + counts[1] + " urgency messages");
            return root.toString();
        } catch (Throwable t) {
            Log.w(TAG, "Couldn't filter a server-driven page", t);
            return json;
        }
    }

    private static boolean mayHaveUrgency(String json) {
        for (String key : URGENCY_KEYS) {
            if (json.contains(key)) return true;
        }
        return URGENCY_ID.matcher(json).find();
    }

    private static void visit(Object node, boolean sponsored, boolean urgency, int[] counts) throws Exception {
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            String type = object.optString("type");
            JSONObject props = object.optJSONObject("props");
            if (sponsored && props != null && (VERTICAL_LIST.equals(type) || TEMPLATED_VERTICAL_LIST.equals(type))) {
                JSONArray items = props.optJSONArray("items");
                if (items != null) counts[0] += removeAds(items);
            }
            if (urgency) {
                for (String key : URGENCY_KEYS) {
                    if (object.opt(key) instanceof String) {
                        object.remove(key);
                        counts[1]++;
                    }
                }
            }
            List<String> keys = new ArrayList<>();
            Iterator<String> it = object.keys();
            while (it.hasNext()) keys.add(it.next());
            for (String key : keys) {
                Object child = object.opt(key);
                if (urgency && isUrgencyComponent(child)) {
                    object.put(key, blank());
                    counts[1]++;
                } else {
                    visit(child, sponsored, urgency, counts);
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.length(); i++) {
                Object child = array.opt(i);
                if (urgency && isUrgencyComponent(child)) {
                    array.put(i, blank());
                    counts[1]++;
                } else {
                    visit(child, sponsored, urgency, counts);
                }
            }
        }
    }

    /** A component ({"type": ..., "props": {...}}) whose test id or id names an urgency message. */
    private static boolean isUrgencyComponent(Object node) {
        if (!(node instanceof JSONObject)) return false;
        JSONObject object = (JSONObject) node;
        if (!isComponentType(object.optString("type"))) return false;
        JSONObject props = object.optJSONObject("props");
        if (props == null) return false;
        return isUrgencyId(props.opt("idForTests")) || isUrgencyId(props.opt("id"));
    }

    /**
     * Types that are surely components that draw something, so an empty component can stand in for
     * them. Actions and data types also have a "type", and putting a component there would break the page.
     */
    private static boolean isComponentType(String type) {
        if (type.equals("core.Accessibility") || type.equals("core.NativeComponent") || type.equals("core.Box")
                || type.equals("core.VisibilityContainer") || type.startsWith("core.layout.")) {
            return true;
        }
        return type.startsWith("bui.") && !type.startsWith("bui.types.") && !type.startsWith("bui.forms.")
                && !type.equals("bui.OpenSheet") && !type.equals("bui.DismissSheet") && !type.equals("bui.ShowToast")
                && !type.equals("bui.TextAction") && !type.equals("bui.AssetMetadata") && !type.equals("bui.Edges");
    }

    private static boolean isUrgencyId(Object id) {
        if (!(id instanceof String)) return false;
        String value = (String) id;
        return URGENCY_ID.matcher(value).find() && !KEEP_ID.matcher(value).find();
    }

    /** An empty component in place of a removed one, so the card keeps its layout. */
    private static JSONObject blank() throws Exception {
        return new JSONObject().put("type", "core.Blank").put("props", new JSONObject());
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
