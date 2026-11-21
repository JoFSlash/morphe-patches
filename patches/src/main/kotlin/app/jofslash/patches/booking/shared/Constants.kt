package app.jofslash.patches.booking.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    const val PACKAGE_NAME = "com.booking"

    val COMPATIBILITY_BOOKING = Compatibility(
        name = "Booking.com",
        packageName = PACKAGE_NAME,
        // APKMirror ships Booking.com as a bundle (APKM).
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x003B95,
        targets = listOf(
            AppTarget(version = "71.1"),
            AppTarget(version = null, isExperimental = true),
        )
    )
}
