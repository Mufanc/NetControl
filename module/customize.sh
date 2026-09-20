[ "$API" -ge 33 ] || abort "Android 13 or newer is required"

set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
set_perm_recursive "$MODPATH/bin" 0 0 0755 0755

# KernelSU sets the webroot permissions and labels itself.
