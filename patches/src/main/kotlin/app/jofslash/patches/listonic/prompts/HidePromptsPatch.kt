package app.jofslash.patches.listonic.prompts

import app.jofslash.patches.listonic.shared.Constants.COMPATIBILITY_LISTONIC
import app.jofslash.patches.listonic.shared.methodOf
import app.jofslash.patches.listonic.shared.methodsWhere
import app.jofslash.patches.listonic.shared.requireLocalRegisters
import app.jofslash.patches.listonic.shared.returnEarly
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val TRIGGERS_MANAGER = "Lcom/listonic/trigger/TriggersManager;"
private const val TRIGGER_SEQUENCE = "Lcom/listonic/trigger/model/TriggerSequence;"
private const val UPDATE_LIBRARY = "Lcom/listonic/updatelibrary/UpdateLibrary;"

@Suppress("unused")
val hideRatingPromptsPatch = bytecodePatch(
    name = "Hide rating prompts",
    description = "Stops the prompt that asks you to rate the app on Google Play.",
    default = true
) {
    category("App")
    compatibleWith(COMPATIBILITY_LISTONIC)

    execute {
        // The rating prompt ("ReviewTrap") is a trigger sequence (app opens, items added, items
        // checked) registered with the TriggersManager. Don't register that one sequence.
        methodsWhere(TRIGGERS_MANAGER, "register(name, sequence, callback)") { method ->
            method.returnType == "V" && method.parameterTypes.size == 3 &&
                    method.parameterTypes[0].toString() == "Ljava/lang/String;" &&
                    method.parameterTypes[1].toString() == TRIGGER_SEQUENCE
        }.single().apply {
            requireLocalRegisters(1)
            addInstructions(
                0,
                """
                    const-string v0, "ReviewTrap"
                    invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :register
                    return-void
                    :register
                    nop
                """
            )
        }
    }
}

@Suppress("unused")
val hideUpdatePromptsPatch = bytecodePatch(
    name = "Hide update prompts",
    description = "Stops the Google Play update-available and forced-update screens. Google Play can't " +
            "update a patched app; update it by patching the new version.",
    default = true
) {
    category("App")
    compatibleWith(COMPATIBILITY_LISTONIC)

    execute {
        methodOf(UPDATE_LIBRARY, "checkForAppUpdate", emptyList()).returnEarly()
        methodOf(UPDATE_LIBRARY, "checkForFrozenAppUpdate", emptyList()).returnEarly()
    }
}
