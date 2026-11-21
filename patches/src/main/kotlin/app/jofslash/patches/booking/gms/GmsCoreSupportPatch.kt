package app.jofslash.patches.booking.gms

import app.jofslash.patches.booking.gms.GmsCoreLists.ACTIONS
import app.jofslash.patches.booking.gms.GmsCoreLists.AUTHORITIES
import app.jofslash.patches.booking.gms.GmsCoreLists.PERMISSIONS
import app.jofslash.patches.booking.shared.Constants.COMPATIBILITY_BOOKING
import app.jofslash.patches.booking.shared.Constants.PACKAGE_NAME
import app.jofslash.patches.booking.shared.Constants.SIGNATURE
import app.jofslash.patches.booking.shared.replaceStrings
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.returnEarly
import org.w3c.dom.Element
import java.util.logging.Logger

/** Package prefix of MicroG-RE, the GmsCore build Morphe and ReVanced use. */
private const val VENDOR = "app.revanced"
private const val GMS_CORE_PACKAGE = "$VENDOR.android.gms"

private const val EXTENSION_CLASS = "Lapp/jofslash/extension/booking/GmsCoreCheck;"

// GooglePlayServicesUtilLight.isGooglePlayServicesAvailable(Context, int) from
// play-services-basement. It checks that Play services is installed and signed by Google;
// MicroG-RE has its own signature, so this always fails in the re-signed app without the patch.
// The public GoogleApiAvailability(Light).isGooglePlayServicesAvailable(...) that Booking's
// sign-in calls delegates to this static method, so forcing it to SUCCESS covers both.
internal object GooglePlayServicesAvailableFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GooglePlayServicesUtilLight;",
    name = "isGooglePlayServicesAvailable",
    returnType = "I",
    parameters = listOf("Landroid/content/Context;", "I"),
)

internal object ApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/booking/BookingApplication;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf(),
)

/** Maps a Google Play services string to its MicroG-RE counterpart, or null to keep it. */
private fun gmsCoreString(value: String): String? {
    if (value == "com.google") return VENDOR
    if (value == "com.google.android.gms") return GMS_CORE_PACKAGE
    if (value in PERMISSIONS || value in ACTIONS || value in AUTHORITIES) {
        return value.replace("com.google", VENDOR)
    }
    if (value.startsWith("content://")) {
        AUTHORITIES.firstOrNull { value.startsWith("content://$it") }?.let {
            return value.replace("content://$it", "content://" + it.replace("com.google", VENDOR))
        }
    }
    return null
}

private val gmsCoreSupportResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            // Push messages and permissions come from MicroG-RE instead of Play services.
            val all = document.getElementsByTagName("*")
            for (i in 0 until all.length) {
                val element = all.item(i) as Element
                val attributes = element.attributes
                for (j in 0 until attributes.length) {
                    val attribute = attributes.item(j)
                    val value = attribute.nodeValue
                    when {
                        value.contains("com.google.android.c2dm") ->
                            attribute.nodeValue = value.replace("com.google.android.c2dm", "$VENDOR.android.c2dm")
                        value in PERMISSIONS ->
                            attribute.nodeValue = value.replace("com.google", VENDOR)
                    }
                }
            }

            val application = document.getElementsByTagName("application").item(0) as Element
            fun metaData(name: String, value: String) = application.appendChild(
                document.createElement("meta-data").apply {
                    setAttribute("android:name", name)
                    setAttribute("android:value", value)
                }
            )
            // MicroG-RE answers Google with the original package and certificate, which is
            // what Booking.com's Google sign-in client is registered for.
            metaData("$GMS_CORE_PACKAGE.SPOOFED_PACKAGE_NAME", PACKAGE_NAME)
            metaData("$GMS_CORE_PACKAGE.SPOOFED_PACKAGE_SIGNATURE", SIGNATURE)
            metaData("app.revanced.MICROG_PACKAGE_NAME", GMS_CORE_PACKAGE)

            // Android 11+ only lets the app see MicroG-RE when it is listed in <queries>.
            val manifest = document.documentElement
            val queries = document.getElementsByTagName("queries").item(0) as Element?
                ?: document.createElement("queries").also { manifest.insertBefore(it, application) }
            queries.appendChild(
                document.createElement("package").apply { setAttribute("android:name", GMS_CORE_PACKAGE) }
            )
        }
    }
}

@Suppress("unused")
val gmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "Makes \"Continue with Google\" work through MicroG-RE instead of Google Play " +
            "services. A patched app can't sign in with Google through Play services, because it " +
            "is no longer signed by Booking.com. Needs MicroG-RE installed, with your Google " +
            "account added to it; without it the app works as before, and says so once.",
    default = true
) {
    category("App")
    compatibleWith(COMPATIBILITY_BOOKING)

    extendWith("extensions/extension.mpe")
    dependsOn(gmsCoreSupportResourcePatch)

    execute {
        val changed = replaceStrings(::gmsCoreString)
        Logger.getLogger("GmsCoreSupport").info("Moved $changed Play services strings to MicroG-RE")

        GooglePlayServicesAvailableFingerprint.method.returnEarly(0)

        // Tell the user when MicroG-RE is missing. BookingApplication.onCreate has many locals,
        // so "this" (p0) sits above v15; the range form of invoke-static reaches any register.
        ApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->check(Landroid/content/Context;)V"
        )
    }
}
