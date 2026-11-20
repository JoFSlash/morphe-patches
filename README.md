# Google Health patches for Morphe

[![Latest release](https://img.shields.io/github/v/release/JoFSlash/google-health-patches?label=release)](https://github.com/JoFSlash/google-health-patches/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/JoFSlash/google-health-patches/total)](https://github.com/JoFSlash/google-health-patches/releases)
[![License](https://img.shields.io/github/license/JoFSlash/google-health-patches)](LICENSE)

Patches for the [Google Health](https://play.google.com/store/apps/details?id=com.fitbit.FitbitMobile) Android app
(formerly Fitbit), applied with [Morphe](https://morphe.software). A patched app signs in through MicroG-RE instead
of Google Play services, and the patches can turn off things the app does that you didn't ask for.

Works with Google Health <!-- VERSIONS_START -->5.13.health-mobile-1104148438-1023845615<!-- VERSIONS_END --> on
Android 12L or newer. No root needed.

These patches don't unlock Google Health Premium. Premium features run on Google's servers and need a
subscription.

## Install

1. Install [MicroG-RE](https://github.com/MorpheApp/MicroG-RE/releases) and add your Google account to it
   (MicroG-RE settings > Accounts). The patched app signs in with this account.
2. Install Morphe Manager from [morphe.software](https://morphe.software).
3. Add this repository as a patch source: open
   [this link](https://morphe.software/add-source?github=JoFSlash/google-health-patches) on your phone, or add
   `https://github.com/JoFSlash/google-health-patches` under sources in Morphe Manager.
4. Download a supported version of Google Health as an APKM file from
   [APKMirror](https://www.apkmirror.com/apk/google-inc/google-health-fitbit/). Pick the file in Morphe Manager,
   then pick the patches you want. Keep **GmsCore support** on.
5. Uninstall the Play Store version before installing the patched one. It is signed with a different key,
   so Android won't install it over the original. Your data stays in your Google account, so sign in again in
   the patched app and it comes back.

## Updating

- **New patches:** a new version of these patches comes out once a week, on Mondays, when there is
  something new. Morphe Manager then marks the patched app with an Update badge. Patch the same Google Health
  file again and install it over the patched app. No need to uninstall: you stay signed in.
- **New Google Health version:** the patched app doesn't update from the Play Store. Wait until a release of
  these patches lists the new version as supported, then patch that version the same way.

## Patches

<!-- PATCHES_START -->
Version 0.1.0, 3 patches. Supported Google Health versions: 5.13.health-mobile-1104148438-1023845615.

<details>
<summary><b>Privacy</b> (1)</summary>

On by default. They change nothing you see.

| Patch | Default | What it does |
| --- | --- | --- |
| Spoof installer | on | Reports Google Play as the app installer, so the app doesn't treat the patched app as installed from an unknown source. |

</details>

<details>
<summary><b>App</b> (2)</summary>

Signing in and how the app behaves.

| Patch | Default | What it does |
| --- | --- | --- |
| Allow screenshots | off | Lets you take screenshots and record the screen on every screen of the app. Some screens, such as sharing a health summary, block them. |
| GmsCore support | on | Makes signing in work through MicroG-RE instead of Google Play services. A patched app can't sign in through Play services, because it is no longer signed by Google. Needs MicroG-RE installed, with your Google account added to it. |

</details>
<!-- PATCHES_END -->

## FAQ

**Why do I need MicroG-RE?**
Google Play services only signs in apps signed by their maker. A patched app is signed with your own key, so
Play services refuses it. MicroG-RE signs the app in with your Google account instead. Without it, the app
stops at "Sign in with Google".

**"Sign in" opens MicroG-RE's Add account screen.**
MicroG-RE has no Google account yet. Add yours there, then tap Sign in in Google Health again.

**Can I keep the Play Store version too?**
No. Both use the same package name, so only one can be installed.

**Do my watch, tracker and data still work?**
Your data is stored in your Google account, so it shows up again after you sign in. Pairing and syncing a watch
or tracker use Bluetooth and the app's own servers, but they haven't been tried with the patched app yet.
If something doesn't work, please [open an issue](https://github.com/JoFSlash/google-health-patches/issues/new?template=bug_report.yml).

**Will you unlock Premium?**
No. Premium features (the AI coach, advanced insights) are computed on Google's servers for paying members, and
getting around the payment isn't something these patches do.

## Building

```
./gradlew buildAndroid
```

Building needs a GitHub token with `read:packages` in `GITHUB_ACTOR` / `GITHUB_TOKEN` (or `gpr.user` /
`gpr.key` in `~/.gradle/gradle.properties`), because the Morphe Gradle plugin comes from GitHub Packages.
`.github/scripts/check_app.py` applies every patch to an APKM and, with `--install`, starts the patched app
on a connected phone or emulator. See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[GPLv3](LICENSE). Google Health and Fitbit are trademarks of Google LLC. This project isn't affiliated with
Google or Morphe.
