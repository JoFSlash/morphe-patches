package app.jofslash.patches.listonic.premium

import app.jofslash.patches.listonic.shared.Constants.COMPATIBILITY_LISTONIC
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

// The R8-renamed classes are found through text they keep. PremiumPromotionData (set id, category,
// texts, end time) is one sale campaign, such as "Black Friday sale! -50%".
internal object PromotionDataToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("PremiumPromotionData(setId="),
)

// The promotion repository: it tags the user as a "FlashSaleUser" for the sale prices.
internal object PromotionRepositoryFingerprint : Fingerprint(
    strings = listOf("FlashSaleUser"),
)

@Suppress("unused")
val hidePremiumOffersPatch = bytecodePatch(
    name = "Hide Premium offers",
    description = "Hides the Premium sale campaigns (such as \"Black Friday sale! -50%\") that show up by " +
            "themselves on the lists screen. Premium itself, its features and the offers you open yourself stay.",
    default = true
) {
    category("App")
    compatibleWith(COMPATIBILITY_LISTONIC)

    execute {
        val promotionData = PromotionDataToStringFingerprint.method.definingClass
        val repository = PromotionRepositoryFingerprint.method.definingClass

        // The step that hands the current campaign to the screens (or null when there is none) is a
        // coroutine created with (campaign, repository, continuation). Start it with no campaign.
        val emitters = mutableListOf<String>()
        classDefForEach { classDef ->
            if (classDef.methods.any { method ->
                    method.name == "<init>" && method.parameterTypes.size == 3 &&
                            method.parameterTypes[0].toString() == promotionData &&
                            method.parameterTypes[1].toString() == repository
                }) emitters += classDef.type
        }
        if (emitters.size != 1) throw PatchException("Expected one promotion emitter, found ${emitters.size}")

        mutableClassDefBy(emitters.single()).methods.single { method ->
            method.name == "<init>" && method.parameterTypes.size == 3 &&
                    method.parameterTypes[0].toString() == promotionData
        }.addInstructions(0, "const/4 p1, 0x0")
    }
}
