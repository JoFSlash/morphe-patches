package app.jofslash.patches.quora.video

import app.jofslash.patches.quora.shared.Constants.COMPATIBILITY_QUORA
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

internal object InitWebViewFingerprint : Fingerprint(
    definingClass = "Lcom/quora/android/fragments/qwvf/QWebViewController;",
    name = "initWebView",
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "Landroid/webkit/WebSettings;",
            name = "setMediaPlaybackRequiresUserGesture",
        )
    )
)

@Suppress("unused")
val disableVideoAutoplayPatch = bytecodePatch(
    name = "Disable video autoplay",
    description = "Videos in the feed and in answers only play when you tap them.",
    default = false
) {
    compatibleWith(COMPATIBILITY_QUORA)

    execute {
        // Quora calls WebSettings.setMediaPlaybackRequiresUserGesture(false), which lets pages
        // start videos on their own. Pass true instead.
        InitWebViewFingerprint.let {
            it.method.apply {
                val index = it.instructionMatches.first().index
                val register = getInstruction<FiveRegisterInstruction>(index).registerD
                addInstruction(index, "const/4 v$register, 0x1")
            }
        }
    }
}
