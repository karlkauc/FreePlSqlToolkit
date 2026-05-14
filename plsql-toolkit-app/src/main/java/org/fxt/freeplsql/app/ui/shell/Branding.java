package org.fxt.freeplsql.app.ui.shell;

import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Static factory for brand resources (icons + logos). */
public final class Branding {

    public static final int[] ICON_SIZES = {16, 32, 64, 128, 256, 512};

    private Branding() {}

    /** Returns {@link Image} instances for every bundled icon size. */
    public static List<Image> stageIcons() {
        List<Image> out = new ArrayList<>(ICON_SIZES.length);
        for (int size : ICON_SIZES) {
            String path = "/branding/icon-" + size + ".png";
            var url = Branding.class.getResource(path);
            if (url == null) continue;
            out.add(new Image(Objects.requireNonNull(url).toExternalForm(),
                    size, size, true, true));
        }
        return out;
    }

    /** Returns the closest-sized icon PNG as an Image, scaled to the requested size. */
    public static Image markImage(int requestedSize) {
        int size = nearest(requestedSize);
        var url = Objects.requireNonNull(
                Branding.class.getResource("/branding/icon-" + size + ".png"),
                "icon-" + size + ".png not found");
        return new Image(url.toExternalForm(), requestedSize, requestedSize, true, true);
    }

    private static int nearest(int requested) {
        int best = ICON_SIZES[0];
        for (int s : ICON_SIZES) {
            if (Math.abs(s - requested) < Math.abs(best - requested)) best = s;
        }
        return best;
    }
}
