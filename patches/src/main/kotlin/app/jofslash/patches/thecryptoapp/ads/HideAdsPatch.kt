package app.jofslash.patches.thecryptoapp.ads

import app.jofslash.patches.thecryptoapp.shared.Constants.COMPATIBILITY_THECRYPTOAPP
import app.jofslash.patches.thecryptoapp.shared.methodOf
import app.jofslash.patches.thecryptoapp.shared.returnBoolean
import app.morphe.patcher.patch.bytecodePatch

// The new-user "ad-free grace window" gate. zs6.a() returns true for a configurable number of
// hours after first install; the ad code gates banners and native news ads behind
// `gd7.c() || zs6.a()` (seen in vf.java), where gd7.c() is the paid Pro entitlement. Pinning
// zs6.a() to true forces the app's OWN ad-free path for everyone, WITHOUT touching Pro
// (cz8.j() / h02 / gd7.c()). The class lives in the DEX default package (jadx shows it under the
// synthetic "defpackage"), so the real descriptor is Lzs6; with no package.
private const val GRACE_GATE = "Lzs6;"

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Forces the app's own new-user ad-free grace window on permanently, so banners and native " +
            "news ads stay hidden. This reuses the app's existing ad-free code path and does not unlock the " +
            "paid Pro tier.",
    default = true
) {
    category("Ads")
    compatibleWith(COMPATIBILITY_THECRYPTOAPP)

    execute {
        // public static final boolean a() -> always true.
        methodOf(GRACE_GATE, "a", emptyList()).returnBoolean(true)
    }
}
