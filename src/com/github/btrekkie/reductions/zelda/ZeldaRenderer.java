package com.github.btrekkie.reductions.zelda;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.PlanarGadgetRenderer;
import com.github.btrekkie.reductions.planar.Point;
import com.github.btrekkie.reductions.planar.TransformedImageDrawer;

/**
 * An IProgrammaticImageRenderer for a ZeldaProblem.  ZeldaRenderer assumes that the working directory
 * (System.getProperty("user.dir")) is the project's root directory.
 */
public class ZeldaRenderer extends PlanarGadgetRenderer {
    /** The number of pixels per unit. */
    private static final int TILE_SIZE = 16;

    /** An image containing all of the sprites for rendering a ZeldaProblem. */
    private BufferedImage sprites;

    /** Constructs a new ZeldaRenderer for rendering the specified problem. */
    public ZeldaRenderer(ZeldaProblem problem) {
        super(new HashMap<IPlanarGadget, Point>(problem.gadgets));
    }

    @Override
    protected double tileWidth() {
        return TILE_SIZE;
    }

    @Override
    protected double tileHeight() {
        return TILE_SIZE;
    }

    @Override
    protected void render(
            Graphics2D graphics, Map<IPlanarGadget, Point> gadgets, int minX, int minY, int width, int height)
            throws IOException {
        // Make sure the sprite sheet is loaded
        synchronized (this) {
            if (sprites == null) {
                sprites = ImageIO.read(new File(System.getProperty("user.dir") + "/assets/zelda_sprites.png"));
            }
        }

        // Create TransformedImageDrawers for the sprites
        Map<ZeldaTile, TransformedImageDrawer> drawers = new HashMap<ZeldaTile, TransformedImageDrawer>();
        TransformedImageDrawer barrierDrawer = new TransformedImageDrawer(
            graphics, sprites.getSubimage(2 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE));
        drawers.put(ZeldaTile.BARRIER, barrierDrawer);
        drawers.put(
            ZeldaTile.BLOCK,
            new TransformedImageDrawer(graphics, sprites.getSubimage(TILE_SIZE, 0, TILE_SIZE, TILE_SIZE)));
        drawers.put(
            ZeldaTile.FINISH,
            new TransformedImageDrawer(graphics, sprites.getSubimage(4 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE)));
        drawers.put(
            ZeldaTile.GROUND, new TransformedImageDrawer(graphics, sprites.getSubimage(0, 0, TILE_SIZE, TILE_SIZE)));
        drawers.put(
            ZeldaTile.LINK,
            new TransformedImageDrawer(graphics, sprites.getSubimage(3 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE)));

        // Fill the image with barriers, in order to fill the region that does not overlap any gadgets
        for (int y = minY; y < minY + height; y++) {
            for (int x = minX; x < minX + width; x++) {
                barrierDrawer.draw(TILE_SIZE * x, TILE_SIZE * y);
                if (Thread.interrupted()) {
                    return;
                }
            }
        }

        // Render the gadgets
        for (Entry<IPlanarGadget, Point> entry : gadgets.entrySet()) {
            // Compute the matrix of tiles for the gadget
            ZeldaGadget gadget = (ZeldaGadget)entry.getKey();
            Point point = entry.getValue();
            int gadgetMinX = Math.max(0, minX - point.x);
            int gadgetMinY = Math.max(0, minY - point.y);
            int gadgetMaxX = Math.min(gadget.width(), minX + width - point.x);
            int gadgetMaxY = Math.min(gadget.height(), minY + height - point.y);
            ZeldaTile[][] tiles = gadget.tiles(
                gadgetMinX, gadgetMinY, gadgetMaxX - gadgetMinX, gadgetMaxY - gadgetMinY);

            for (int y = gadgetMinY; y < gadgetMaxY; y++) {
                ZeldaTile[] row = tiles[y - gadgetMinY];
                for (int x = gadgetMinX; x < gadgetMaxX; x++) {
                    ZeldaTile tile = row[x - gadgetMinX];
                    drawers.get(tile).draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                    if (Thread.interrupted()) {
                        return;
                    }
                }
            }
            if (Thread.interrupted()) {
                return;
            }
        }
    }
}
