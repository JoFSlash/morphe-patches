package app.jofslash.patches.booking.history

import app.jofslash.patches.booking.shared.Constants.COMPATIBILITY_BOOKING
import app.jofslash.patches.booking.shared.callAfterIn
import app.jofslash.patches.booking.shared.priceRecordingPatch
import app.morphe.patcher.patch.bytecodePatch

private const val EXTENSION_CLASS = "Lapp/jofslash/extension/booking/DiscountCheck;"

// The property page's price ("Price for 2 nights, € 79 € 59") is drawn by this facet's Compose
// content, which calls PriceViewHPComposeView(modifier, props, onClick, onReward, onDetails,
// composer, changed).
private const val PRICE_FACET = "Lcom/booking/price/ui/marken/priceview/FacetHpPriceViewCompose"
private const val PRICE_VIEW = "Lcom/booking/price/ui/compose/PriceViewHPComposeKt;"

@Suppress("unused")
val checkCrossedOutPricesPatch = bytecodePatch(
    name = "Check crossed-out prices",
    description = "When a hotel's page shows a crossed-out \"original\" price, adds a line under " +
            "the price that compares it with the prices saved on your phone for the same dates " +
            "and guests: whether you ever saw that price, or only lower ones. Says so when there " +
            "aren't enough saved prices yet, and when the discount is your Genius or app-only price.",
    default = true
) {
    category("Prices")
    compatibleWith(COMPATIBILITY_BOOKING)

    // The check needs the saved prices, whether or not Price history is on.
    dependsOn(priceRecordingPatch)

    execute {
        callAfterIn(
            PRICE_FACET,
            PRICE_VIEW,
            "PriceViewHPComposeView",
            6,
            "$EXTENSION_CLASS->priceView(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;" +
                    "Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V"
        )
    }
}
