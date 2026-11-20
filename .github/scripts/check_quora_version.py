"""Checks a new Quora version against this bundle in one go.

It applies every patch to the APK or APKM with the Morphe CLI, lists the patches that no longer apply
and why, and says whether the version is already listed as supported. With --install it also installs
the patched app on a phone over adb, starts it and looks for a crash at start.

Usage:
    python .github/scripts/check_quora_version.py <quora.apkm> --cli <morphe-cli.jar> [--mpp <bundle.mpp>]
        [--install [--expect-login]] [--adb <adb>]

With --install, Clone app is left out (it would install under another package name). The emulator
test in .github/workflows/emulator.yml runs this with --install --expect-login.

Build the bundle first (./gradlew buildAndroid); by default the newest .mpp in patches/build/libs is used.
"""
import argparse
import json
import pathlib
import re
import subprocess
import sys
import tempfile
import time

ROOT = pathlib.Path(__file__).resolve().parents[2]
CONSTANTS = ROOT / "patches/src/main/kotlin/app/jofslash/patches/quora/shared/Constants.kt"
PACKAGE = "com.quora.android"


def newest_mpp():
    files = [f for f in (ROOT / "patches/build/libs").glob("*.mpp")
             if "javadoc" not in f.name and "sources" not in f.name]
    if not files:
        sys.exit("No .mpp in patches/build/libs. Run ./gradlew buildAndroid first, or pass --mpp.")
    return max(files, key=lambda f: f.stat().st_mtime)


def patch_names(cli=None, mpp=None):
    """The patches in the bundle itself; patches-list.json is only updated by a release."""
    if cli and mpp:
        run = subprocess.run(["java", "-jar", cli, "list-patches", f"--patches={mpp}"],
                             capture_output=True, text=True, encoding="utf8", errors="replace")
        names = re.findall(r"^Name: (.+)$", run.stdout, re.M)
        if names:
            return [n.strip() for n in names]
    return [p["name"] for p in json.loads((ROOT / "patches-list.json").read_text(encoding="utf8"))["patches"]]


def listed_versions():
    return re.findall(r'AppTarget\(version = "([^"]+)"', CONSTANTS.read_text(encoding="utf8"))


