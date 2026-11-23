package app.jofslash.patches.imdb.ads

import app.jofslash.patches.imdb.shared.Constants.COMPATIBILITY_IMDB
import app.jofslash.patches.imdb.shared.methodOf
import app.jofslash.patches.imdb.shared.returnEarly
import app.morphe.patcher.patch.bytecodePatch

// The single ad-init entry. AmazonAdInitter.init() (called from
// ConsumerSpecificApplicationInitializer.doNowOnThisThread) starts Amazon Publisher Services
// (AdRegistration.getInstance) and, through it, Google Mobile Ads (MobileAds.initialize).
private const val AMAZON_AD_INITTER = "Lcom/imdb/advertising/AmazonAdInitter;"

// The coordinator the init entry drives. Its boolean field thirdPartyAdsEligible gates the whole
// render path; updateThirdPartyAdsEligibility(Boolean) is the only writer, fed by the server's
// GraphQL ThirdPartyAdsEligibilityQuery.
private const val APS_COORDINATOR =
    "Lcom/imdb/advertising/amazonpublisherservice/AmazonPublisherServiceCoordinator;"

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Never starts the ad networks (Amazon Publisher Services and Google Ad Manager) and keeps " +
            "every ad slot empty, so no banner or inline ad loads. IMDb is free with no paid ad-removal tier, " +
            "so nothing you pay for is unlocked.",
    default = true
) {
    category("Ads")
    compatibleWith(COMPATIBILITY_IMDB)

    execute {
        // init() runs initializeAmazonPublisherService(): AdRegistration.getInstance (APS) and the
        // coroutine that calls MobileAds.initialize (GMA). Returning at once starts neither SDK, so
        // nothing bids, loads or renders an ad.
        methodOf(AMAZON_AD_INITTER, "init").returnEarly()

        // Render safety-net. Ads are server-gated: updateThirdPartyAdsEligibility(Boolean) is where
        // the GraphQL answer turns the pipeline on. No-opping it pins thirdPartyAdsEligible at its
        // default false, so getGoogleAdFlow() takes the app's own "ads not active" branch and
        // returns null for every slot. MultiSourceAdRequest already skips null flows, so no ad view
        // is ever built - this reuses a code path the stock app runs whenever the server disables
        // ads, which is why it can't NPE on a null ad flow.
        methodOf(APS_COORDINATOR, "updateThirdPartyAdsEligibility").returnEarly()
    }
}
