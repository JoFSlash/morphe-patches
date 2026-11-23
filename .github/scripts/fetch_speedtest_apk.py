"""Downloads a Speedtest by Ookla APK from APKMirror and checks that it is Ookla's own.

It follows APKMirror's pages from the release in the feed to the file, with a pause between
requests and a few tries, as one person would. Speedtest ships as a single (arm64-v8a) APK, not a
bundle. Before the file is used it checks that it is org.zwanoo.android.speedtest at the asked
version and signed only with Ookla's certificate (the SHA-1 in Constants.kt).

Usage:
    python .github/scripts/fetch_speedtest_apk.py <version> <out.apk> [--apksigner <path>] [--aapt2 <path>]
    python .github/scripts/fetch_speedtest_apk.py --verify-only <file.apk> <version> [--apksigner ...] [--aapt2 ...]

Exit codes: 0 done, 4 APKMirror or the network turned the download away (for example Cloudflare
or a timeout), 5 there is no APK of the version on APKMirror, 6 the file isn't the app's (wrong
package, version or signature), 7 apksigner or aapt2 couldn't run.
"""
import argparse
import base64
import hashlib
import http.client
import http.cookiejar
import pathlib
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
CONSTANTS = ROOT / "patches/src/main/kotlin/app/jofslash/patches/speedtest/shared/Constants.kt"
BASE = "https://www.apkmirror.com"
FEED = BASE + "/apk/ookla/speedtest/feed/"
PACKAGE = "org.zwanoo.android.speedtest"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.9",
}
PAUSE = 4  # seconds between requests
# A cookie jar carries APKMirror's session cookie through the pages to the file; download.php can
# answer 403 without it.
OPENER = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))


class Blocked(Exception):
    """APKMirror or the network turned a request away."""


class NotFound(Exception):
    """APKMirror has no APK of the version."""


def get(url, referer=None, binary=False):
    headers = dict(HEADERS)
    if referer:
        headers["Referer"] = referer
    last = None
    for attempt in range(3):
        if attempt:
            time.sleep(PAUSE * 4 * attempt)
        try:
            with OPENER.open(urllib.request.Request(url, headers=headers), timeout=60) as response:
                data = response.read()
                kind = response.headers.get("Content-Type", "")
        except urllib.error.HTTPError as ex:
            last = f"HTTP {ex.code}"
            if ex.code in (404, 410):
                raise NotFound(f"{url}: {last}")
            continue
        except (OSError, http.client.HTTPException) as ex:
            last = f"{type(ex).__name__}: {ex}"
            continue
        time.sleep(PAUSE)
        if binary:
            if "html" in kind:
                raise Blocked(f"got a page instead of the file at {url}")
            return data
        text = data.decode("utf8", errors="replace")
        if re.search(r"<title>Just a moment", text):
            last = "Cloudflare challenge"
            continue
        return text
    raise Blocked(f"{url}: {last}")


def release_page(version):
    feed = get(FEED)
    for item in re.findall(r"<item>(.*?)</item>", feed, re.S):
        title = re.search(r"<title>([^<]*)</title>", item)
        link = re.search(r"<link>([^<]*)</link>", item)
        if title and link and re.search(rf"\b{re.escape(version)}\b", title.group(1)) \
                and not re.search(r"\b(beta|alpha)\b", title.group(1), re.I):
            return link.group(1).strip()
    return None


def first(pattern, text, what):
    match = re.search(pattern, text)
    if not match:
        raise Blocked(f"couldn't find {what}; APKMirror's page may have changed")
    return match.group(1).replace("&amp;", "&")


def download(version, out):
    release = release_page(version)
    if not release:
        raise NotFound(f"Speedtest {version} isn't in APKMirror's feed")
    page = get(release)
    variant = BASE + first(r'href="(/apk/ookla/speedtest/[^"]*-android-apk-download/)"', page,
                           "the download page")
    page = get(variant, release)
    keyed = BASE + first(r'href="(/apk/[^"]*/download/\?key=[^"]+)"', page, "the download button")
    page = get(keyed, variant)
    file_url = BASE + first(r'href="(/wp-content/themes/APKMirror/download\.php\?[^"]+)"', page, "the file link")
    data = get(file_url, keyed, binary=True)
    out.write_bytes(data)
    print(f"Downloaded {len(data)} bytes to {out}.")


