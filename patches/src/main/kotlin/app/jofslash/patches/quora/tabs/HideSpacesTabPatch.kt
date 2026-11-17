package app.jofslash.patches.quora.tabs

import app.jofslash.patches.quora.shared.Constants.COMPATIBILITY_QUORA
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/jofslash/extension/quora/NativeTweaksPatch;"

internal object CreateTabListFingerprint : Fingerprint(
    definingClass = "Lcom/quora/android/controls/QTabBar;",
    name = "createTabList",
    returnType = "Ljava/util/ArrayList;",
    parameters = listOf(),
)

@Suppress("unused")
val hideSpacesTabPatch = bytecodePatch(
    name = "Hide Spaces tab",
    description = "Removes the Spaces tab from the bottom bar.",
    default = false
) {
    compatibleWith(COMPATIBILITY_QUORA)

    extendWith("extensions/extension.mpe")

    execute {
        CreateTabListFingerprint.method.apply {
            // Right after the last tab is added. Other paths jump straight to the return,
            // so a call placed before it would be skipped.
            val instructions = implementation!!.instructions
            val addIndex = instructions.indexOfLast {
                it.opcode == Opcode.INVOKE_VIRTUAL &&
                        ((it as ReferenceInstruction).reference as MethodReference).name == "add"
            }
            val register = getInstruction<FiveRegisterInstruction>(addIndex).registerC
            val insertIndex =
                if (instructions.elementAt(addIndex + 1).opcode == Opcode.MOVE_RESULT) addIndex + 2 else addIndex + 1
            addInstruction(
                insertIndex,
                "invoke-static { v$register }, $EXTENSION_CLASS->removeSpacesTab(Ljava/util/List;)V"
            )
        }
    }
}
