"""Looks for a Kalorické Tabulky version newer than the ones this bundle supports.

The app isn't on APKMirror, so there is no feed to read. This reads the version on the app's Google
Play page (the version Google Play installs now), and when Google Play can't be read, the version
APKPure lists. Both only say that a new version is out; the APK itself is added to
JoFSlash/morphe-apks by hand (see .github/workflows/kaloricke-tabulky-version.yml). Versions look
like "3.14.71" and are compared number by number.

Prints a short report. With --github-output <file> it also writes, for a GitHub workflow:
  new=<newest version not listed yet, or empty>
  supported=<listed versions, comma separated>
  source=<where the version was read: Google Play or APKPure>

Usage: python .github/scripts/watch_kaloricke_tabulky_version.py [--github-output <file>]
Exits 0 when it could read a version, 2 when it couldn't.
"""
import argparse
import pathlib
import re
import sys
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
CONSTANTS = ROOT / "patches/src/main/kotlin/app/jofslash/patches/kaloricketabulky/shared/Constants.kt"
PACKAGE = "cz.psc.android.kaloricketabulky"
PLAY = f"https://play.google.com/store/apps/details?id={PACKAGE}&hl=en&gl=CZ"
APKPURE = f"https://apkpure.com/kalorick%C3%A9-tabulky/{PACKAGE}"
USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"
VERSION = re.compile(r"\d+(?:\.\d+){2,}")


def key(version):
    return tuple(int(part) for part in version.split(".")) if VERSION.fullmatch(version) else (0,)


def fetch(url):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept-Language": "en"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf8", errors="replace")


def play_version():
    """The version in the data of the Google Play page. It sits in a nested list of its own,
    [[["3.14.71"]], ...], which nothing else on the page looks like."""
    found = re.findall(r'\[\[\["(\d+(?:\.\d+){2,})"\]\]', fetch(PLAY))
    return max(found, key=key) if found else None


def apkpure_version():
    """The version in the description of APKPure's page ("Kalorické Tabulky 3.14.71 APK download").
    Other version numbers on the page are APKPure's own app's."""
    found = re.findall(r'<meta name="description" content="[^"]*?(\d+(?:\.\d+){2,}) APK download', fetch(APKPURE))
    return found[0] if found else None


def listed_versions():
    return re.findall(r'AppTarget\(version = "([^"]+)"', CONSTANTS.read_text(encoding="utf8"))


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--github-output", help="File to append workflow outputs to")
    args = parser.parse_args()

    supported = listed_versions()
    newest_supported = max((key(v) for v in supported), default=(0,))
    version, source = None, None
    for name, read in (("Google Play", play_version), ("APKPure", apkpure_version)):
        try:
            version = read()
        except Exception as ex:  # Try the next source; the workflow reports when none works.
            print(f"Could not read {name}: {ex}")
            continue
        if version:
            source = name
            break
        print(f"No version found on {name}.")
    if not version:
        sys.exit(2)

    new = version if key(version) > newest_supported else ""
    print(f"Supported: {', '.join(supported) or 'none'}")
    print(f"{source}: {version}")
    print(f"New: {new}" if new else "Nothing newer than the supported versions.")

    if args.github_output:
        with open(args.github_output, "a", encoding="utf8") as out:
            out.write(f"new={new}\n")
            out.write(f"supported={', '.join(supported)}\n")
            out.write(f"source={source}\n")


if __name__ == "__main__":
    main()
