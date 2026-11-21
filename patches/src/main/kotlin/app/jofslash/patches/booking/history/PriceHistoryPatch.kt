package app.jofslash.patches.booking.history

import app.jofslash.patches.booking.shared.Constants.COMPATIBILITY_BOOKING
import app.jofslash.patches.booking.shared.callFromPropertyPageConstructor
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch

private const val EXTENSION_CLASS = "Lapp/jofslash/extension/booking/PriceHistory;"
private const val HOTEL = "Lcom/booking/common/data/Hotel;"

// Called when the property page has loaded the hotel again, for example for new dates.
internal object OnHotelUpdateFingerprint : Fingerprint(
    definingClass = "Lcom/booking/property/facet/PropertyPageViewModelImpl;",
    name = "onHotelUpdate",
    returnType = "V",
    parameters = listOf(HOTEL, "Lcom/booking/hotelinfo/data/propertyinfo/HotelPageTracking\$Reason;"),
)

// Turns a hotel into a property card on the search results map, once per card.
internal object PropertyCardToDataFingerprint : Fingerprint(
    definingClass = "Lcom/booking/propertycard/model/PropertyCardMappersKt;",
    name = "toData",
    parameters = listOf(HOTEL, "Lcom/booking/propertycard/model/AccommodationSearchConfig;"),
)

@Suppress("unused")
val priceHistoryPatch = bytecodePatch(
    name = "Price history",
    description = "Saves the hotel prices the app shows you on your phone and shows how the " +
            "price for your dates changed as a chart on the property page, with the lowest and " +
            "highest price and a check of crossed-out \"original\" prices.",
    default = true
) {
    category("Prices")
    compatibleWith(COMPATIBILITY_BOOKING)

    extendWith("extensions/extension.mpe")

    execute {
        callFromPropertyPageConstructor("$EXTENSION_CLASS->onPropertyPage(Landroid/app/Activity;)V")

        OnHotelUpdateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS->onHotelUpdate(Ljava/lang/Object;)V"
        )
        PropertyCardToDataFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p1 }, $EXTENSION_CLASS->onSearchCard(Ljava/lang/Object;Ljava/lang/Object;)V"
        )
    }
}
