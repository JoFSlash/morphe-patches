package app.jofslash.extension.booking;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Used by the Stays only patch. The home screen's row of product tabs (Stays, Flights, Car
 * rental, Taxi, Attractions, Flight + Hotel) is drawn from ProductsHeaderReactor.State, which
 * lists the products to show and the one picked. The patch keeps only Stays in that list; with one
 * product left the app draws no row, and the home screen shows the stays search. Flights, cars,
 * taxis and attractions still open from links and from your bookings, as their screens don't read
 * this list.
 */
@SuppressWarnings("unused")
public final class StaysOnly {
    private static final String TAG = "JoFSlash";
    /** ProductsHeaderReactor.Product.Accommodations, the Stays tab. */
    private static final String STAYS = "com.booking.appindex.productsheader.ProductsHeaderReactor$Product$Accommodations";

    private StaysOnly() {
    }

    /**
     * Called first in the State constructor with its list of products. Returns the list with
     * only Stays, or the list as it is when Stays isn't in it.
     */
    public static List<?> products(List<?> products) {
        try {
            if (products == null || products.size() <= 1) return products;
            List<Object> stays = new ArrayList<>();
            for (Object product : products) {
                if (isStays(product)) stays.add(product);
            }
            return stays.isEmpty() ? products : stays;
        } catch (Throwable t) {
            Log.w(TAG, "Couldn't hide the product tabs", t);
            return products;
        }
    }

    /**
     * Called first in the State constructor with the picked product and the list
     * {@link #products} returned. When the picked product isn't in it (the app remembers the last
     * tab you used), Stays is picked instead.
     */
    public static Object selected(List<?> products, Object selected) {
        try {
            if (products == null || selected == null || products.contains(selected)) return selected;
            for (Object product : products) {
                if (isStays(product)) return product;
            }
            return selected;
        } catch (Throwable t) {
            return selected;
        }
    }

    private static boolean isStays(Object product) {
        return product != null && STAYS.equals(product.getClass().getName());
    }
}
