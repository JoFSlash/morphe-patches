# Contributing

Thanks for helping. Google Health is an obfuscated app: most class and method names change with every
version, so patches find their code by strings, framework calls or the few names that stay.

## Reporting a problem

Open a [bug report](https://github.com/JoFSlash/google-health-patches/issues/new?template=bug_report.yml) with:

- what you expected and what happened, and on which screen,
- the versions of Google Health, the patches, Morphe Manager, MicroG-RE and Android,
- for a crash, the output of `adb logcat | grep -E "AndroidRuntime|JoFSlashPatches"`.

Please don't post health data, account names or tokens from your logs.

## How the patches work

- `patches/src/main/kotlin/app/jofslash/patches/googlehealth/` holds the patches. Each one finds code in the
  app with a fingerprint, or changes every call to a framework method (`shared/InvokeReplace.kt`), and
  inserts a call or a few instructions.
- `extensions/extension/src/main/java/app/jofslash/extension/googlehealth/` holds the code those calls run
  inside the app.
- **GmsCore support** moves the app from Google Play services to MicroG-RE. MicroG-RE only lets an app use the
  Google account when it knows the app as a Google app: it knows Google Health by its package name and Fitbit's
  original signing certificate (`Constants.SIGNATURE`), not by the newer rotated one. That certificate is what
  the patch tells MicroG-RE to report.

## Testing a change

Build the bundle and apply it to an APKM from APKMirror:

```
./gradlew buildAndroid
python .github/scripts/check_app.py google-health.apkm --cli morphe-cli.jar --install
```

`check_app.py` applies every patch, lists the ones that fail, installs the patched app on the phone or
emulator connected over adb and checks that it starts without errors from the patches. Then try the change
in the app itself, signed in through MicroG-RE.

### A new Google Health version

Run `check_app.py` on the new APKM. When every patch applies and the app starts, add the version to
`AppTarget` in `patches/src/main/kotlin/app/jofslash/patches/googlehealth/shared/Constants.kt` (newest
first) and upload the APKM to the private repository `JoFSlash/google-health-apks` as a release named
`google-health-<version>`, so the emulator test can use it.

## Continuous integration

- **Build** (pull requests): builds the bundle and checks the scripts.
- **Emulator test** (pull requests and before each release): applies every patch to the newest supported
  version and starts the app on Android 15, 16 and 17 emulators. It needs the `GOOGLE_HEALTH_APKS_TOKEN`
  secret (a fine-grained token that can read `JoFSlash/google-health-apks`); without it, pull requests
  skip the test.
- **Release** (Mondays 08:00 UTC, or by hand): runs the emulator test, then semantic-release publishes the
  bundle when there are `feat:` or `fix:` commits since the last release.

## Pull requests

- Use a [Conventional Commit](https://www.conventionalcommits.org/) title (`feat:`, `fix:`, `docs:` ...);
  the release notes are made from it.
- Describe what you tested and on which Google Health version.
- New patches are off by default unless they only stop something without changing what you see.
