# AdBlock VPN — AdBlock Shield

On-device DNS ad blocker for Android (Android 15 ready, works on Android 7+).
No root, no remote servers: everything happens on your phone.

## Download

Grab `app-release.apk` from the [latest release](../../releases/latest),
install it, open **AdBlock VPN** and tap the shield. Android asks once for
VPN permission — that's how the filtering works.

## How it works

- The app starts a local VPN that claims **only its own virtual DNS
  servers** (10.0.0.1 / fd00::1). Your normal traffic is never tunnelled,
  slowed down, or sent anywhere.
- Every DNS lookup is checked on-device against:
  - a bundled core blocklist of **77,000+ ad/tracker domains**
    (from the [StevenBlack hosts](https://github.com/StevenBlack/hosts)
    project, MIT-licensed),
  - curated categories for mobile-game ad networks (UnityAds, AppLovin,
    AdMob, Vungle, ironSource…), trackers, and web ad exchanges — each can
    be toggled in the Filters tab,
  - your own custom block rules and whitelist (Rules tab), which are
    saved and survive restarts.
- Blocked domains get an instant NXDOMAIN answer. Everything else is
  resolved through 8.8.8.8 / 1.1.1.1 and answered normally.
- The Log tab shows every inspected domain, what was blocked, and why.
- The Log tab's **Ad Beacons** view flags every call your apps make to
  known ad-network verification/telemetry hosts (UnityAds, AppLovin,
  Vungle, ironSource, AdMob…) — the pings that report ad impressions,
  completions and skips back to the network. You can see that a beacon
  fired, when, and whether it was blocked. What the beacon *reported*
  travels inside encrypted HTTPS, so no DNS tool can read or rewrite it.

## Honest limits

- DNS filtering can't block ads served from the same domain as the
  content (e.g. YouTube's own ad servers), and apps that hard-code their
  own encrypted DNS (DoH) can bypass any DNS blocker.
- Blocking a domain that an app needs to function can break that feature;
  whitelist it in the Rules tab if so.

## Building

```sh
./gradlew assembleRelease
```

Release signing reads `KEYSTORE_PATH`, `STORE_PASSWORD`, and
`KEY_PASSWORD` from the environment. GitHub Actions builds every push to
`main` and publishes a release (with the APK attached) for `v*` tags.
