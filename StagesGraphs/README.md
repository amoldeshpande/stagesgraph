# Stages Graphs

## Version 1.3 heart-rate zones and users

Tap **User → Add user**, enter a username, and enter the starting values for zones 2–5. Zone 1 is below the first value; zone 5 includes everything at or above the final value. The four values must increase. No example values are saved by default. Without a selected profile, the heart-rate graph remains unshaded.

Select a saved username from **User**, or choose **Edit zones** to change that user's boundaries. Names are matched without case sensitivity. Profiles and the last selected user are stored locally and restored on app restart. They do not require Android user accounts or an internet connection. Clearing app data or uninstalling deletes the profiles.

The heart-rate plot has five fixed horizontal bands: gray, blue, green, orange, and red, labeled Z1–Z5. The y-axis frames the configured zones and expands to include readings outside that range. The current zone appears beside the connection status. Power has no zone shading. User selection and editing are disabled during a workout; the workout's zone settings remain attached to its graph after Stop, until the next Start.

## Version 1.2 workouts

Choose sensors with **Heart rate** and **Power**. Press **Start** to clear the previous workout and begin plotting; press **Stop** to freeze the graphs, elapsed time, current values, and averages. Pressing Start again begins a new workout. Sensor connections remain available between workouts.

Both graphs span the whole workout, expanding their time axes as it progresses. No history is discarded at five minutes. Current and average figures appear above each graph; heart rate has no unit suffix and power uses W. Graphs sample at roughly one-second intervals. Averages are time-weighted from valid sensor readings, excluding missing data. Zero power is included. Readings expire after five seconds without updates, and missing intervals appear as gaps.

Leaving the app pauses sensor collection but keeps the workout clock running. Returning reconnects the selected sensors and resumes collection, with a gap for missing data. Only Stop finishes the workout. Closing or restarting the app loses the in-memory workout. No demo or separate clear button is provided.

## Version 1.1 text fix

The first Windows build interpreted UTF-8 Java source as Windows-1252. For example, the bullet U+2022 became three characters U+00E2, U+20AC, U+00A2 in the compiled class. Both build paths now explicitly use UTF-8. The portable build also checks the actual compiled Unicode strings. This check rejected the original build and passed version 1.1. Install the new APK over the previous app; the application ID and signing key are unchanged, and the version code increased to 2.

Native Android 5.1.1 (API 22) landscape app with heart-rate and cycling-power workout graphs. Uses Android's Bluetooth LE APIs with no third-party application libraries. No account, network permission, or cloud service is used by the app.

## Install on the bike

1. Copy `StagesGraphs.apk` to the console using USB or another file-transfer method available on the console.
2. On Android 5.1.1, enable **Settings > Security > Unknown sources** if required.
3. Open the APK in the console's file manager and install it. Open **Stages Graphs**.
4. Wake the power meter by pedaling and put on the heart-rate strap. Disconnect other apps that may hold the sensors' Bluetooth connections.
5. Tap **Heart rate**, then choose the heart-rate sensor. Tap **Power**, then choose the bike's power meter. Scanning lasts 15 seconds and shows all nearby BLE devices, including unsupported devices. An unsupported selection displays an explanation.
6. Press **Start** to begin the workout; press **Stop** to finish.

No recent reading is shown as a dash after five seconds; gaps in the data are not joined into a continuous line. Graph value axes expand when needed. Negative power values are supported.

The app keeps the screen awake while open. Moving it into the background disconnects sensors and stops scanning. While the app stays open, lost connections retry with a delay capped at 30 seconds. Sensor selections are not saved across app restarts. History is held only in memory and is not exported.

## Compatibility boundaries

The console must support Bluetooth **Low Energy central/GATT** connections. General Bluetooth support alone does not establish this. The power meter must expose the standard Cycling Power service (0x1818, measurement 0x2A63), and the heart-rate sensor the Heart Rate service (0x180D, measurement 0x2A37). Proprietary Stages protocols, Bluetooth Classic, ANT+, and FTMS-only bikes are not implemented in this version. Actual radio operation requires a test on the console and sensors.

This APK targets Android 5.1.1 specifically. It does not implement the newer runtime Bluetooth/location permission flows required for newer Android releases. The build produces a development-signed APK for direct installation; retain the local build keystore if you want future APKs to update the existing installation.

## Build from source (Windows)

