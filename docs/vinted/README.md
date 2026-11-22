# Vinted patches for Morphe

[![Latest release](https://img.shields.io/github/v/release/JoFSlash/morphe-patches?label=release)](https://github.com/JoFSlash/morphe-patches/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/JoFSlash/morphe-patches/total)](https://github.com/JoFSlash/morphe-patches/releases)
[![License](https://img.shields.io/github/license/JoFSlash/morphe-patches)](../../LICENSE)

Patches for the [Vinted](https://play.google.com/store/apps/details?id=fr.vinted) Android app, the second-hand
clothes marketplace, applied with [Morphe](https://morphe.software). The patches take out the ads, the rating and
update prompts and the marketing pop-ups, and the marketing trackers.

Works with Vinted <!-- VERSIONS_START -->26.45.0<!-- VERSIONS_END --> on Android 12L or newer. No root needed.

- **No ads:** no ad banners in the feed, search, inbox and order screens, no partner offers after a purchase,
  and the ad networks are never started.
- **No nagging:** no "rate the app" dialog, no weekly "new version available" prompt, no marketing pop-ups.
- **Less tracking:** no Adjust, Facebook app events, Firebase or Google Analytics, Crashlytics or Braze events,
  and no advertising ID.
- **Sign in with Google without Google Play services:** through MicroG-RE, or sign in with your email.

Nothing that sellers pay Vinted for is changed: bumped items, promoted closets (Spotlight) and the other paid
features show and work as before, and the Buyer Protection fee and shipping prices are Vinted's own.

## Before you install

- **Vinted checks the app it talks to.** For registering, paying, payouts and listing items, Vinted asks Google
  Play for an integrity verdict, and a fraud-protection service (Incognia) looks at the app too. A patched app
  isn't the Play Store's copy, so Vinted can see that. Whether Vinted then turns those actions down or reviews
  the account is decided on Vinted's servers and may change at any time. Browsing and signing in worked in our
  tests; **buying, selling and payouts haven't been tried with the patched app.** If something you need is
  refused, use the Play Store app for it.
- **Signing in with Google needs MicroG-RE.** Google only signs in the app signed by its maker, so the patched
  app signs in through [MicroG-RE](https://github.com/MorpheApp/MicroG-RE/releases) instead of Google Play
  services (keep **GmsCore support** on). Install it and add your Google account to it (MicroG-RE settings >
  Accounts). Push notifications come through MicroG-RE too. Without it, sign in with your email and password.
- **Signing in with Facebook may not work,** as Facebook checks the app's signature. Use Google or your email.
- **Google Pay doesn't work through MicroG-RE.** Pay by card or with another method Vinted offers.
- **The patched app replaces the Play Store version.** It is signed with a different key, so uninstall the Play
  Store version first. Your account, messages and orders are stored by Vinted, so they're all there after you
  sign in.

## Install

1. To sign in with Google, install [MicroG-RE](https://github.com/MorpheApp/MicroG-RE/releases) and add your
   Google account to it (MicroG-RE settings > Accounts).
2. Install Morphe Manager from [morphe.software](https://morphe.software).
3. Add this repository as a patch source: open
   [this link](https://morphe.software/add-source?github=JoFSlash/morphe-patches) on your phone, or add
   `https://github.com/JoFSlash/morphe-patches` under sources in Morphe Manager.
4. Get a supported version of Vinted: download it as an XAPK from
   [APKPure](https://apkpure.com/vinted/fr.vinted), or save the split APKs of the Play Store version from your
   phone as an APKS file (for example with SAI). Pick the file in Morphe Manager, then pick the patches you
   want. Keep **GmsCore support** on to sign in with Google.
5. Uninstall the Play Store version.
6. Tap Install. Android asks you to allow Morphe Manager to install unknown apps. Google Play Protect may then
   say it hasn't seen the app before: tap **More details** and **Install without scanning**.

## Updating

- **New patches:** a new version of these patches comes out every Saturday when something changed (see
  [Updating](../../README.md#updating)). Morphe Manager then marks the patched app with an Update badge. Patch
  the same Vinted file again and install it over the patched app. No need to uninstall: you stay signed in.
- **New Vinted version:** the patched app doesn't update from the Play Store. New versions on Google Play are
  noticed automatically every day, but the app has to be added by hand before it can be tested, so support can
  take a few days. When a release of these patches lists the new version as supported, patch that version the
  same way. **Hide update prompts** only hides the "a new version is available" prompt; when Vinted says the
  version is too old to keep working, the prompt still shows.

## Patches

<!-- PATCHES_START -->
Version 1.10.0, 7 patches. Supported Vinted versions: 26.45.0.

<details>
<summary><b>Ads</b> (1)</summary>

Ad banners, sponsored ads in the feed and partner offers.

| Patch | Default | What it does |
| --- | --- | --- |
| Hide ads | on | Hides the ad banners in the feed, search, inbox and order screens and the partner offers after a purchase, and never starts the ad networks (Google Ad Manager, AppLovin, Amazon, Rokt and the networks they mediate). Promoted closets and bumped items, which sellers pay Vinted for, stay as they are. |

</details>

<details>
<summary><b>App</b> (4)</summary>

Prompts, pop-ups and signing in with Google.

| Patch | Default | What it does |
| --- | --- | --- |
| GmsCore support | on | Makes "Continue with Google", push notifications and the pick-up point maps work through MicroG-RE instead of Google Play services. A patched app can't sign in with Google through Play services, because it is no longer signed by Vinted. Needs MicroG-RE installed, with your Google account added to it; without it, signing in with email still works, and the app says once that MicroG-RE is missing. Google Pay doesn't work through MicroG-RE; pay by card instead. |
| Hide marketing pop-ups | on | Hides the marketing messages (Braze in-app messages) that pop up over the app: campaigns, promotions and feature announcements. Messages from buyers and sellers, notifications and order updates aren't touched. |
| Hide rating prompts | on | Hides the dialog that asks you to rate the app on Google Play after an order is completed, after you leave positive feedback or after you mark an item as sold. |
| Hide update prompts | on | Hides the "a new version is available" prompt that Vinted shows once a week when a newer version is out. When Vinted says the version is too old to keep working (a forced update), the prompt still shows. |

</details>

<details>
<summary><b>Privacy</b> (2)</summary>

Trackers and permissions. They change nothing you see.

| Patch | Default | What it does |
| --- | --- | --- |
| Block tracking | on | Stops the marketing and analytics trackers: Adjust install and purchase tracking, Facebook app events, Firebase and Google Analytics, Firebase Crashlytics and Sessions, and the events the app sends to Braze. Vinted's own fraud protection (DataDome, Incognia, Play Integrity) isn't touched. |
| Remove unneeded permissions | on | Removes the permissions that only serve advertising and install tracking: the advertising ID, Android's ad services (ad ID, attribution, topics and custom audiences), the Google Play install referrer, the Google Play license check and Amazon's ad attestation. |

</details>
<!-- PATCHES_END -->

## FAQ

**Why aren't bumped items and promoted closets hidden?**
Sellers pay Vinted to show them. The patches hide ads from other companies and Vinted's own prompts, not
the listings of other Vinted members.

**Why do I need MicroG-RE for Google sign-in?**
Google Play services only signs in apps signed by their maker. A patched app is signed with your own key, so
Play services refuses it. MicroG-RE signs the app in with your Google account instead, and tells Google the
certificate Vinted signs the app with. Without it, "Continue with Google" fails; email still works.

**Something doesn't work.**
Please [open an issue](https://github.com/JoFSlash/morphe-patches/issues/new?template=bug_report.yml) with the
app's version, and leave out your name, email, address and order details.

## Building

```
./gradlew :patches:buildAndroid
```

Building needs a GitHub token with `read:packages` in `GITHUB_ACTOR` / `GITHUB_TOKEN` (or `gpr.user` /
`gpr.key` in `~/.gradle/gradle.properties`), because the Morphe Gradle plugin comes from GitHub Packages.
`python .github/scripts/check_vinted.py <app.xapk> --cli <morphe-cli.jar> --install --expect-screen`
applies every patch to the app (an XAPK, APKS or APKM with its split APKs) and starts it on a connected phone or
emulator, as the emulator test does.

## License

[GPLv3](../../LICENSE). Vinted is a trademark of Vinted UAB. This project isn't affiliated with Vinted or
Morphe.
