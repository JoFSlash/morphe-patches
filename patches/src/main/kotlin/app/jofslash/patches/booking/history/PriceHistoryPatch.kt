package app.jofslash.patches.booking.history

import app.jofslash.patches.booking.shared.Constants.COMPATIBILITY_BOOKING
import app.jofslash.patches.booking.shared.PRICE_HISTORY
import app.jofslash.patches.booking.shared.callFromPropertyPageConstructor
import app.jofslash.patches.booking.shared.priceRecordingPatch
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val priceHistoryPatch = bytecodePatch(
    name = "Price history",
    description = "Saves the hotel prices the app shows you on your phone and shows how the " +
            "price for your dates changed as a chart on the property page, with the lowest and " +
            "highest price and a check of crossed-out \"original\" prices.",
    default = true
) {
    category("Prices")
    compatibleWith(COMPATIBILITY_BOOKING)

    dependsOn(priceRecordingPatch)

    execute {
        // The Price history button and tab; the prices are saved by priceRecordingPatch.
        callFromPropertyPageConstructor("$PRICE_HISTORY->onPropertyPage(Landroid/app/Activity;)V")
    }
}
