package app.jofslash.extension.booking;

/**
 * Every text the Compare prices and Price history patches show, in one place so translations can
 * be added later.
 */
public final class Strings {
    private Strings() {
    }

    // Property page button
    public static final String BUTTON_COMPARE = "Compare prices";
    public static final String BUTTON_HISTORY = "Price history";

    // Panel
    public static final String DIALOG_TITLE = "Compare prices";
    public static final String CLOSE = "Close";
    public static final String TAB_HISTORY = "Price history";
    public static final String TAB_OFFICIAL = "Official site";
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
}
