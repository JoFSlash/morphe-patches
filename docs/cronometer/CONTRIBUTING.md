# Contributing to the Cronometer patches

Cronometer is a Flutter app: its own logic is compiled into `libapp.so`, which the patches don't touch. They work
on the Java side of the Flutter plugins and SDKs (google_mobile_ads, Adjust, Braze), whose classes keep their
names, and on the manifest switches Firebase reads; the R8'd Amplitude SDK is found through a text it logs. Each
patch fails loudly when what it hooks is gone.

Versions after 4.57.2 start through PairIP, so only 4.57.2 is supported. The app isn't on APKMirror; Google
Play ships split APKs, kept in JoFSlash/morphe-apks as an .apks (base.apk and the config splits zipped), and
APKPure's XAPK works too:
`python .github/scripts/check_app.py cronometer <app.apks> --cli <morphe-cli.jar> --install --expect-screen`
patches it with every Cronometer patch and starts it on a phone or emulator, as the emulator test does.

See the [general guide](../../CONTRIBUTING.md) for building, pull requests and where things are.
