package app.jofslash.extension.booking;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans a list of the app's loaded review objects (com.booking.ugc.review.model.HotelReview) for
 * scam / money / booking-integrity keywords. Pure logic: it reads the review text by reflection
 * (so nothing breaks at build time) and returns a {@link Result}. It is a heads-up based on
 * keyword matching, not AI and not fake-review detection.
 */
final class ReviewScanner {
    private ReviewScanner() {
    }

    /** One flagged review: a short snippet and the categories it matched. */
    static final class Flag {
        final String snippet;
        final String labels;

        Flag(String snippet, String labels) {
            this.snippet = snippet;
            this.labels = labels;
        }
    }

    static final class Result {
        /** How many reviews were looked at (the loaded "recent reviews"). */
        final int scanned;
        final List<Flag> flags;

        Result(int scanned, List<Flag> flags) {
            this.scanned = scanned;
            this.flags = flags;
        }

        int flaggedCount() {
            return flags.size();
        }
    }

    static Result scan(List<?> reviews) {
        List<Flag> flags = new ArrayList<>();
        int scanned = 0;
        if (reviews != null) {
            for (Object review : reviews) {
                if (review == null) continue;
                scanned++;
                String pros = HotelInfo.str(HotelInfo.call(review, "getPositiveComment"));
                String cons = HotelInfo.str(HotelInfo.call(review, "getNegativeComment"));
                String title = HotelInfo.str(HotelInfo.call(review, "getTitle"));
                // Booking's own English translation, when the app loaded one.
                Object translation = HotelInfo.call(review, "getHotelReviewTranslation");
                String tPros = HotelInfo.str(HotelInfo.call(translation, "getPositiveComment"));
                String tCons = HotelInfo.str(HotelInfo.call(translation, "getNegativeComment"));
                String tTitle = HotelInfo.str(HotelInfo.call(translation, "getTitle"));

                String haystack = lower(join(pros, cons, title, tPros, tCons, tTitle));
                if (haystack.isEmpty()) continue;

                Set<String> matched = new LinkedHashSet<>();
                for (int i = 0; i < Strings.FLAG_KEYWORDS.length; i++) {
                    for (String keyword : Strings.FLAG_KEYWORDS[i]) {
                        if (haystack.contains(keyword)) {
                            matched.add(Strings.FLAG_LABELS[i]);
                            break;
                        }
                    }
                }
                if (!matched.isEmpty()) {
                    // Count the review once even if it hit several categories.
                    flags.add(new Flag(snippet(cons, pros, title, tCons, tPros, tTitle),
                            joinLabels(matched)));
                }
            }
        }
        return new Result(scanned, flags);
    }

    /** First non-empty text, collapsed to a single line and trimmed to a snippet. */
    private static String snippet(String... parts) {
        for (String part : parts) {
            if (part != null) {
                String clean = part.replaceAll("\\s+", " ").trim();
                if (!clean.isEmpty()) {
                    return clean.length() > 180 ? clean.substring(0, 177) + "…" : clean;
                }
            }
        }
        return "";
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                sb.append(part).append('\n');
            }
        }
        return sb.toString();
    }

    private static String joinLabels(Set<String> labels) {
        StringBuilder sb = new StringBuilder();
        for (String label : labels) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(label);
        }
        return sb.toString();
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(java.util.Locale.ROOT);
    }
}
