package com.github.btrekkie.programmatic_image;

import java.awt.image.BufferedImage;

/**
 * An entry in a ProgrammaticImageCache: a region of an IProgrammaticImageRenderer image along with the BufferedImage
 * result of rendering the region.
 */
public class ProgrammaticImageResult {
    /** The image result. */
    public final BufferedImage image;

    /** The rendered region of the underlying image. */
    public final ProgrammaticImageRect rect;

    /** The next most recently used cache entry, if any. */
    public ProgrammaticImageResult prev;

    /** The next least recently used cache entry, if any. */
    public ProgrammaticImageResult next;

    public ProgrammaticImageResult(BufferedImage image, ProgrammaticImageRect rect) {
        this.image = image;
        this.rect = rect;
    }
}