def app_signatures():
    match = re.search(r'SIGNATURE = "([0-9a-f]{40})"', CONSTANTS.read_text(encoding="utf8"))
    return {match.group(1)}


def run_tool(command):
    """Runs apksigner or aapt2. Exits with 7 when the tool itself can't run, so a missing tool
    isn't mistaken for a file that isn't the app's."""
    try:
        return subprocess.run(command, capture_output=True, text=True)
    except OSError as ex:
        print(f"Couldn't run {command[0]}: {ex}")
        sys.exit(7)


def signer_digests(output):
    """SHA-1 digests of the signer certificates in `apksigner verify --print-certs-pem` output.
    The source stamp certificate (Google's, added by Play) is left out; every other signer counts."""
    digests, label, pem = [], "", None
    for line in output.splitlines():
        line = line.strip()
        match = re.match(r"(.+?):? certificate DN:", line)
        if match:
            label = match.group(1)
        elif line == "-----BEGIN CERTIFICATE-----":
            pem = []
        elif line == "-----END CERTIFICATE-----" and pem is not None:
            if "Signer" in label and not label.startswith("Source Stamp"):
                digests.append(hashlib.sha1(base64.b64decode("".join(pem))).hexdigest())
            pem = None
        elif pem is not None:
            pem.append(line)
    return digests


def verify(apk, version, apksigner, aapt2):
    """Exits with 6 unless the APK is Speedtest at this version and signed with Ookla's certificate
    and no others."""
    expected = app_signatures()
    problems = []
    apk = str(apk)
    badging = run_tool([aapt2, "dump", "badging", apk]).stdout
    match = re.search(r"package: name='([^']+)' versionCode='\d+' versionName='([^']+)'", badging)
    found_package, found_version = match.groups() if match else ("?", "?")
    if found_package != PACKAGE:
        problems.append(f"package is {found_package}")
    if found_version != version:
        problems.append(f"version is {found_version}, not {version}")
    certs = run_tool([apksigner, "verify", "--print-certs-pem", apk])
    digests = signer_digests(certs.stdout + "\n" + certs.stderr)
    if certs.returncode != 0:
        problems.append("signature doesn't verify: " + (certs.stderr or certs.stdout).strip()[:200])
    elif not digests or not set(digests) <= expected:
        problems.append(f"signed by {digests}, not Ookla")
        print(f"{apksigner} output (exit {certs.returncode}):\n{certs.stdout.strip()[:1500]}\n{certs.stderr.strip()[:1500]}")
    if problems:
        print("This isn't Speedtest's APK: " + "; ".join(problems))
        sys.exit(6)
    print(f"Checked: {PACKAGE} {version}, signed by Ookla ({', '.join(sorted(expected))}).")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("first")
    parser.add_argument("second")
    parser.add_argument("--verify-only", action="store_true", help="Only check an APK you already have")
    parser.add_argument("--apksigner", default="apksigner")
    parser.add_argument("--aapt2", default="aapt2")
    args = parser.parse_args()
    if args.verify_only:
        verify(pathlib.Path(args.first), args.second, args.apksigner, args.aapt2)
        return
    version, out = args.first, pathlib.Path(args.second)
    if not re.fullmatch(r"\d+(\.\d+)+", version):
        sys.exit("The version looks like 7.1.5.")
    try:
        download(version, out)
    except NotFound as ex:
        print(f"Not on APKMirror: {ex}")
        sys.exit(5)
    except Blocked as ex:
        print(f"APKMirror or the network turned the download away: {ex}")
        sys.exit(4)
    verify(out, version, args.apksigner, args.aapt2)


if __name__ == "__main__":
    main()
