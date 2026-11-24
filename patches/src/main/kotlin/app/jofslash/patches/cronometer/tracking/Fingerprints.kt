package app.jofslash.patches.cronometer.tracking

import app.morphe.patcher.Fingerprint

// Amplitude's process(event): every tracked event, identify and revenue call passes through it on
// the way to the event pipeline. Its first step is the opt-out check, which logs this line. The SDK's
// logger has the same literal in a method without arguments, so the one-argument void shape keeps the
// match on process(). R8 renames the class and the method.
internal object AmplitudeProcessEventFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("Skip event for opt out config."),
    custom = { method, _ -> method.parameterTypes.size == 1 },
)
