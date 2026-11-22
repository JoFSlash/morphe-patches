package app.jofslash.patches.vinted.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    const val PACKAGE_NAME = "fr.vinted"

    /**
     * SHA-1 of the certificate Vinted signs the app with (from Google Play's base.apk). Its Google
     * sign-in client is registered for it, and MicroG-RE answers Google with it for the patched app.
     */
    const val SIGNATURE = "0c7f19fb846167c8fa03bfa9a584273491daf3e5"

    /** The app's own classes keep their names; only the libraries and some members are renamed. */
    const val APP = "Lcom/vinted"

    val COMPATIBILITY_VINTED = Compatibility(
        name = "Vinted",
        packageName = PACKAGE_NAME,
        // Google Play installs it as split APKs; APKPure has it as an XAPK.
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x007782,
        targets = listOf(
            AppTarget(version = "26.45.0"),
            AppTarget(version = null, isExperimental = true),
        )
    )
}
