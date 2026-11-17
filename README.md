# 🧩 JoFSlash Patches

Patches for [Quora](https://play.google.com/store/apps/details?id=com.quora.android) for use with [Morphe](https://morphe.software).

🌐 Website: https://jofslash.github.io/quora-patches/  
📲 Get Morphe Manager from the official site: https://morphe.software

## ❓ About

- **Remove ads**: hides promoted answers, sponsored posts and Google ads in the feed and on question pages, and blocks requests to ad servers.
- **Disable analytics**: turns off Firebase Analytics, Crashlytics, Firebase Performance and Facebook app events, and stops the advertising ID from being collected.

Tested with Quora 3.2.42 and 3.2.30 from [APKMirror](https://www.apkmirror.com/apk/quora-inc/quora/) (APKM bundle).

### How to use these patches

Click the button below, or manually add this repo url to Morphe Manager: https://github.com/JoFSlash/quora-patches

<a href="https://morphe.software/add-source?github=JoFSlash/quora-patches">
  <img src=".github/assets/add-to-morphe.svg" alt="Add to Morphe" height="48">
</a>

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.2.0-dev.2](https://github.com/JoFSlash/quora-patches/releases/tag/v1.2.0-dev.2)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;31 patches total
<details open>
<summary>📦 Quora&nbsp;&nbsp;•&nbsp;&nbsp;31 patches</summary>
<br>

**Package name:** `com.quora.android`

**🎯 Supported versions:**

| 3.2.42 | 3.2.30 |
| :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Block advertising ID](#block-advertising-id) | Stops the app from sending your advertising ID and Android ID to Quora. |  |
| [Block app telemetry](#block-app-telemetry) | Stops the app from sending usage logs to Quora, such as reading time, tab presses, experiments, app speed and errors. |  |
| [Block clipboard reading](#block-clipboard-reading) | Stops Quora's pages from reading what you copied. Pasting with the keyboard still works. |  |
| [Block tracking logs](#block-tracking-logs) | Stops Quora's pages from passing performance and sign-up tracking events to the app. |  |
| [Block web trackers](#block-web-trackers) | Blocks requests from Quora pages to third-party analytics and tracking services. |  |
| [Clean shared links](#clean-shared-links) | Removes tracking parameters from Quora links you copy or share. |  |
| [Disable WebView debugging](#disable-webview-debugging) | Stops the app from letting a connected computer inspect its pages, which would show your account's cookies. |  |
| [Disable analytics](#disable-analytics) | Turns off Firebase Analytics, Crashlytics, Firebase Performance and Facebook app events, and stops the advertising ID from being collected. |  |
| [Disable charging tracking](#disable-charging-tracking) | Stops the hourly job that tells Quora whether your phone is charging. |  |
| [Disable notification prompt](#disable-notification-prompt) | Stops the app from asking for permission to show notifications. You can still turn notifications on in Android's settings. |  |
| [Disable rating prompts](#disable-rating-prompts) | Stops the app from asking you to rate it on Google Play. |  |
| [Disable update prompts](#disable-update-prompts) | Stops the app from showing the Google Play update screen. |  |
| [Disable vibration](#disable-vibration) | Stops the app from vibrating when you tap buttons such as upvote. |  |
| [Disable video autoplay](#disable-video-autoplay) | Videos in the feed and in answers only play when you tap them. |  |
| [Enable text selection](#enable-text-selection) | Lets you select and copy text on every page. |  |
| [Hide AI answers](#hide-ai-answers) | Hides answers written by Poe bots. |  |
| [Hide Poe](#hide-poe) | Hides links and buttons that open Poe or Quora's AI assistant. |  |
| [Hide Quora+ answers](#hide-quora-answers) | Hides answers and posts that can only be read with a Quora+ subscription. |  |
| [Hide Quora+ promotions](#hide-quora-promotions) | Hides buttons that ask you to try or join Quora+. |  |
| [Hide Spaces suggestions](#hide-spaces-suggestions) | Hides the Spaces you might like suggestions in the feed. |  |
| [Hide badges](#hide-badges) | Hides the red dots and counts on the bottom bar tabs. |  |
| [Hide counts](#hide-counts) | Hides upvote, view and share counts on answers and posts. |  |
| [Hide downvote button](#hide-downvote-button) | Hides the downvote button on answers and posts. |  |
| [Hide related questions](#hide-related-questions) | Hides the related questions lists on question pages. |  |
| [Hide suggested questions](#hide-suggested-questions) | Hides the questions for you to answer in the feed. |  |
| [Hide tabs](#hide-tabs) | Removes the tabs you pick from the bottom bar. Home always stays. | • Hide Following tab<br>• Hide Answer tab<br>• Hide Spaces tab<br>• Hide Notifications tab |
| [Open links in browser](#open-links-in-browser) | Opens links to other websites in your default browser instead of an in-app browser tab. |  |
| [Remove ads](#remove-ads) | Hides promoted answers, sponsored posts and Google ads in the feed and on question pages, and blocks requests to ad servers. |  |
| [Spoof installer](#spoof-installer) | Reports Google Play as the app installer, so Quora does not treat the patched app as installed from an unknown source. |  |
| [Start tab](#start-tab) | Opens the app on the tab you pick instead of Home. | • Start tab |
| [Text size](#text-size) | Makes the text on every page larger or smaller. | • Text size |

</details>

<!-- PATCHES_END -->

## 🧑‍💻 Building

Requires JDK 21 and a GitHub token with `read:packages` (see the [Morphe docs](https://github.com/MorpheApp/morphe-documentation/blob/main/docs/morphe-development/1_setup.md)).

```bash
./gradlew buildAndroid
```

The bundle is written to `patches/build/libs/patches-*.mpp`.

## 📜 License

These patches are licensed under the [GNU General Public License v3.0](LICENSE). See [NOTICE](NOTICE).
This project is not affiliated with Quora or with the Morphe project.