Run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build.ps1` from this folder. The script downloads Microsoft OpenJDK 17, Gradle 8.7, and Android SDK command-line tools; accepts the SDK component licenses; installs API 28/build tools; runs nine Bluetooth payload checks; and builds the debug APK. Compilation with SDK 28 does not change the app's minimum/target runtime of API 22.

All explicit download, cache, SDK, Java, temporary, and build paths are inside this project under the requested output directory. The APK is copied to the parent output directory as `StagesGraphs.apk`. Build tools and caches can consume several GB. Initial compilation needs internet access.

If the native Android resource linker crashes, the script falls back to `build-portable.ps1`. This requires Node.js on PATH and is appropriate for this resource-free app: it compiles the same Java sources, desugars them with Android's D8, encodes the source manifest as binary XML using SDK attribute IDs, packages the APK, and signs it with Android's APK signer. The SDK manifest reader verifies that the resulting binary manifest is readable. The fallback manifest version/minimum SDK values match the Gradle configuration; update both if changing them later. This development package uses compressed DEX and contains no native libraries; it does not require native library page alignment.

## Validation of this build

- Nine Bluetooth packet decoding checks passed.
- Fifteen workout checks passed, including time-weighted averages, missing data, frozen stopped values, restart reset, and retaining a two-hour workout.
- Zone/profile checks cover exact boundaries, independent users, saving/restoring the selected user and Unicode usernames, editing, invalid input, and damaged records.
- Compiled Unicode text regression checks passed.
- Java compilation and Android DEX conversion completed.
- APK signature verification passed, including the v1 signature needed by Android 5.1.1.
- Android SDK manifest inspection confirmed the launcher activity, Bluetooth permissions, landscape orientation, and API 22 minimum/target.
- Native Windows Android resource linkers crashed on this computer; the delivered APK uses the portable packaging path above.
- An Android API 22 emulator and system image were installed inside the output directory for version 1.1 testing. The emulator's acceleration check returned code 6 (Windows Hypervisor Platform needs enabling). Software CPU emulation was attempted, but Android did not reach a usable boot or expose its device port. No app screenshot or emulator installation check was obtained. No Windows features or drivers were installed or changed. Installation, UI rendering, and Bluetooth connections still require testing on your console.

## Emulator work

The emulator and API 22 image have been restored in the current checkout. `run-emulator.ps1` now opens a visible window by default; `-Headless` hides it and `-CheckAcceleration` runs the hardware check only. The current restricted command runner receives Access Denied from Windows' hypervisor capability API, and the emulator still exits with an acceleration error there. This does not establish the state of the feature outside that runner.

To retry from an ordinary Windows PowerShell window, run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\amol\Documents\Codex\2026-11-04\i-n\outputs\stagesgraph\StagesGraphs\preview-on-desktop.ps1"`. This checks hardware acceleration, opens the emulator, waits for boot, installs the preview, and captures `outputs/emulator-preview.png`. No administrator rights are requested by the launcher; if the ordinary-process acceleration check also fails, Windows/firmware configuration still needs investigation.

`build-portable.ps1 -Preview` builds an emulator-only APK with instrumentation that supplies synthetic heart-rate and power readings and a clearly labeled preview banner. That instrumentation is excluded from the normal build and the bike APK. It creates a local Preview user with illustrative zone boundaries only in the emulator. The preview is built and signature-verified, and has now been installed and run successfully on the API 22 emulator launched from desktop PowerShell. A screenshot is saved as outputs/emulator-preview.png. The direct ADB transfer helper was fixed to ignore packets after closing a completed transfer.

`run-emulator.ps1` configures a 1280 x 800 Android 5.1/API 22 virtual device with all emulator data under `.tools`. It expects the downloaded emulator extracted into `.tools/emulator-manual` and the API 22 x86_64 system image extracted into `.tools/image22`. Those large downloads are retained locally but excluded from the source ZIP. The default launch uses available hardware acceleration; `-SoftwareCpu` attempts software emulation. On this computer, neither produced a booted Android device. The launch diagnostics are in `emulator-run.log`.

The local-only `checks/emulator-device.js` helper is intended to communicate directly with an unsecured SDK emulator on port 5557, without a host ADB server or Windows profile key files. It could not be exercised because the guest never exposed that port; it is not used to build or run the app on the bike.

## Reference

Android Bluetooth GATT notification API: https://developer.android.com/reference/android/bluetooth/BluetoothGatt

Bluetooth SIG assigned service/characteristic numbers: https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/index-en.html
