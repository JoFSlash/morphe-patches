package app.jofslash.extension.booking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Small SQLite store for the prices the app showed. It keeps at most {@link #MAX_ROWS} rows and
 * skips a price that is the same as one saved for the same stay in the last {@link #DEDUPE_MS}.
 */
final class PriceDb extends SQLiteOpenHelper {
    private static final String NAME = "jofslash_price_history.db";
    private static final int VERSION = 1;
    static final int MAX_ROWS = 5000;
    static final long DEDUPE_MS = 6L * 60 * 60 * 1000;
    /** Stays that ended longer ago than this are dropped. */
    private static final long KEEP_AFTER_STAY_MS = 365L * 24 * 60 * 60 * 1000;

    static final String SOURCE_PROPERTY = "property";
    static final String SOURCE_CARD = "card";

    static final class Row {
        int hotelId;
        String name;
        String checkIn;
        String checkOut;
        int adults;
        int rooms;
        double price;
        double strike;
        String currency;
        long time;
        String source;
    }

    private static PriceDb instance;

    static synchronized PriceDb get(Context context) {
        if (instance == null) instance = new PriceDb(context.getApplicationContext());
        return instance;
    }

    private PriceDb(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE prices (_id INTEGER PRIMARY KEY AUTOINCREMENT, hotel_id INTEGER NOT NULL, "
                + "name TEXT, checkin TEXT NOT NULL, checkout TEXT NOT NULL, adults INTEGER NOT NULL, "
                + "rooms INTEGER NOT NULL, price REAL NOT NULL, strike REAL, currency TEXT NOT NULL, "
                + "time INTEGER NOT NULL, source TEXT)");
        db.execSQL("CREATE INDEX prices_stay ON prices (hotel_id, checkin, checkout, adults, rooms)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Only one version so far.
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // A newer build may have added columns this one doesn't use; keep the data.
    }

    /** Saves a price unless the same one was saved for this stay recently. Returns true when saved. */
    synchronized boolean add(HotelInfo hotel, Stay stay, String source, long now) {
        SQLiteDatabase db = getWritableDatabase();
        String[] key = keyArgs(hotel.id, stay);
        try (Cursor c = db.rawQuery("SELECT price, currency FROM prices WHERE hotel_id=? AND checkin=? "
                + "AND checkout=? AND adults=? AND rooms=? AND time>? ORDER BY time DESC LIMIT 1",
                new String[]{key[0], key[1], key[2], key[3], key[4], String.valueOf(now - DEDUPE_MS)})) {
            if (c.moveToFirst() && Math.abs(c.getDouble(0) - hotel.price) < 0.005
                    && hotel.currency.equals(c.getString(1))) {
                return false;
            }
        }
        ContentValues v = new ContentValues();
        v.put("hotel_id", hotel.id);
        v.put("name", hotel.name);
        v.put("checkin", stay.checkIn);
        v.put("checkout", stay.checkOut);
        v.put("adults", stay.adults);
        v.put("rooms", stay.rooms);
        v.put("price", hotel.price);
        if (!Double.isNaN(hotel.strike)) v.put("strike", hotel.strike);
        v.put("currency", hotel.currency);
        v.put("time", now);
        v.put("source", source);
        db.insert("prices", null, v);
        trim(db, now);
        return true;
    }

    private void trim(SQLiteDatabase db, long now) {
        String oldStay = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(new java.util.Date(now - KEEP_AFTER_STAY_MS));
        db.delete("prices", "checkout<?", new String[]{oldStay});
        db.execSQL("DELETE FROM prices WHERE _id NOT IN (SELECT _id FROM prices ORDER BY time DESC LIMIT "
                + MAX_ROWS + ")");
    }

    /** Prices for one stay, oldest first. */
    synchronized List<Row> forStay(int hotelId, Stay stay) {
        String[] key = keyArgs(hotelId, stay);
        return query("hotel_id=? AND checkin=? AND checkout=? AND adults=? AND rooms=?", key,
                "time ASC", null);
    }

    /** Every price saved for this hotel, for any stay, oldest first. */
    synchronized List<Row> forHotel(int hotelId) {
        return query("hotel_id=?", new String[]{String.valueOf(hotelId)}, "time ASC", null);
    }

    /** The latest price for each other stay at this hotel, newest first. */
    synchronized List<Row> otherStays(int hotelId, Stay stay, int limit) {
        List<Row> rows = query("hotel_id=? AND NOT (checkin=? AND checkout=? AND adults=? AND rooms=?) "
                        + "AND _id IN (SELECT MAX(_id) FROM prices WHERE hotel_id=? "
                        + "GROUP BY checkin, checkout, adults, rooms)",
                new String[]{String.valueOf(hotelId), stay.checkIn, stay.checkOut,
                        String.valueOf(stay.adults), String.valueOf(stay.rooms), String.valueOf(hotelId)},
                "time DESC", String.valueOf(limit));
        return rows;
    }

    private List<Row> query(String where, String[] args, String order, String limit) {
        List<Row> rows = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("prices", new String[]{"hotel_id", "name", "checkin",
                "checkout", "adults", "rooms", "price", "strike", "currency", "time", "source"},
                where, args, null, null, order, limit)) {
            while (c.moveToNext()) {
                Row r = new Row();
                r.hotelId = c.getInt(0);
                r.name = c.getString(1);
                r.checkIn = c.getString(2);
                r.checkOut = c.getString(3);
                r.adults = c.getInt(4);
                r.rooms = c.getInt(5);
                r.price = c.getDouble(6);
                r.strike = c.isNull(7) ? Double.NaN : c.getDouble(7);
                r.currency = c.getString(8);
                r.time = c.getLong(9);
                r.source = c.getString(10);
                rows.add(r);
            }
        }
        return rows;
    }

    private static String[] keyArgs(int hotelId, Stay stay) {
        return new String[]{String.valueOf(hotelId), stay.checkIn, stay.checkOut,
                String.valueOf(stay.adults), String.valueOf(stay.rooms)};
    }
}
