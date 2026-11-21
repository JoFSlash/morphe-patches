package app.jofslash.patches.booking.sponsored

import app.jofslash.patches.booking.shared.Constants.COMPATIBILITY_BOOKING
import app.jofslash.patches.booking.shared.enableSabaJsonFilter
import app.jofslash.patches.booking.shared.fieldGettersOf
import app.jofslash.patches.booking.shared.returnEarly
import app.jofslash.patches.booking.shared.sabaJsonFilterPatch
import app.morphe.patcher.patch.bytecodePatch

private const val HOTEL = "Lcom/booking/common/data/Hotel;"
private const val PROPERTY_MARKER = "Lcom/booking/maps/data/PropertyMarker;"

@Suppress("unused")
val hideSponsoredListingsPatch = bytecodePatch(
    name = "Hide sponsored listings",
    description = "Removes the properties marked \"Ad\" (sponsored listings) from search results. On the map, " +
            "sponsored properties are shown like any other property.",
    default = true
) {
    category("Search")
    compatibleWith(COMPATIBILITY_BOOKING)

    dependsOn(sabaJsonFilterPatch)

    execute {
        // Search results are a server-driven page: the list items that carry Booking's native ad
        // actions (ad impression and ad click) are taken out of its JSON.
        enableSabaJsonFilter("hideSponsored")

        // Native property cards and map markers: no sponsored data, so no "Ad" badge, no sponsored
        // marker style and no ad impression or click tracking.
        fieldGettersOf(HOTEL, "sponsoredListing").forEach { it.returnEarly() }
        fieldGettersOf(PROPERTY_MARKER, "sponsoredListing").forEach { it.returnEarly() }
    }
}
