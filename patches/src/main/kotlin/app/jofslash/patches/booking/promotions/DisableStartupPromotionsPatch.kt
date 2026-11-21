package app.jofslash.patches.booking.promotions

import app.jofslash.patches.booking.shared.Constants.COMPATIBILITY_BOOKING
import app.jofslash.patches.booking.shared.methodOf
import app.jofslash.patches.booking.shared.returnEarly
import app.morphe.patcher.patch.bytecodePatch

/** Collects the sheets the home screen shows when the app starts (the "launch sheet"). */
private const val LAUNCH_SHEET_REACTOR =
    "Lcom/booking/appindexcomponents/productpatterns/homelaunchsheet/HomeLaunchSheetReactor;"
private const val LAUNCH_SHEET_UTILS = "Lcom/booking/appindex/presentation/utils/HomeLaunchSheetUtils;"

@Suppress("unused")
val disableStartupPromotionsPatch = bytecodePatch(
    name = "Disable start-up promotions",
    description = "Stops the sheets that pop up on the home screen when the app starts, such as campaign " +
            "deals (\"Black Friday Deals are now live\"), Genius and sign-in offers, the co-branded credit " +
            "card and wallet offers. The same deals and sign in stay reachable from the app itself.",
    default = true
) {
    category("App")
    compatibleWith(COMPATIBILITY_BOOKING)

    execute {
        // Server-driven sheets, such as campaign deals: when a product tab (Stays, Car rentals,
        // Attractions, Taxis) is picked, the app asks Booking for a sheet to show. Don't ask.
        methodOf(LAUNCH_SHEET_REACTOR, "checkSabaLaunchSheet").returnEarly()

        // The app's own sheets (Genius sign in and onboarding, wallet, co-branded card, chat
        // assistant): this decides which one to show, and shows it. Show none.
        methodOf(LAUNCH_SHEET_UTILS, "decideShowingSheetType").returnEarly()
    }
}
