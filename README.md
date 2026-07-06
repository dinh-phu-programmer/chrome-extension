# Open Folder — Chrome Extension

A Chrome extension that opens any Windows folder in Explorer directly from the browser. Supports manual path input, a native folder picker dialog, and recent folder history.

---

## Prerequisites

| Requirement | Minimum version | Check |
|---|---|---|
| Google Chrome | any recent | `chrome://version` |
| Java JDK | 11+ | `java -version` and `javac -version` |

> **Note:** JRE alone is not enough for the build step — you need the full JDK (which includes `javac` and `jar`).

---

## Project Structure

```
chrome-plugin/
├── extension/              Chrome extension source
│   ├── manifest.json
│   ├── popup.html
│   ├── popup.js
│   └── background.js
└── native-host/            Java native messaging host
    ├── NativeHost.java     Source code
    ├── build.bat           Compiles and packages the JAR
    ├── install.bat         Registers the host in Windows registry
    └── open_folder_host.bat  Launcher called by Chrome
```

---

## Installation

### Step 1 — Build the Java native host

Open `native-host\` in File Explorer and run **`build.bat`** as Administrator, or run it in a terminal:

```cmd
cd native-host
build.bat
```

This compiles `NativeHost.java` and produces `NativeHost.jar` in the same folder.

**Expected output:**
```
Compiling NativeHost.java...
Packaging NativeHost.jar...
[OK] NativeHost.jar built successfully.
```

If you see an error like `'javac' is not recognized`, Java JDK is not on your PATH. Install the JDK and make sure its `bin\` folder is in your system `PATH` environment variable.

---

### Step 2 — Load the extension in Chrome

1. Open Chrome and go to: `chrome://extensions`
2. Enable **Developer mode** using the toggle in the top-right corner.
3. Click **Load unpacked**.
4. Select the `extension\` folder from this project.
5. The extension appears in the list. **Copy the Extension ID** — it looks like:

```
abcdefghijklmnopqrstuvwxyz123456
```

![Extension ID location](https://i.imgur.com/placeholder.png)

> The Extension ID is shown directly below the extension name and description in the card.

---

### Step 3 — Register the native host

Double-click **`native-host\install.bat`** (or right-click → Run as administrator if it fails).

```cmd
cd native-host
install.bat
```

When prompted, paste the Extension ID you copied in Step 2 and press Enter.

**What the script does:**
- Writes the correct paths into `open_folder_host.json`
- Adds a registry entry under `HKCU\Software\Google\Chrome\NativeMessagingHosts\com.example.open_folder`

**Expected output:**
```
Paste your Extension ID here: abcdefghijklmnopqrstuvwxyz123456

Manifest written to: ...\open_folder_host.json
The operation completed successfully.

SUCCESS! Native host registered.
```

---

### Step 4 — Reload the extension

1. Go back to `chrome://extensions`.
2. Find **Open Folder** and click the **reload icon** (circular arrow).
3. This is required after the native host is registered.

---

### Step 5 — Test

1. Click the puzzle-piece icon in the Chrome toolbar and pin **Open Folder**, or click it directly.
2. The popup opens with a path input field.
3. Click **Browse** — a native Windows folder picker dialog appears.
4. Select any folder. The full absolute path (e.g. `D:\Projects\myapp`) fills the input.
5. Click **Open in Explorer** — Windows Explorer opens at that folder.

---

## Usage

| Action | How |
|---|---|
| Browse for a folder | Click **Browse** — native OS dialog |
| Open a known path | Type or paste the path, press **Enter** or click **Open in Explorer** |
| Reopen a recent folder | Click any entry in the **Recent** list |
| Remove from history | Click **×** next to any recent entry |

---

## Troubleshooting

### "Native host not connected"

The native messaging host is not registered or the extension was not reloaded after registration.

**Fix:**
1. Re-run `install.bat` and confirm the Extension ID matches exactly.
2. Reload the extension in `chrome://extensions`.
3. Verify the registry key exists:
   ```cmd
   reg query "HKCU\Software\Google\Chrome\NativeMessagingHosts\com.example.open_folder"
   ```
   The value should be the full path to `open_folder_host.json`.

---

### "Path does not exist"

The path typed manually does not exist on this machine.

**Fix:** Use the **Browse** button to pick an existing folder, or double-check the typed path.

---

### Browse dialog does not appear / appears behind Chrome

This is a rare Swing focus issue on some Windows configurations.

**Fix:** Click anywhere on the Windows desktop first, then click Browse again. The dialog will appear on top.

---

### Build fails — `'javac' is not recognized`

JDK is not installed or not on PATH.

**Fix:**
1. Download and install [Eclipse Temurin JDK 21](https://adoptium.net/) (LTS).
2. During installation, check **"Add to PATH"**.
3. Open a new terminal and verify: `javac -version`
4. Re-run `build.bat`.

---

### Native host crashes silently

Check the log file generated next to the JAR:

```
native-host\native_host.log
```

This contains any JVM errors or stack traces from the native host process.

---

## How it works

Chrome extensions cannot access the local filesystem or launch native applications directly — browser security blocks this. This extension uses [Chrome Native Messaging](https://developer.chrome.com/docs/apps/nativeMessaging/) to bridge the gap:

```
Chrome popup → background.js → Native Messaging → NativeHost.jar → explorer.exe / JFileChooser
```

1. The extension sends a JSON message to `NativeHost.jar` via stdin/stdout.
2. For **Browse**: the JAR opens a `JFileChooser` dialog and returns the selected absolute path.
3. For **Open**: the JAR calls `explorer.exe` with the given path.
4. The full Windows path (`D:\...`) is available because the JAR runs as a native process with full filesystem access.
