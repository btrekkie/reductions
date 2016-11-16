package com.github.btrekkie.reductions.pokemon;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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
 * An IProgrammaticImageRenderer for a PokemonProblem.  PokemonRenderer assumes that the working directory
 * (System.getProperty("user.dir")) is the project's root directory.
 */
public class PokemonRenderer extends PlanarGadgetRenderer {
    /** The number of pixels per unit. */
    private static final int TILE_SIZE = 16;

    /** An image containing all of the sprites for rendering a PokemonProblem. */
    private BufferedImage sprites;

    /** Constructs a new PokemonRenderer for rendering the specified problem. */
    public PokemonRenderer(PokemonProblem problem) {
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

    /**
     * Renders an indication of the sight lines of all of the trainers in the specified gadget (other than the player),
     * for render(Graphics2D, Map, int, int, int, int).  This method assumes that antialiasing is enabled and the stroke
     * (as in graphics.setStroke) is set to the dotted line stroke for drawing the outside border of the sight line
     * rectangles.
     * @param graphics The Graphics2D object to which to render the sight lines.
     * @param gadget The gadget.
     * @param point The position of the top-left corner of the gadget.
     */
    private void renderSightLines(Graphics2D graphics, PokemonGadget gadget, Point point) {
        if (gadget.hasTrainer()) {
            PokemonTile[][] tiles = gadget.tiles();
            Map<Point, Integer> sightLimits = gadget.sightLimits();
            for (int i = 0; i < 2; i++) {
                for (int y = 0; y < gadget.height(); y++) {
                    PokemonTile[] row = tiles[y];
                    for (int x = 0; x < gadget.width(); x++) {
                        PokemonTile tile = row[x];
                        boolean isStrong;
                        switch (tile) {
                            case STRONG_TRAINER_DOWN:
                            case STRONG_TRAINER_LEFT:
                            case STRONG_TRAINER_RIGHT:
                            case STRONG_TRAINER_UP:
                                isStrong = true;
                                break;
                            case WEAK_TRAINER_DOWN:
                            case WEAK_TRAINER_RIGHT:
                                isStrong = false;
                                break;
                            default:
                                continue;
                        }
                        if (isStrong == (i == 0)) {
                            continue;
                        }

                        // Determine the sight line rectangle
                        Integer sightLimitObj = sightLimits.get(new Point(x, y));
                        int sightLimit;
                        if (sightLimitObj != null) {
                            sightLimit = sightLimitObj;
                        } else {
                            sightLimit = Integer.MAX_VALUE;
                        }
                        int sightMinX;
                        int sightMaxX;
                        int sightMinY;
                        int sightMaxY;
                        switch (tile) {
                            case STRONG_TRAINER_UP:
                            {
                                int distance = 0;
                                int sightY;
                                for (sightY = y - 1; distance < sightLimit; sightY--) {
                                    if (tiles[sightY][x] == PokemonTile.ROCK) {
                                        break;
                                    }
                                    distance++;
                                }
                                sightMinX = x;
                                sightMaxX = x + 1;
                                sightMinY = sightY + 1;
                                sightMaxY = y + 1;
                                break;
                            }
                            case STRONG_TRAINER_RIGHT:
                            case WEAK_TRAINER_RIGHT:
                            {
                                int distance = 0;
                                int sightX;
                                for (sightX = x + 1; distance < sightLimit; sightX++) {
                                    if (row[sightX] == PokemonTile.ROCK) {
                                        break;
                                    }
                                    distance++;
                                }
                                sightMinX = x;
                                sightMaxX = sightX;
                                sightMinY = y;
                                sightMaxY = y + 1;
                                break;
                            }
                            case STRONG_TRAINER_DOWN:
                            case WEAK_TRAINER_DOWN:
                            {
                                int distance = 0;
                                int sightY;
                                for (sightY = y + 1; distance < sightLimit; sightY++) {
                                    if (tiles[sightY][x] == PokemonTile.ROCK) {
                                        break;
                                    }
                                    distance++;
                                }
                                sightMinX = x;
                                sightMaxX = x + 1;
                                sightMinY = y;
                                sightMaxY = sightY;
                                break;
                            }
                            case STRONG_TRAINER_LEFT:
                            {
                                int distance = 0;
                                int sightX;
                                for (sightX = x - 1; distance < sightLimit; sightX--) {
                                    if (row[sightX] == PokemonTile.ROCK) {
                                        break;
                                    }
                                    distance++;
                                }
                                sightMinX = sightX + 1;
                                sightMaxX = x + 1;
                                sightMinY = y;
                                sightMaxY = y + 1;
                                break;
                            }
                            default:
                                continue;
                        }

                        // Render the sight line
                        Color color;
                        if (isStrong) {
                            color = new Color(103, 103, 255, 122);
                        } else {
                            color = new Color(235, 94, 94, 111);
                        }
                        graphics.setColor(color);
                        graphics.fillRect(
                            TILE_SIZE * (point.x + sightMinX), TILE_SIZE * (point.y + sightMinY),
                            TILE_SIZE * (sightMaxX - sightMinX), TILE_SIZE * (sightMaxY - sightMinY));
                        graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
                        graphics.draw(
                            new Rectangle(
                                TILE_SIZE * (point.x + sightMinX), TILE_SIZE * (point.y + sightMinY),
                                TILE_SIZE * (sightMaxX - sightMinX), TILE_SIZE * (sightMaxY - sightMinY)));
                    }
                }
            }
        }
    }

    @Override
    protected void render(
            Graphics2D graphics, Map<IPlanarGadget, Point> gadgets, int minX, int minY, int width, int height)
            throws IOException {
        // Make sure the sprite sheet is loaded
        synchronized (this) {
            if (sprites == null) {
                sprites = ImageIO.read(new File(System.getProperty("user.dir") + "/assets/pokemon_sprites.png"));
            }
        }

        // Create TransformedImageDrawers for the sprites
        Map<PokemonTile, TransformedImageDrawer> drawers = new HashMap<PokemonTile, TransformedImageDrawer>();
        drawers.put(
            PokemonTile.FINISH,
            new TransformedImageDrawer(graphics, sprites.getSubimage(9 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE)));
        drawers.put(
            PokemonTile.GROUND, new TransformedImageDrawer(graphics, sprites.getSubimage(0, 0, TILE_SIZE, TILE_SIZE)));
        drawers.put(
            PokemonTile.PLAYER,
            new TransformedImageDrawer(graphics, sprites.getSubimage(8 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE)));
        TransformedImageDrawer rockDrawer = new TransformedImageDrawer(
            graphics, sprites.getSubimage(TILE_SIZE, 0, TILE_SIZE, TILE_SIZE));
        drawers.put(PokemonTile.ROCK, rockDrawer);
        drawers.put(
            PokemonTile.STRONG_TRAINER_DOWN,
            new TransformedImageDrawer(graphics, sprites.getSubimage(4 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE)));
        drawers.put(
            PokemonTile.STRONG_TRAINER_LEFT,
            new TransformedImageDrawer(graphics, sprites.getSubimage(5 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE)));
        drawers.put(
            PokemonTile.STRONG_TRAINER_RIGHT,
            new TransformedImageDrawer(graphics, sprites.getSubimage(3 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE)));
        drawers.put(
            PokemonTile.STRONG_TRAINER_UP,
            new TransformedImageDrawer(graphics, sprites.getSubimage(2 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE)));
        drawers.put(
            PokemonTile.WEAK_TRAINER_DOWN,
            new TransformedImageDrawer(graphics, sprites.getSubimage(7 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE)));
        drawers.put(
            PokemonTile.WEAK_TRAINER_RIGHT,
            new TransformedImageDrawer(graphics, sprites.getSubimage(6 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE)));

        // Fill the image with rocks, in order to fill the region that does not overlap any gadgets
        for (int y = minY; y < minY + height; y++) {
            for (int x = minX; x < minX + width; x++) {
                rockDrawer.draw(TILE_SIZE * x, TILE_SIZE * y);
                if (Thread.interrupted()) {
                    return;
                }
            }
        }

        // Render the gadgets
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setStroke(
            new BasicStroke(0.7f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 10, new float[]{0.8f, 1.2f}, 0));
        for (Entry<IPlanarGadget, Point> entry : gadgets.entrySet()) {
            // Compute the matrix of tiles for the gadget
            PokemonGadget gadget = (PokemonGadget)entry.getKey();
            Point point = entry.getValue();
            int gadgetMinX = Math.max(0, minX - point.x);
            int gadgetMinY = Math.max(0, minY - point.y);
            int gadgetMaxX = Math.min(gadget.width(), minX + width - point.x);
            int gadgetMaxY = Math.min(gadget.height(), minY + height - point.y);
            PokemonTile[][] tiles = gadget.tiles(
                gadgetMinX, gadgetMinY, gadgetMaxX - gadgetMinX, gadgetMaxY - gadgetMinY);

            for (int y = gadgetMinY; y < gadgetMaxY; y++) {
                PokemonTile[] row = tiles[y - gadgetMinY];
                for (int x = gadgetMinX; x < gadgetMaxX; x++) {
                    PokemonTile tile = row[x - gadgetMinX];
                    drawers.get(tile).draw(TILE_SIZE * (point.x + x), TILE_SIZE * (point.y + y));
                    if (Thread.interrupted()) {
                        return;
                    }
                }
            }
            renderSightLines(graphics, gadget, point);
            if (Thread.interrupted()) {
                return;
            }
        }
    }
}
