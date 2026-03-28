package net.sf.jsignpdf.fx.util;

import java.awt.image.BufferedImage;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

/**
 * Utility for converting between AWT BufferedImage and JavaFX Image.
 */
public final class SwingFxImageConverter {

    private SwingFxImageConverter() {
    }

    public static Image toFxImage(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return null;
        }
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }
}
