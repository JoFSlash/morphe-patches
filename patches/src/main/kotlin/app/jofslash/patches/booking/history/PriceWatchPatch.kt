package app.jofslash.patches.booking.history

import app.jofslash.patches.booking.shared.Constants.COMPATIBILITY_BOOKING
import app.jofslash.patches.booking.shared.callFromPropertyPageConstructor
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private const val EXTENSION_PACKAGE = "app.jofslash.extension.booking"
private const val EXTENSION_CLASS = "Lapp/jofslash/extension/booking/PriceWatch;"

// The daily job and the adb-only debug trigger. The extension checks for the job service to know
// whether this patch is on.
private val priceWatchResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element

            document.createElement("service").apply {
                setAttribute("android:name", "$EXTENSION_PACKAGE.PriceWatchJob")
                setAttribute("android:permission", "android.permission.BIND_JOB_SERVICE")
                setAttribute("android:exported", "false")
                application.appendChild(this)
            }

            // Only senders holding android.permission.DUMP (adb's shell, the system) can reach it.
            document.createElement("receiver").apply {
                setAttribute("android:name", "$EXTENSION_PACKAGE.PriceWatchReceiver")
                setAttribute("android:permission", "android.permission.DUMP")
                setAttribute("android:exported", "true")
                appendChild(document.createElement("intent-filter").apply {
                    appendChild(document.createElement("action").apply {
                        setAttribute("android:name", "app.jofslash.booking.PRICE_WATCH_NOW")
                    })
                })
                application.appendChild(this)
            }
        }
    }
}

@Suppress("unused")
val priceWatchPatch = bytecodePatch(
    name = "Price watch",
    description = "Adds a \"Watch this price\" button to the Price history tab. Once a day, on Wi-Fi, " +
            "the app checks the price of each stay you watch (at most 20), saves it into the price " +
            "history and notifies you when it drops. Nothing is checked until you watch a stay.",
    default = true
) {
    category("Prices")
    compatibleWith(COMPATIBILITY_BOOKING)

    dependsOn(priceHistoryPatch, priceWatchResourcePatch)
    extendWith("extensions/extension.mpe")

    execute {
        // Keeps the daily job scheduled while stays are watched.
        callFromPropertyPageConstructor("$EXTENSION_CLASS->onPropertyPage(Landroid/app/Activity;)V")
    }
}
