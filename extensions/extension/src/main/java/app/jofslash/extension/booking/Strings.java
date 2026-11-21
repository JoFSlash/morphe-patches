package app.jofslash.extension.booking;

/**
 * Every text the patches show, in one place so translations can be added later.
 */
public final class Strings {
    private Strings() {
    }

    // Property page button
    public static final String BUTTON_COMPARE = "Compare prices";
    public static final String BUTTON_HISTORY = "Price history";
    public static final String BUTTON_CONTACT = "Contact";

    // Panel
    public static final String DIALOG_TITLE = "Compare prices";
    public static final String CLOSE = "Close";
    public static final String TAB_HISTORY = "Price history";
    public static final String TAB_OFFICIAL = "Official site";
    public static final String TAB_CONTACT = "Contact";
    public static final String LOADING = "The hotel is still loading. Try again in a moment.";
    public static final String NO_BROWSER = "No app can open the link.";

    public static final String ADULTS_ONE = "1 adult";
    public static final String ADULTS_MANY = "%d adults";
    public static final String ROOMS_ONE = "1 room";
    public static final String ROOMS_MANY = "%d rooms";
    /** Check-in, check-out, adults, rooms. */
    public static final String HISTORY_STAY = "%1$s – %2$s · %3$s · %4$s";

    // Price history tab
    public static final String HISTORY_HEADER = "Prices you've seen for these dates";
    public static final String HISTORY_EMPTY =
            "No saved prices for these dates yet. Booking.com keeps no price history and older "
                    + "prices can't be fetched, so the history starts when this patch was installed: "
                    + "from now on, every price the app shows you is saved on your phone.";
    public static final String HISTORY_NONE_IN_RANGE = "No prices for these dates in this period.";
    public static final String HISTORY_NONE = "The dates of this stay aren't known yet.";
    public static final String HISTORY_NOW = "Now";
    public static final String HISTORY_LOWEST = "Lowest";
    public static final String HISTORY_HIGHEST = "Highest";
    public static final String HISTORY_CHECKS_ONE = "1 price saved, on %1$s";
    public static final String HISTORY_CHECKS = "%1$d prices saved since %2$s";
    /** Label of the dashed line in the chart. */
    public static final String CHART_STRIKE = "Crossed out: %1$s";
    public static final String HISTORY_FIRST =
            "This is the first price saved for these dates. Open the hotel again later to see how "
                    + "it changes.";
    public static final String HISTORY_OTHER_DATES = "This hotel on other dates";

    // Range buttons
    public static final String RANGE_1M = "1M";
    public static final String RANGE_6M = "6M";
    public static final String RANGE_1Y = "1Y";
    public static final String RANGE_ALL = "All";

    // Hotel chart: every saved price of the hotel, any dates
    public static final String HOTEL_HEADER = "This hotel over the past year";
    public static final String HOTEL_EXPLAIN =
            "All stay dates together, not one stay: the lowest price per night you saw each day, "
                    + "for any dates and guests. Shows the hotel's general trend and seasons.";
    public static final String HOTEL_EMPTY =
            "No prices saved for this hotel in this period. Prices are saved from when this patch "
                    + "was installed; older ones can't be fetched.";
    public static final String HOTEL_LOWEST = "Lowest per night";
    public static final String HOTEL_HIGHEST = "Highest per night";
    /** Days with a price, different stays. */
    public static final String HOTEL_COUNT = "%1$d days with prices, %2$d different stays";
    /** Check-in, check-out, price, day it was seen. */
    public static final String HISTORY_OTHER_ROW = "%1$s – %2$s: %3$s (seen %4$s)";
    public static final String VERDICT_LOW = "The price is low compared with what you've seen";
    public static final String VERDICT_TYPICAL = "The price is typical for what you've seen";
    public static final String VERDICT_HIGH = "The price is high compared with what you've seen";
    public static final String VERDICT_SAME = "The price hasn't changed";
    public static final String VERDICT_FEW = "Too few prices yet to say whether this one is low or high";

    public static final String STRIKE_HIGHER =
            "Booking.com shows a crossed-out price of %1$s. The highest price saved for these dates "
                    + "is %2$s: you never saw the crossed-out price here.";
    public static final String STRIKE_SEEN =
            "Booking.com shows a crossed-out price of %1$s. You saw a price this high on %2$s.";
    public static final String STRIKE_FEW =
            "Booking.com shows a crossed-out price of %1$s. With more saved prices you'll see "
                    + "whether it was ever the real price.";

