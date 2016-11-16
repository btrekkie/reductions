package com.github.btrekkie.reductions.planar;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.btrekkie.programmatic_image.IProgrammaticImageRenderer;

/** An IProgrammaticImageRenderer for rendering layouts of IPlanarGadget. */
public abstract class PlanarGadgetRenderer implements IProgrammaticImageRenderer {
    /** The width and height of the buckets into which we divide the gadgets in cellToGadgets. */
    private static final int CELL_SIZE = 20;

    /**
     * A map from each gadget to its top-left corner.  The top-left corner of the bounding box must be (0, 0).  No pair
     * of gadgets may overlap.
     */
    private Map<IPlanarGadget, Point> gadgets;

    /**
     * A map from the top-left corner of each CELL_SIZE x CELL_SIZE tile that overlaps at least one gadget to the
     * gadgets it overlaps.
     */
    private Map<Point, Collection<IPlanarGadget>> cellToGadgets;

    /** The x coordinate of the right edge of the bounding box for the gadgets. */
    private final int maxX;

    /** The y coordinate of the bottom edge of the bounding box for the gadgets. */
    private final int maxY;

    public PlanarGadgetRenderer(Map<IPlanarGadget, Point> gadgets) {
        this.gadgets = gadgets;

        // Compute cellToGadgets
        cellToGadgets = new HashMap<Point, Collection<IPlanarGadget>>();
        for (Entry<IPlanarGadget, Point> entry : gadgets.entrySet()) {
            IPlanarGadget gadget = entry.getKey();
            Point point = entry.getValue();
            for (int y = point.y / CELL_SIZE * CELL_SIZE;
                    y <= (point.y + gadget.height() + CELL_SIZE - 1) / CELL_SIZE * CELL_SIZE; y += CELL_SIZE) {
                for (int x = point.x / CELL_SIZE * CELL_SIZE;
                        x <= (point.x + gadget.width() + CELL_SIZE - 1) / CELL_SIZE * CELL_SIZE; x += CELL_SIZE) {
                    Point cell = new Point(x, y);
                    Collection<IPlanarGadget> cellGadgets = cellToGadgets.get(cell);
                    if (cellGadgets == null) {
                        cellGadgets = new ArrayList<IPlanarGadget>();
                        cellToGadgets.put(cell, cellGadgets);
                    }
                    cellGadgets.add(gadget);
                }
            }
        }

        // Compute maxX and maxY
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Entry<IPlanarGadget, Point> entry : gadgets.entrySet()) {
            IPlanarGadget gadget = entry.getKey();
            Point point = entry.getValue();
            maxX = Math.max(maxX, point.x + gadget.width());
            maxY = Math.max(maxY, point.y + gadget.height());
        }
        this.maxX = maxX;
        this.maxY = maxY;
    }

    /** Returns the number of pixels per unit in the horizontal direction. */
    protected abstract double tileWidth();

    /** Returns the number of pixels per unit in the vertical direction. */
    protected abstract double tileHeight();

    /**
     * Renders the specified rectangular region.  The transformation for "graphics" is set so that (x, y) in the gadget
     * coordinate space corresponds to (tileWidth() * x, tileHeight() * y) in the graphics coordinate space.
     * @param graphics The Graphics2D object on which to perform the rendering.
     * @param gadgets A map from each gadget that intersects the region to the position of its top-left corner.
     * @param minX The x coordinate of the left edge of the region, in the gadget coordinate space.
     * @param minX The y coordinate of the top edge of the region, in the gadget coordinate space.
     * @param width The width of the region, in the gadget coordinate space.
     * @param height The height of the region, in the gadget coordinate space.
     * @throws IOException If there was an I/O error loading any assets required to render the image.
     */
    protected abstract void render(
        Graphics2D graphics, Map<IPlanarGadget, Point> gadgets, int minX, int minY, int width, int height)
        throws IOException;

    @Override
    public double width() {
        return tileWidth() * maxX;
    }

    @Override
    public double height() {
        return tileHeight() * maxY;
    }

    @Override
    public BufferedImage render(double scale, double minX, double minY, int width, int height) throws IOException {
        // Convert the bounds to gadget coordinates
        double tileWidth = tileWidth();
        double tileHeight = tileHeight();
        int minTileX = Math.max(0, (int)(minX / (scale * tileWidth)));
        int maxTileX = Math.min(this.maxX, (int)Math.ceil((minX + width) / (scale * tileWidth)));
        int minTileY = Math.max(0, Math.min(this.maxY, (int)(minY / (scale * tileHeight))));
        int maxTileY = Math.min(this.maxY, (int)Math.ceil((minY + height) / (scale * tileHeight)));

        // Identify the gadgets that overlap cells that overlap the region
        Set<IPlanarGadget> overlappingCellGadgets = new HashSet<IPlanarGadget>();
        for (int y = minTileY / CELL_SIZE * CELL_SIZE; y <= (maxTileY + CELL_SIZE - 1) / CELL_SIZE * CELL_SIZE;
                y += CELL_SIZE) {
            for (int x = minTileX / CELL_SIZE * CELL_SIZE; x <= (maxTileX + CELL_SIZE - 1) / CELL_SIZE * CELL_SIZE;
                    x += CELL_SIZE) {
                Collection<IPlanarGadget> cellGadgets = cellToGadgets.get(new Point(x, y));
                if (cellGadgets != null) {
                    overlappingCellGadgets.addAll(cellGadgets);
                }
                if (Thread.interrupted()) {
                    return null;
                }
            }
        }

        // Identify the gadgets that overlap the region
        Map<IPlanarGadget, Point> overlappingGadgets = new HashMap<IPlanarGadget, Point>();
        for (IPlanarGadget gadget : overlappingCellGadgets) {
            Point point = gadgets.get(gadget);
            if (point.x < maxTileX && point.x + gadget.width() > minTileX &&
                    point.y < maxTileY && point.y + gadget.height() > minTileY) {
                overlappingGadgets.put(gadget, point);
            }
            if (Thread.interrupted()) {
                return null;
            }
        }

        // Render the image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.translate(-minX, -minY);
        graphics.scale(scale, scale);
        render(graphics, overlappingGadgets, minTileX, minTileY, maxTileX - minTileX, maxTileY - minTileY);
        return image;
    }
}
