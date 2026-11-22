package app.jofslash.patches.vinted.messages

import app.jofslash.patches.vinted.shared.Constants.APP
import app.jofslash.patches.vinted.shared.Constants.COMPATIBILITY_VINTED
import app.jofslash.patches.vinted.shared.methodOf
import app.jofslash.patches.vinted.shared.returnEarly
import app.morphe.patcher.patch.bytecodePatch

private const val CRM_IN_APPS = "$APP/feature/crm/inapps"
private const val CRM_IN_APP_DISPLAY_MANAGER = "$CRM_IN_APPS/CrmInAppDisplayManagerImpl;"
private const val CRM_IN_APP = "$CRM_IN_APPS/CrmInApp;"

@Suppress("unused")
val hideMarketingPopupsPatch = bytecodePatch(
    name = "Hide marketing pop-ups",
    description = "Hides the marketing messages (Braze in-app messages) that pop up over the app: campaigns, " +
            "promotions and feature announcements. Messages from buyers and sellers, notifications and " +
            "order updates aren't touched.",
    default = true
) {
    category("App")
    compatibleWith(COMPATIBILITY_VINTED)

    execute {
        // Every Braze in-app message the app gets (modal, full screen or slide-up) is shown here.
        methodOf(CRM_IN_APP_DISPLAY_MANAGER, "showInApp", listOf(CRM_IN_APP)).returnEarly()
    }
}
