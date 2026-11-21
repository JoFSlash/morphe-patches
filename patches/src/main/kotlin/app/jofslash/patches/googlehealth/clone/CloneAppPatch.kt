package app.jofslash.patches.googlehealth.clone

import app.jofslash.patches.googlehealth.shared.Constants.COMPATIBILITY_GOOGLE_HEALTH
import app.jofslash.patches.googlehealth.shared.Constants.PACKAGE_NAME
import app.jofslash.patches.googlehealth.shared.replaceStrings
import app.morphe.patcher.patch.Option
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import org.w3c.dom.Element
import java.util.logging.Logger

private const val DEFAULT_CLONE_PACKAGE = "com.fitbit.FitbitMobile.clone"
private const val DEFAULT_CLONE_NAME = "Health Clone"

/** The launcher label in the original manifest. */
private const val ORIGINAL_LABEL = "Health"

private lateinit var packageNameOption: Option<String>
private lateinit var appNameOption: Option<String>

private fun clonePackageName(): String = packageNameOption.value ?: DEFAULT_CLONE_PACKAGE

private fun renamed(value: String, newPackage: String) =
    if (value.startsWith("$PACKAGE_NAME.")) newPackage + value.removePrefix(PACKAGE_NAME) else value

/**
 * Renames the package, the app's own permissions and its content provider authorities,
 * so the patched app installs next to the official one.
 */
private val cloneAppResourcePatch = resourcePatch {
    execute {
        val newPackage = clonePackageName()

        document("AndroidManifest.xml").use { document ->
            document.documentElement.setAttribute("package", newPackage)

            // Name the launcher icon, so the two apps can be told apart.
            val appName = appNameOption.value?.takeIf { it.isNotBlank() } ?: DEFAULT_CLONE_NAME
            val all = document.getElementsByTagName("*")
            for (i in 0 until all.length) {
                val element = all.item(i) as Element
                if (element.getAttribute("android:label") == ORIGINAL_LABEL) {
                    element.setAttribute("android:label", appName)
                }
            }

            // The app's own permissions, such as DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION, which
            // AndroidX builds from getPackageName() at runtime.
            for (tag in listOf("permission", "uses-permission")) {
                val nodes = document.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    val element = nodes.item(i) as Element
                    element.setAttribute("android:name", renamed(element.getAttribute("android:name"), newPackage))
                }
            }

            // Two installs can't declare the same authority.
            val providers = document.getElementsByTagName("provider")
            for (i in 0 until providers.length) {
                val provider = providers.item(i) as Element
                val authorities = provider.getAttribute("android:authorities")
                if (authorities.isEmpty()) continue
                provider.setAttribute(
                    "android:authorities",
                    authorities.split(';').joinToString(";") {
                        if (it.startsWith("$PACKAGE_NAME.")) renamed(it, newPackage) else "${newPackage}_$it"
                    }
                )
            }
        }
    }
}

@Suppress("unused")
val cloneAppPatch = bytecodePatch(
    name = "Clone app",
    description = "Changes the package name, so the patched app installs next to the official Google Health " +
            "app instead of replacing it. Both apps sign in on their own. Push notifications don't work in " +
            "the clone, and a watch or tracker can only be paired with one of the two apps.",
    default = false
) {
    category("App")
    compatibleWith(COMPATIBILITY_GOOGLE_HEALTH)

    packageNameOption = stringOption(
        key = "packageName",
        default = DEFAULT_CLONE_PACKAGE,
        values = mapOf("Default" to DEFAULT_CLONE_PACKAGE),
        title = "Package name",
        description = "Package name of the cloned app. Each install needs its own package name.",
        required = true,
    ) {
        it != null && it != PACKAGE_NAME && it.matches(Regex("""^[a-zA-Z]\w*(\.[a-zA-Z]\w*)+$"""))
    }

    appNameOption = stringOption(
        key = "appName",
        default = DEFAULT_CLONE_NAME,
        values = mapOf("Default" to DEFAULT_CLONE_NAME, "Health" to "Health"),
        title = "App name",
        description = "Name shown under the clone's launcher icon.",
        required = true,
    ) { !it.isNullOrBlank() }

    dependsOn(cloneAppResourcePatch)

    execute {
        // The app hands out files (photos, exports) through its own file provider by authority.
        val newPackage = clonePackageName()
        val changed = replaceStrings { value ->
            if (value == "$PACKAGE_NAME.provider") renamed(value, newPackage) else null
        }
        Logger.getLogger("CloneApp").info("Renamed $changed file provider authorities")
    }
}
