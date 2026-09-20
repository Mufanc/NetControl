#!/system/bin/sh
MODDIR=${0%/*}

umask 077

rm -f "$MODDIR/endpoint"
nohup sh "$MODDIR/bin/netc-daemon.ash" "$MODDIR" >> "$MODDIR/daemon.log" 2>&1 < /dev/null &
