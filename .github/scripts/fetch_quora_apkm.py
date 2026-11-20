"""Downloads a Quora APKM from APKMirror and checks that it is Quora's own.

It follows APKMirror's pages from the release in the feed to the file, with a pause between
requests and a few tries, as one person would. It is meant to run once for each new Quora
version, not more. Before the file is used it checks that base.apk is com.quora.android at the
asked version and signed with Quora's certificate (the SHA-1 in Constants.kt).

Usage:
    python .github/scripts/fetch_quora_apkm.py <version> <out.apkm> [--apksigner <path>] [--aapt2 <path>]
    python .github/scripts/fetch_quora_apkm.py --verify-only <file.apkm> <version> [--apksigner ...] [--aapt2 ...]

Exit codes: 0 done, 4 APKMirror turned the download away (for example Cloudflare), 5 the version
isn't on APKMirror, 6 the file isn't Quora's (wrong package, version or signature).
"""
import argparse
import pathlib
import re
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
CONSTANTS = ROOT / "patches/src/main/kotlin/app/jofslash/patches/quora/shared/Constants.kt"
BASE = "https://www.apkmirror.com"
FEED = BASE + "/apk/quora-inc/quora/feed/"
PACKAGE = "com.quora.android"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.9",
}
PAUSE = 4  # seconds between requests


class Blocked(Exception):
    pass


def get(url, referer=None, binary=False):
    headers = dict(HEADERS)
    if referer:
        headers["Referer"] = referer
    last = None
    for attempt in range(3):
        if attempt:
            time.sleep(PAUSE * 4 * attempt)
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=60) as response:
                data = response.read()
                kind = response.headers.get("Content-Type", "")
        except urllib.error.HTTPError as ex:
            last = f"HTTP {ex.code}"
            if ex.code in (403, 429, 503):
                continue
            raise
        except urllib.error.URLError as ex:
            last = str(ex.reason)
            continue
        time.sleep(PAUSE)
        if binary:
            if "html" in kind:
                raise Blocked(f"got a page instead of the file at {url}")
            return data
        text = data.decode("utf8", errors="replace")
        # Normal pages also load Cloudflare's script; only its challenge page has this title.
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
        print(f"Quora {version} isn't on APKMirror.")
        sys.exit(5)
    page = get(release)
    variant = BASE + first(r'href="(/apk/quora-inc/quora/[^"]*-android-apk-download/)"', page, "the download variant")
    page = get(variant, release)
    keyed = BASE + first(r'href="(/apk/[^"]*/download/\?key=[^"]+)"', page, "the download button")
    page = get(keyed, variant)
    file_url = BASE + first(r'href="(/wp-content/themes/APKMirror/download\.php\?[^"]+)"', page, "the file link")
    data = get(file_url, keyed, binary=True)
    out.write_bytes(data)
    print(f"Downloaded {len(data)} bytes to {out}.")


def quora_signature():
    match = re.search(r'QUORA_SIGNATURE = "([0-9a-f]{40})"', CONSTANTS.read_text(encoding="utf8"))
    return match.group(1)


def verify(apkm, version, apksigner, aapt2):
    """Exits with 6 unless base.apk is Quora at this version, signed with Quora's certificate."""
    with tempfile.TemporaryDirectory() as work:
        try:
            with zipfile.ZipFile(apkm) as archive:
                archive.extract("base.apk", work)
        except (zipfile.BadZipFile, KeyError) as ex:
            print(f"Not an APKM with a base.apk: {ex}")
            sys.exit(6)
        base = str(pathlib.Path(work) / "base.apk")
        badging = subprocess.run([aapt2, "dump", "badging", base], capture_output=True, text=True).stdout
        match = re.search(r"package: name='([^']+)' versionCode='\d+' versionName='([^']+)'", badging)
        certs = subprocess.run([apksigner, "verify", "--print-certs", base], capture_output=True, text=True)
    found_package, found_version = match.groups() if match else ("?", "?")
    digests = re.findall(r"Signer #1 certificate SHA-1 digest: ([0-9a-f]+)", certs.stdout)
    problems = []
    if found_package != PACKAGE:
        problems.append(f"package is {found_package}")
    if found_version != version:
        problems.append(f"version is {found_version}, not {version}")
    if certs.returncode != 0:
        problems.append("signature doesn't verify: " + (certs.stderr or certs.stdout).strip()[:200])
    elif digests != [quora_signature()]:
        problems.append(f"signed by {digests}, not Quora")
    if problems:
        print("This isn't Quora's APK: " + "; ".join(problems))
        sys.exit(6)
    print(f"Checked: {PACKAGE} {version}, signed by Quora ({digests[0]}).")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("first")
    parser.add_argument("second")
    parser.add_argument("--verify-only", action="store_true", help="Only check an APKM you already have")
    parser.add_argument("--apksigner", default="apksigner")
    parser.add_argument("--aapt2", default="aapt2")
    args = parser.parse_args()
    if args.verify_only:
        verify(pathlib.Path(args.first), args.second, args.apksigner, args.aapt2)
        return
    version, out = args.first, pathlib.Path(args.second)
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        sys.exit("The version looks like 3.2.47.")
    try:
        download(version, out)
    except Blocked as ex:
        print(f"APKMirror turned the download away: {ex}")
        sys.exit(4)
    verify(out, version, args.apksigner, args.aapt2)


if __name__ == "__main__":
    main()
