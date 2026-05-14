package org.fxt.freeplsql.app.ui.shell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FontLoaderTest {

    @Test
    void resourcePathsAreDeclared() {
        assertEquals(3, FontLoader.RESOURCE_PATHS.size(),
                "Expect 3 bundled fonts");
        for (String path : FontLoader.RESOURCE_PATHS) {
            assertTrue(path.startsWith("/fonts/") && path.endsWith(".ttf"),
                    "Bad path: " + path);
            assertNotNull(FontLoader.class.getResource(path),
                    "Missing resource on classpath: " + path);
        }
    }
}
