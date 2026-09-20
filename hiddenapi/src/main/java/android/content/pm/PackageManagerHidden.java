package android.content.pm;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(PackageManager.class)
public class PackageManagerHidden {
    public static final int MATCH_ANY_USER = stub();

    private static int stub() {
        throw new RuntimeException("Stub!");
    }
}
