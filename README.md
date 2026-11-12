# Call Filter

*[Leer en español](README.es.md)*

An Android app that filters incoming calls with your own rules: block specific countries,
allow domestic calls only, reject hidden numbers, silence prefixes... It's built with Kotlin,
Jetpack Compose and Material 3, and it fits into the system: dynamic color (Material You),
dark theme, themed icon, a quick settings tile and per-app language.

<p align="center">
  <img src="docs/screenshots/rules.png" width="200" alt="Rule list" />
  <img src="docs/screenshots/editor.png" width="200" alt="Rule editor" />
  <img src="docs/screenshots/countries.png" width="200" alt="Country picker" />
  <img src="docs/screenshots/settings.png" width="200" alt="Settings" />
</p>
<p align="center"><sub>Rules · Editor · Country picker · Settings</sub></p>

## How it works

The app uses Android's `CallScreeningService` API. Once it holds the "caller ID & spam" role,
Android asks it about every incoming call that isn't from a saved contact, and the app decides
whether to let it through, silence it or reject it.

Rules are checked from top to bottom and the first enabled rule that matches wins. If none
match, the call goes through.

| Condition | Matches |
|---|---|
| Specific countries | numbers from the selected countries (country detected with libphonenumber) |
| International | any country other than your home country ("domestic calls only") |
| Prefix | numbers starting with some digits; with `+` it's compared with the international format, without `+` with the national number |
| Hidden numbers | calls with no number or a restricted number |
| Any number | every call that reaches the filter |

| Action | Effect |
|---|---|
| Block | the call is rejected without the phone ringing |
| Silence | the call comes in without ringing or vibrating; you can still answer it from the screen |
| Allow | the call goes through as usual; use it for exceptions above other rules |

Other features:

- **Home country**: taken from the SIM, and can be set manually. It defines what counts as
  "domestic".
- **History**: every call that goes through the filter, with the rule that was applied.
- **Quick settings**: a tile to pause or resume filtering without losing your rules.
- **Logcat**: every decision is logged with the `CallRules` tag.

## Privacy

Everything stays on the phone. The app doesn't request internet, contacts or call log
permissions. Rules, settings and history are stored as JSON in its internal storage
(`rules.json` and `history.json`).

## Languages

English (fallback language), Spanish, French, Italian, German and Portuguese. By default the
app follows the system language; on Android 13+ you can pick a different one just for this app
from Settings → Language. AGP generates `locales_config.xml` from the `values-xx` folders
(`generateLocaleConfig` + `res/resources.properties`), so adding a language only takes a new
folder. Country names come from the system itself, in the active language.

## Requirements

- Android 10 (API 29) or later.
- To build: JDK 17 and the Android SDK with platform 36.

## Build and install

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

To filter calls, the app needs the call screening role. You can grant it from the app itself
(the "Turn on" button, which opens the system dialog) or with adb:

```bash
adb shell cmd role add-role-holder android.app.role.CALL_SCREENING com.icaria.callrules
```

## Signed releases

The [Release workflow](.github/workflows/release.yml) builds, tests and signs the app when a
version tag is pushed, and publishes a GitHub release with the APK, the AAB and their SHA-256
checksums. The version name comes from the tag (`v1.2.0` → `1.2.0`) and the version code from
the workflow run number. Tags with a hyphen (`v1.2.0-beta1`) are published as pre-releases.

```bash
git tag v1.0.0
git push origin v1.0.0
```

It needs four repository secrets (Settings → Secrets and variables → Actions):

| Secret | Content |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | the release keystore, base64-encoded |
| `SIGNING_STORE_PASSWORD` | keystore password |
| `SIGNING_KEY_ALIAS` | key alias |
| `SIGNING_KEY_PASSWORD` | key password |

To create a keystore and upload the secrets with the GitHub CLI:

```bash
keytool -genkeypair -keystore callrules-release.jks -storetype PKCS12 \
  -alias callrules -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 callrules-release.jks | gh secret set SIGNING_KEYSTORE_BASE64
gh secret set SIGNING_STORE_PASSWORD
gh secret set SIGNING_KEY_ALIAS --body callrules
gh secret set SIGNING_KEY_PASSWORD
```

Keep the keystore and its passwords backed up somewhere safe and outside the repository:
updates must be signed with the same key, and a lost key means users have to uninstall the app
to install a new version.

The same signing setup works locally: if `SIGNING_KEYSTORE_PATH`, `SIGNING_STORE_PASSWORD`,
`SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD` are set, `./gradlew :app:assembleRelease`
produces a signed APK; without them it produces an unsigned one.

## Project structure

```
app/src/main/java/com/icaria/callrules/
  data/      model (rules, settings, history) and JSON repository
  engine/    number analysis (libphonenumber), rule engine, home country
  service/   RuleScreeningService (CallScreeningService)
  tile/      quick settings tile
  ui/        Compose screens: rules, editor, history, settings, country picker
app/src/test/  JVM tests for the rule engine and the JSON format
docs/screenshots/  README screenshots
```
