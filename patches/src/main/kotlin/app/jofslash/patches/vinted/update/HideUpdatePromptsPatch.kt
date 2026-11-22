package app.jofslash.patches.vinted.update

import app.jofslash.patches.vinted.shared.Constants.APP
import app.jofslash.patches.vinted.shared.Constants.COMPATIBILITY_VINTED
import app.jofslash.patches.vinted.shared.methodsWhere
import app.jofslash.patches.vinted.shared.unitField
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

private const val APPLICATION_UPDATE = "$APP/feature/applicationupdate"
private const val UPDATE_HELPER = "$APPLICATION_UPDATE/helper/AppUpdateNotificationHelperImpl;"
private const val UPDATE_TYPE = "$APPLICATION_UPDATE/api/entity/UpdateType;"

@Suppress("unused")
val hideUpdatePromptsPatch = bytecodePatch(
    name = "Hide update prompts",
    description = "Hides the \"a new version is available\" prompt that Vinted shows once a week when a newer " +
            "version is out. When Vinted says the version is too old to keep working (a forced update), the " +
            "prompt still shows.",
    default = true
) {
    category("App")
    compatibleWith(COMPATIBILITY_VINTED)

    execute {
        val unit = unitField()
        // handleResponse(current version, newest version, UpdateType, ...) gets the server's answer
        // about the app version and shows the soft (can be skipped) or the forced update prompt.
        val methods = methodsWhere(UPDATE_HELPER, "handleResponse(int, int, UpdateType, ...)") {
            it.name == "handleResponse" && it.parameterTypes.map { type -> type.toString() }.take(3) == listOf("I", "I", UPDATE_TYPE)
        }
        if (methods.size != 1) throw PatchException("Expected one $UPDATE_HELPER->handleResponse, found ${methods.size}")
        // p0 is this, p1 and p2 the versions, p3 the update type. A soft update finishes at once.
        methods.single().addInstructions(
            0,
            """
                sget-object v0, $UPDATE_TYPE->soft:$UPDATE_TYPE
                if-ne p3, v0, :keep
                sget-object v0, $unit
                return-object v0
                :keep
                nop
            """
        )
    }
}
