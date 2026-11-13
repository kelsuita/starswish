# Starwish

**English** · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md)

> Write down what you want to buy, and watch it slowly come true.

An Android savings-goal app built with Kotlin and Jetpack Compose. The interface is based on
**MIUIX 0.9.4** and borrows the large-title and settings-page language of HyperOS: whole-page
horizontal paging, a title that collapses while scrolling, a full color scheme derived from the
accent color, and optional background blur.

[Download](https://github.com/kelsuita/starswish/releases/latest) ·
[Issues](https://github.com/kelsuita/starswish/issues) ·
[Donate on Afdian](https://afdian.com/a/kelsuita)

## Introduction

Starwish never touches your money. It does one thing: it works out how much you want to save,
how much is still missing, and roughly how much you need to put aside each day — and keeps that
in front of you every day.

You can maintain several savings goals at once and bind each goal's progress to one or more
savings accounts. The banner on the home page shows the goal you choose, and the progress bar,
percentage and daily suggestion update live with the account balances. All amounts are stored
as integer cents, so there is no `0.1 + 0.2` rounding error.

If you also use Xiaoxing Ledger (小星记账), the app can read the visible "CNY balance" when you
open an account detail page yourself, and sync it to a goal after you confirm it. There is no
background sync, no transaction history is read, and nothing is confirmed on your behalf.

## Download

Current release: [Starwish Dr.Ratio 1.6.0](https://github.com/kelsuita/starswish/releases/tag/v1.6.0)

- `app-universal-release.apk`: universal package, choose this if you are unsure about the ABI
- `app-armeabi-v7a-release.apk`: 32-bit ARM devices
- `app-arm64-v8a-release.apk`: 64-bit ARM devices
- `app-x86-release.apk`: 32-bit x86 devices
- `app-x86_64-release.apk`: 64-bit x86 devices
- All release packages above are signed with the same project release key; debug builds use the Android debug key
- Requirements: Android 8.0 (API 26) or newer
- The two signing keys are different and cannot overwrite each other on install
- The release key differs from the debug key used for the first 1.0.0 release. Users still on an older build usually have to uninstall it first; uninstalling deletes app data, so back up anything important first

Future releases will keep using the same release key, so release builds can be upgraded in place.
Do not lose or replace that key — Android will refuse to install a new version as an update otherwise.

## What's new in Dr.Ratio 1.6.0

- WebDAV sync (testing)
- Unified popups as bottom sheets
- Improved landscape bottom navigation and transient message timing
- Fixed missing card backgrounds and mismatched press-feedback shapes
- Stability and smoothness improvements

## Dr.Ratio 1.6.0 更新内容

- 测试中：WebDAV同步
- 统一弹窗为 BottomSheet 样式
- 优化底部导航栏在横屏下的显示
- 优化提示消息的显示时长和一致性
- 修复部分界面卡片背景缺失、按压反馈形状不一致等问题
- 提高应用的稳定性和流畅度

## Quick start

1. Download and install the release APK that matches your device ABI; choose `app-universal-release.apk` if unsure.
2. Add an account on the "Accounts" tab and enter its balance; you can maintain balances entirely by hand without Xiaoxing Ledger.
3. Create a wish on the "Home" tab: set the target amount, link accounts and pick how progress is calculated.
4. Update account balances to see goal progress. Theme, accent color and the home banner live under "Settings → Theme settings".

Reading balances from Xiaoxing Ledger is optional and requires enabling the accessibility service
separately; read the integration notes below before using it for the first time.

## Features

### Savings goals

- Set a goal name, target amount and optional deadline; the home banner, progress bar and icons follow the app accent color
- Each goal can be linked to one or more savings accounts and shows the progress percentage, saved amount, remaining amount and daily suggestion
- Two progress modes: based on the accounts' **current balance**, or based on the **starting balance** recorded when the goal was created
- Progress is clamped to 0%–100%; balances of multiple goals are never added together or shared

### Savings accounts

- Add, edit and delete accounts by hand; deleting an account that is still linked to a goal is blocked
- Read the current balance from Xiaoxing Ledger: after you start a request, open the Xiaoxing account detail page and the amount is recognized automatically; confirm the sync back in Starwish
- Reading is limited to one account per request and stops if it is not finished within two minutes or the page is ambiguous

### Theme and appearance

Appearance options:

- Theme mode: follow system / light / dark
- Pure black background in dark mode: dark mode only; the page background becomes black while cards keep their depth
- Accent color: default, Monet, blue, green, orange, purple
- Palette style and Material 3 color spec (2021 / 2025), with the full scheme derived by MIUIX `ThemeController`; also applies to Monet colors
- Home banner: light / dark; choose which goal the banner shows; compact goal list
- Background blur: Android 13 and newer, applied to the large title bar and the bottom navigation
- Language: follow system / Simplified Chinese / Traditional Chinese / English, switchable at the top of the settings page without restarting; follows the system on first launch

The background blur switch is under "Settings → Appearance"; the other options live on the theme
settings page. Settings are stored on the device and survive restarts.

### Interaction details

- Home, Accounts and Settings pages switch horizontally as a whole page; titles and content move together, the bottom navigation stays fixed, and each page keeps its own scroll position
- The large title collapses while scrolling, and lists bounce when they reach an edge
- "About" is a separate page with the GitHub project, Afdian donation entry, usage steps, data and permission notes, progress calculation and the official API documentation
- The quote at the top of the settings page is only shown in Simplified or Traditional Chinese with the option enabled: it is on by default for Chinese interfaces and can be turned off under "Settings → Appearance → Theme settings → Home"; it is hidden and stops requesting in other languages, and the previous choice is kept when switching back. When enabled and no local quote exists, one is fetched from a public API and saved; switching pages or restarting does not refresh existing content, and tapping it fetches a new one

### JSON backup

- Entry point: "Settings → Backup → Import and export", below the appearance group and above the app group
- Exports accounts, goals, appearance settings and the saved quote as JSON through the system file picker
- Import validates the format, version, amount ranges and account links, then replaces all existing data after confirmation; exporting the current data first is recommended
- Backups are plain-text files containing account names and balances, so store them safely; up to 1 MB is supported
- Temporary read requests, pending balances and accessibility permissions are not included; permissions must be enabled again on a new device

## Privacy and data

- All goal and account data stays in the app's private storage on the device: no account, no login, no ads, no analytics SDK
- No card numbers, passwords or full statements are read; no taps, transfers or changes to Xiaoxing Ledger data are performed
- Requests are only made when the Chinese UI has the quote feature enabled and content is needed or you tap refresh; no requests are made in other languages or with the option off, and no account, balance or goal data is sent to that API; opening GitHub, donation or documentation links hands over to the external app and the corresponding site
- Reading Xiaoxing balances relies on the Android accessibility service, only recognizes a specific package and account detail page, and can be turned off in system settings at any time
- Android auto backup and device transfer are disabled; export a JSON backup manually instead. Uninstalling the app deletes local data, so keep backups outside the app in advance

## Xiaoxing Ledger integration and limits

Official documentation checked for this project:

- URL Scheme: <https://cxincx.com/support/scheme.html>
- Encrypted backup notes: <https://cxincx.com/support/backup/7.html>

According to the documentation checked while building this project, the scheme can insert
transactions and open the automatic bookkeeping dialog; it does not offer an account-balance query
usable here, and the backup file cannot be opened directly. The current implementation therefore
recognizes the balance from the visible page: it does not parse the encrypted database and does not
use the accumulated transaction total instead of the balance.

The Android accessibility service reads the **visible account detail page of Xiaoxing Ledger**
that you open yourself:

1. Add a savings account on the "Accounts" tab with a name exactly matching the one in Xiaoxing Ledger, and enter the initial balance.
2. Read the in-app explanation, then enable "Starwish · read Xiaoxing accounts" in system settings.
3. Tap "Read Xiaoxing balance" on the account card; the app opens Xiaoxing Ledger.
4. Open the matching account detail page within two minutes. When the service sees `com.cxincx.xxjz.account.AccountDetailActivity` it reads the page automatically and requires the full account name plus the visible "CNY balance" at the top; balances in the transaction history below are ignored. No extra tap on the Xiaoxing page is needed.
5. After a successful read, a capsule saying "Read, please return to Starwish" appears at the top of the Xiaoxing page; tapping it opens Starwish's Accounts confirmation page directly. Check the recognized amount and tap "Confirm sync". The capsule fades out after three seconds, or you can go back to Starwish manually to see the pending result.

The top capsule is a local accessibility overlay in a fluid-cloud style, not an OPPO system
fluid cloud. It uses `TYPE_ACCESSIBILITY_OVERLAY` from the already enabled service, requests no
extra overlay permission, shows no account name or balance, and never confirms a sync on its own.
A native OPPO fluid cloud would still need platform authorization, UPK templates and the
SeedlingSupportSDK; it is not integrated. The capsule is removed when the service is interrupted
or destroyed; taps work both when Starwish is cold-started and when it is already in the background.
The prompt only appears after a balance was recognized and saved as a candidate, and it cannot
replace real-device integration testing.

**This is not background syncing.** A request covers a single account and stops after a candidate
result or a two-minute timeout. Recognition is refused when the name is ambiguous, the balance label
is missing, several different balances exist, or the page shows credit limits, foreign currency or
an all-assets overview; manual updates are always available. If nothing was read, go back and edit
the balance by hand or start another read. No read is started unless permission is enabled.

The top CNY balance was adapted from the activity name and account detail screenshot provided by
the user, with parsing tests covering transaction-history balances and split amount nodes. Real
device accessibility node integration has not been completed yet; some pages may not expose
accessible text or may use different labels. **Screenshots and parser tests cannot replace a read
on a real Xiaoxing build.**

Page reading is restricted to the Xiaoxing package `com.cxincx.xxjz`; it does not tap, modify
transactions or transfer money, and it does not save screenshots or full page text. Requests to
`v1.hitokoto.cn` are only made when the Chinese UI has the quote feature enabled and content is
needed or manually refreshed, and no account, balance or goal data is sent there; no requests are
made in other languages or with the option off. The app holds the INTERNET permission; backup and
device transfer are disabled. The service can be turned off in system settings at any time.
Uninstalling deletes app data.

On Android 13 and newer, the system may restrict enabling accessibility from an installed file
first; if that happens, allow restricted settings from the app's system "App info" menu as the
system prompts, then enable it manually. Permissions must always be confirmed by the device owner.

## Build and install

Requires JDK 21, Android SDK Platform 37, and network access or an existing dependency cache. The
Gradle wrapper is included. Minimum Android 8.0 (API 26), targetSdk 36.

Windows (run unit tests, build the debug APK and run static checks):

```powershell
powershell -ExecutionPolicy Bypass -File tools\build.ps1
```

Alternatively open this directory in Android Studio, sync Gradle and run. Android Studio can create
the local `local.properties` file; it should not be committed.

Gradle tasks can also be run directly:

```powershell
./gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release signing and release builds

Published release APKs are signed with a dedicated project release key and never fall back to the
Android debug key. The private key and the local configuration containing passwords are not part
of the repository. Maintainers building a release in a new environment need to restore the key
from a secure backup and create `keystore.properties` in the project root:

```properties
storeFile=C:/secure/path/starswish-release.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=starswish
keyPassword=YOUR_KEY_PASSWORD
```

`keystore.properties` is excluded by `.gitignore`, and the key file must not be committed or
uploaded to a public repository. With that in place, run:

```powershell
./gradlew.bat assembleRelease
```

Signed APKs are written to `app/build/outputs/apk/release/app-universal-release.apk`. Without a
local signing configuration the build produces unsigned APKs that cannot be published as
installers. Self-built packages can use your own key, but they cannot overwrite this project's
official release APKs.

Before publishing, verify the signature with `apksigner` from the Android SDK Build Tools:

```powershell
apksigner verify --print-certs app/build/outputs/apk/release/app-universal-release.apk
```

Add the Build Tools directory to `PATH` or use the full path to the tool. When maintaining official
releases, confirm that the signing certificate matches existing releases and keep an independent
secure backup of the key and passwords.

## Verification

- `SavingsLogicTest`: amount precision and input bounds, linked-account calculation, calculation from a starting balance, negative/overflow progress, balance labels, multiple accounts, ambiguous results, credit limits and foreign currency rejection.
- `lintDebug`: static checks. Dependency versions are pinned; version upgrade suggestions and KTX style suggestions do not affect the build.
- The top capsule has been loaded through an isolated accessibility helper service on an emulator using the actual prompt component from a real build, verifying display, auto-dismiss, removal when the service is disabled, and tapping back into a running Starwish; the cold-start entry from the confirmation page was verified too. That component test did not trigger a real Xiaoxing read and does not mean real balance reading has passed integration testing.
- `tools/smoke-test.ps1`: installs the current build and verifies adding an account, required goal validation, progress calculation, restart persistence, permission disclosure, the About page and system back, edge back gestures and theme settings on a dedicated isolated emulator. The script clears this app's data on the specified test emulator; do not run it against a daily-use device.
- `tools/start-test-emulator.ps1`: starts an isolated test emulator on port 5580 from an existing local Android 35 x86_64 image. It never touches a connected physical phone. Test scripts only clear this app's data on the specified isolated emulator.

## Known limitations

- Balance reading from Xiaoxing Ledger has not completed real-device accessibility node integration; page structure changes in other versions may break recognition
- Fixed the status bar backdrop sampling range and blur failing on the bottom bar after switching tabs; verified on an Android 35 emulator with the host GPU, behavior on other vendor systems and GPUs still needs device testing. See the [Miuix review notes](docs/miuix-review.md)
- Only CNY savings accounts are supported; no multi-currency, automatic transfers or fund allocation
- Official APKs are currently sideloaded; no app store release pipeline is configured

## Project structure

```text
app/src/main/java/com/kelsuita/starswish/
  MainActivity.kt                      Compose pages, editors and system entry points
  data/Models.kt                       Amounts, accounts, goals and progress math
  data/SavingsStore.kt                 Atomic local persistence, candidate balance and confirmation
  data/Appearance.kt                   Local appearance model
  data/Hitokoto.kt                     Quote network request (cache is managed by SavingsStore)
  sync/BalanceParser.kt                Conservative page balance parsing
  sync/XiaoxingAccessibilityService.kt Time-limited, package-restricted visible page reading
  sync/BalanceReadCapsule.kt           Accessibility overlay prompt after a successful read
app/src/test/                          Unit tests for core logic
tools/                                 Build and isolated emulator verification scripts
```

### Language packs

All in-app copy is addressed by stable identifiers such as `home.current_wish`; the text itself
lives in `app/src/main/assets/i18n/` (`en`, `zh-CN`, `zh-TW`, `ja`). To add or update a translation, edit only
those files — no Kotlin changes are needed, and the flat dotted keys can be uploaded to Crowdin
directly. Missing keys fall back to English, then to the identifier itself. The app name and the
accessibility service label/description are generated from the same packs into Android resources at
build time, because Android reads them before the app starts. To add a language, drop a
`<code>.json` copy of `en.json` into that folder and append one entry to `AppLanguage` in
`Language.kt`; resource folders, follow-system matching and the language index range update
automatically. The entry must be appended last because stored choices use the enum index.

The same account can back several independent goals; this does not allocate funds or move money.
The home banner only shows the progress of one chosen goal (the first by default). Renaming a goal
or changing its amount or date does not reset the starting point; changing linked accounts or the
calculation mode does.

## License

This project is released under the [MIT License](LICENSE), which allows use, modification and
redistribution, including commercial use; the copyright notice and license text must be retained
when redistributing. The software is provided as is, without any warranty.

Third-party dependencies keep their own licenses.
