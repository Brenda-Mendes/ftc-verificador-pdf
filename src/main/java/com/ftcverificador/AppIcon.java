package com.ftcverificador;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

public final class AppIcon {

    private static final int[] ICON_SIZES = {
            16, 24, 32, 48, 64, 128, 256
    };

    private AppIcon() {
    }

    public static List<Image> loadImages() {
        List<Image> images = new ArrayList<>();

        for (int size : ICON_SIZES) {
            String resource = "/icons/app-icon-" + size + ".png";

            try (InputStream input =
                         AppIcon.class.getResourceAsStream(resource)) {

                if (input != null) {
                    images.add(ImageIO.read(input));
                }
            } catch (IOException ignored) {
                // A aplicação continua funcionando mesmo se um tamanho falhar.
            }
        }

        return images.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(images);
    }
}

