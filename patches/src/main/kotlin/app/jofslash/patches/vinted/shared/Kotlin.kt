package app.jofslash.patches.vinted.shared

import app.jofslash.patches.vinted.shared.Constants.APP
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod

private const val UNIT = "Lkotlin/Unit;"

/**
 * The static field that holds kotlin.Unit, as "Lkotlin/Unit;->a:Lkotlin/Unit;". R8 renames it
 * (INSTANCE is "a" in 26.45.0), so it is looked up instead of written down.
 */
internal fun BytecodePatchContext.unitField(): String {
    val unit = mutableClassDefByOrNull(UNIT) ?: throw PatchException("Couldn't find $UNIT")
    val field = unit.staticFields.singleOrNull { it.type == UNIT }
        ?: throw PatchException("Couldn't find the one static $UNIT field of $UNIT")
    return "$UNIT->${field.name}:$UNIT"
}

/**
 * Makes a method that returns Object (a suspend function or a coroutine body) return Unit at once,
 * so it finishes without doing anything.
 */
internal fun MutableMethod.returnUnit(unitField: String) {
    if (returnType != "Ljava/lang/Object;") throw PatchException("$definingClass->$name doesn't return Object")
    addInstructions(0, "sget-object v0, $unitField\nreturn-object v0")
}

/** The application class; onCreate keeps its name because Android calls it. */
internal const val APPLICATION = "$APP/MDApplication;"

