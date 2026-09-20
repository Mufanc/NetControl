#!/usr/bin/env just --justfile

package-module variant="release": (build variant)
    #!/bin/sh
    set -eu
    rm -rf build/module build/outputs/netc-{{variant}}.zip
    cp -R module build/module
    mkdir -p build/module/bin build/outputs
    set -- daemon/build/outputs/apk/{{variant}}/*.ash
    [ "$#" -eq 1 ] && [ -f "$1" ]
    cp "$1" build/module/bin/netc-daemon.ash
    cd build/module && zip -r ../outputs/netc-{{variant}}.zip .
    rm -rf build/module

build variant="debug": webui
    #!/bin/sh
    set -eu
    case "{{variant}}" in
        debug) task=assembleDebug ;;
        release) task=assembleRelease ;;
        *) echo "Unknown variant: {{variant}}" >&2; exit 1 ;;
    esac
    ./gradlew --no-daemon ":daemon:$task"

webui:
    pnpm --dir webui install --frozen-lockfile
    pnpm --dir webui build

clean:
    rm -rf build daemon/build hiddenapi/build webui/node_modules module/webroot/assets
