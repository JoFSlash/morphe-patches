package app.jofslash.patches.booking.sponsored

import app.jofslash.patches.booking.shared.Constants.COMPATIBILITY_BOOKING
import app.jofslash.patches.booking.shared.fieldGettersOf
import app.jofslash.patches.booking.shared.returnEarly
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private const val EXTENSION_CLASS = "Lapp/jofslash/extension/booking/HideSponsoredListingsPatch;"

/** Turns the JSON of a server-driven page into the app's components. */
private const val SABA_PARSER = "Lcom/booking/saba/SabaParser;"

private const val HOTEL = "Lcom/booking/common/data/Hotel;"
private const val PROPERTY_MARKER = "Lcom/booking/maps/data/PropertyMarker;"

private val INVOKES = setOf(
    Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_STATIC, Opcode.INVOKE_DIRECT, Opcode.INVOKE_INTERFACE,
)

@Suppress("unused")
val hideSponsoredListingsPatch = bytecodePatch(
    name = "Hide sponsored listings",
    description = "Removes the properties marked \"Ad\" (sponsored listings) from search results. On the map, " +
            "sponsored properties are shown like any other property.",
    default = true
) {
    category("Search")
    compatibleWith(COMPATIBILITY_BOOKING)

    extendWith("extensions/extension.mpe")

    execute {
        // Search results are a server-driven page. Every page's JSON goes through SabaParser, which
        // reads it with kotlinx Json.decodeFromString. The JSON is filtered just before that call.
        var hooked = 0
        mutableClassDefBy(SABA_PARSER).methods.forEach { method ->
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            // From the end, so the indexes of the calls not yet changed stay right.
            instructions.indices.reversed().forEach { index ->
                val insn = instructions[index]
                if (insn.opcode !in INVOKES) return@forEach
                val reference = (insn as ReferenceInstruction).reference as? MethodReference ?: return@forEach
                if (reference.name != "decodeFromString") return@forEach
                val stringParameter = reference.parameterTypes.indexOfFirst { it.toString() == "Ljava/lang/String;" }
                if (stringParameter < 0) return@forEach
                val call = insn as Instruction35c
                // The receiver comes first for the non-static calls.
                val registerIndex = stringParameter + if (insn.opcode == Opcode.INVOKE_STATIC) 0 else 1
                val register = listOf(call.registerC, call.registerD, call.registerE, call.registerF, call.registerG)
                    .getOrNull(registerIndex) ?: throw PatchException("Unexpected decodeFromString call")
                method.addInstructions(
                    index,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS->filter(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                    """
                )
                hooked++
            }
        }
        if (hooked == 0) throw PatchException("Couldn't find where server-driven pages are read")
        Logger.getLogger("HideSponsoredListings").info("Filtering $hooked server-driven page reads")

        // Native property cards and map markers: no sponsored data, so no "Ad" badge, no sponsored
        // marker style and no ad impression or click tracking.
        fieldGettersOf(HOTEL, "sponsoredListing").forEach { it.returnEarly() }
        fieldGettersOf(PROPERTY_MARKER, "sponsoredListing").forEach { it.returnEarly() }
    }
}
