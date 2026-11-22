package app.jofslash.patches.vinted.review

import app.jofslash.patches.vinted.shared.Constants.APP
import app.jofslash.patches.vinted.shared.Constants.COMPATIBILITY_VINTED
import app.jofslash.patches.vinted.shared.methodsWhere
import app.jofslash.patches.vinted.shared.returnUnit
import app.jofslash.patches.vinted.shared.unitField
import app.morphe.patcher.patch.bytecodePatch

private const val RATE_APP_TRIGGER_HANDLER = "$APP/feature/rateapp/presenters/RateAppDialogTriggerHandler;"

@Suppress("unused")
val hideRatingPromptsPatch = bytecodePatch(
    name = "Hide rating prompts",
    description = "Hides the dialog that asks you to rate the app on Google Play after an order is completed, " +
            "after you leave positive feedback or after you mark an item as sold.",
    default = true
) {
    category("App")
    compatibleWith(COMPATIBILITY_VINTED)

    execute {
        // Every trigger (an order marked as fine, positive feedback left, an item marked as sold) goes through onTrigger, which checks
        // the timeouts and then shows RateAppProJetDialogFragment. Finished at once, it shows
        // nothing. It is a suspend function, so it returns Unit.
        val unit = unitField()
        methodsWhere(RATE_APP_TRIGGER_HANDLER, "onTrigger(AppRateActionEvent, Continuation)") {
            it.name == "onTrigger" && it.parameterTypes.size == 2
        }.forEach { it.returnUnit(unit) }
    }
}
