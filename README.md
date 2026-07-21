# mmhue-watch

A Wear OS app for [mmhue](https://github.com/SMZ70/mmhue) — control your Philips
Hue lights from your wrist.

Part of a small Hue trio:

- **[mmhue](https://github.com/SMZ70/mmhue)** — the controller: Telegram + web
  interfaces, scriptable light dances.
- **[mirrorball](https://github.com/SMZ70/mirrorball)** — a 50fps light
  sequencer over the Hue Entertainment API.
- **mmhue-watch** (this repo) — the Wear OS client for mmhue's panel API.

On/off, brightness and warmth for the lights, on your wrist.

It is deliberately not the web panel. Colour, scenes and dances are not here:
on a 1.4" screen they cost more than they return, and the panel already does
them well. This is for "turn the kitchen off" without finding your phone.

## What it does

- **Root screen** — `3 of 8 on`, then All off, then All on, then the rooms.
- **Room** — the lights in it. Flip the switch to toggle, tap the name to open.
- **Light** — on/off, and brightness on the **rotating crown**.

All off sits above All on. It is the button you reach for in a hurry, so it
should never require aiming past its opposite.

## Design notes

**Optimistic updates.** The web panel polls every 2.5s and treats that as truth.
On a watch that reads as lag: you tap, nothing moves, then a beat later the row
flips. Here a tap mutates local state immediately and the poll reconciles. A
failed request rolls back and says why.

**The crown, not a slider.** A slider on this screen means covering the value
with your thumb and hitting a target a few millimetres wide. The crown is a
physical dial you can work without looking. That is the whole argument for
putting this on a wrist.

**Brightness is debounced.** The crown emits a burst of events per turn and the
Hue bridge accepts roughly ten commands a second across all lights. The UI
follows every event; only the settled value is sent.

**Brightness floors at 20%.** Same as the panel. Below that the bulbs behave
inconsistently and some cut out entirely.

## Building

Everything installs into your home directory. Nothing needs root.

```sh
export JAVA_HOME=~/.local/opt/jdk-21.0.11+10
export ANDROID_HOME=~/Android/Sdk

~/.local/opt/gradle-8.13/bin/gradle testDebugUnitTest   # 13 tests, no device needed
~/.local/opt/gradle-8.13/bin/gradle assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Installing on the watch

The Pixel Watch charger is pogo-pin with no data lines, so ADB goes over Wi-Fi.
The watch, this machine and the Pi all need to be on the same network.

1. On the watch: **Settings → System → About → Versions**, tap **Build number**
   seven times to unlock Developer options.
2. **Settings → Developer options**, enable **ADB debugging** and
   **Debug over Wi-Fi**.
3. Under Debug over Wi-Fi the watch shows an IP and port, and a
   **Pair new device** entry with a separate pairing port and a 6-digit code.

```sh
adb pair <watch-ip>:<pairing-port>      # enter the 6-digit code
adb connect <watch-ip>:<debug-port>     # the port shown under Debug over Wi-Fi
adb devices                             # confirm it is listed
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The app appears in the watch's app list as **mmhue**.

## If it cannot reach the panel

The app talks to `http://mm.fritz.box` directly. It has no phone-side
companion, so the *watch itself* must resolve that name.

- Watch on mobile data instead of Wi-Fi is the usual cause. This app is
  home-network-only by design.
- If `mm.fritz.box` does not resolve on the watch but the Pi is reachable by
  address, change `MMHUE_BASE_URL` in `app/build.gradle.kts` to
  `http://192.168.178.50` and rebuild. Both hosts are already allowed in
  `network_security_config.xml`.

## Security

The panel runs with `MMHUE_WEB_OPEN` and no password, which is fine while it
stays on the LAN. This app assumes exactly that. If mmhue is ever exposed
beyond the home network it needs TLS and auth first, and the cleartext
exemptions in `network_security_config.xml` should go away.

## Tests

`MmhueClientTest` covers parsing and the endpoint paths. `RealPayloadTest`
parses a payload captured verbatim from the Pi and asserts the room/light
counts agree with each other. Re-capture it after changing mmhue:

```sh
curl -s http://mm.fritz.box/api/state > app/src/test/resources/real_state.json
```
