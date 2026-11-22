# Patches in detail

What each patch does, in full. The [README](../README.md#patches) has the short list. All patches are on by default
except [Hide insurance offers](#hide-insurance-offers); you can switch any of them on or off when you patch.

- **Price:** [Compare prices](#compare-prices) · [Price history](#price-history) · [Price watch](#price-watch) ·
  [Check crossed-out prices](#check-crossed-out-prices) · [Show full price up front](#show-full-price-up-front)
- **Search:** [Hide urgency messages](#hide-urgency-messages) · [Hide sponsored listings](#hide-sponsored-listings) ·
  [Label Preferred properties](#label-preferred-properties) · [Review check](#review-check) ·
  [Recent rating](#recent-rating) · [Hotel contact](#hotel-contact)
- **App:** [Disable start-up promotions](#disable-start-up-promotions) · [Hide upsells](#hide-upsells) ·
  [Hide insurance offers](#hide-insurance-offers) · [Disable update and rating prompts](#disable-update-and-rating-prompts) ·
  [Allow screenshots](#allow-screenshots) · [Follow system theme](#follow-system-theme) ·
  [GmsCore support](#gmscore-support)
- **Privacy:** [Block tracking](#block-tracking)

Texts the patches add are in English only, also when the app is set to another language.

## Price

Added features. They show you more; they don't change what you book.

### Compare prices

Default: on.

Adds a **Compare prices** button to a hotel's page. It opens one panel, which also holds the **Price history** and
**Contact** tabs when those patches are on.

The **Official site** tab loads Google Hotels for that hotel, your dates and guests inside the app, reads the price
from the hotel's own website and puts it next to the Booking.com price, with a link to book there. Prices load for
your own region and currency.

When Google rates the price against the hotel's usual prices ("great deal, 32% less than usual"), that line shows
too. Google rarely shows it in the EU, so a **Check Google's deal rating (US data)** link reads it from the US
version of the page on request, without changing the prices shown.

### Price history

Default: on.

Saves the prices the app shows you (at most 5000, the same price only once per 6 hours) and shows them for your
dates as a chart, with the lowest, highest and current price and a check of crossed-out "original" prices.

A second chart shows the hotel over the past year across all stay dates (the lowest price per night each day).
**1M / 6M / 1Y / All** buttons pick the period.

The history starts when the patch is installed; prices from before that can't be fetched, because no site publishes
past hotel prices.

### Price watch

Default: on. Needs [Price history](#price-history).

Adds a **Watch this price** button to the **Price history** tab. Once a day, on Wi-Fi and with the battery not low,
the app asks Booking.com for the price of each stay you watch (hotel, dates, adults and rooms; at most 20), with the
same request the hotel's page makes, and saves it into the price history.

When the price fell by more than 1%, it shows a **Price drops** notification; tap it to open the hotel for those
dates. The app makes this notification on your phone itself, so it works in the patched app even though Booking.com's
own push messages don't (see [GmsCore support](#gmscore-support)).

The tab lists every watched stay with its last price; a watch ends by itself on the check-in day. Nothing is checked
until you watch a stay.

### Check crossed-out prices

Default: on.

When a hotel's page shows a crossed-out "original" price, a line under the price compares it with the prices saved on
your phone in the last 30 days for the same dates, guests, rooms and currency:

- "Crossed-out € 79: you only saw € 58–62 for these dates since 3 Nov (6 prices)", in red, or
- "you saw € 79 for these dates on 5 Nov" when it was the real price before.

It adds when the price was crossed out on nearly every check, which means the "discount" is the normal price. With
fewer than 3 saved prices, or all from one day, it says there aren't enough yet instead of judging. A Genius or
app-only discount is a real discount for you, so then it says just that.

It saves prices like Price history (the two share the same history), also when Price history is off. It compares the
hotel's headline price (the cheapest room, which the page shows); prices of single rooms on the room list aren't
saved or checked.

### Show full price up front

Default: on.

Shows the total price with the taxes and charges Booking.com otherwise adds on top (such as a city tax or cleaning fee
paid at the property): in search results, on the map (markers and cards), on the property page and in the room list.
Instead of "€ 60" and "+ € 12 taxes and charges" you see "€ 72" and "Total, incl. € 12 taxes and charges"; a
crossed-out price gets the same charges added, so the discount stays the same. It uses the amounts Booking.com already
sends with each price, no extra requests.

When you search from the EU, Booking.com already includes them in every price we checked ("Includes taxes and
charges", also for US hotels), so there the patch changes nothing.

Charges Booking.com can't work out in advance ("Additional charges may apply") still can't be added. The booking form
is left as it is.

## Search

What the search results show.

### Hide urgency messages

Default: on.

Hides scarcity and pressure lines on hotel pages and in search results, such as "We have 1 left at this price",
"Rare find", "booked N times" and "in high demand". Keeps the real information: the price, free cancellation and
whether a hotel is sold out.

### Hide sponsored listings

Default: on.

Removes sponsored ("Ad") properties from the results list and sponsored markers from the map, so you see only the
ranked results. In testing it took out about 2–3 ads per city on the first page of results.

### Label Preferred properties

Default: on.

Keeps the thumbs-up "Preferred" and "Preferred Plus" badges but adds "Pays Booking higher commission" next to them,
in the results list and on the hotel's page. Booking.com gives the badge to properties in its Preferred Partner
Programme, which pay it a higher commission; it isn't a quality rating. Screen readers hear the same.

### Review check

Default: on.

On a hotel's reviews screen, scans the reviews the app has loaded for red flags: extra charges and hidden fees, paying
outside Booking.com, "not as described", bookings not honoured, and scams or money taken.

A banner above the booking button says how many reviews mention them ("3 of 25 recent reviews mention extra charges or
booking problems"), in red when guests report a possible scam; tap it to read those reviews.

It's keyword matching on your phone, a heads-up and not a verdict: not AI, not fake-review detection. Keywords cover
English, Slovak, Czech, German, Polish, Hungarian, Italian, French and Spanish (with or without accents); reviews in
other languages may be missed.

### Recent rating

Default: on.

Under a hotel's name and address, shows the average score of its reviews from the last 6 months, "Last 6 months: 6.1
from 42 reviews · overall 8.5", in red when it's 0.5 or more below the overall score. The same line shows on the
reviews screen above the [Review check](#review-check) banner.

Booking.com sends no recent score, so the patch loads the newest 50 reviews once per hotel while the app runs, through
the app's own request for reviews (when the reviews screen is already sorted newest first, it uses those). When the
50 newest are all from the last 6 months, it says "Latest 50 reviews (since 3 Nov): 8.4" instead.

It's hidden under 10 recent reviews and leaves out reviews from guests who didn't stay. Busy hotels get 50 reviews in
a few weeks, so the line often covers less than 6 months.

### Hotel contact

Default: on.

Adds a **Contact** button above **Compare prices** on a hotel's page (a **Contact** tab in the same panel). It shows:

- the address, with **Open in Maps** (a pin at the hotel in your maps app) and **Copy address**;
- the phone number, email, company name, business address and registration that the hotel gives Booking.com for EU
  consumer law.

Tap the phone number to dial it, or the email to write with the hotel's name as the subject; long-press any detail to
copy it. The app already receives these details but hides them under Legal information.

For chains they can be the head office (an Ibis gives Accor's), and private hosts and hotels outside the EU and a few
other countries don't have them.

## App

The app around the search.

### Disable start-up promotions

Default: on.

Stops the promotional sheets the app shows on start, such as the "Black Friday Deals" sheet and the app's own Genius
sign-in, wallet and credit-card prompts, and the full-screen sign-in page on the first start. You can still sign in
from My account, or when booking or saving. The cookie dialog and Android's notification prompt are left alone.

### Hide upsells

Default: on.

Hides marketing that sells you something besides your stay:

- on the home screen: campaign banners (such as "Up to 40% off stays worldwide"), the "Offers" row ("Save 20% on
  attractions", "Explore Black Friday Deals"), the carousels of attractions, cars and taxis for your upcoming trip and
  your last search, and the AI trip planner card;
- Booking.com's promotional areas on the hotel page, in the booking form, on the confirmation and in My account;
- offers to add a taxi, car rental, flight or attraction ("Plan ahead", "Add to your stay") on the hotel, room and
  cancellation pages, in the booking form, after booking and in your booking details;
- the Booking.com credit card banners and sign-up sheet;
- the Genius VIP price-match banner on hotel pages, the "book 1 more stay to reach Genius Level 2" nudge in the room
  list and the "Year in travel" sheet.

Prices, Genius discounts and badges, and rewards you have earned stay. On the home screen the search box, your
recent searches, your Genius level, saved properties and your trips stay. Insurance is the next patch.

The home screen is drawn from Booking.com's servers. The patch takes a block out by the name Booking.com's own
tracking gives it (one with "offer", "deal", "campaign" or "attraction" in it), so a new block with another name can
still show; blocks it isn't sure about stay.

### Hide insurance offers

Default: **off**.

Hides the offers to add room cancellation or travel insurance in the booking form, and its extra "Protect your trip"
page. Off by default, because some people want insurance: turn it on if you never do.

It only hides the offers: insurance you have added still shows, with its price and the option to remove it, and
nothing is added or removed for you.

Not verified in the app yet: the test account was never offered insurance.

### Disable update and rating prompts

Default: on.

Stops the "update available" and "this version is no longer supported" prompts, which you can't act on in a patched
app (it doesn't update from the Play Store), and the Google Play "rate this app" card after a booking.

### Allow screenshots

Default: on.

Lets you take screenshots and record the screen on booking and payment screens, which the app normally blocks (they
come out black). Bank verification (3-D Secure) screens still block them.

### Follow system theme

Default: on.

Makes the app light or dark to match your phone. Booking.com has a dark theme built in (**My account > Settings >
Theme**), but it starts on Light; this makes it start on **System** instead. A theme you pick there yourself still
wins.

### GmsCore support

Default: on.

Makes "Continue with Google" work through [MicroG-RE](https://github.com/MorpheApp/MicroG-RE) instead of Google Play
services, which won't sign in an app it didn't sign. Needs MicroG-RE installed with your Google account added; without
it the app says so once and Google sign-in won't work. Email sign-in works either way.

Booking.com's own push messages stay off in the patched app. Notifications the app makes on the phone itself, such as
[Price watch](#price-watch)'s **Price drops**, still work.

With MicroG-RE the app's maps are MicroG's (MapLibre, not Google's own tiles); the patch also works around a MicroG bug
that crashed the app when a map opened or switched between the default and satellite view.

## Privacy

Stops data collection; changes nothing you see.

### Block tracking

Default: on.

Turns off low-risk analytics and marketing: Firebase Analytics, AppsFlyer, Embrace, Booking.com's own c360 and Squeaks
analytics, and the ID5 ad-identity service, and removes the advertising-ID and Android AdServices permissions. It
leaves anti-fraud (ThreatMetrix, FingerprintJS, Riskified), sign-in and payment untouched, so they keep working.

It also stops Google's app services from reporting on you:

- **Firebase Crashlytics** and **Firebase Sessions** (crash reports and app sessions): off by their own switches.
  The app turns Crashlytics back on when it starts, so the patch also answers Crashlytics' "may I collect" check with
  no.
- **Firebase logging** (firebaselogging-pa.googleapis.com), the channel Crashlytics, Sessions, Performance, ML Kit
  and push delivery logs are sent through: its uploader is removed, so whatever is queued for it is dropped.
- **Firebase Installations** (firebaseinstallations.googleapis.com), the install ID the Firebase services above
  share: its requests fail on the phone as if the service were down. The install ID is still made, but never sent.
  Push needs it only for an extra token, and push doesn't work in the patched app anyway (see
  [GmsCore support](#gmscore-support)).
- **Google Tag Manager**: it only runs on Firebase Analytics events, which are off; its service and preview screen
  are switched off too, and the app no longer builds the marketing events (search, booking, confirmation with price)
  meant for it.

Firebase Remote Config is left in place: the app's own code doesn't use it.

It doesn't block everything: Facebook profile pictures (graph.facebook.com) load on reviews.
