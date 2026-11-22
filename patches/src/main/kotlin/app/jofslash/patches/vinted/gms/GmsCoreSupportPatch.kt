package app.jofslash.patches.vinted.gms

import app.jofslash.patches.vinted.gms.GmsCoreLists.ACTIONS
import app.jofslash.patches.vinted.gms.GmsCoreLists.AUTHORITIES
import app.jofslash.patches.vinted.gms.GmsCoreLists.PERMISSIONS
import app.jofslash.patches.vinted.shared.APPLICATION
import app.jofslash.patches.vinted.shared.Constants.COMPATIBILITY_VINTED
import app.jofslash.patches.vinted.shared.Constants.PACKAGE_NAME
import app.jofslash.patches.vinted.shared.Constants.SIGNATURE
import app.jofslash.patches.vinted.shared.methodOf
import app.jofslash.patches.vinted.shared.replaceStrings
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import org.w3c.dom.Element
import java.util.logging.Logger

/** Package prefix of MicroG-RE, the GmsCore build Morphe and ReVanced use. */
private const val VENDOR = "app.revanced"
private const val GMS_CORE_PACKAGE = "$VENDOR.android.gms"

private const val EXTENSION_CLASS = "Lapp/jofslash/extension/vinted/GmsCoreCheck;"
private const val MAPS_EXTENSION_CLASS = "Lapp/jofslash/extension/vinted/MicroGMaps;"

/** The app's side of a Google map; play-services-maps implements it with a binder proxy. */
private const val MAP_DELEGATE = "Lcom/google/android/gms/maps/internal/IGoogleMapDelegate;"
private const val MAP_STYLE_OPTIONS = "Lcom/google/android/gms/maps/model/MapStyleOptions;"

// GooglePlayServicesUtilLight.isGooglePlayServicesAvailable(Context, int), obfuscated. It checks that
// Play services is installed and signed by Google; MicroG-RE has its own signature.
internal object GooglePlayServicesAvailableFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "I",
    parameters = listOf("Landroid/content/Context;", "I"),
    strings = listOf(
        "The Google Play services resources were not found. Check your project configuration to ensure that the resources are included.",
    ),
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
            // what Vinted's Google sign-in client is registered for.
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
    description = "Makes \"Continue with Google\", push notifications and the pick-up point maps work through " +
            "MicroG-RE instead of Google Play services. A patched app can't sign in with Google through Play " +
            "services, because it is no longer signed by Vinted. Needs MicroG-RE installed, with your Google " +
            "account added to it; without it, signing in with email still works, and the app says once that " +
            "MicroG-RE is missing. Google Pay doesn't work through MicroG-RE; pay by card instead.",
    default = true
) {
    category("App")
    compatibleWith(COMPATIBILITY_VINTED)

    extendWith("extensions/vinted.mpe")
    dependsOn(gmsCoreSupportResourcePatch)

    execute {
        val changed = replaceStrings(::gmsCoreString)
        Logger.getLogger("GmsCoreSupport").info("Moved $changed Play services strings to MicroG-RE")

        GooglePlayServicesAvailableFingerprint.method.returnEarly(0)

        // "Continue with Google" uses the GoogleSignIn client of play-services-auth (not Credential
        // Manager), which binds to MicroG-RE through the strings above.

        // Say once when MicroG-RE is missing. The range form reaches "this" whatever the number of
        // locals.
        methodOf(APPLICATION, "onCreate", emptyList()).addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->check(Landroid/content/Context;)V"
        )

        // MicroG-RE's maps crash the app when the map style is reloaded after the map has loaded
        // (see MicroGMaps). Every setMapType / setMapStyle call goes through the binder proxy of
        // IGoogleMapDelegate.
        val proxies = mutableListOf<String>()
        classDefForEach { classDef ->
            if (MAP_DELEGATE in classDef.interfaces) proxies += classDef.type
        }
        if (proxies.isEmpty()) throw PatchException("Couldn't find the IGoogleMapDelegate proxy")
        proxies.forEach { type ->
            val proxy = mutableClassDefBy(type)
            proxy.methods.single { it.name == "setMapType" && it.parameterTypes.map(Any::toString) == listOf("I") }
                .addInstructions(
                    0,
                    """
                        invoke-interface { p0 }, Landroid/os/IInterface;->asBinder()Landroid/os/IBinder;
                        move-result-object v0
                        invoke-static { v0, p1 }, $MAPS_EXTENSION_CLASS->skipMapType(Landroid/os/IBinder;I)Z
                        move-result v0
                        if-eqz v0, :keep
                        return-void
                        :keep
                        nop
                    """
                )
            proxy.methods.single { it.name == "setMapStyle" && it.parameterTypes.map(Any::toString) == listOf(MAP_STYLE_OPTIONS) }
                .addInstructions(
                    0,
                    """
                        invoke-interface { p0 }, Landroid/os/IInterface;->asBinder()Landroid/os/IBinder;
                        move-result-object v0
                        invoke-static { v0, p1 }, $MAPS_EXTENSION_CLASS->skipMapStyle(Landroid/os/IBinder;Ljava/lang/Object;)Z
                        move-result v0
                        if-eqz v0, :keep
                        const/4 v0, 0x1
                        return v0
                        :keep
                        nop
                    """
                )
        }
    }
}
