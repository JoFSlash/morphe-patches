package app.jofslash.patches.booking.tracking

import app.jofslash.patches.booking.shared.Constants.COMPATIBILITY_BOOKING
import app.jofslash.patches.booking.shared.methodOf
import app.jofslash.patches.booking.shared.returnEarly
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

/** Permissions that only serve advertising: the advertising ID and Android's ad services. */
private val AD_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
)

private val blockTrackingResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val permissions = document.getElementsByTagName("uses-permission")
            for (i in permissions.length - 1 downTo 0) {
                val permission = permissions.item(i) as Element
                if (permission.getAttribute("android:name") in AD_PERMISSIONS) {
                    permission.parentNode.removeChild(permission)
                }
            }

            // Firebase Analytics (also behind Google Analytics and Tag Manager here): switched off
            // for good, so the app's own consent code can't turn it back on.
            val application = document.getElementsByTagName("application").item(0) as Element
            val metaData = application.getElementsByTagName("meta-data")
            fun setMetaData(name: String, value: String) {
                val existing = (0 until metaData.length).map { metaData.item(it) as Element }
                    .firstOrNull { it.getAttribute("android:name") == name }
                val element = existing ?: document.createElement("meta-data").also {
                    it.setAttribute("android:name", name)
                    application.appendChild(it)
                }
                element.setAttribute("android:value", value)
            }
            setMetaData("firebase_analytics_collection_deactivated", "true")
            setMetaData("firebase_analytics_collection_enabled", "false")
            setMetaData("google_analytics_adid_collection_enabled", "false")
        }
    }
}

@Suppress("unused")
val blockTrackingPatch = bytecodePatch(
    name = "Block tracking",
    description = "Stops analytics and marketing trackers: AppsFlyer, Firebase and Google Analytics, Embrace, " +
            "the ID5 advertising ID, and Booking's own C360 and \"squeaks\" usage reports. Also removes the " +
            "advertising ID permissions. Security, payment and sign-in checks are left alone.",
    default = true
) {
    category("Privacy")
    compatibleWith(COMPATIBILITY_BOOKING)

    dependsOn(blockTrackingResourcePatch)

    execute {
        // AppsFlyer (install attribution and marketing events) is only started here, once you
        // allow marketing cookies. Without it the app uses its built-in "no AppsFlyer" tracker,
        // the same as when you decline.
        methodOf("Lcom/booking/di/marketing/AppsFlyerProviderImpl;", "enableAppsFlyer").returnEarly()

        // Embrace (sessions, network calls, logs and traces sent to embrace.io). The app falls back
        // to its no-op tracker when creating the Embrace tracker fails, so make it fail.
        methodOf("Lcom/booking/observability/internal/embrace/EmbraceTracker;", "<init>").addInstructions(
            0,
            """
                new-instance v0, Ljava/lang/IllegalStateException;
                const-string v1, "Embrace is blocked"
                invoke-direct { v0, v1 }, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V
                throw v0
            """
        )

        // Booking's C360 customer tracking: both managers only queue events to send later.
        methodOf("Lcom/booking/c360tracking/C360TrackingManager;", "track").returnEarly()
        methodOf("Lcom/booking/c360tracking/C360TrackingPAcManager;", "track").returnEarly()

        // Squeaks, Booking's usage and error reports: every report is sent through this.
        methodOf("Lcom/booking/core/squeaks/Squeak\$Builder;", "send").returnEarly()

        // ID5 builds a shared advertising ID from your hashed email and advertising ID. Answer its
        // consent check with "not given", so it never asks for one.
        methodOf("Lcom/booking/nativeads/id5/data/eligibility/DefaultID5ConsentProvider;", "awaitCurrentState")
            .addInstructions(
                0,
                """
                    sget-object v0, Lcom/booking/nativeads/id5/domain/eligibility/ID5ConsentState;->NOT_GIVEN:Lcom/booking/nativeads/id5/domain/eligibility/ID5ConsentState;
                    return-object v0
                """
            )
    }
}