    // Official site tab
    public static final String OFFICIAL_LOADING = "Looking up the hotel on Google Hotels…";
    public static final String OFFICIAL_CONSENT =
            "Google asks about cookies first. Choose an option below to see the prices.";
    public static final String OFFICIAL_BLOCKED =
            "Google wants to check that you're not a robot. Solve it below to see the prices.";
    public static final String OFFICIAL_NOT_FOUND =
            "Couldn't find this hotel's prices automatically. Pick the hotel on the page below.";
    public static final String OFFICIAL_NO_OFFER =
            "Google doesn't list a price from the hotel's own website for these dates. Other "
                    + "sites' prices are on the page below.";
    public static final String OFFICIAL_SITE = "Official site";
    /** Google's own judgment of the price against the hotel's usual prices, as Google words it. */
    public static final String GOOGLE_SIGNAL = "Google: %1$s";
    public static final String GOOGLE_DEAL = "deal";
    /** The same judgment, read from the US version of the page on request. */
    public static final String GOOGLE_SIGNAL_US = "Google (US data): %1$s";
    public static final String US_RATING = "Check Google's deal rating (US data)";
    public static final String US_RATING_LOADING = "Checking Google's deal rating (US data)…";
    public static final String US_RATING_NONE = "no deal rating for this hotel";
    public static final String US_RATING_FAILED = "couldn't read it";
    public static final String GOOGLE_GREAT_DEAL = "great deal";
    /** The hotel's name on Google Hotels. */
    public static final String GOOGLE_LISTING = "On Google Hotels: %1$s";
    public static final String OFFICIAL_MEMBER = "Member rate: %1$s";
    public static final String BOOKING_HERE = "Booking.com (this app)";
    public static final String BOOKING_ON_GOOGLE = "Booking.com on Google";
    public static final String CHEAPEST_ON_GOOGLE = "Cheapest on Google: %1$s (%2$s)";
    /** Total for the stay, price per night. */
    public static final String TOTAL_PER_NIGHT = "%1$s total · %2$s per night";
    public static final String OFFICIAL_CHEAPER = "The official site is %1$s cheaper (%2$d%%)";
    public static final String BOOKING_CHEAPER = "Booking.com is %1$s cheaper (%2$d%%)";
    public static final String SAME_PRICE = "Same price on both";
    public static final String ONE_ROOM_NOTE = "Google shows prices for one room.";
    public static final String BOOK_OFFICIAL = "Book on the official site";
    public static final String FIND_WEBSITE = "Find the hotel's website";
    public static final String RELOAD = "Read the prices again";
    /** Appended to the hotel name and city when searching for the hotel's website. */
    public static final String WEBSITE_QUERY_SUFFIX = " official website";

    // Contact tab
    public static final String CONTACT_ADDRESS = "Address";
    public static final String CONTACT_NO_ADDRESS = "The address hasn't loaded yet.";
    public static final String CONTACT_OPEN_MAPS = "Open in Maps";
    public static final String CONTACT_COPY_ADDRESS = "Copy address";
    public static final String CONTACT_COPIED = "Copied";
    public static final String CONTACT_HEADER = "Contact the property";
    public static final String CONTACT_PHONE = "Phone";
    public static final String CONTACT_EMAIL = "Email";
    public static final String CONTACT_BUSINESS = "Business";
    public static final String CONTACT_BUSINESS_ADDRESS = "Business address";
    public static final String CONTACT_REGISTRATION = "Registration";
    public static final String CONTACT_NOT_LOADED =
            "Booking.com sent no contact details for this property. They're only sent for properties "
                    + "in the EU and a few other countries.";
    public static final String CONTACT_PRIVATE_HOST =
            "The host is a private person, not a business, so Booking.com doesn't share their contact "
                    + "details. You can message them through Booking.com after you book.";
    public static final String CONTACT_NONE = "Booking.com sent no phone number or email for this business.";
    public static final String CONTACT_SOURCE =
            "From the business details Booking.com has to show under EU consumer law (in the app under "
                    + "Legal information). They can belong to the company that runs the property rather "
                    + "than its front desk. Long-press a detail to copy it.";

    // Preferred label
    /**
     * Next to the Preferred and Preferred Plus badges. Booking.com: Preferred Partners pay a higher
     * commission. Short, so it fits next to the badge on a search result card.
     */
    public static final String PREFERRED_LABEL = "Pays Booking higher commission";
    /** Added to the screen reader text after "This is a Preferred property." */
    public static final String PREFERRED_A11Y = "Preferred properties pay Booking.com a higher commission.";

    // -------------------------------------------------------------------------------------------
    // Review check
    //
    // Keyword matching is a heads-up, not a verdict, and not fake-review detection. Matching is a
    // case-insensitive substring test on each review's text, so the lists are easy to extend: add
    // a phrase to the right row. Note that a few entries (for example "walked", "credit card")
    // can also match innocent sentences; that's accepted for a heads-up banner. Keywords are
    // English, so reviews written in other languages are only caught through Booking's own
    // English translation when the app loaded one.

    // Two severity tiers. Tier 1 is the hard scam case (a guest who was outright ripped off or had
    // money taken); it leads the banner and sorts first in the dialog. Tier 2 is the milder
    // complaints. A review in tier 1 is counted once and shown with the tier-1 label first.

