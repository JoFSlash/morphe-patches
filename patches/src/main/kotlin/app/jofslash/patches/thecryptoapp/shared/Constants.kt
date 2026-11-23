package app.jofslash.patches.thecryptoapp.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    const val PACKAGE_NAME = "com.crypter.cryptocyrrency"

    /** The app's own Application subclass (com.pairip.application.Application extends it). */
    const val APP_CLASS = "Lcom/crypter/cryptocyrrency/app/App;"

    val COMPATIBILITY_THECRYPTOAPP = Compatibility(
        name = "The Crypto App",
        packageName = PACKAGE_NAME,
        // Ships as a single universal APK, not a bundle.
        apkFileType = ApkFileType.APK,
        appIconColor = 0x00C087,
        targets = listOf(
            AppTarget(version = "4.0.5"),
            AppTarget(version = null, isExperimental = true),
        )
    )
}
