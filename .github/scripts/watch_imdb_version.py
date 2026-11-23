"""Looks for an IMDb version newer than the ones this bundle supports.

Reads the version APKPure's direct endpoint serves as the latest (where users get the APK to patch)
and compares it with the versions listed in Constants.kt. APKMirror's pages are a Cloudflare 403 to
automation, so APKPure is the source. IMDb's version names look like "9.3.7.109370600" and are
compared number by number. APKPure's endpoint only ever hands out the latest build, so this sees
only the newest version, not a history.

Prints a short report. With --github-output <file> it also writes, for a GitHub workflow:
  new=<the APKPure latest version if it isn't listed yet, else empty>
  supported=<listed versions, comma separated>

Usage: python .github/scripts/watch_imdb_version.py [--github-output <file>]
Exits 0 when it could read APKPure, 2 when it couldn't.
"""
import argparse
import http.client
import pathlib
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
CONSTANTS = ROOT / "patches/src/main/kotlin/app/jofslash/patches/imdb/shared/Constants.kt"
ENDPOINT = "https://d.apkpure.com/b/APK/com.imdb.mobile?version=latest"
USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"
VERSION = re.compile(r"\b(\d+(?:\.\d+)+)\b")


def key(version):
    return tuple(int(part) for part in version.split(".")) if VERSION.fullmatch(version) else (0,)


def apkpure_latest():
    """The version APKPure's latest endpoint serves, read from the redirect without downloading the
    APK. Both the Location URL and the Content-Disposition filename carry the version name."""
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, *args):
            return None

    opener = urllib.request.build_opener(NoRedirect)
    request = urllib.request.Request(ENDPOINT, headers={"User-Agent": USER_AGENT}, method="HEAD")
    try:
        opener.open(request, timeout=60)
        raise RuntimeError("the endpoint didn't redirect to a file")
    except urllib.error.HTTPError as ex:
        if ex.code not in (301, 302, 303, 307, 308):
            raise
        where = urllib.parse.unquote(ex.headers.get("Location", "") + " " + ex.headers.get("Content-Disposition", ""))
    # The longest dotted number is the versionName (e.g. 9.3.7.109370600), not a short date.
    candidates = sorted(set(re.findall(r"\d+(?:\.\d+){2,}", where)), key=lambda v: len(v), reverse=True)
    if not candidates:
        raise RuntimeError(f"couldn't read the version from {where[:300]}")
    return candidates[0]


def listed_versions():
    return re.findall(r'AppTarget\(version = "([^"]+)"', CONSTANTS.read_text(encoding="utf8"))


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--github-output", help="File to append workflow outputs to")
    args = parser.parse_args()

    supported = listed_versions()
    newest_supported = max((key(v) for v in supported), default=(0,))
    try:
        latest = apkpure_latest()
    except (urllib.error.URLError, OSError, http.client.HTTPException, RuntimeError) as ex:
        print(f"Could not read APKPure: {ex}")
        sys.exit(2)

    is_new = key(latest) > newest_supported
    print(f"Supported: {', '.join(supported) or 'none'}")
    print(f"APKPure latest: {latest}")
    print(f"New on APKPure: {latest}" if is_new else "Nothing newer than the supported versions.")

    if args.github_output:
        with open(args.github_output, "a", encoding="utf8") as out:
            out.write(f"new={latest if is_new else ''}\n")
            out.write(f"supported={', '.join(supported)}\n")


if __name__ == "__main__":
    main()
