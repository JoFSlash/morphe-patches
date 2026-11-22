package app.jofslash.patches.vinted.ads

import app.jofslash.patches.vinted.shared.Constants.APP
import app.jofslash.patches.vinted.shared.Constants.COMPATIBILITY_VINTED
import app.jofslash.patches.vinted.shared.editManifest
import app.jofslash.patches.vinted.shared.elements
import app.jofslash.patches.vinted.shared.methodOf
import app.jofslash.patches.vinted.shared.removeNamed
import app.jofslash.patches.vinted.shared.returnEarly
import app.jofslash.patches.vinted.shared.returnUnit
import app.jofslash.patches.vinted.shared.unitField
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch

private const val AD_MANAGER_INIT = "$APP/shared/ads/VintedAdManager\$init\$1;"
private const val ROKT_INITIALIZER = "$APP/shared/ads/rokt/RoktSdkInitializer;"

/** androidx.startup initializer of the Unity Ads SDK. */
private const val UNITY_ADS_INITIALIZER = "com.unity3d.services.core.configuration.AdsSdkInitializer"

private val hideAdsResourcePatch = resourcePatch {
    execute {
        editManifest { document ->
            // Content providers the ad SDKs start themselves with at app start, before the app
            // decides anything. Without them, the SDKs only start when the app asks, which it
            // no longer does.
            document.removeNamed(
                "provider",
                setOf(
                    "com.google.android.gms.ads.MobileAdsInitProvider",
                    "com.applovin.sdk.AppLovinInitProvider",
                    "com.vungle.ads.VungleProvider",
                    "com.facebook.ads.AudienceNetworkContentProvider",
                )
            )
            val unity = document.elements("meta-data").filter { it.getAttribute("android:name") == UNITY_ADS_INITIALIZER }
            if (unity.isEmpty()) throw PatchException("Couldn't find $UNITY_ADS_INITIALIZER in the manifest")
            unity.forEach { it.parentNode.removeChild(it) }
        }
    }
}

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Hides the ad banners in the feed, search, inbox and order screens and the partner offers " +
            "after a purchase, and never starts the ad networks (Google Ad Manager, AppLovin, Amazon, Rokt and " +
            "the networks they mediate). Promoted closets and bumped items, which sellers pay Vinted for, " +
            "stay as they are.",
    default = true
) {
    category("Ads")
    compatibleWith(COMPATIBILITY_VINTED)

    dependsOn(hideAdsResourcePatch)

    execute {
        val unit = unitField()

        // Each screen's VintedAdManager loads the ad configuration and starts the ad networks in
        // this coroutine (the banner ads through Google Ad Manager or AppLovin MAX, Vinted's own
        // ad network for native ads, and Rokt for offers). Finished at once, the manager has no
        // ad source: every banner, native ad and offer it is asked for is "none", and the
        // screens leave their ad slot out.
        methodOf(AD_MANAGER_INIT, "invokeSuspend", listOf("Ljava/lang/Object;")).returnUnit(unit)

        // Rokt (partner offers after checkout) is also started by the app itself at start.
        methodOf(ROKT_INITIALIZER, "onCreate", emptyList()).returnEarly()
    }
}
