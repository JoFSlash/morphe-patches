## 1.0.0 (2026-11-22)

### Patches

The first release: 23 patches for Booking.com 71.1, on Android 8 or newer, no root needed. All are on by default
except the two marked *off by default*. [Each patch in detail](https://github.com/JoFSlash/booking-patches/blob/main/docs/patches.md).

**Price**

- **Compare prices**: the price on the hotel's own website (from Google Hotels) next to the Booking.com price, with a link to book there.
- **Price watch**: checks the price of stays you watch once a day, charts it and notifies you when it drops.
- **Check crossed-out prices**: says whether a crossed-out "original" price matches what you actually saw for those dates before.
- **Show full price up front**: shows the total with the taxes and charges Booking.com adds on top.
- **Show price per night**: the price per night under the total of each search result for stays of 2 or more nights.

**Search**

- **Hide urgency messages**: hides scarcity and pressure lines such as "We have 1 left at this price".
- **Hide sponsored listings**: removes sponsored ("Ad") properties from the results list and the map.
- **Label Preferred properties**: adds "Pays Booking higher commission" next to the Preferred and Preferred Plus badges.
- **Remember result sort**: keeps the sort you last picked, such as lowest price, instead of "Our top picks".
- **Review check**: scans a hotel's loaded reviews for extra charges, scams and other red flags.
- **Recent rating**: a hotel's average score from the last 6 months next to its overall score.
- **Newest reviews first**: opens a hotel's reviews sorted newest first.
- **Hotel contact**: a Contact tab with the hotel's address, phone, email and company details.

**App**

- **Disable start-up promotions**: stops the promotional and sign-in sheets the app shows on start.
- **Hide upsells**: hides home screen offers, offers for taxis, cars, flights, attractions and credit cards, and Genius nudges.
- **Hide insurance offers** (*off by default*): hides the cancellation and travel insurance offers in the booking form.
- **Disable update and rating prompts**: stops the update prompts, which a patched app can't act on, and the "rate this app" card.
- **Allow screenshots**: lets you take screenshots on booking and payment screens.
- **Follow system theme**: makes the app light or dark to match your phone.
- **GmsCore support**: makes "Continue with Google" work through MicroG-RE.
- **Open links in browser**: opens links to other websites in your browser; Booking.com pages, sign-in and payment stay in the app.
- **Stays only** (*off by default*): hides the Flights, Car rental, Taxi and Attractions tabs on the home screen.

**Privacy**

- **Block tracking**: turns off analytics, crash reporting and marketing trackers; leaves anti-fraud, sign-in and payment alone.

### Before you use it

- **Use a throwaway account.** Booking.com's anti-fraud checks stay on. Email sign-in, Google sign-in through
  MicroG-RE and a free-cancellation booking worked in one test on a throwaway account, but that's one test, not
  a guarantee. Don't put an important booking on it yet. Facebook sign-in doesn't work.
- **Show full price up front does nothing from the EU**, where Booking.com already includes taxes and charges.
- **Texts the patches add are in English only**, also when the app is set to another language.
- Uninstall the Play Store version first: the patched app is signed with a different key. It doesn't update from
  the Play Store, and Booking.com's push messages don't arrive (Price watch's own notifications do).

The bundle is signed with this repository's key ([morphe-signing.pub](https://github.com/JoFSlash/booking-patches/blob/main/morphe-signing.pub)).
