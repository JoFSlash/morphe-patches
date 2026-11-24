# Contributing to the YouTube Kids patches

YouTube Kids is obfuscated like YouTube (classes in the default package), so patches find their methods by
strings and signatures. It has no anti-tamper and no Play Integrity check. The universal APK has x86_64
libraries: `python .github/scripts/check_app.py youtubekids <youtubekids.apk> --cli <morphe-cli.jar> --install
--expect-screen` patches it with every patch and starts it on a phone or emulator, as the emulator test does.

Playback needs GmsCore support: the player asks Play services for a DroidGuard result (thread "DG"), and Play
services throws `SecurityException: PackageVerificationRslt: not allowed` for a re-signed caller, which kills the
app. Through MicroG-RE the DroidGuard request fails quietly and videos still play, so YouTube's Spoof video streams
isn't needed (checked with 11.47.541, signed out). If videos start to stop after a minute or get 403 errors,
that is the place to look.

See the [general guide](../../CONTRIBUTING.md) for building, pull requests and where things are.
