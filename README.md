# NetControl

NetControl is a [KernelSU](https://kernelsu.org/) module for controlling network access on Android 13 and later. It uses Android's built-in OEM firewall chains to block selected apps and provides a WebUI for managing the policy.

## Features

- Block network access by Android app ID
- Apply one policy across every Android user and profile
- Discover installed apps and react to package or user changes
- Preserve the blacklist across reboots and module updates
- Manage apps from a token-protected, loopback-only WebUI

## Requirements

- Android 13 or later
- KernelSU with WebUI support
- An unused Android OEM deny firewall chain

NetControl uses one of the three OEM deny chains exposed by Android. It will not start if all three chains are already enabled, and a vendor service may still claim or modify the selected chain after startup.

## Installation

1. Download or build the module ZIP.
2. Install it from KernelSU's module screen.
3. Reboot the device.
4. Open NetControl from KernelSU and choose which apps may access the network.

Policies are stored by app ID, so packages sharing an app ID also share the same rule. Changes apply to new network connections; existing connections are not terminated.

## Building

The build requires JDK 17, Android SDK Platform 36, `pnpm`, [`just`](https://github.com/casey/just), and `zip`.

```sh
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
just package-module release
```

The module is written to `build/outputs/netc-release.zip`. Replace `release` with `debug` for a debug build.

## How it works

KernelSU starts a root `app_process` daemon during the module service stage. The daemon waits for Android's connectivity service, selects an unused OEM deny chain, restores the saved blacklist, and keeps the chain synchronized with installed apps and Android users.

The blacklist is stored atomically in `/data/adb/netc/blacklist.bin`. The firewall chain itself lives in Android's pinned system BPF maps and lasts only for the current boot. Removing the module clears its persisted data on the next boot; the active chain expires on reboot.

The management server listens on a random loopback port. Each daemon start creates a new authentication token and writes the complete URL to the module's mode `0600` `endpoint` file. KernelSU's WebUI bootstrap reads that file and redirects to the authenticated session.

## Repository layout

| Path | Purpose |
| --- | --- |
| `daemon` | Kotlin/AProc daemon, HTTP server, app registry, and firewall policy |
| `hiddenapi` | Compile-time stubs for Android hidden APIs |
| `webui` | Vue management interface source |
| `module` | KernelSU metadata, lifecycle scripts, and packaged WebUI |

## License

Released into the public domain under [The Unlicense](LICENSE).
