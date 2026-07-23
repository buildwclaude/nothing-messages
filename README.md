# Messages — SMS/MMS app for Nothing Phone (3a)

A native Android messaging app built to replace Google Messages as the **default SMS app**
on a Nothing Phone (3a) running Nothing OS 4.0 / Android 16. Sideload only — not on the Play Store.

- **SMS and MMS only.** RCS has no public API for third-party apps, so there are no typing
  indicators, read receipts, or end-to-end encryption with RCS users. Their chats fall back to SMS/MMS.
- **No Google dependencies, no accounts, no servers.** The app itself has **no internet permission**.
  The only network activity is the carrier MMS transfer, which Android's own system service performs.
- **UI** follows the "Messaging App UI" Figma community design (blue `#2F80ED`, Plus Jakarta typeface).

## How to get the APK on your phone

1. Open this repository on your phone's browser → folder **`apk/`** → tap **`Messages.apk`** → tap the **Download** icon (or "Download raw file").
2. Every time new code is pushed, GitHub Actions rebuilds the APK and updates `apk/Messages.apk`
   automatically (also available under **Actions → latest run → Artifacts**).

## Install on Nothing OS 4.0

1. Open the downloaded `Messages.apk` from your notification shade or the Files app.
2. Android will say the browser/Files app is "not allowed to install unknown apps" —
   tap **Settings → toggle "Allow from this source"**, then go back and tap **Install**.
3. Open the app. It will ask for SMS/contacts/notification permissions — allow them.
4. Tap the blue banner **"Not your default SMS app"** (or Settings → *Set as default SMS app*).
   Android shows its own confirmation — choose **Messages**.
   (Manual path: **Settings → Apps → Default apps → SMS app → Messages**.)
5. Optional but recommended for scheduled send: in the app's **Settings → Exact alarms**, allow alarms.

To go back to Google Messages at any time: Settings → Apps → Default apps → SMS app → Messages (Google).

## Updating

There is no auto-update. Download the new `apk/Messages.apk` and install it over the old one
(your messages are stored in Android's system message store, not in the app, so nothing is lost).

## For maintainers (build system)

- Built entirely by **GitHub Actions** (`.github/workflows/build-release.yml`): JDK 17, Gradle 8.13,
  `assembleRelease`, signed with a PKCS12 keystore stored in repo secrets
  (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
- A manual **debug** workflow (`build-debug.yml`) exists under the Actions tab → "Run workflow".
- The signing keystore was generated with `openssl` (no Android Studio needed):
  ```
  openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 10950 -nodes -subj "/CN=Messages"
  openssl pkcs12 -export -inkey key.pem -in cert.pem -out release.p12 -name <alias> -passout pass:<password>
  base64 -w0 release.p12   # → KEYSTORE_BASE64 secret
  ```
  **Keep `release.p12` and its password safe** — updates must be signed with the same key.
- Licenses: app code Apache-2.0; vendored AOSP MMS library (`com.android.messaging.mmslib`) Apache-2.0;
  Plus Jakarta Sans font SIL OFL (`PLUS_JAKARTA_SANS_LICENSE.txt`); Feather icons MIT.

See [TESTING.md](TESTING.md) for how to verify everything works, and [DESIGN.md](DESIGN.md)
for the design tokens and how the Figma file maps to screens.
