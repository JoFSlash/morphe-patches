package app.jofslash.patches.booking.sponsored

import app.jofslash.patches.booking.shared.Constants.COMPATIBILITY_BOOKING
import app.jofslash.patches.booking.shared.enableSabaJsonFilter
import app.jofslash.patches.booking.shared.fieldGettersOf
import app.jofslash.patches.booking.shared.returnEarly
import app.jofslash.patches.booking.shared.sabaJsonFilterPatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val HOTEL = "Lcom/booking/common/data/Hotel;"
private const val PROPERTY_MARKER = "Lcom/booking/maps/data/PropertyMarker;"

@Suppress("unused")
val hideSponsoredListingsPatch = bytecodePatch(
    name = "Hide sponsored listings",
    description = "Removes the properties marked \"Ad\" (sponsored listings) from search results. On the map, " +
            "sponsored properties are shown like any other property, without the ad megaphone.",
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
        // The search map reads the marker's field directly (the megaphone "ad" pin, its ad
        // tracking and its own sponsored layer), so the field is left empty when a marker is made:
        // the property shows as an ordinary price pin.
        var cleared = 0
        mutableClassDefBy(PROPERTY_MARKER).methods.filter { it.name == "<init>" }.forEach { constructor ->
            val instructions = constructor.implementation?.instructions?.toList() ?: return@forEach
            instructions.indices.reversed().forEach { index ->
                val instruction = instructions[index]
                if (instruction.opcode != Opcode.IPUT_OBJECT) return@forEach
                val field = (instruction as ReferenceInstruction).reference as FieldReference
                if (field.definingClass != PROPERTY_MARKER || field.name != "sponsoredListing") return@forEach
                constructor.addInstruction(index, "const/4 v${(instruction as Instruction22c).registerA}, 0x0")
                cleared++
            }
        }
        if (cleared == 0) throw PatchException("Couldn't find where a map marker's sponsored data is set")
    }
}
