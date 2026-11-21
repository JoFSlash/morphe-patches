# Booking.com patches

Morphe patches for the Booking.com app (`com.booking`). Work in progress.

## Patches

| Patch | Default | What it does |
|---|---|---|
| Compare prices | On | Adds a Compare prices button to the property page. Its Official site tab opens Google Hotels for the same hotel, dates and guests inside the app, reads the price from the hotel's own website and puts it next to the Booking.com price, with a link to book there. Google's prices are loaded for your own region. When Google judges the price against the hotel's usual prices ("great deal, 32% less than usual"), that line is shown too; Google rarely shows it in the EU, so a "Check Google's deal rating (US data)" link reads it from the US version of the page on request, without changing the prices shown. |
| Price history | On | Saves the hotel prices the app shows on the phone (at most 5000, the same price only once per 6 hours) and shows them for your dates as a chart, with the lowest, highest and current price and a check of crossed-out "original" prices. A second chart shows the hotel over the past year across all stay dates (lowest price per night each day). 1M / 6M / 1Y / All buttons pick the period. The history starts when the patch is installed. |

With both on, the button opens one panel with a tab for each.
