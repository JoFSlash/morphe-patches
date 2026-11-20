package app.jofslash.patches.quora.gms

import app.jofslash.patches.quora.clone.QUORA_PACKAGE
import app.jofslash.patches.quora.gms.GmsCoreLists.ACTIONS
import app.jofslash.patches.quora.gms.GmsCoreLists.AUTHORITIES
import app.jofslash.patches.quora.gms.GmsCoreLists.PERMISSIONS
import app.jofslash.patches.quora.shared.Constants.COMPATIBILITY_QUORA
import app.jofslash.patches.quora.shared.replaceStrings
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.returnEarly
import org.w3c.dom.Element
import java.util.logging.Logger

/** Package prefix of MicroG-RE, the GmsCore build Morphe and ReVanced use. */
private const val VENDOR = "app.revanced"
private const val GMS_CORE_PACKAGE = "$VENDOR.android.gms"

/** SHA-1 of the certificate Quora signs its releases with. */
private const val QUORA_SIGNATURE = "62869ae6b37ac8407cc65331add254f12ad435e5"

private const val EXTENSION_CLASS = "Lapp/jofslash/extension/quora/GmsCoreCheck;"

// The client library checks that Play Services is installed and signed by Google.
// MicroG-RE has its own signature, so this always fails without the patch.
internal object GooglePlayServicesAvailableFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GooglePlayServicesUtilLight;",
    name = "isGooglePlayServicesAvailable",
    returnType = "I",
    parameters = listOf("Landroid/content/Context;", "I"),
)

// Firebase Installations sends the app's package name and certificate with Quora's API key.
// The key only accepts Quora's own, so a re-signed or renamed app gets 403 and no push token.
internal object FisOpenConnectionFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/installations/remote/FirebaseInstallationServiceClient;",
    filters = listOf(string("X-Android-Package")),
)

internal object FisCertificateFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/installations/remote/FirebaseInstallationServiceClient;",
    name = "getFingerprintHashForPackage",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
)

internal object ApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/quora/android/Quora;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf(),
)

/** Maps a Google Play Services string to its MicroG-RE counterpart, or null to keep it. */
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
            // Push messages come from MicroG-RE instead of Play Services.
            val all = document.getElementsByTagName("*")
            for (i in 0 until all.length) {
                val element = all.item(i) as Element
                val attributes = element.attributes
                for (j in 0 until attributes.length) {
                    val attribute = attributes.item(j)
                    val value = attribute.nodeValue
                    if (value.contains("com.google.android.c2dm")) {
                        attribute.nodeValue = value.replace("com.google.android.c2dm", "$VENDOR.android.c2dm")
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
            // what Quora's Google sign-in client is registered for.
            metaData("$GMS_CORE_PACKAGE.SPOOFED_PACKAGE_NAME", QUORA_PACKAGE)
            metaData("$GMS_CORE_PACKAGE.SPOOFED_PACKAGE_SIGNATURE", QUORA_SIGNATURE)
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
    description = "Makes Google sign-in and push notifications work through MicroG-RE instead of " +
            "Google Play services. A patched app can't sign in with Google through Play services, " +
            "because it is no longer signed by Quora. Needs MicroG-RE installed, with your Google " +
            "account added to it.",
    default = false
) {
    category("App")
    compatibleWith(COMPATIBILITY_QUORA)

    extendWith("extensions/extension.mpe")
    dependsOn(gmsCoreSupportResourcePatch)

    execute {
        val changed = replaceStrings(::gmsCoreString)
        Logger.getLogger("GmsCoreSupport").info("Moved $changed Play services strings to MicroG-RE")

        GooglePlayServicesAvailableFingerprint.method.returnEarly(0)

        // Push needs a Firebase installation, so report Quora's package and certificate.
        FisOpenConnectionFingerprint.let { match ->
            match.method.apply {
                val instructions = implementation!!.instructions.toList()
                val start = match.instructionMatches.first().index
                // The package name is read just before the header name is loaded.
                val callIndex = (start downTo 0).first { i ->
                    val insn = instructions[i]
                    insn.opcode == Opcode.INVOKE_VIRTUAL &&
                        ((insn as ReferenceInstruction).reference as MethodReference).name == "getPackageName"
                }
                val moveResult = instructions[callIndex + 1]
                if (moveResult.opcode != Opcode.MOVE_RESULT_OBJECT) throw PatchException("No package name result")
                val register = (moveResult as OneRegisterInstruction).registerA
                addInstruction(callIndex + 2, "const-string v$register, \"$QUORA_PACKAGE\"")
            }
        }
        FisCertificateFingerprint.method.returnEarly(QUORA_SIGNATURE.uppercase())

        // Tell the user when MicroG-RE is missing.
        ApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION_CLASS->check(Landroid/content/Context;)V"
        )
    }
}
