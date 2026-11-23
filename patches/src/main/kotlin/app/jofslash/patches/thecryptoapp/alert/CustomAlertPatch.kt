package app.jofslash.patches.thecryptoapp.alert

import app.jofslash.patches.thecryptoapp.shared.ApplicationOnCreateFingerprint
import app.jofslash.patches.thecryptoapp.shared.Constants.COMPATIBILITY_THECRYPTOAPP
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private const val EXTENSION_PACKAGE = "app.jofslash.extension.thecryptoapp"
private const val EXTENSION_CLASS = "Lapp/jofslash/extension/thecryptoapp/CustomAlertPoc;"

// Registers the extension's own launcher Activity and the alarm/boot BroadcastReceiver into the
// host manifest (the extension's own manifest stays a bare <manifest/>; components are declared
// here, the same way the extension dex supplies their classes).
private val customAlertResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement
            val application = document.getElementsByTagName("application").item(0) as Element

            // Exact alarms so the check fires through Doze. USE_EXACT_ALARM (API 33+) is auto-granted
            // for an alarm-style app; SCHEDULE_EXACT_ALARM covers API 31-32. The extension falls back
            // to inexact if neither is effective (canScheduleExactAlarms() == false).
            fun addPermission(name: String) {
                val exists = document.getElementsByTagName("uses-permission").let { list ->
                    (0 until list.length).any {
                        (list.item(it) as Element).getAttribute("android:name") == name
                    }
                }
                if (!exists) manifest.insertBefore(
                    document.createElement("uses-permission").apply { setAttribute("android:name", name) },
                    application
                )
            }
            addPermission("android.permission.USE_EXACT_ALARM")
            addPermission("android.permission.SCHEDULE_EXACT_ALARM")

            // Standalone config screen, shown as a second launcher icon "Crypto Alerts". A framework
            // theme (DayNight) and a framework icon keep it free of any host-app resource dependency.
            val activity = document.createElement("activity").apply {
                setAttribute("android:name", "$EXTENSION_PACKAGE.AlertsActivity")
                setAttribute("android:exported", "true")
                setAttribute("android:label", "Crypto Alerts")
                setAttribute("android:icon", "@android:drawable/ic_popup_reminder")
                setAttribute("android:theme", "@android:style/Theme.DeviceDefault.DayNight")
                setAttribute("android:taskAffinity", "$EXTENSION_PACKAGE.alerts")
                appendChild(document.createElement("intent-filter").apply {
                    appendChild(document.createElement("action").apply {
                        setAttribute("android:name", "android.intent.action.MAIN")
                    })
                    appendChild(document.createElement("category").apply {
                        setAttribute("android:name", "android.intent.category.LAUNCHER")
                    })
                })
            }
            application.appendChild(activity)

            // Receives the ~15 min alarm tick (explicit intent) and BOOT_COMPLETED (to reschedule).
            val receiver = document.createElement("receiver").apply {
                setAttribute("android:name", "$EXTENSION_PACKAGE.AlertReceiver")
                setAttribute("android:exported", "true")
                appendChild(document.createElement("intent-filter").apply {
                    appendChild(document.createElement("action").apply {
                        setAttribute("android:name", "android.intent.action.BOOT_COMPLETED")
                    })
                })
            }
            application.appendChild(receiver)
        }
    }
}

@Suppress("unused")
val customAlertPatch = bytecodePatch(
    name = "Custom price alerts",
    description = "Adds a self-contained price-alert feature: a \"Crypto Alerts\" screen to manage rules " +
            "(symbol, target price, above/below), and an AlarmManager check (~15 min) that fetches prices from " +
            "a free public API and posts a local notification when a rule is met. It never touches the app's " +
            "Pro tier or its server-side alerts, and needs no account.",
    default = true
) {
    category("Misc")
    compatibleWith(COMPATIBILITY_THECRYPTOAPP)

    extendWith("extensions/thecryptoapp.mpe")
    dependsOn(customAlertResourcePatch)

    execute {
        // At the very top of App.onCreate, hand the Application (a Context) to our extension, which
        // creates the channel, schedules the alarm and runs an immediate check. The range form
        // reaches p0 regardless of how many locals onCreate uses.
        ApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->init(Landroid/content/Context;)V"
        )
    }
}
