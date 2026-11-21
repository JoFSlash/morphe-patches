package app.jofslash.extension.booking;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    //
    // On a search result card, "We have 3 left at this price" is drawn as
    //   core.layout.Row { items: [ core.OnAppear { actions: [core.et.TrackCustomGoal],
    //       component: bui.Label { color: "destructive_foreground", text: "We have 3 left ..." } },
    //     core.layout.Spacer ] }
    // or, in the other arm of Booking's experiment, with the OnAppear holding a core.layout.Row of
    // a red bui.Icon (dot, in a core.Margin) and a bui.ComposedLabel whose text spans are red.
    // The card's accessibilityLabel repeats it as "\nWe have 3 left at this price.". No other
    // label on the page uses the destructive (red) foreground; free cancellation, breakfast and
    // "No prepayment needed" are constructive (green), the price is plain (a struck-out old price
    // uses destructive_background).

    private static final String LABEL = "bui.Label";
    private static final String COMPOSED_LABEL = "bui.ComposedLabel";
    private static final String ICON = "bui.Icon";
    private static final String MARGIN = "core.Margin";
    private static final String URGENCY_COLOR = "destructive_foreground";
    private static final String ON_APPEAR = "core.OnAppear";
    private static final String SPACER = "core.layout.Spacer";
    private static final String LAYOUT_PREFIX = "core.layout.";

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
            boolean urgency = hideUrgency() && json.contains(URGENCY_COLOR);
            if (!sponsored && !urgency) return json;

            String trimmed = json.trim();
            Object root;
            if (trimmed.startsWith("{")) root = new JSONObject(trimmed);
            else if (trimmed.startsWith("[")) root = new JSONArray(trimmed);
            else return json;

            int ads = sponsored ? removeAds(root) : 0;
            int urgencies = 0;
            if (urgency) {
                Set<String> texts = new LinkedHashSet<>();
                urgencies = removeUrgency(root, texts);
                if (!texts.isEmpty()) cleanAccessibilityLabels(root, texts);
            }
            if (ads + urgencies == 0) return json;
            Log.d(TAG, "Removed " + ads + " sponsored results and " + urgencies + " urgency messages");
            return root.toString();
        } catch (Throwable t) {
            Log.w(TAG, "Couldn't filter a server-driven page", t);
            return json;
        }
    }

    // Sponsored listings.

    private static int removeAds(Object node) {
        int removed = 0;
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            String type = object.optString("type");
            JSONObject props = object.optJSONObject("props");
            if (props != null && (VERTICAL_LIST.equals(type) || TEMPLATED_VERTICAL_LIST.equals(type))) {
                JSONArray items = props.optJSONArray("items");
                if (items != null) removed += removeAdItems(items);
            }
            for (String key : keysOf(object)) removed += removeAds(object.opt(key));
        } else if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.length(); i++) removed += removeAds(array.opt(i));
        }
        return removed;
    }

    /** Takes out the items that are ads. Keeps the list as it is when every item looks like one. */
    private static int removeAdItems(JSONArray items) {
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

    // Urgency messages.

    /**
     * A red label: the urgency line. Its text is added to [texts]. Booking tests two forms: a
     * bui.Label with color "destructive_foreground", and a bui.ComposedLabel whose text spans all
     * have that color (shown after a red dot).
     */
    private static boolean isUrgencyLabel(Object node, Set<String> texts) {
        if (!(node instanceof JSONObject)) return false;
        JSONObject object = (JSONObject) node;
        String type = object.optString("type");
        JSONObject props = object.optJSONObject("props");
        if (props == null) return false;
        if (LABEL.equals(type)) {
            if (!URGENCY_COLOR.equals(props.optString("color"))) return false;
            Object text = props.opt("text");
            if (text instanceof String && isSoldOut((String) text)) return false;
            if (text instanceof String) texts.add((String) text);
            return true;
        }
        if (COMPOSED_LABEL.equals(type)) {
            JSONArray spans = props.optJSONArray("text");
            if (spans == null || spans.length() == 0) return false;
            List<String> found = new ArrayList<>();
            for (int i = 0; i < spans.length(); i++) {
                JSONObject span = spans.optJSONObject(i);
                if (span == null || !URGENCY_COLOR.equals(span.optString("color"))) return false;
                String text = span.optString("text");
                if (isSoldOut(text)) return false;
                found.add(text);
            }
            texts.addAll(found);
            return true;
        }
        return false;
    }

    /** Sold out is useful to know, so a red line that says so stays. */
    private static boolean isSoldOut(String text) {
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("sold out") || lower.contains("no availability");
    }

    /** The red dot shown before the urgency line. */
    private static boolean isUrgencyDot(Object node) {
        if (!(node instanceof JSONObject)) return false;
        JSONObject object = (JSONObject) node;
        JSONObject props = object.optJSONObject("props");
        if (props == null) return false;
        String type = object.optString("type");
        if (ICON.equals(type)) return URGENCY_COLOR.equals(props.optString("color"));
        if (MARGIN.equals(type)) return isUrgencyDot(props.opt("component"));
        return false;
    }

    /**
     * A part that shows nothing but the urgency line: the red label, the OnAppear around it (which
     * only tracks that the line was seen), a margin around it, or a layout row of only those, red
     * dots and spacers.
     */
    private static boolean isOnlyUrgency(Object node, Set<String> texts) {
        if (isUrgencyLabel(node, texts)) return true;
        if (!(node instanceof JSONObject)) return false;
        JSONObject object = (JSONObject) node;
        String type = object.optString("type");
        JSONObject props = object.optJSONObject("props");
        if (props == null) return false;
        if (ON_APPEAR.equals(type) || MARGIN.equals(type)) return isOnlyUrgency(props.opt("component"), texts);
        if (type.startsWith(LAYOUT_PREFIX) && !SPACER.equals(type)) {
            JSONArray items = props.optJSONArray("items");
            if (items == null || items.length() == 0) return false;
            boolean found = false;
            for (int i = 0; i < items.length(); i++) {
                Object item = items.opt(i);
                if (item instanceof JSONObject && SPACER.equals(((JSONObject) item).optString("type"))) continue;
                if (isUrgencyDot(item)) continue;
                if (!isOnlyUrgency(item, texts)) return false;
                found = true;
            }
            return found;
        }
        return false;
    }

    /**
     * Takes urgency-only parts out of the lists they are in. Where one is not in a list (a single
     * component slot), it becomes an empty component, so the card keeps its layout.
     */
    private static int removeUrgency(Object node, Set<String> texts) throws Exception {
        int removed = 0;
        if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = array.length() - 1; i >= 0; i--) {
                Object child = array.opt(i);
                if (isOnlyUrgency(child, texts)) {
                    array.remove(i);
                    removed++;
                } else {
                    removed += removeUrgency(child, texts);
                }
            }
        } else if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            for (String key : keysOf(object)) {
                Object child = object.opt(key);
                if (isOnlyUrgency(child, texts)) {
                    object.put(key, new JSONObject().put("type", "core.Blank").put("props", new JSONObject()));
                    removed++;
                } else {
                    removed += removeUrgency(child, texts);
                }
            }
        }
        return removed;
    }

    /** Takes the removed lines out of the cards' screen reader text ("...\nWe have 3 left at this price."). */
    private static void cleanAccessibilityLabels(Object node, Set<String> texts) throws Exception {
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            for (String key : keysOf(object)) {
                Object child = object.opt(key);
                if ("accessibilityLabel".equals(key) && child instanceof String) {
                    String label = (String) child;
                    for (String text : texts) {
                        label = label.replace("\n" + text + ".", "").replace(text + ".", "").replace(text, "");
                    }
                    object.put(key, label);
                } else {
                    cleanAccessibilityLabels(child, texts);
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.length(); i++) cleanAccessibilityLabels(array.opt(i), texts);
        }
    }

    private static List<String> keysOf(JSONObject object) {
        List<String> keys = new ArrayList<>();
        Iterator<String> it = object.keys();
        while (it.hasNext()) keys.add(it.next());
        return keys;
    }
}
