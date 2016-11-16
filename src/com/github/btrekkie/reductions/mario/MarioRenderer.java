package com.github.btrekkie.reductions.mario;

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
 * An IProgrammaticImageRenderer for a MarioProblem.  MarioRenderer assumes that the working directory
 * (System.getProperty("user.dir")) is the project's root directory.
 */
public class MarioRenderer extends PlanarGadgetRenderer {
    /** The number of pixels per unit. */
    private static final int TILE_SIZE = 16;

    /** The maximum number of units above a 1 x 1 cell to which we will draw when drawing the cell's MarioTile. */
    private static final int EXTRA_TILES_TOP = 10;

    /**
     * The maximum number of units to the left of a 1 x 1 cell to which we will draw when drawing the cell's MarioTile.
     */
    private static final int EXTRA_TILES_LEFT = 2;

    /** The maximum number of units below a 1 x 1 cell to which we will draw when drawing the cell's MarioTile. */
    private static final int EXTRA_TILES_BOTTOM = 2;

    /**
     * The maximum number of units to the right of a 1 x 1 cell to which we will draw when drawing the cell's MarioTile.
     */
    private static final int EXTRA_TILES_RIGHT = 4;

    /** An image containing all of the sprites for rendering a MarioProblem. */
    private BufferedImage sprites;

    /** Constructs a new MarioRenderer for rendering the specified problem. */
    public MarioRenderer(MarioProblem problem) {
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
                sprites = ImageIO.read(new File(System.getProperty("user.dir") + "/assets/mario_sprites.gif"));
            }
        }

        // Create TransformedImageDrawers for the sprites
        TransformedImageDrawer airDrawer = new TransformedImageDrawer(
            graphics, sprites.getSubimage(274, 331, TILE_SIZE, TILE_SIZE));
        TransformedImageDrawer blockDrawer = new TransformedImageDrawer(
            graphics, sprites.getSubimage(373, 142, TILE_SIZE, TILE_SIZE));
        TransformedImageDrawer brickDrawer = new TransformedImageDrawer(
            graphics, sprites.getSubimage(373, 102, TILE_SIZE, TILE_SIZE));
        TransformedImageDrawer castleDrawer = new TransformedImageDrawer(
            graphics, sprites.getSubimage(272, 218, 5 * TILE_SIZE, 5 * TILE_SIZE));
        TransformedImageDrawer fireBarBlockDrawer = new TransformedImageDrawer(
            graphics, sprites.getSubimage(393, 65, TILE_SIZE, TILE_SIZE));
        TransformedImageDrawer fireDrawer = new TransformedImageDrawer(graphics, sprites.getSubimage(336, 945, 8, 8));
        TransformedImageDrawer flagDrawer = new TransformedImageDrawer(
            graphics, sprites.getSubimage(260, 46, TILE_SIZE + 7, 10 * TILE_SIZE + 8));
        TransformedImageDrawer goombaDrawer = new TransformedImageDrawer(
            graphics, sprites.getSubimage(208, 894, TILE_SIZE, TILE_SIZE));
        TransformedImageDrawer groundBlockDrawer = new TransformedImageDrawer(
            graphics, sprites.getSubimage(373, 124, TILE_SIZE, TILE_SIZE));
        TransformedImageDrawer marioDrawer = new TransformedImageDrawer(
            graphics, sprites.getSubimage(22, 507, TILE_SIZE, TILE_SIZE));
        TransformedImageDrawer questionMarkDrawer = new TransformedImageDrawer(
            graphics, sprites.getSubimage(372, 160, TILE_SIZE, TILE_SIZE));

        // Fill the image with blocks, in order to fill the region that does not overlap any gadgets
        for (int y = minY; y < minY + height; y++) {
            for (int x = minX; x < minX + width; x++) {
                blockDrawer.draw(TILE_SIZE * x, TILE_SIZE * y);
                if (Thread.interrupted()) {
                    return;
                }
            }
        }

        // Render the gadgets
        for (Entry<IPlanarGadget, Point> entry : gadgets.entrySet()) {
            // Compute the matrix of tiles for the gadget
            MarioGadget gadget = (MarioGadget)entry.getKey();
            Point point = entry.getValue();
            int gadgetMinX = Math.max(0, minX - point.x - EXTRA_TILES_RIGHT);
            int gadgetMinY = Math.max(0, minY - point.y - EXTRA_TILES_BOTTOM);
            int gadgetMaxX = Math.min(gadget.width(), minX + width - point.x + EXTRA_TILES_LEFT);
            int gadgetMaxY = Math.min(gadget.height(), minY + height - point.y + EXTRA_TILES_TOP);
            MarioTile[][] tiles = gadget.tiles(
                gadgetMinX, gadgetMinY, gadgetMaxX - gadgetMinX, gadgetMaxY - gadgetMinY);

            for (int layer = 0; layer < 2; layer++) {
                for (int y = gadgetMinY; y < gadgetMaxY; y++) {
                    MarioTile[] row = tiles[y - gadgetMinY];
                    for (int x = gadgetMinX; x < gadgetMaxX; x++) {
                        MarioTile tile = row[x - gadgetMinX];
                        switch (tile) {
                            case AIR:
                                if (layer == 0) {
                                    airDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                }
                                break;
                            case BLOCK:
                                if (layer == 0) {
                                    blockDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                }
                                break;
                            case BRICK:
                                if (layer == 0) {
                                    brickDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                }
                                break;
                            case CASTLE:
                                if (layer == 0) {
                                    airDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                } else {
                                    castleDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y - 4));
                                }
                                break;
                            case FIRE_BAR_DOWN_LEFT:
                                if (layer == 0) {
                                    airDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                    fireBarBlockDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                } else {
                                    for (int delta : new int[]{0, 5, 11, 16, 22, 28}) {
                                        fireDrawer.draw(
                                            TILE_SIZE * (point.x + x) + 4 - delta,
                                            TILE_SIZE * (point.y + y) + 5 + delta);
                                    }
                                }
                                break;
                            case FIRE_BAR_UP_RIGHT:
                                if (layer == 0) {
                                    airDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                    fireBarBlockDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                } else {
                                    for (int delta : new int[]{0, 5, 11, 16, 22, 28}) {
                                        fireDrawer.draw(
                                            TILE_SIZE * (point.x + x) + 4 + delta,
                                            TILE_SIZE * (point.y + y) + 5 - delta);
                                    }
                                }
                                break;
                            case FLAG:
                                if (layer == 1) {
                                    flagDrawer.draw(TILE_SIZE * (point.x + x) - 8, TILE_SIZE * (point.y + y - 9) - 8);
                                    blockDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                }
                                break;
                            case GOOMBA:
                                if (layer == 0) {
                                    airDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                } else {
                                    goombaDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                }
                                break;
                            case GROUND_BLOCK:
                                if (layer == 0) {
                                    groundBlockDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                }
                                break;
                            case MARIO:
                                if (layer == 0) {
                                    airDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                } else {
                                    marioDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                }
                                break;
                            case QUESTION_MARK_MUSHROOM:
                            case QUESTION_MARK_STAR:
                                if (layer == 0) {
                                    airDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                    questionMarkDrawer.draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                                }
                                break;
                            default:
                                throw new RuntimeException("Unhandled tile " + tile);
                        }
                        if (Thread.interrupted()) {
                            return;
                        }
                    }
                }
            }
            if (Thread.interrupted()) {
                return;
            }
        }
    }
}
