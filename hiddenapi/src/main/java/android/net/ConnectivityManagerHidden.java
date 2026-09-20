package android.net;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(ConnectivityManager.class)
public class ConnectivityManagerHidden {
    public static final int FIREWALL_CHAIN_OEM_DENY_1 = 7;
    public static final int FIREWALL_CHAIN_OEM_DENY_2 = 8;
    public static final int FIREWALL_CHAIN_OEM_DENY_3 = 9;

    public boolean getFirewallChainEnabled(int chain) {
        throw new RuntimeException("Stub!");
    }

    public void replaceFirewallChain(int chain, int[] uids) {
        throw new RuntimeException("Stub!");
    }

    public void setFirewallChainEnabled(int chain, boolean enabled) {
        throw new RuntimeException("Stub!");
    }
}
