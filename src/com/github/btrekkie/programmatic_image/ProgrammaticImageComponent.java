package com.github.btrekkie.programmatic_image;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 * A JComponent that renders an IProgrammaticImageRenderer image.  ProgrammaticImageComponent performs best if
 * IProgrammaticImageRenderer.render terminates in a timely fashion when its thread is interrupted, as in
 * Thread.interrupted().
 */
/* ProgrammaticImageComponent maintains a cache of BufferedImages for different subrectangles of the image at different
 * levels of zoom.  We use background threads to render the image at visible rectangle, but we render it in pieces.  The
 * pieces tile the underlying image into imageWidth x imageHeight images, starting at the origin, where
 * imageWidth x imageHeight is (normally) somewhat smaller than the visible rectangle.  At any given moment, for each
 * tile that overlaps the visible rectangle, either the cache contains the image for the tile, or we are computing the
 * image in a background thread.  By making the tiles smaller than the visible rectangle, we can display different
 * portions of the image as they become available.  This is useful if rendering takes a long time.  We also compute
 * images for tiles that are just outside the visible rectangle, so that if the ProgrammaticImageComponent is in a
 * scroll pane and the user scrolls slowly enough, we will always be able to draw the entire visible rectangle.
 */
public class ProgrammaticImageComponent extends JComponent {
    /**
     * The amount by which to multiply the size of the visible rectangle to obtain a minimum value of
     * cache.maxPixelCount.
     */
    private static final double MAX_PIXEL_COUNT_MULT = 10;

    /** The minimum value of cache.maxPixelCount. */
    private static final int MIN_MAX_PIXEL_COUNT = 10000000;

    /**
     * The minimum factor by which the desired value of imageWidth or imageHeight must change, as suggested by
     * CACHE_IMAGES_PER_AXIS and MIN_IMAGE_SIZE, before changing the image size.
     */
    private static final double MIN_SIZE_CHANGE_MULT = 1.8;

    /** The maximum number of cache images that fit in the visible rectangle in the vertical or horizontal direction. */
    private static final double CACHE_IMAGES_PER_AXIS = 6;

    /** The minimum values of imageWidth and imageHeight. */
    private static final int MIN_IMAGE_SIZE = 100;

    private static final long serialVersionUID = 9212293386643020026L;

    /** The IProgrammaticImageRenderer whose image we are rendering. */
    private final IProgrammaticImageRenderer renderer;

    /** The cache for storing different portions of the underlying image. */
    private ProgrammaticImageCache cache;

    /** The ProgrammaticImageWorkers that are currently rendering different portions of the image. */
    private Map<ProgrammaticImageRect, ProgrammaticImageWorker> workers =
        new HashMap<ProgrammaticImageRect, ProgrammaticImageWorker>();

    /** The factor by which we are scaling the image. */
    private double scale = 1;

    /** Whether the component has been "initialized", or properly added to the hierarchy of some frame or window. */
    private boolean hasInitialized = false;

    /**
     * The width of the images we store in "cache" (although the images at the far right of "renderer" might be narrower
     * than this).
     */
    private int imageWidth = 1;

    /**
     * The height of the images we store in "cache" (although the images at the bottom of "renderer" might be shorter
     * than this).
     */
    private int imageHeight = 1;