def describe(failure):
    if isinstance(failure, str):
        return failure, ""
    name = failure.get("name") or failure.get("patch") or "?"
    error = failure.get("error") or failure.get("message") or json.dumps(failure)
    return name, str(error).strip().splitlines()[0][:300]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("apk", help="Quora APK or APKM to check")
    parser.add_argument("--cli", required=True, help="Morphe CLI or Morphe Desktop jar")
    parser.add_argument("--mpp", help="Bundle to check (default: newest in patches/build/libs)")
    parser.add_argument("--install", action="store_true", help="Install on a phone and look for a crash at start")
    parser.add_argument("--adb", default="adb", help="Path to adb")
    parser.add_argument("--expect-login", action="store_true",
                        help="With --install: also require Quora's sign-in page on screen (a fresh install)")
    parser.add_argument("--exclude", action="append", default=[], metavar="PATCH",
                        help="Leave this patch out (can be given more than once)")
    args = parser.parse_args()

    mpp = pathlib.Path(args.mpp) if args.mpp else newest_mpp()
    work = pathlib.Path(tempfile.mkdtemp(prefix="quora-check-"))
    result_file, out_apk = work / "result.json", work / "patched.apk"

    names = patch_names(args.cli, mpp)
    if args.install:
        # Clone app would install under another package name than the one started below.
        names = [n for n in names if n != "Clone app"]
    unknown = [n for n in args.exclude if n not in names]
    if unknown:
        sys.exit("Not in the bundle: " + ", ".join(unknown))
    names = [n for n in names if n not in args.exclude]
    command = ["java", "-jar", args.cli, "patch", f"-p={mpp}"]
    command += [f"-e={name}" for name in names]
    # -f: also try versions that are not listed yet; keep going so every failure shows up.
    command += ["-f", "--continue-on-error"]
    # Only an installed APK needs a signature.
    if not args.install:
        command.append("--unsigned")
    command += [f"-r={result_file}", f"-o={out_apk}", f"-t={work / 'tmp'}", args.apk]
    print(f"Patching {pathlib.Path(args.apk).name} with {mpp.name} ...")
    run = subprocess.run(command, capture_output=True, text=True, encoding="utf8", errors="replace")
    (work / "patch.log").write_text(run.stdout + run.stderr, encoding="utf8")

    if not result_file.exists():
        print(run.stdout[-3000:] + run.stderr[-3000:])
        sys.exit(f"Patching did not finish. Full log: {work / 'patch.log'}")

    result = json.loads(result_file.read_text(encoding="utf8"))
    if result.get("packageName") != PACKAGE:
        sys.exit(f"\nThis is {result.get('packageName')}, not Quora ({PACKAGE}).")
    version = result.get("packageVersion", "?")
    applied = result.get("appliedPatches", [])
    failed = [describe(f) for f in result.get("failedPatches", [])]
    # Patches that neither applied nor failed, for example because they were skipped.
    skipped = [n for n in names if n not in applied and n not in [f[0] for f in failed]]
    failed += [(n, "not applied") for n in skipped]
    listed = listed_versions()

    print(f"\nQuora {version}: {len(applied)} of {len(names)} patches applied.")
    for name, error in failed:
        print(f"  FAILED  {name}: {error}")
    for step in result.get("stepResults", []):
        if not step.get("success", True):
            print(f"  Step {step.get('step')} failed: {step.get('error', '')}")
    if version in listed:
        print(f"{version} is already listed as supported in Constants.kt.")
    elif not failed:
        print(f"Every patch applies. Add AppTarget(version = \"{version}\") to Constants.kt, "
              "then try the page patches on a phone: Quora may also have changed its pages.")
    print(f"Log and patched APK: {work}")

    if args.install and out_apk.exists():
        print("\nInstalling on the phone ...")
        adb = args.adb
        install = subprocess.run([adb, "install", "-r", str(out_apk)], capture_output=True, text=True)
        output = (install.stdout + install.stderr).strip()
        if "Success" not in output:
            # For example an older version over a newer one, or a different signature.
            sys.exit("Install failed: " + output.splitlines()[-1] if output else "Install failed.")
        subprocess.run([adb, "logcat", "-c"])
        subprocess.run([adb, "shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1"],
                       capture_output=True)
        time.sleep(15)
        log = subprocess.run([adb, "logcat", "-d"], capture_output=True, text=True, errors="replace").stdout
        crashes = [line for line in log.splitlines()
                   if re.search(r"FATAL EXCEPTION|VerifyError|Verifier rejected", line)]
        pid = subprocess.run([adb, "shell", "pidof", PACKAGE], capture_output=True, text=True).stdout.strip()
        if crashes or not pid:
            print("The app crashed at start:" if crashes else "The app is not running after 15 seconds.")
            for line in crashes[:10]:
                print("  " + line)
            sys.exit(1)
        print("The app started and is running.")
        if args.expect_login and not login_page_shown(adb):
            sys.exit("Quora's sign-in page didn't show within a minute.")
        if args.expect_login:
            print("Quora's sign-in page loaded.")
        problems = patch_errors(adb, pid)
        if problems:
            print("Errors from the patches in the log:")
            for line in problems[:20]:
                print("  " + line)
            sys.exit(1)
        print("No errors from the patches in the log.")
        print("Check the feed with the page patches by hand.")

    sys.exit(1 if failed else 0)


def patch_errors(adb, pid):
    """Error lines our extension logged, and missing classes or methods in the app's process.

    The extension catches its own exceptions and logs them as E/JoFSlashPatches, so a patch that
    stops working on some Android version shows up here and not as a crash. A class or method that
    an older Android doesn't have shows up as NoSuchMethodError or NoClassDefFoundError.
    """
    log = subprocess.run([adb, "logcat", "-d", "-v", "brief"], capture_output=True, text=True,
                         errors="replace").stdout
    pids = set(pid.split())
    lines = log.splitlines()
    problems = []
    for i, line in enumerate(lines):
        if line.startswith("E/JoFSlashPatches"):
            problems.append(line)
            continue
        m = re.match(r"^[A-Z]/[^(]*\(\s*(\d+)\)", line)
        if not m or m.group(1) not in pids:
            continue
        if not re.search(r"NoSuchMethodError|NoClassDefFoundError|NoSuchFieldError|AbstractMethodError", line):
            continue
        # Only when the stack trace that follows goes through our code.
        if any("app.jofslash" in later for later in lines[i:i + 25]):
            problems.append(line)
    return problems


def login_page_shown(adb, timeout=60):
    """Whether Quora's sign-in page is on screen. The cookie dialog in the EU covers it at first."""
    end = time.time() + timeout
    while time.time() < end:
        subprocess.run([adb, "shell", "uiautomator", "dump", "/sdcard/window.xml"], capture_output=True)
        screen = subprocess.run([adb, "shell", "cat", "/sdcard/window.xml"],
                                capture_output=True, text=True, errors="replace").stdout
        if re.search(r'text="(Continue with Google|Sign up with email|Login|Reject All|I Accept)"', screen):
            return True
        time.sleep(5)
    return False


if __name__ == "__main__":
    main()
