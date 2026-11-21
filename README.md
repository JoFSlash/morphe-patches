# Booking.com patches for Morphe

[![Latest release](https://img.shields.io/github/v/release/JoFSlash/booking-patches?label=release)](https://github.com/JoFSlash/booking-patches/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/JoFSlash/booking-patches/total)](https://github.com/JoFSlash/booking-patches/releases)
[![License](https://img.shields.io/github/license/JoFSlash/booking-patches)](LICENSE)

Patches for the [Booking.com](https://play.google.com/store/apps/details?id=com.booking) Android app, applied with
[Morphe](https://morphe.software). They add a price comparison with the hotel's own website and a price history,
and they take out the pressure: scarcity messages, sponsored results, start-up promotions and tracking.

Works with Booking.com <!-- VERSIONS_START -->71.1<!-- VERSIONS_END --> on Android 8 or newer. No root needed.
All patches are on by default; you can switch any of them off when you patch.

> **Status: work in progress.** These patches are made for **browsing and comparing**, not for signing in or
> booking. Booking.com runs anti-fraud checks (ThreatMetrix, FingerprintJS) that these patches leave switched on,
> and a re-signed app has not been tested through sign-in or payment. Social sign-in (Google, Facebook) won't work
> in a patched app. **Use a throwaway account if you sign in at all, and book in the official app, on the website,
> or directly with the hotel.** See [Account and booking](#account-and-booking).

## Install

1. Install Morphe Manager from [morphe.software](https://morphe.software).
2. Add this repository as a patch source: open
   [this link](https://morphe.software/add-source?github=JoFSlash/booking-patches) on your phone, or add
   `https://github.com/JoFSlash/booking-patches` under sources in Morphe Manager.
3. Download a supported version of Booking.com as an APKM file from APKMirror:
   [71.1](https://www.apkmirror.com/apk/booking-com-hotel-reservations/booking-com-hotel-reservations/booking-com-hotels-travel-71-1-release/).
   Pick the file in Morphe Manager, then pick the patches you want.
4. Uninstall the Play Store version before installing the patched one. It is signed with a different key, so
   Android won't install it over the original. No root needed.

## Updating

- **New patches:** patch the same Booking.com file again and install it over the patched app. No need to uninstall.
- **New Booking.com version:** the patched app doesn't update from the Play Store. Wait until a release of these
  patches lists the new version as supported, then patch that version the same way. Booking.com puts out a new
  version most weeks; a patch set made for one version usually still applies to the next, but isn't guaranteed to.

## Patches

<!-- PATCHES_START -->
6 patches, all on by default. Supported Booking.com versions: 71.1.

<details open>
<summary><b>Price</b> (2)</summary>

Added features. They show you more; they don't change what you book.

| Patch | Default | What it does |
| --- | --- | --- |
| Compare prices | on | Adds a **Compare prices** button to a hotel's page. It opens one panel. The **Official site** tab loads Google Hotels for that hotel, your dates and guests inside the app, reads the price from the hotel's own website and puts it next to the Booking.com price, with a link to book there. Prices load for your own region and currency. When Google rates the price against the hotel's usual prices ("great deal, 32% less than usual"), that line shows too; Google rarely shows it in the EU, so a **Check Google's deal rating (US data)** link reads it from the US version of the page on request, without changing the prices shown. |
| Price history | on | Saves the prices the app shows you (at most 5000, the same price only once per 6 hours) and shows them for your dates as a chart, with the lowest, highest and current price and a check of crossed-out "original" prices. A second chart shows the hotel over the past year across all stay dates (the lowest price per night each day). **1M / 6M / 1Y / All** buttons pick the period. The history starts when the patch is installed; prices from before that can't be fetched, because no site publishes past hotel prices. |

</details>

<details>
<summary><b>Search</b> (2)</summary>

What the search results show.

| Patch | Default | What it does |
| --- | --- | --- |
| Hide urgency messages | on | Hides scarcity and pressure lines on hotel pages and in search results, such as "We have 1 left at this price", "Rare find", "booked N times" and "in high demand". Keeps the real information: the price, free cancellation and whether a hotel is sold out. |
| Hide sponsored listings | on | Removes sponsored ("Ad") properties from the results list and sponsored markers from the map, so you see only the ranked results. |

</details>

<details>
<summary><b>App</b> (1)</summary>

The app around the search.

| Patch | Default | What it does |
| --- | --- | --- |
| Disable start-up promotions | on | Stops the promotional sheets the app shows on start, such as the "Black Friday Deals" sheet and the app's own Genius sign-in, wallet and credit-card prompts. The cookie dialog and Android's notification prompt are left alone. |

</details>

<details>
<summary><b>Privacy</b> (1)</summary>

On by default. Stops data collection; changes nothing you see.

| Patch | Default | What it does |
| --- | --- | --- |
| Block tracking | on | Turns off low-risk analytics and marketing: AppsFlyer, Embrace, Booking.com's own c360 and Squeaks analytics, and the ID5 ad-identity service, and removes the advertising-ID and Android AdServices permissions. It leaves anti-fraud (ThreatMetrix, FingerprintJS), sign-in and payment untouched, so they keep working. |

</details>
<!-- PATCHES_END -->

## FAQ

**What is Morphe?**
An app that patches other Android apps on your phone: you give it the original app, pick patches, and it builds a
changed copy. These patches are one source you add to it.

**Why do I have to uninstall the Play Store version?**
The patched app is signed with a different key than Booking.com's, and Android won't install an app over one with a
different signature. The same is why the patched app doesn't update from the Play Store.

<a name="account-and-booking"></a>
**Can I sign in and book with the patched app?**
It isn't tested, and it's the one part that carries real risk. Booking.com profiles the device at sign-in and
payment with anti-fraud services (ThreatMetrix, FingerprintJS) that these patches leave on. A re-signed app may be
let through, may be refused, or may make the account look suspicious. Social sign-in (Google, Facebook) won't work
at all, because Google and Facebook only sign in apps signed by their maker. **Treat the patched app as a way to
search and compare.** If you want to book, do it in the official app, on the website, or directly with the hotel —
and if you sign in to the patched app to test, use a throwaway account, not your main one.

**Can my Booking.com account get banned for this?**
Unknown — this isn't released or widely used. The patches change what the app shows and what it sends for
analytics; they don't book, pay or act in your name. It's still a modified app, so there's no guarantee. This is
why signing in with your main account isn't recommended.

**Where do the "official site" prices come from?**
From Google Hotels, loaded in the app only when you open the Official site tab — the same page you'd see in a
browser. The patch reads the prices off that page. Nothing is sent anywhere on its own, and no price is fetched in
the background.

**Is it safe?**
All the code is in this repository, and the patch files in the releases are built from it by GitHub Actions. The
privacy patch only stops the app from sending things. The one feature that reaches the internet is **Compare
prices**: when you open its Official site tab it loads Google Hotels, as a browser would. Found a security problem?
See [SECURITY.md](SECURITY.md) for how to report it privately.

## Known issues

- **Price history has no past data.** It starts the day you install the patch and fills as you browse. No site
  publishes past hotel prices (unlike flights), so there's nothing to back-fill from. Google's "vs usual" rating
  is the only backward-looking signal, and it comes from Google, not from stored history.
- **The "official site" price depends on Google.** It's read off the Google Hotels page, so it breaks if Google
  changes that page, and it isn't shown for hotels Google has no official offer for. Google's prices are for one
  room; for 2 or more rooms the panel says so.
- **Search results are drawn from Booking.com's servers.** The patches that hide things in the results list work
  on the data the server sends, so a message in a form the patch doesn't match can still get through. Hiding on a
  hotel's own page is more reliable.
- **Sign-in and payment are untested** in a patched app. See the FAQ above.

Something else wrong? [Open an issue](https://github.com/JoFSlash/booking-patches/issues/new/choose).

## Building

You need JDK 21 and a GitHub token with `read:packages`
(see the [Morphe docs](https://github.com/MorpheApp/morphe-documentation/blob/main/docs/morphe-development/1_setup.md)).

```bash
./gradlew buildAndroid
```

The bundle ends up in `patches/build/libs/patches-*.mpp`.

## License

[GPL-3.0](LICENSE), see also [NOTICE](NOTICE). Not affiliated with Booking.com or with the Morphe project.
