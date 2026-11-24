# YouTube Kids patches for Morphe

[![Latest release](https://img.shields.io/github/v/release/JoFSlash/morphe-patches?label=release)](https://github.com/JoFSlash/morphe-patches/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/JoFSlash/morphe-patches/total)](https://github.com/JoFSlash/morphe-patches/releases)
[![License](https://img.shields.io/github/license/JoFSlash/morphe-patches)](../../LICENSE)

Patches for [YouTube Kids](https://play.google.com/store/apps/details?id=com.google.android.apps.youtube.kids)
(Google), applied with [Morphe](https://morphe.software). For now they make the patched app play videos through
MicroG-RE; more patches will follow.

Works with YouTube Kids <!-- VERSIONS_START -->11.47.541<!-- VERSIONS_END --> on Android 7 or newer. No account
and no root needed.

## Before you install

- **Install MicroG-RE first.** A patched YouTube Kids is no longer signed by Google, so Google Play services
  refuses it and the app crashes when a video starts. With GmsCore support it uses
  [MicroG-RE](https://github.com/MorpheApp/MicroG-RE) (`app.revanced.android.gms`) instead.
- **The patched app replaces the Play Store version.** It keeps the package name and is signed with a different
  key, so uninstall the Play Store version first.
- **Use it signed out.** Skip signing in during setup; signing in with a parent's account isn't tested yet.

## Install

1. Install Morphe Manager from [morphe.software](https://morphe.software), and MicroG-RE.
2. Add this repository as a patch source: open
   [this link](https://morphe.software/add-source?github=JoFSlash/morphe-patches) on your phone, or add
   `https://github.com/JoFSlash/morphe-patches` under sources in Morphe Manager.
3. Get a supported version of YouTube Kids as an APK, signed by Google. Pick the file in Morphe Manager, then pick
   the patches you want.
4. Uninstall the Play Store version.
5. Tap Install. Android asks you to allow Morphe Manager to install unknown apps. Google Play Protect may then
   say it hasn't seen the app before: tap **More details** and **Install without scanning**.

## Patches

<!-- PATCHES_START -->
<!-- PATCHES_END -->

## Something doesn't work

Please [open an issue](https://github.com/JoFSlash/morphe-patches/issues/new?template=bug_report.yml) with the
app's version, and leave out your name and any personal details.

## License

[GPLv3](../../LICENSE). YouTube and YouTube Kids are trademarks of Google LLC. This project isn't affiliated with
Google or Morphe.
