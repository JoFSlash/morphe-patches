"""Downloads an IMDb APK from APKPure and checks that it is IMDb's own.

IMDb ships as a single universal APK (not a bundle). APKPure serves it from a direct endpoint
(d.apkpure.com/b/APK/com.imdb.mobile?version=latest) that only hands out the newest version;
APKMirror's pages are a Cloudflare 403 to automation. The download is therefore always the latest
APKPure build, and it is used only when its version matches the one asked for. Before the file is
used it checks that it is com.imdb.mobile at the asked version and signed only with IMDb's
certificate (the SHA-1 in Constants.kt).

Usage:
    python .github/scripts/fetch_imdb_apk.py <version> <out.apk> [--apksigner <path>] [--aapt2 <path>]
    python .github/scripts/fetch_imdb_apk.py --verify-only <file.apk> <version> [--apksigner ...] [--aapt2 ...]

Exit codes: 0 done, 4 APKPure or the network turned the download away (for example a block or a
timeout), 5 APKPure's latest isn't the asked version, 6 the file isn't the app's (wrong package,
version or signature), 7 apksigner or aapt2 couldn't run.
"""
import argparse
import base64
import hashlib
import http.client
import pathlib
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
CONSTANTS = ROOT / "patches/src/main/kotlin/app/jofslash/patches/imdb/shared/Constants.kt"
PACKAGE = "com.imdb.mobile"
ENDPOINT = "https://d.apkpure.com/b/APK/com.imdb.mobile?version=latest"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36",
    "Accept": "application/vnd.android.package-archive,application/octet-stream,*/*",
    "Accept-Language": "en-US,en;q=0.9",
}
PAUSE = 4  # seconds between tries


class Blocked(Exception):
    """APKPure or the network turned the request away."""


class NotFound(Exception):
    """APKPure's latest build isn't the asked version."""


def open_endpoint():
    """Opens the APKPure endpoint, following its redirect to the file. Returns (response, filename)."""
    last = None
    for attempt in range(3):
        if attempt:
            time.sleep(PAUSE * 4 * attempt)
        try:
            response = urllib.request.urlopen(urllib.request.Request(ENDPOINT, headers=HEADERS), timeout=120)
        except urllib.error.HTTPError as ex:
            last = f"HTTP {ex.code}"
            if ex.code in (404, 410):
                raise NotFound(f"{ENDPOINT}: {last}")
            continue
        except (OSError, http.client.HTTPException) as ex:
            last = f"{type(ex).__name__}: {ex}"
            continue
        kind = response.headers.get("Content-Type", "")
        if "html" in kind:
            last = "got a page instead of the file"
            response.close()
            continue
        disposition = response.headers.get("Content-Disposition", "")
        return response, urllib.parse.unquote(disposition) + " " + response.geturl()
    raise Blocked(f"{ENDPOINT}: {last}")


def endpoint_version():
    """The version APKPure's latest endpoint would serve, read from the redirect without downloading."""
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, *args):
            return None

    opener = urllib.request.build_opener(NoRedirect)
    request = urllib.request.Request(ENDPOINT, headers=HEADERS, method="HEAD")
    try:
        opener.open(request, timeout=60)
        return None
    except urllib.error.HTTPError as ex:
        if ex.code in (301, 302, 303, 307, 308):
            where = urllib.parse.unquote(ex.headers.get("Location", "") + " " + ex.headers.get("Content-Disposition", ""))
            match = re.search(r"(\d+(?:\.\d+){2,})", where)
            return match.group(1) if match else None
        raise Blocked(f"{ENDPOINT}: HTTP {ex.code}")
    except (OSError, http.client.HTTPException) as ex:
        raise Blocked(f"{ENDPOINT}: {type(ex).__name__}: {ex}")


def download(version, out):
    latest = None
    try:
        latest = endpoint_version()
    except Blocked:
        pass  # fall through to the full download, which also carries the version
    if latest and latest != version:
        raise NotFound(f"APKPure's latest IMDb is {latest}, not {version}")
    response, where = open_endpoint()
    match = re.search(r"(\d+(?:\.\d+){2,})", where)
    served = match.group(1) if match else None
    if served and served != version:
        response.close()
        raise NotFound(f"APKPure's latest IMDb is {served}, not {version}")
    with response:
        data = response.read()
    if len(data) < 1_000_000 or data[:2] != b"PK":
        raise Blocked(f"the download wasn't an APK ({len(data)} bytes)")
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
    """Exits with 6 unless the APK is IMDb at this version and signed with IMDb's certificate
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
        problems.append(f"signed by {digests}, not IMDb")
        print(f"{apksigner} output (exit {certs.returncode}):\n{certs.stdout.strip()[:1500]}\n{certs.stderr.strip()[:1500]}")
    if problems:
        print("This isn't IMDb's APK: " + "; ".join(problems))
        sys.exit(6)
    print(f"Checked: {PACKAGE} {version}, signed by IMDb ({', '.join(sorted(expected))}).")


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
        sys.exit("The version looks like 9.3.7.109370600.")
    try:
        download(version, out)
    except NotFound as ex:
        print(f"Not on APKPure: {ex}")
        sys.exit(5)
    except Blocked as ex:
        print(f"APKPure or the network turned the download away: {ex}")
        sys.exit(4)
    verify(out, version, args.apksigner, args.aapt2)


if __name__ == "__main__":
    main()
