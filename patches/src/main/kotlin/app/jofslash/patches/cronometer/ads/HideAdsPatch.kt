package app.jofslash.patches.cronometer.ads

import app.jofslash.patches.cronometer.shared.Constants.COMPATIBILITY_CRONOMETER
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

private const val ADS_PACKAGE = "Lio/flutter/plugins/googlemobileads"
private const val FLUTTER_AD = "$ADS_PACKAGE/FlutterAd;"
private const val LOAD_AD_ERROR = "$ADS_PACKAGE/FlutterAd\$FlutterLoadAdError;"
private const val RESPONSE_INFO = "$ADS_PACKAGE/FlutterAd\$FlutterResponseInfo;"
private const val AD_INSTANCE_MANAGER = "$ADS_PACKAGE/AdInstanceManager;"

// Every ad type the google_mobile_ads plugin can load. Cronometer's Dart code loads banners and
// interstitials; covering the whole family means another format turned on from the server still gets
// nothing. FluidAdManagerBannerAd inherits FlutterAdManagerBannerAd.load().
private val AD_CLASSES = listOf(
    "FlutterBannerAd",
    "FlutterAdManagerBannerAd",
    "FlutterInterstitialAd",
    "FlutterAdManagerInterstitialAd",
    "FlutterRewardedAd",
    "FlutterRewardedInterstitialAd",
    "FlutterAppOpenAd",
    "FlutterNativeAd",
)

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes the banner ads and the full-screen ads between screens. No ad is ever " +
            "requested: the app is told no ad was available, so it hides the ad space the same way it " +
            "does when Google has none to show.",
    default = true
) {
    category("Ads")
    compatibleWith(COMPATIBILITY_CRONOMETER)

    execute {
        // Whether to show an ad is decided in Dart (libapp.so), but every ad is loaded through the
        // google_mobile_ads plugin: Dart asks it to load one and the plugin reports back over the
        // channel. Each ad type's load() is where the Google SDK request starts. Replace it with an
        // immediate "no fill" report (code 3, ERROR_CODE_NO_FILL), so Dart runs its normal
        // failed-to-load path and drops the ad. onAdFailedToLoad posts to the main looper, so the
        // report reaches Dart after the load call has returned, in the same order as a real failure.
        AD_CLASSES.forEach { simpleName ->
            val type = "$ADS_PACKAGE/$simpleName;"
            val adClass = mutableClassDefByOrNull(type)
                ?: throw PatchException("$type not found. The ads plugin changed.")
            val load = adClass.methods.singleOrNull {
                it.name == "load" && it.returnType == "V" && it.parameterTypes.isEmpty()
            } ?: throw PatchException("$simpleName.load() not found.")
            val manager = adClass.fields.singleOrNull { it.type == AD_INSTANCE_MANAGER }
                ?: throw PatchException("$simpleName has no single AdInstanceManager field.")

            // The code below uses v0-v3, so load() needs at least 4 locals (p0 sits above them).
            val registers = load.implementation?.registerCount
                ?: throw PatchException("$simpleName.load() has no code.")
            if (registers - 1 < 4) throw PatchException("$simpleName.load() has too few registers.")

            // FlutterLoadAdError(code, domain, message, responseInfo): one string fills domain and
            // message, and responseInfo is null, which the plugin's codec allows.
            load.addInstructions(
                0,
                """
                    new-instance v0, $LOAD_AD_ERROR
                    const/4 v1, 0x3
                    const-string v2, "com.google.android.gms.ads"
                    const/4 v3, 0x0
                    invoke-direct {v0, v1, v2, v2, v3}, $LOAD_AD_ERROR-><init>(ILjava/lang/String;Ljava/lang/String;$RESPONSE_INFO)V
                    iget v1, p0, $FLUTTER_AD->adId:I
                    iget-object v2, p0, $type->${manager.name}:$AD_INSTANCE_MANAGER
                    invoke-virtual {v2, v1, v0}, $AD_INSTANCE_MANAGER->onAdFailedToLoad(I$LOAD_AD_ERROR)V
                    return-void
                """
            )
        }
    }
}
