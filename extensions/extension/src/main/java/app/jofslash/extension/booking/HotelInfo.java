package app.jofslash.extension.booking;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * What the patches need from the app's Hotel (com.booking.common.data.Hotel) and search objects,
 * read by reflection so nothing breaks at build time when the app changes.
 */
public final class HotelInfo {
    static final String TAG = "JoFBookingPrices";

    public int id;
    public String name;
    public String city;
    public String address;
    /** Total price for the stay as the app shows it, or NaN. */
    public double price = Double.NaN;
    /** Crossed-out ("original") total price, or NaN. */
    public double strike = Double.NaN;
    public String currency;

    private HotelInfo() {
    }

    /** Reads a Hotel object. Returns null when it isn't one or has no id. */
    public static HotelInfo of(Object hotel) {
        if (hotel == null) return null;
        try {
            HotelInfo info = new HotelInfo();
            Object id = call(hotel, "getHotelId");
            if (!(id instanceof Integer) || (Integer) id <= 0) return null;
            info.id = (Integer) id;
            info.name = str(call(hotel, "getHotelName"));
            info.city = str(call(hotel, "getCity"));
            info.address = str(call(hotel, "getAddress"));
            Object breakdown = field(hotel, "compositePriceBreakdown");
            if (breakdown != null) {
                Object gross = call(breakdown, "getGrossAmount");
                if (gross == null) gross = field(breakdown, "grossAmount");
                double value = money(gross);
                if (value > 0) {
                    info.price = value;
                    info.currency = str(call(gross, "getCurrency"));
                }
                Object strike = call(breakdown, "getStrikethroughPrice");
                if (strike == null) strike = field(breakdown, "strikethroughPrice");
                double strikeValue = money(strike);
                if (strikeValue > 0 && info.currency != null
                        && info.currency.equals(str(call(strike, "getCurrency")))) {
                    info.strike = strikeValue;
                }
            }
            return info;
        } catch (Throwable t) {
            Log.w(TAG, "Couldn't read the hotel", t);
            return null;
        }
    }

    public boolean hasPrice() {
        return !Double.isNaN(price) && currency != null && !currency.isEmpty();
    }

    private static double money(Object money) {
        if (money == null) return Double.NaN;
        Object value = call(money, "getValue");
        return value instanceof Double ? (Double) value : Double.NaN;
    }

    static String str(Object o) {
        return o == null ? null : o.toString();
    }

    /** Calls a method without parameters, looking through the class and its superclasses. */
    static Object call(Object target, String name) {
        if (target == null) return null;
        for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // Try the superclass.
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    static Object field(Object target, String name) {
        if (target == null) return null;
        for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) {
                // Try the superclass.
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }
}
