package com.github.btrekkie.programmatic_image;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * A rectangular image where we may render an aribtrary subrectangle at an arbitrary zoom.  This is useful in some cases
 * where BufferedImage is insufficient.  For example, this might be useful for images that support an arbitrary level of
 * detail as we zoom in, or for extremely large images that we cannot fit in main memory, but we can render in pieces.
 */
public interface IProgrammaticImageRenderer {
    /** The width of the image in units. */
    public double width();

    /** The height of the image in units. */
    public double height();

    /**
     * Renders a subrectangle of the image at the specified scale.  If the subrectangle lies outside of the image, the
     * method should render something neutral for those portions that lie outside the image, such as transparent pixels.
     * The return value is unspecified if the thread is interrupted, as in Thread.interrupted().
     * @param scale The factor by which to scale the image.
     * @param minX The x coordinate of the left edge of the subrectangle, after applying the scale.  Thus, the left edge
     *     of the subrectangle is really minX / scale.
     * @param minY The y coordinate of the top edge of the subrectangle, after applying the scale.  Thus, the top edge
     *     of the subrectangle is really minY / scale.
     * @param width The width of the resulting image in pixels.  This must be at least 1.  The width of the subrectangle
     *     is width / scale.
     * @param height The height of the resulting image in pixels.  This must be at least 1.  The width of the
     *     subrectangle is height / scale.
     * @return An image containing the results of the rendering.
     * @throws IOException If there was an I/O error loading any assets required to render the image.
     */
    public BufferedImage render(double scale, double minX, double minY, int width, int height) throws IOException;
}
