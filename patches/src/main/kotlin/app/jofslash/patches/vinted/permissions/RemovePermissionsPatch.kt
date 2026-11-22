package app.jofslash.patches.vinted.permissions

import app.jofslash.patches.vinted.shared.Constants.COMPATIBILITY_VINTED
import app.jofslash.patches.vinted.shared.editManifest
import app.jofslash.patches.vinted.shared.removeNamed
import app.morphe.patcher.patch.resourcePatch

@Suppress("unused")
val removeUnneededPermissionsPatch = resourcePatch(
    name = "Remove unneeded permissions",
    description = "Removes the permissions that only serve advertising and install tracking: the advertising ID, " +
            "Android's ad services (ad ID, attribution, topics and custom audiences), the Google Play install " +
            "referrer, the Google Play license check and Amazon's ad attestation.",
    default = true
) {
    category("Privacy")
    compatibleWith(COMPATIBILITY_VINTED)

    execute {
        editManifest {
            it.removeNamed(
                "uses-permission",
                setOf(
                    // The ad networks, Firebase Analytics, Adjust and Braze only.
                    "com.google.android.gms.permission.AD_ID",
                    "android.permission.ACCESS_ADSERVICES_AD_ID",
                    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
                    "android.permission.ACCESS_ADSERVICES_TOPICS",
                    "android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE",
                    // Where the install came from: read by Adjust, Facebook and the ad SDKs.
                    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
                    // Google Play licensing, asked for by an ad SDK. A patched app isn't the Play
                    // Store's copy anyway.
                    "com.android.vending.CHECK_LICENSE",
                    // Amazon's ad SDK (privacy pass tokens for ad requests).
                    "com.amazon.privacypass.ATTEST",
                )
            )
        }
    }
}
