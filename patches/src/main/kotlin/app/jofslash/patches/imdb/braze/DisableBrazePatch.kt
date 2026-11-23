package app.jofslash.patches.imdb.braze

import app.jofslash.patches.imdb.shared.Constants.COMPATIBILITY_IMDB
import app.jofslash.patches.imdb.shared.methodOf
import app.jofslash.patches.imdb.shared.returnUnit
import app.morphe.patcher.patch.bytecodePatch

// Braze (Appboy) marketing/engagement. enableAndInitializeBraze(Continuation) is the suspend init:
// it enables the Braze SDK, opens a session, subscribes to push events and starts content cards.
private const val BRAZE_COORDINATOR = "Lcom/imdb/mobile/braze/BrazeCoordinator;"

@Suppress("unused")
val disableBrazePatch = bytecodePatch(
    name = "Disable Braze",
    description = "Stops the Braze (Appboy) marketing SDK from starting. This also turns off Braze push " +
            "notifications, content cards and in-app messages, so it is off by default; turn it on only if you " +
            "don't want Braze at all.",
    default = false
) {
    category("Privacy")
    compatibleWith(COMPATIBILITY_IMDB)

    execute {
        // The suspend init returns Unit at once, so the Braze SDK is never enabled or initialized.
        methodOf(BRAZE_COORDINATOR, "enableAndInitializeBraze").returnUnit()
    }
}
