package org.fxt.freeplsql.app.ui.shell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrandingTest {

    @Test
    void allIconSizesAreOnClasspath() {
        for (int size : Branding.ICON_SIZES) {
            String path = "/branding/icon-" + size + ".png";
            assertNotNull(Branding.class.getResource(path),
                    "Missing icon resource " + path);
        }
    }

    @Test
    void svgMarkIsOnClasspath() {
        assertNotNull(Branding.class.getResource("/branding/logo-mark.svg"));
        assertNotNull(Branding.class.getResource("/branding/logo-wordmark.svg"));
    }
}