    public ProgrammaticImageComponent(IProgrammaticImageRenderer renderer) {
        this.renderer = renderer;
        cache = new ProgrammaticImageCache();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                handleVisibleRectChanged();
            }
        });
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorMoved(AncestorEvent event) {
                handleVisibleRectChanged();
            }

            @Override
            public void ancestorAdded(AncestorEvent event) {
                handleVisibleRectChanged();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                handleVisibleRectChanged();
            }
        });
    }

    /**
     * Returns the regions of the image we should make sure are available, given the current visible rectangle and level
     * of zoom.
     */
    private Collection<ProgrammaticImageRect> rects() {
        Collection<ProgrammaticImageRect> rects = new ArrayList<ProgrammaticImageRect>();
        Rectangle visibleRect = getVisibleRect();
        Dimension preferredSize = getPreferredSize();
        int minX = Math.max(0, visibleRect.x / imageWidth * imageWidth - imageWidth);
        int maxX = Math.min((int)preferredSize.getWidth(), visibleRect.x + visibleRect.width + imageWidth);
        int minY = Math.max(0, visibleRect.y / imageHeight * imageHeight - imageHeight);
        int maxY = Math.min((int)preferredSize.getHeight(), visibleRect.y + visibleRect.height + imageHeight);
        for (int y = minY; y < maxY; y += imageHeight) {
            for (int x = minX; x < maxX; x += imageWidth) {
                rects.add(
                    new ProgrammaticImageRect(
                        scale, x, y, Math.min(imageWidth, (int)preferredSize.getWidth() - x),
                        Math.min(imageHeight, (int)preferredSize.getHeight() - y)));
            }
        }
        return rects;
    }

    /**
     * Responds to a (potential) change in what we are displaying - either a change in the visible rectangle or the
     * level of zoom.
     */
    private void handleVisibleRectOrScaleChanged() {
        setPreferredSize(
            new Dimension((int)Math.ceil(renderer.width() * scale), (int)Math.ceil(renderer.height() * scale)));
        revalidate();

        // Cancel any workers for regions we are no longer interested in
        Set<ProgrammaticImageRect> rects = new LinkedHashSet<ProgrammaticImageRect>(rects());
        Map<ProgrammaticImageRect, ProgrammaticImageWorker> newWorkers =
            new HashMap<ProgrammaticImageRect, ProgrammaticImageWorker>();
        for (Entry<ProgrammaticImageRect, ProgrammaticImageWorker> entry : workers.entrySet()) {
            ProgrammaticImageRect rect = entry.getKey();
            ProgrammaticImageWorker worker = entry.getValue();
            if (rects.contains(rect)) {
                newWorkers.put(rect, worker);
            } else {
                worker.cancel(true);
            }
        }

        // Start workers for regions we are newly interesed in
        for (ProgrammaticImageRect rect : rects) {
            if (cache.result(rect) == null && !newWorkers.containsKey(rect)) {
                ProgrammaticImageWorker worker = new ProgrammaticImageWorker(renderer, rect, this);
                worker.execute();
                newWorkers.put(rect, worker);
            }
        }
        workers = newWorkers;
    }

    /**
     * Sets the factor by which we are scaling the image.
     *
     * Note that ProgrammaticImageComponent may use the exact scale factor value as part of a caching scheme.  Thus, if
     * the user zooms in and then zooms back out by the same amount, we should make sure to pass the same value for
     * "scale".  It would not be sufficient if zooming in multiplied the scale factor by some amount, and zooming out
     * divided the scale factor by this amount.  Due to floating point imprecision, multiplying by a certain amount and
     * then dividing by the same amount may result in a different value.
     */
    public void setScale(double scale) {
        if (scale != this.scale) {
            this.scale = scale;
            if (hasInitialized) {
                repaint();
                handleVisibleRectOrScaleChanged();
            }
        }
    }

    /** Responds to a (potential) change in the visible rectangle. */
    private void handleVisibleRectChanged() {
        if (!hasInitialized) {
            hasInitialized = true;
            addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
                @Override
                public void ancestorResized(HierarchyEvent e) {
                    handleVisibleRectChanged();
                }
            });
        }

        // Update imageWidth and imageHeight
        Rectangle visibleRect = getVisibleRect();
        int targetWidth = Math.max(MIN_IMAGE_SIZE, (int)(visibleRect.width / CACHE_IMAGES_PER_AXIS));
        int targetHeight = Math.max(MIN_IMAGE_SIZE, (int)(visibleRect.height / CACHE_IMAGES_PER_AXIS));
        if (targetWidth < imageWidth / MIN_SIZE_CHANGE_MULT || targetWidth > MIN_SIZE_CHANGE_MULT * imageWidth ||
                targetHeight < imageHeight / MIN_SIZE_CHANGE_MULT ||
                targetHeight > MIN_SIZE_CHANGE_MULT * imageHeight) {
            imageWidth = targetWidth;
            imageHeight = targetHeight;

            // Ideally, we would use the existing cache entries to construct new imageWidth x imageHeight cache entries
            cache.clear();
        }

        cache.setMaxPixelCount(
            Math.max(MIN_MAX_PIXEL_COUNT, (int)(MAX_PIXEL_COUNT_MULT * getWidth() * getHeight())));
        handleVisibleRectOrScaleChanged();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        for (ProgrammaticImageRect rect : rects()) {
            BufferedImage image = cache.result(rect);
            if (image != null) {
                graphics.drawImage(image, rect.x, rect.y, null);
            }
        }
    }

    /** Stores the specified BufferedImage as the result of rendering the specified region. */
    void handleResult(BufferedImage image, ProgrammaticImageRect rect) {
        cache.addResult(image, rect);
        workers.remove(rect);
        repaint(rect.x, rect.y, rect.width, rect.height);
    }
}
