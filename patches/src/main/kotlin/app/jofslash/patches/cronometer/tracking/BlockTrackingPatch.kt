package app.jofslash.patches.cronometer.tracking

import app.jofslash.patches.cronometer.shared.Constants.COMPATIBILITY_CRONOMETER
import app.jofslash.patches.cronometer.shared.editManifest
import app.jofslash.patches.cronometer.shared.setApplicationMetaData
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.smali.ExternalLabel

// Braze plugin calls that only report what the user does or who they are. Push, in-app messages,
// content cards, feature flags and changeUser keep working, because Cronometer sends its own news
// through them. Dismissing a content card is kept too: it is how a card stays hidden.
private val BRAZE_TRACKING_CALLS = listOf(
    "logCustomEvent",
    "logPurchase",
    "setStringCustomUserAttribute",
    "setDoubleCustomUserAttribute",
    "setDateCustomUserAttribute",
    "setIntCustomUserAttribute",
    "setBoolCustomUserAttribute",
    "setLocationCustomAttribute",
    "setNestedCustomUserAttribute",
    "setCustomUserAttributeArrayOfStrings",
    "setCustomUserAttributeArrayOfObjects",
    "incrementCustomUserAttribute",
    "addToCustomAttributeArray",
    "removeFromCustomAttributeArray",
    "unsetCustomUserAttribute",
    "setLastKnownLocation",
    "requestLocationInitialization",
    "setGoogleAdvertisingId",
    "setAdTrackingEnabled",
    "setAttributionData",
    "logContentCardImpression",
    "logContentCardClicked",
    "logInAppMessageImpression",
    "logInAppMessageClicked",
    "logInAppMessageButtonClicked",
    "logBannerImpression",
    "logBannerClicked",
    "logFeatureFlagImpression",
)

private const val ADJUST_INSTANCE = "Lcom/adjust/sdk/AdjustInstance;"
private const val BRAZE_PLUGIN = "Lcom/braze/brazeplugin/BrazePlugin;"

private val blockTrackingResourcePatch = resourcePatch {
    execute {
        editManifest { document ->
            // Firebase reads these before any Dart code runs. "deactivated" is the permanent switch:
            // setAnalyticsCollectionEnabled(true) can't turn it back on. Cronometer's Dart code never
            // calls setCrashlyticsCollectionEnabled, so the Crashlytics flag holds as well.
            document.setApplicationMetaData("firebase_analytics_collection_deactivated", "true")
            document.setApplicationMetaData("google_analytics_adid_collection_enabled", "false")
            document.setApplicationMetaData("google_analytics_default_allow_ad_personalization_signals", "false")
            document.setApplicationMetaData("firebase_crashlytics_collection_enabled", "false")
            document.setApplicationMetaData("firebase_sessions_enabled", "false")
        }
    }
}

@Suppress("unused")
val blockTrackingPatch = bytecodePatch(
    name = "Block tracking",
    description = "Stops the usage analytics and crash reports: Firebase Analytics, Crashlytics, Adjust " +
            "install tracking and Amplitude events, and the Braze calls that log what you do or store " +
            "details about you. Your diary, sync and Braze messages from Cronometer keep working.",
    default = true
) {
    category("Privacy")
    compatibleWith(COMPATIBILITY_CRONOMETER)

    dependsOn(blockTrackingResourcePatch)

    execute {
        // Adjust: start the SDK disabled. initSdk copies startEnabled into the config, and the
        // ActivityHandler queues setEnabled(false) whenever it is set, so this also turns Adjust off
        // on an install that already ran it. Nothing is sent while it is disabled, and the getters
        // Dart can call still answer. enable() is the only way back on, so it does nothing.
        val adjust = mutableClassDefByOrNull(ADJUST_INSTANCE)
            ?: throw PatchException("$ADJUST_INSTANCE not found. The Adjust SDK changed.")
        if (adjust.fields.none { it.name == "startEnabled" && it.type == "Ljava/lang/Boolean;" }) {
            throw PatchException("AdjustInstance.startEnabled not found.")
        }
        val initSdk = adjust.methods.singleOrNull {
            it.name == "initSdk" && it.returnType == "V" &&
                    it.parameterTypes.map(CharSequence::toString) == listOf("Lcom/adjust/sdk/AdjustConfig;")
        } ?: throw PatchException("AdjustInstance.initSdk(AdjustConfig) not found.")
        initSdk.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                iput-object v0, p0, $ADJUST_INSTANCE->startEnabled:Ljava/lang/Boolean;
            """
        )
        val enable = adjust.methods.singleOrNull {
            it.name == "enable" && it.returnType == "V" && it.parameterTypes.isEmpty()
        } ?: throw PatchException("AdjustInstance.enable() not found.")
        enable.addInstructions(0, "return-void")

        // Amplitude: drop every event at process(), before it is stored or queued for upload. Only
        // the analytics SDK is affected; Cronometer's Amplitude feature flags are fetched separately.
        AmplitudeProcessEventFingerprint.method.addInstructions(0, "return-void")

        // Braze: answer the tracking calls from Dart without passing them on. Nothing is live at the
        // start of onMethodCall, so v0/v1 are free. The parameters sit above v15, out of reach of
        // iget/invoke, so they are copied down with move-object/from16 first.
        val braze = mutableClassDefByOrNull(BRAZE_PLUGIN)
            ?: throw PatchException("$BRAZE_PLUGIN not found. The Braze plugin changed.")
        val onMethodCall = braze.methods.singleOrNull {
            it.name == "onMethodCall" && it.returnType == "V" &&
                    it.parameterTypes.map(CharSequence::toString) == listOf(
                "Lio/flutter/plugin/common/MethodCall;",
                "Lio/flutter/plugin/common/MethodChannel\$Result;",
            )
        } ?: throw PatchException("BrazePlugin.onMethodCall not found.")
        val checks = BRAZE_TRACKING_CALLS.joinToString("\n") { name ->
            """
                const-string v1, "$name"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-nez v1, :drop
            """
        }
        onMethodCall.addInstructionsWithLabels(
            0,
            """
                move-object/from16 v0, p1
                iget-object v0, v0, Lio/flutter/plugin/common/MethodCall;->method:Ljava/lang/String;
                if-eqz v0, :original
                $checks
                goto :original
                :drop
                move-object/from16 v0, p2
                const/4 v1, 0x0
                invoke-interface {v0, v1}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                return-void
            """,
            ExternalLabel("original", onMethodCall.getInstruction(0))
        )
    }
}
