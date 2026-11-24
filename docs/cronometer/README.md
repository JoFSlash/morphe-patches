# Cronometer patches for Morphe

[![Latest release](https://img.shields.io/github/v/release/JoFSlash/morphe-patches?label=release)](https://github.com/JoFSlash/morphe-patches/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/JoFSlash/morphe-patches/total)](https://github.com/JoFSlash/morphe-patches/releases)
[![License](https://img.shields.io/github/license/JoFSlash/morphe-patches)](../../LICENSE)

Patches for the [Cronometer](https://play.google.com/store/apps/details?id=com.cronometer.android.gold) Android
app, the nutrition and calorie tracker, applied with [Morphe](https://morphe.software). The patches take out the
ads and stop the app's analytics, so you can log your food without being watched.

Works with Cronometer <!-- VERSIONS_START -->4.57.2<!-- VERSIONS_END --> on Android 8 or newer. No root needed.

- **No ads:** no banners and no full-screen ads between screens. The ads are never requested: the app is told
  none was available and hides the ad space, as it does when Google has nothing to show.
- **Less tracking:** Firebase Analytics, Crashlytics, Adjust install tracking and Amplitude events are off, and
  the Braze calls that log what you do or store details about you are dropped. Your diary, sync and the messages
  Cronometer sends through Braze keep working.

These patches don't unlock Cronometer Gold or any paid feature. Gold runs on Cronometer's servers and needs a
subscription; the patches don't change what your subscription gives you.

## Before you install

- **You need a Cronometer account.** The app only works signed in. A free account works: sign up in the app or
  sign in with your email and password.
- **Only 4.57.2.** Newer versions (4.57.4 and later, 4.60.4 at the time of writing) start through Google's PairIP
  protection, which checks the app's signature and Play license and closes an app that was patched and signed
  again. They aren't supported. Get exactly 4.57.2.
- **Some of your profile still reaches Braze.** Cronometer's own server copies some profile attributes to its
  Braze account. That happens on Cronometer's side, so the app patch can't stop it; it only stops what the app
  itself sends.
- **The patched app replaces the Play Store version.** It is signed with a different key, so uninstall the Play
  Store version first. Your diary is in your account, so sign back in afterwards.
- **Arm phones only.** Cronometer ships arm64 libraries only; the patched app runs on arm phones (nearly all of
  them), not on an x86 device or emulator without arm translation.

## Install

1. Install Morphe Manager from [morphe.software](https://morphe.software).
2. Add this repository as a patch source: open
   [this link](https://morphe.software/add-source?github=JoFSlash/morphe-patches) on your phone, or add
   `https://github.com/JoFSlash/morphe-patches` under sources in Morphe Manager.
3. Get Cronometer 4.57.2. Google Play only installs the newest version, so download 4.57.2 as an XAPK from
   [APKPure](https://apkpure.com/cronometer-calorie-counter/com.cronometer.android.gold) (its older versions
   list). Pick the file in Morphe Manager, then pick the patches you want.
4. Uninstall the Play Store version.
5. Tap Install. Android asks you to allow Morphe Manager to install unknown apps. Google Play Protect may then
   say it hasn't seen the app before: tap **More details** and **Install without scanning**.
6. Open the app and sign in.

## Updating

- **New patches:** a new version of these patches comes out every Saturday when something changed (see
  [Updating](../../README.md#updating)). Morphe Manager then marks the patched app with an Update badge. Patch
  the same Cronometer file again and install it over the patched app. No need to uninstall: you stay signed in.
- **New Cronometer version:** the patched app doesn't update from the Play Store. New versions are noticed
  automatically every day, but they have to be added by hand before they can be tested, and the current ones
  use PairIP, so don't expect support for a newer version soon.

## Patches

<!-- PATCHES_START -->
Version 1.11.0, 2 patches. Supported Cronometer versions: 4.57.2.

<details>
<summary><b>Ads</b> (1)</summary>

Banners and full-screen ads.

| Patch | Default | What it does |
| --- | --- | --- |
| Hide ads | on | Removes the banner ads and the full-screen ads between screens. No ad is ever requested: the app is told no ad was available, so it hides the ad space the same way it does when Google has none to show. |

</details>

<details>
<summary><b>Privacy</b> (1)</summary>

Analytics and tracking calls. They change nothing you see.

| Patch | Default | What it does |
| --- | --- | --- |
| Block tracking | on | Stops the usage analytics and crash reports: Firebase Analytics, Crashlytics, Adjust install tracking and Amplitude events, and the Braze calls that log what you do or store details about you. Your diary, sync and Braze messages from Cronometer keep working. |

</details>
<!-- PATCHES_END -->

## FAQ

**Is hiding ads allowed when "no ads" is part of Gold?**
The patches hide what you didn't ask to see on your own phone. They don't touch your subscription or any Gold
feature, and Gold works as before if you have it.

**Does this unlock Gold?**
No, and it won't. Requests for that are closed (see the [code of conduct](../../CODE_OF_CONDUCT.md)).

**Something doesn't work.**
Please [open an issue](https://github.com/JoFSlash/morphe-patches/issues/new?template=bug_report.yml) with the
app's version, and leave out your name, email and your diary or health data.

## Building

```
./gradlew :patches:buildAndroid
```

Building needs a GitHub token with `read:packages` in `GITHUB_ACTOR` / `GITHUB_TOKEN` (or `gpr.user` /
`gpr.key` in `~/.gradle/gradle.properties`), because the Morphe Gradle plugin comes from GitHub Packages.
`python .github/scripts/check_app.py cronometer <app.apks> --cli <morphe-cli.jar> --install --expect-screen`
applies every patch to the app (an APKS or XAPK with its split APKs) and starts it on a connected phone or
emulator, as the emulator test does.

## License

[GPLv3](../../LICENSE). Cronometer is a trademark of Cronometer Software Inc. This project isn't affiliated with
Cronometer or Morphe.
