package app.jofslash.patches.listonic.permissions

import app.jofslash.patches.listonic.shared.Constants.COMPATIBILITY_LISTONIC
import app.jofslash.patches.listonic.shared.editManifest
import app.jofslash.patches.listonic.shared.removeNamed
import app.morphe.patcher.patch.resourcePatch

@Suppress("unused")
val removeUnneededPermissionsPatch = resourcePatch(
    name = "Remove unneeded permissions",
    description = "Removes the permissions that only serve advertising and tracking: the advertising ID, " +
            "Android's ad services, the Google Play install referrer, phone state, the advertising IDs of " +
            "some phone makers and Amazon's ad attestation. With the ad and tracking patches on, nothing in " +
            "the app uses them.",
    default = true
) {
    category("Privacy")
    compatibleWith(COMPATIBILITY_LISTONIC)

    execute {
        editManifest {
            it.removeNamed(
                "uses-permission",
                setOf(
                    // The advertising ID and Android's ad-services APIs: ads and attribution only.
                    "com.google.android.gms.permission.AD_ID",
                    "android.permission.ACCESS_ADSERVICES_AD_ID",
                    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
                    "android.permission.ACCESS_ADSERVICES_TOPICS",
                    // Where the install came from: install-attribution trackers only.
                    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
                    // Phone state: read by the ad networks for device data; the lists don't need it.
                    "android.permission.READ_PHONE_STATE",
                    "android.permission.READ_BASIC_PHONE_STATE",
                    // Advertising IDs (MSA OAID) of Asus and Freeme phones.
                    "com.asus.msa.SupplementaryDID.ACCESS",
                    "freemme.permission.msa",
                    // Amazon ads' Private Access Token attestation.
                    "com.amazon.privacypass.ATTEST",
                )
            )
        }
    }
}
