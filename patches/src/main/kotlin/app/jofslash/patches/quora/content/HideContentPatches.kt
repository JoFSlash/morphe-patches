package app.jofslash.patches.quora.content

import app.jofslash.patches.quora.ads.PageFinishedFingerprint
import app.jofslash.patches.quora.shared.Constants.COMPATIBILITY_QUORA
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch

private const val EXTENSION_CLASS = "Lapp/jofslash/extension/quora/HideContentPatch;"

/**
 * Quora renders its content in a WebView. Each patch injects a stylesheet into every page
 * that hides one kind of element.
 */
private fun hideContentPatch(name: String, description: String, extensionMethod: String) = bytecodePatch(
    name = name,
    description = description,
    default = false
) {
    compatibleWith(COMPATIBILITY_QUORA)

    extendWith("extensions/extension.mpe")

    execute {
        PageFinishedFingerprint.method.addInstruction(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->$extensionMethod(Landroid/webkit/WebView;)V"
        )
    }
}

@Suppress("unused")
val hideQuoraPlusAnswersPatch = hideContentPatch(
    name = "Hide Quora+ answers",
    description = "Hides answers and posts that can only be read with a Quora+ subscription.",
    extensionMethod = "hideQuoraPlusAnswers",
)

@Suppress("unused")
val hideRelatedQuestionsPatch = hideContentPatch(
    name = "Hide related questions",
    description = "Hides the related questions lists on question pages.",
    extensionMethod = "hideRelatedQuestions",
)

@Suppress("unused")
val hideSpacesSuggestionsPatch = hideContentPatch(
    name = "Hide Spaces suggestions",
    description = "Hides the Spaces you might like suggestions in the feed.",
    extensionMethod = "hideSpacesSuggestions",
)
