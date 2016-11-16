package com.github.btrekkie.programmatic_image;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** Caches different subrectangles of an image at different zooms, e.g. an IProgrammaticImageRenderer image. */
/* The cache uses an LRU eviction policy.  That is, when it needs to make space, it removes the image least recently
 * accessed.
 */
public class ProgrammaticImageCache {
    /** The maximum number of image pixels to store in the cache. */
    private int maxPixelCount = 10000000;

    /** A map from each region stored in the cache to the result for the region. */
    private Map<ProgrammaticImageRect, ProgrammaticImageResult> results =
        new HashMap<ProgrammaticImageRect, ProgrammaticImageResult>();

    /** The most recently accessed result in the cache, if any. */
    private ProgrammaticImageResult head;

    /** The least recently accessed result in the cache, if any. */
    private ProgrammaticImageResult tail;

    /** The number of image pixels in the cache. */
    private int pixelCount;

    public void clear() {
        results.clear();
        head = null;
        tail = null;
        pixelCount = 0;
    }

    /** Removes the "tail" entry from the cache. */
    private void removeTail() {
        results.remove(tail.rect);
        pixelCount -= tail.rect.width * tail.rect.height;
        tail = tail.prev;
        if (tail != null) {
            tail.next = null;
        } else {
            head = null;
        }
    }

    public void setMaxPixelCount(int maxPixelCount) {
        this.maxPixelCount = maxPixelCount;
        while (pixelCount > maxPixelCount) {
            removeTail();
        }
    }

    /**
     * Moves the specified entry to the head of the cache, making it the most recently accessed entry.  Assumes the
     * entry is currently in the cache.
     */
    private void moveToHead(ProgrammaticImageResult result) {
        if (result.prev != null) {
            if (result.next != null) {
                result.next.prev = result.prev;
            } else {
                tail = result.prev;
            }
            result.prev.next = result.next;
            result.prev = null;
            result.next = head;
            head.prev = result;
            head = result;
        }
    }

    /**
     * Adds the specified result to the cache.
     * @param image The image result.
     * @param rect The rectangular region for the image.
     */
    public void addResult(BufferedImage image, ProgrammaticImageRect rect) {
        int size = rect.width * rect.height;
        if (size <= maxPixelCount) {
            ProgrammaticImageResult result = results.get(rect);
            if (result != null) {
                moveToHead(result);
            } else {
                while (pixelCount + size > maxPixelCount && tail != null) {
                    removeTail();
                }
                result = new ProgrammaticImageResult(image, rect);
                results.put(rect, result);
                result.next = head;
                if (head != null) {
                    head.prev = result;
                } else {
                    tail = result;
                }
                head = result;
                pixelCount += size;
            }
        }
    }

    /** Returns the cached image for the specified region.  Returns null if there is no cache entry for the region. */
    public BufferedImage result(ProgrammaticImageRect rect) {
        ProgrammaticImageResult result = results.get(rect);
        if (result == null) {
            return null;
        } else {
            moveToHead(result);
            return result.image;
        }
    }
}
