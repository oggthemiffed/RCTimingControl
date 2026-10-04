# Installing on a venue laptop

RC Timing Control installs as one package on the laptop that sits by the decoder. The package carries its own Java runtime, so the laptop needs no Java and no Docker. The app runs as a background service with no window and starts when the laptop boots. Officials' phones and tablets and the spectator boards open it in a browser over the venue network.

## What you need

- A laptop running Windows 10 or 11 (64-bit), macOS 12 or later, or a Debian-based Linux such as Ubuntu.
- The installer for that system:

| System | File |
|--------|------|
| Windows | `RCTimingControl-<version>.msi` |
| macOS | `RCTimingControl-<version>.pkg` |
| Linux | `rctimingcontrol_<version>_amd64.deb` |

To build an installer yourself, see [Building the installers](#building-the-installers).

## Install

**Windows.** Double-click the `.msi` file and follow the prompts. Windows may say it protected your PC, because the installer is not signed yet. Choose **More info**, then **Run anyway**. The installer also adds a Windows Firewall rule so other devices can connect.

**macOS.** The package is not signed yet, so double-clicking it shows a warning. Right-click the `.pkg` file, choose **Open**, then **Open** again. Or open **System Settings → Privacy & Security** and choose **Open Anyway**.

**Linux.** Run:

```bash
sudo apt install ./rctimingcontrol_<version>_amd64.deb
```

The service starts as soon as the install finishes.

## First run

1. On the laptop, open **http://localhost:8080** in a browser. The setup wizard asks for the first official's account, then the club, track and decoder details.
2. Open **http://localhost:8080/about** to see the addresses other devices should use, for example `http://192.168.1.50:8080/`. The same addresses are written to the log each time the app starts.
3. On a phone connected to the same Wi-Fi or network, open one of those addresses and sign in.

The address by the laptop's name, such as `http://timing-laptop:8080/`, works on most phones and keeps working if the laptop gets a different IP address. If it does not work on a phone, use the IP address instead.

If a phone cannot connect, check that it is on the same network as the laptop and not on mobile data. Then check the laptop's firewall. On macOS with the firewall turned on, allow incoming connections for RC Timing Control in **System Settings → Network → Firewall → Options**. On Linux with `ufw` enabled, run `sudo ufw allow 8080/tcp`.

## Where things are kept

The data lives outside the install folder, so installing a newer version keeps it.

| System | Data folder |
|--------|-------------|
| Windows | `C:\ProgramData\RCTimingControl` |
| macOS | `/Library/Application Support/RCTimingControl` |
| Linux | `/var/lib/rctimingcontrol` |

The folder holds:

- `rctiming.db`, the database;
- `backups/`, the automatic backups;
- `uploads/`, the club logo and announcer clips;
- `logs/rctiming.log`, the app's log;
- `jwt-secret`, the key that signs officials' sign-ins, created on first start. Keep it private.

## Changing settings

To change a setting such as the port, create a file called `application.properties` in the data folder, then restart the service. For example:

```properties
# Use port 80, so devices can open http://timing-laptop/ with no port number
server.port=80
# Keep backups on a USB stick
rctiming.backup.directory=E:/rctiming-backups
```

## Starting and stopping the service

| System | Stop | Start |
|--------|------|-------|
| Windows | `sc stop RCTimingControl` (or the Services app) | `sc start RCTimingControl` |
| macOS | `sudo launchctl bootout system/dev.monkeypatch.rctiming-RCTimingControl` | `sudo launchctl bootstrap system /Library/LaunchDaemons/dev.monkeypatch.rctiming-RCTimingControl.plist` |
| Linux | `sudo systemctl stop rctimingcontrol-RCTimingControl` | `sudo systemctl start rctimingcontrol-RCTimingControl` |

Run Windows commands from an administrator Command Prompt.

## Restoring a backup

Stop the service, then run the restore command as an administrator (Windows) or with `sudo` (macOS, Linux):

| System | Command |
|--------|---------|
| Windows | `"C:\Program Files\RCTimingControl\RCTimingControl.exe" restore "C:\ProgramData\RCTimingControl\backups\<file>"` |
| macOS | `sudo /Applications/RCTimingControl.app/Contents/MacOS/RCTimingControl restore "/Library/Application Support/RCTimingControl/backups/<file>"` |
| Linux | `sudo /opt/rctimingcontrol/bin/RCTimingControl restore /var/lib/rctimingcontrol/backups/<file>` |

Then start the service again.

## Upgrading

Install the newer package over the old one, the same way as the first install. The service stops, the new version is put in place, and the service starts again. The new version updates the database to its format when it starts.

## Uninstalling

Uninstall it the usual way for the system: **Settings → Apps** on Windows, or `sudo apt remove rctimingcontrol` on Linux. On macOS, run `sudo launchctl bootout system/dev.monkeypatch.rctiming-RCTimingControl`, then delete `/Library/LaunchDaemons/dev.monkeypatch.rctiming-RCTimingControl.plist` and `/Applications/RCTimingControl.app`. Uninstalling leaves the data folder in place. Delete it as well to remove everything.

## Building the installers

Each installer is built on the system it is for. You need JDK 21 and Node 20, and:

- **Windows:** the WiX Toolset 3, and the 64-bit `nssm.exe` from [NSSM 2.24](https://nssm.cc/download), which registers the service.
- **macOS:** the Xcode command line tools.
- **Linux:** `fakeroot` (`sudo apt install fakeroot`).

Then run:

```bash
./gradlew -PbundleFrontend :app:installer
# Windows also needs the path to nssm.exe:
#   gradlew -PbundleFrontend -PwindowsServiceInstaller=C:\tools\nssm\win64\nssm.exe :app:installer
```

The installer is written to `app/build/installer/out/`. The installer's version is the numeric part of `VERSION` (`0.1.0-alpha.1` builds `0.1.0`); pass `-PinstallerVersion=0.1.1` to set it. macOS does not accept versions starting with 0, so pre-1.0 macOS packages show a leading 1 (`0.1.0` shows as `1.1.0`). The About page always shows the real version.

`-PbundleFrontend` on its own builds a jar with the UI inside: `./gradlew -PbundleFrontend :app:bootJar`, then `java -jar app/build/libs/app.jar` serves everything on port 8080.
