package com.github.btrekkie.reductions.planar;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 * Provides the ability to render an image to a Graphics2D so that the result looks good with a certain
 * Graphics2D.getTransform() transform.  In particular, this produces good results when the scale factor is less than 1.
 */
public class TransformedImageDrawer {
    /** The Graphics2D to which to render the image. */
    private final Graphics2D graphics;

    /** An version of the original image appropriately scaled for the Graphics2D.getTransform() transform. */
    private final BufferedImage scaledImage;

    /** The width of the original image. */
    private final int width;

    /** The height of the original image. */
    private final int height;

    /**
     * Constructs a new TransformedImageDrawer designed for the current return value of graphics.getTransform().
     * @param graphics The Graphics2D to which we render the image.
     * @param image The image.
     */
    public TransformedImageDrawer(Graphics2D graphics, BufferedImage image) {
        this.graphics = graphics;
        width = image.getWidth();
        height = image.getHeight();
        int scaledWidth = Math.max(1, Math.min(width, (int)(graphics.getTransform().getScaleX() * width + 0.5)));
        int scaledHeight = Math.max(1, Math.min(height, (int)(graphics.getTransform().getScaleY() * height + 0.5)));
        if (scaledWidth == width && scaledHeight == height) {
            scaledImage = image;
        } else {
            scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D scaledImageGraphics = scaledImage.createGraphics();
            scaledImageGraphics.drawImage(
                image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        }
    }

    /**
     * Renders the image to the Graphics2D, scaled to fit the axis-aligned rectangle with corners (x, y) and
     * (x + width, y + height) in the graphics coordinate space, where "width" and "height" are the width and height of
     * the image passed to the constructor respectively.  This is equivalent to graphics.drawImage(image, x, y, null),
     * where "graphics" and "image" are the arguments passed to the constructor, except the result looks better at the
     * intended transform.
     */
    public void draw(int x, int y) {
        if (scaledImage.getWidth() == width && scaledImage.getHeight() == height) {
            graphics.drawImage(scaledImage, x, y, null);
        } else {
            graphics.drawImage(scaledImage, x, y, width, height, null);
        }
    }
}
