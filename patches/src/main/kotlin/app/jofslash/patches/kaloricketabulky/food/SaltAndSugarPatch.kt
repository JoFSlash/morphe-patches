package app.jofslash.patches.kaloricketabulky.food

import app.jofslash.patches.kaloricketabulky.shared.Constants.APP
import app.jofslash.patches.kaloricketabulky.shared.Constants.COMPATIBILITY_KALORICKE_TABULKY
import app.jofslash.patches.kaloricketabulky.shared.methodOf
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/jofslash/extension/kaloricketabulky/OpenFoodFacts;"

/** Turns a food's detail from the API (FoodstuffDetailDto) into the app's Food; it has the barcodes. */
private const val FOODSTUFF_UTILS = "$APP/network/jsonApi/foodstuff/UtilsKt;"

/** The base of the app's Compose pages; its onCreateView makes the page's ComposeView. */
private const val COMPOSABLE_FRAGMENT = "$APP/compose/viewcompat/ComposableFragment;"

@Suppress("unused")
val saltAndSugarPatch = bytecodePatch(
    name = "Salt and sugar from Open Food Facts",
    description = "Adds a Salt and sugar button to the page of a food with a barcode. It looks the barcode up in " +
            "Open Food Facts, a free and open food database, and shows the salt and sugars it lists per 100 g. " +
            "Only the barcode is sent, only when you tap the button. Premium isn't unlocked: the app's own " +
            "values stay as they are, the numbers come from Open Food Facts.",
    default = true
) {
    category("Food")
    compatibleWith(COMPATIBILITY_KALORICKE_TABULKY)

    extendWith("extensions/kaloricketabulky.mpe")

    execute {
        // Keep each loaded food's barcodes, by food id, for its page.
        methodOf(FOODSTUFF_UTILS, "foodstuffDetailDtoToFood").addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->remember(Ljava/lang/Object;)V"
        )

        // The food's page gets the button: the extension puts the page in a frame with it. The
        // fragment is handed over at the start, as its register is reused before the return.
        val onCreateView = methodOf(
            COMPOSABLE_FRAGMENT,
            "onCreateView",
            listOf("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;", "Landroid/os/Bundle;")
        )
        val instructions = onCreateView.implementation!!.instructions.toList()
        val returns = instructions.indices.reversed().filter { instructions[it].opcode == Opcode.RETURN_OBJECT }
        if (returns.isEmpty()) throw PatchException("ComposableFragment.onCreateView returns nothing")
        returns.forEach { index ->
            val register = (instructions[index] as OneRegisterInstruction).registerA
            if (register > 15) throw PatchException("ComposableFragment.onCreateView returns a high register")
            onCreateView.addInstructions(
                index,
                """
                    invoke-static { v$register }, $EXTENSION_CLASS->wrap(Landroid/view/View;)Landroid/view/View;
                    move-result-object v$register
                """
            )
        }
        onCreateView.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->creating(Ljava/lang/Object;)V"
        )
    }
}
