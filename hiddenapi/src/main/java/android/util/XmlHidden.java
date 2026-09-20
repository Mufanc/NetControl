package android.util;

import com.android.modules.utils.TypedXmlPullParser;

import java.io.IOException;
import java.io.InputStream;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(Xml.class)
public final class XmlHidden {
    public static TypedXmlPullParser resolvePullParser(InputStream input) throws IOException {
        throw new RuntimeException("Stub!");
    }
}
