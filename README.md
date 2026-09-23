# GhostBlocker 2.0

Standalone Android APK for fixed ghost/touch-blocking areas.

## What changed

- Python controller removed.
- ADB broadcasts removed from normal runtime.
- `ghost_areas.json` is bundled inside the APK as fixed configuration.
- `ghost_state.txt` removed; state is stored internally with SharedPreferences.
- Area list/add/edit/delete/wizard UI removed.
- The APK owns the BLACK/TRANSPARENT/OFF state.
- Transparent mode still intercepts touches; the area is invisible but remains clickable.
- Black mode shows the same fixed rectangles for visual verification.
- OFF removes all overlays.
- Foreground service restores the saved mode after service recreation when overlay permission is available.

## Fixed areas

The current bundled configuration is:

```json
[
  {"name":"kiri1","x":0,"y":655,"w":620,"h":100},
  {"name":"kiri2","x":150,"y":490,"w":60,"h":50},
  {"name":"kiri3","x":100,"y":485,"w":50,"h":50},
  {"name":"kanan1","x":465,"y":1070,"w":100,"h":100}
]
```

Coordinates are intentionally absolute pixels to preserve the behavior of the original configuration. No automatic density/resolution scaling is applied.

## Build on Windows

Requirements:

- JDK 17+
- Android SDK with platform 35 and build-tools 35.0.0
- PowerShell 5.1+ / PowerShell 7+

The script can download Gradle 8.11.1 automatically if `gradle` is not already installed.

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\build-release.ps1
```

Build and install:

```powershell
.\build-release.ps1 -Install -Launch
```

The resulting installable debug APK is:

`release/GhostBlocker.apk`

## Runtime

1. Install the APK.
2. Grant **Display over other apps**.
3. Open GhostBlocker.
4. Use **SHOW · BLACK** to verify the fixed rectangles.
5. Use **ACTIVATE · TRANSPARENT** for normal operation.
6. Use **CLEAR / OFF** to remove them.

No Python script or ADB command is required after installation.
