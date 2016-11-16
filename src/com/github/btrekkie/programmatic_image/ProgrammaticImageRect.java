package com.github.btrekkie.programmatic_image;

/** A subrectangle region of an IProgrammaticImageRendererImage, along with a level of zoom. */
public class ProgrammaticImageRect {
    /** The scale factor. */
    public final double scale;

    /**
     * The x coordinate of the left edge of the subrectangle, as in the minX argument to
     * IProgrammaticImageRenderer.render.
     */
    public final int x;

    /**
     * The y coordinate of the top edge of the subrectangle, as in the minY argument to
     * IProgrammaticImageRenderer.render.
     */
    public final int y;

    /** The width of the subrectangle, as in the "width" argument to IProgrammaticImageRenderer.render. */
    public final int width;

    /** The height of the subrectangle, as in the "height" argument to IProgrammaticImageRenderer.render. */
    public final int height;

    public ProgrammaticImageRect(double scale, int x, int y, int width, int height) {
        this.scale = scale;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProgrammaticImageRect)) {
            return false;
        }
        ProgrammaticImageRect rect = (ProgrammaticImageRect)obj;
        return scale == rect.scale && x == rect.x && y == rect.y && width == rect.width && height == rect.height;
    }

    @Override
    public int hashCode() {
        return new Double(scale).hashCode() + 31 * x + 41 * y + 61 * width + 71 * height;
    }
}
