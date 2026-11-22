package app.jofslash.patches.vinted.tracking

import app.jofslash.patches.vinted.shared.Constants.APP
import app.jofslash.patches.vinted.shared.Constants.COMPATIBILITY_VINTED
import app.jofslash.patches.vinted.shared.clearBooleanParameter
import app.jofslash.patches.vinted.shared.editManifest
import app.jofslash.patches.vinted.shared.elements
import app.jofslash.patches.vinted.shared.methodOf
import app.jofslash.patches.vinted.shared.returnEarly
import app.jofslash.patches.vinted.shared.setApplicationMetaData
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch

private const val EXTERNAL_EVENTS = "$APP/shared/externalevents"
private const val EXTERNAL_EVENT_PUBLISHER = "$EXTERNAL_EVENTS/ExternalEventPublisher;"
private const val EXTERNAL_EVENT = "$EXTERNAL_EVENTS/ExternalEvent;"
private const val ADJUST_INITIALIZER = "$EXTERNAL_EVENTS/adjust/AdjustSdkInitializer;"
private const val FACEBOOK_INITIALIZER = "$EXTERNAL_EVENTS/facebook/FacebookEventsSdkInitializer;"
private const val BRAZE_EVENT_TRACKER = "$APP/feature/crm/BrazeEventTracker;"

/** Google's data transport (Firelog) backend that uploads to firebaselogging-pa.googleapis.com. */
private const val CCT_BACKEND = "backend:com.google.android.datatransport.cct.CctBackendFactory"

private val blockTrackingResourcePatch = resourcePatch {
    execute {
        editManifest { document ->
            // Firebase Analytics, switched off for good, so the consent screen can't turn it on.
            document.setApplicationMetaData("firebase_analytics_collection_deactivated", "true")
            document.setApplicationMetaData("firebase_analytics_collection_enabled", "false")
            document.setApplicationMetaData("google_analytics_adid_collection_enabled", "false")
            // Crashlytics crash reports and Firebase Sessions, by their own switches.
            document.setApplicationMetaData("firebase_crashlytics_collection_enabled", "false")
            document.setApplicationMetaData("firebase_sessions_enabled", "false")
            // The Facebook SDK stays for signing in with Facebook, but logs no app events by itself
            // and doesn't read the advertising ID.
            document.setApplicationMetaData("com.facebook.sdk.AutoLogAppEventsEnabled", "false")
            document.setApplicationMetaData("com.facebook.sdk.AdvertiserIDCollectionEnabled", "false")

            // Google's data transport sends Crashlytics, Sessions and push delivery logs through
            // its "cct" backend. Without the backend, the transport drops whatever is queued for it.
            val backends = document.elements("meta-data").filter { it.getAttribute("android:name") == CCT_BACKEND }
            if (backends.isEmpty()) throw PatchException("Couldn't find $CCT_BACKEND in the manifest")
            backends.forEach { it.parentNode.removeChild(it) }
        }
    }
}

@Suppress("unused")
val blockTrackingPatch = bytecodePatch(
    name = "Block tracking",
    description = "Stops the marketing and analytics trackers: Adjust install and purchase tracking, Facebook " +
            "app events, Firebase and Google Analytics, Firebase Crashlytics and Sessions, and the events the " +
            "app sends to Braze. Vinted's own fraud protection (DataDome, Incognia, Play Integrity) isn't " +
            "touched.",
    default = true
) {
    category("Privacy")
    compatibleWith(COMPATIBILITY_VINTED)

    dependsOn(blockTrackingResourcePatch)

    execute {
        // Every marketing event (item viewed, favourite, add to cart, purchase, registration,
        // login, screen open...) goes through ExternalEventPublisher.track to Adjust, Facebook and
        // Firebase. It now does nothing, and the listeners that forward the queued events to
        // each SDK are never set up.
        methodOf(EXTERNAL_EVENT_PUBLISHER, "track", listOf(EXTERNAL_EVENT)).returnEarly()
        listOf("listenToAdjustEvents", "listenToFacebookEvents", "listenToFirebaseEvents").forEach {
            methodOf(EXTERNAL_EVENT_PUBLISHER, it, emptyList()).returnEarly()
        }

        // Adjust (install attribution) is started by the app once the consent screen allows it.
        methodOf(ADJUST_INITIALIZER, "initializeSdk", emptyList()).returnEarly()

        // initializeSdk(true) turns Facebook's automatic app events and advertiser ID on, and
        // (false) off; always take the "off" branch.
        methodOf(FACEBOOK_INITIALIZER, "initializeSdk", listOf("Z")).clearBooleanParameter(0)

        // Custom events (what you look at and do) the app reports to Braze, its marketing CRM.
        methodOf(BRAZE_EVENT_TRACKER, "logCustomEvent").returnEarly()
    }
}
