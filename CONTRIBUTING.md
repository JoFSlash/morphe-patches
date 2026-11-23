# Contributing

Thanks for helping. Each app is patched differently, so each has its own guide:

- [Quora](docs/quora/CONTRIBUTING.md): a WebView app; most patches inject CSS or scripts into Quora's pages.
- [Google Health](docs/google-health/CONTRIBUTING.md): an obfuscated app; patches find code by strings and
  framework calls.
- Booking.com: a Compose app whose search results and home screen come from Booking.com's servers; see
  [Patches in detail](docs/booking/patches.md) for how each patch works.
  `python .github/scripts/check_booking.py <apkm> --cli <morphe-cli.jar> --install --expect-screen` patches
  an APKM with every Booking.com patch and starts it on a phone or emulator, as the emulator test does.
- Kalorické Tabulky: a Kotlin app whose own classes keep their names (only its libraries are renamed), so
  patches find code by class and method name; a changed app fails loudly instead of being patched halfway.
  It isn't on APKMirror: `python .github/scripts/check_kaloricke_tabulky.py <app.apks> --cli <morphe-cli.jar>
  --install --expect-screen` patches the split APKs Google Play installs (zipped into an .apks) and starts the
  app, as the emulator test does.
- Speedtest (Ookla): an R8'd app whose own packages keep their names (classes and methods are renamed), so
  patches hook the named managers (the ads, Appvestor, CellRebel and review managers) and fail loudly when one
  is gone. It ships as a single arm64 APK on APKMirror:
  `python .github/scripts/check_speedtest.py <speedtest.apk> --cli <morphe-cli.jar> --install --expect-screen`
  patches it with every Speedtest patch and starts it on a phone or emulator, as the emulator test does.

Please follow the [code of conduct](CODE_OF_CONDUCT.md), and report security problems privately as described in
[SECURITY.md](SECURITY.md).

## Where things are

- `patches/src/main/kotlin/app/jofslash/patches/<app>/`: the patches. `<app>` is `quora`, `booking`,
  `googlehealth`, `kaloricketabulky` or `speedtest`.
- `extensions/<app>/`: the code those patches call inside the app. Each app has its own extension, so a patched
  app gets only its own code. A patch loads it with `extendWith("extensions/<app>.mpe")`.
- `docs/<app>/README.md`: the app's page. The patch tables in it are generated from the patches' descriptions
  at release (`.github/scripts/generate_<app>_readme.py`).

## Building

Requires JDK 21 and a GitHub token with `read:packages`:

```bash
GITHUB_ACTOR=<you> GITHUB_TOKEN=<token> ./gradlew :patches:buildAndroid
```

The bundle, with every app's patches, is written to `patches/build/libs/patches-*.mpp`.

## Pull requests

Work on a branch from `dev` and open the pull request against `dev`: the weekly pre-release is made from it, and
the stable release from main. Give the pull request a [Conventional Commit](https://www.conventionalcommits.org/)
title (`feat:`, `fix:`, `docs:` ...), as the release notes are made from it.
