# Contributing

Thanks for helping. Each app is patched differently, so each has its own guide, `docs/<app>/CONTRIBUTING.md`,
next to its page (for example [Quora](docs/quora/CONTRIBUTING.md) and
[Google Health](docs/google-health/CONTRIBUTING.md)). It says how the app is patched and how to try the patches
on a phone or emulator with its `check_<app>.py` script, as the emulator test does.

Please follow the [code of conduct](CODE_OF_CONDUCT.md), and report security problems privately as described in
[SECURITY.md](SECURITY.md).

## Where things are

- `patches/src/main/kotlin/app/jofslash/patches/<app>/`: the patches. The folder of each app is in its
  settings, `.github/apps/<app>.json`.
- `extensions/<app>/`: the code those patches call inside the app. Each app has its own extension, so a patched
  app gets only its own code. A patch loads it with `extendWith("extensions/<app>.mpe")`.
- `docs/<app>/README.md`: the app's page. The patch tables in it are generated from the patches' descriptions
  at release (`.github/scripts/generate_<app>_readme.py`).

## Adding an app

An app brings its own files and changes none of the shared ones, so pull requests for different apps don't
get in each other's way:

- `.github/apps/<app>.json`: its name, Play Store link and one-line summary (for the README table and the
  issue forms), its owners (for the README's "Not affiliated with" line), where its APK is in `JoFSlash/morphe-apks`, the Android versions to test and the check
  command (and `test_inputs`, other files the check uses). An app is tested on the emulators after a
  merge into dev changes its code (`emulator-tests.yml`), and the release tests whatever wasn't
  (`.github/scripts/tested.py` keeps track). The release writes the README table, its owners line and the issue
  forms' app lists from these files (`.github/scripts/sync_apps.py`).
- `.github/workflows/emulator-<app>.yml`: runs `emulator-app.yml` for the app's own pull requests.
- `.github/scripts/check_<app>.py` and `generate_<app>_readme.py`, and the version workflow if it has one.
- `docs/<app>/README.md` and `docs/<app>/CONTRIBUTING.md`.

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