    /** Tier 1 (top severity): outright scam / money taken. */
    public static final String FLAG_TIER1_LABEL = "Scam / money taken";

    /**
     * Tier-1 keywords (lowercase). These are the phrases guests write when they were actually
     * scammed or lost money, kept specific so a top-severity flag is credible. A few entries from
     * the brief were left out or tightened because bare forms also match innocent sentences:
     * "doesn't exist"/"didn't exist" (e.g. "hot water doesn't exist"), bare "no room" ("no room in
     * the fridge"), bare "money back"/"stole"/"charged me for" ("charged me for breakfast"). The
     * tightened forms are used instead. Bare "cash only" stays in tier 2, since small legitimate
     * guesthouses are cash only; tier 1 keeps "demanded cash" / "forced to pay".
     */
    public static final String[] FLAG_TIER1_KEYWORDS = {
            "scam", "scammed", "fraud", "fraudulent", "credit card fraud",
            "rip off", "ripoff", "ripped off", "con artist",
            "robbed", "stole my", "stole our", "stolen", "took my money", "took our money",
            "kept my money", "kept our money",
            "never refunded", "not refunded", "no refund", "wouldn't refund", "would not refund",
            "refused to refund", "never got my money back", "didn't get my money back",
            "want my money back", "no money back",
            "charged twice", "double charged", "charged me twice", "charged without",
            "unauthorized charge", "unauthorised charge", "card was charged",
            "had to pay again", "paid twice", "made me pay again",
            "demanded cash", "forced to pay", "forced to pay cash",
            "room wasn't there", "reservation didn't exist", "booking didn't exist",
            "property doesn't exist", "hotel doesn't exist", "fake listing",
            "chargeback", "disputed with my bank", "reported to police",
    };

    /** Tier-2 labels, in the same order as {@link #FLAG_KEYWORDS}. */
    public static final String[] FLAG_LABELS = {
            "Extra charges / hidden fees",
            "Asked to pay outside Booking",
            "Not as described",
            "Booking not honoured",
    };

    /** Tier-2 keywords per category (lowercase), in the same order as {@link #FLAG_LABELS}. */
    public static final String[][] FLAG_KEYWORDS = {
            // Hidden / extra fees & overcharging
            {
                    "hidden fee", "hidden fees", "extra charge", "extra charges", "extra fee",
                    "charged extra", "charged me extra", "overcharged", "over charged",
                    "charged more than", "charged more", "surprise fee", "surprise charge",
                    "additional charge", "additional fee", "resort fee", "deposit not returned",
                    "did not return my deposit", "didn't return the deposit", "kept my deposit",
                    "city tax",
            },
            // Pay outside the platform
            {
                    "pay cash", "pay in cash", "cash only", "cash payment",
                    "asked to pay outside", "pay outside", "bank transfer", "wire transfer",
                    "pay in person only", "pay directly", "off booking", "western union",
            },
            // Not as described
            {
                    "not as described", "not as pictured", "not as advertised",
                    "nothing like the photos", "nothing like the pictures", "not like the pictures",
                    "misleading", "false advertising", "not what was advertised", "photos are fake",
            },
            // Booking not honoured
            {
                    "cancelled my booking", "canceled my booking", "cancelled my reservation",
                    "overbooked", "double booked", "no reservation", "no record of my booking",
                    "did not have my reservation", "turned away", "room not available",
                    "no room available", "gave away my room", "were walked", "got walked",
                    "walked us",
            },
    };

    // Banner
    public static final String REVIEW_CHECKING = "Checking recent reviews…";
    public static final String REVIEW_NONE = "No scam or money red flags in the %d recent reviews";
    public static final String REVIEW_NONE_FEW = "No scam or money red flags in the recent reviews";
    // Tier 1 (leads the banner when any hard scam case is found). Kept honest: "report", "possible".
    public static final String REVIEW_TIER1_ONE =
            "⚠ 1 review reports a possible scam or money taken — tap to read";
    public static final String REVIEW_TIER1_MANY =
            "⚠ %d reviews report a possible scam or money taken — tap to read";
    // Tier 2 only (the milder complaints).
    public static final String REVIEW_FLAGGED_ONE =
            "1 of %d recent reviews mentions extra charges or booking problems — tap to read";
    public static final String REVIEW_FLAGGED_MANY =
            "%1$d of %2$d recent reviews mention extra charges or booking problems — tap to read";

    // Flagged-reviews dialog
    public static final String REVIEW_DIALOG_TITLE = "Reviews worth reading";
    public static final String REVIEW_DIALOG_SUBTITLE =
            "Keyword matches in the recent reviews — a heads-up, not a verdict. These are things "
                    + "other guests mentioned; read them and decide for yourself.";
    public static final String REVIEW_DIALOG_NOTE =
            "Simple keyword matching, not AI and not fake-review detection. Keywords are English, "
                    + "so reviews in other languages may be missed.";
    public static final String REVIEW_CLOSE = "Close";
}
