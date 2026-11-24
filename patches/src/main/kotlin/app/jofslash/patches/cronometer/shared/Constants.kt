package app.jofslash.patches.cronometer.shared

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.jofslash.patches.cronometer.shared.COMPATIBILITY_CRONOMETER

object Constants {
    const val PACKAGE_NAME = "com.cronometer.android.gold"

    /** SHA-1 of Cronometer's original signing certificate (O=cronometer.com, Canmore, Alberta). */
    const val SIGNATURE = "9cdff67578800a88e8fea84e66d7553f2ef7ff8a"

    /**
     * Cronometer is a Flutter app: its own logic is compiled into libapp.so. The patches work on the
     * Java side of the Flutter plugins and SDKs, whose classes keep their names.
     */
    val COMPATIBILITY_CRONOMETER = Compatibility(
        name = "Cronometer",
        packageName = PACKAGE_NAME,
        appIconColor = 0xF26B21,
        // Newer versions start through PairIP: 4.60.4 has com.pairip.application.Application with a
        // Play license check, which closes a re-signed app, and 4.57.4 already needed a PairIP bypass.
        // So no experimental "any version" target.
        targets = listOf(AppTarget(version = "4.57.2"))
    )
}
