package app.jofslash.patches.quora.privacy

import app.jofslash.patches.quora.shared.Constants.COMPATIBILITY_QUORA
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch

private const val EXTENSION_CLASS = "Lapp/jofslash/extension/quora/BlockTelemetryPatch;"

// Every call to Quora's /api/do_action_POST goes through here.
internal object CallApiFingerprint : Fingerprint(
    definingClass = "Lcom/quora/android/networking/QNetworkCalls;",
    name = "callApi",
    returnType = "V",
    parameters = listOf(
        "Lcom/quora/android/util/QJSONObject;",
        "Lcom/quora/android/networking/QNetworkCalls\$QApiCallback;",
    ),
)

// Schedules the hourly charging status job and registers the charging receiver.
internal object ScheduleChargingStatusLogFingerprint : Fingerprint(
    definingClass = "Lcom/quora/android/logging/QChargingStatusWorker\$Companion;",
    name = "scheduleChargingStatusLog",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

// What the hourly job runs: registers the receiver and logs the charging state.
internal object LogChargingStatusChangedFingerprint : Fingerprint(
    definingClass = "Lcom/quora/android/logging/QChargingStatusWorkerKt;",
    name = "logChargingStatusChanged",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

@Suppress("unused")
val blockAppTelemetryPatch = bytecodePatch(
    name = "Block app telemetry",
    description = "Stops the app from sending usage logs to Quora, such as reading time, " +
            "tab presses, experiments, app speed and errors.",
    default = true
) {
    compatibleWith(COMPATIBILITY_QUORA)

    extendWith("extensions/extension.mpe")

    execute {
        // p1 is the API call. The callers of logging calls don't wait for an answer,
        // so the call is dropped without calling the callback.
        CallApiFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS->isTelemetry(Lorg/json/JSONObject;)Z
                move-result v0
                if-eqz v0, :call
                return-void
                :call
                nop
            """
        )
    }
}

@Suppress("unused")
val disableChargingTrackingPatch = bytecodePatch(
    name = "Disable charging tracking",
    description = "Stops the hourly job that tells Quora whether your phone is charging.",
    default = true
) {
    compatibleWith(COMPATIBILITY_QUORA)

    execute {
        // Cancel the job if an unpatched install already scheduled it, and don't schedule it again.
        ScheduleChargingStatusLogFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p1 }, Landroidx/work/WorkManager;->getInstance(Landroid/content/Context;)Landroidx/work/WorkManager;
                move-result-object v0
                const-string v1, "QChargingStatusWorker"
                invoke-virtual { v0, v1 }, Landroidx/work/WorkManager;->cancelUniqueWork(Ljava/lang/String;)Landroidx/work/Operation;
                return-void
            """
        )

        // In case the job still runs once before it is cancelled.
        LogChargingStatusChangedFingerprint.method.addInstructions(0, "return-void")
    }
}
