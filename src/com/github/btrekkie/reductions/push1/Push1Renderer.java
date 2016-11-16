package com.github.btrekkie.reductions.push1;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.PlanarGadgetRenderer;
import com.github.btrekkie.reductions.planar.Point;

/** An IProgrammaticImageRenderer for a Push1Problem. */
public class Push1Renderer extends PlanarGadgetRenderer {
    /** The color of ground tiles. */
    private static final Color GROUND_COLOR = Color.WHITE;

    /** The color of the border around blocks and the robot. */
    private static final Color BORDER_COLOR = Color.BLACK;

    /** The color of blocks of type Push1Tile.BLOCK. */
    private static final Color BLOCK_COLOR = new Color(207, 207, 207);

    /** The color of effectively immovable blocks.  See Push1Tile.EFFECTIVELY_IMMOVABLE_BLOCK. */
    private static final Color EFFECTIVELY_IMMOVABLE_BLOCK_COLOR = new Color(166, 166, 166);

    /** The color of the robot. */
    private static final Color ROBOT_COLOR = new Color(127, 127, 127);

    /** The color of finish tiles. */
    private static final Color FINISH_COLOR = Color.BLACK;

    /** The size of the robot. */
    private static final double ROBOT_SIZE = 0.466;

    /** The size of the border around blocks and the robot. */
    private static final double BORDER_WIDTH = 0.064;

    /** Constructs a new Push1Renderer for rendering the specified problem. */
    public Push1Renderer(Push1Problem problem) {
        super(new HashMap<IPlanarGadget, Point>(problem.gadgets));
    }

    @Override
    protected double tileWidth() {
        return 1;
    }

    @Override
    protected double tileHeight() {
        return 1;
    }

    @Override
    protected void render(
            Graphics2D graphics, Map<IPlanarGadget, Point> gadgets, int minX, int minY, int width, int height)
            throws IOException {
        // Fill the image with blocks, in order to fill the region that does not overlap any gadgets
        graphics.setColor(EFFECTIVELY_IMMOVABLE_BLOCK_COLOR);
        graphics.fillRect(minX, minY, width, height);
        graphics.setStroke(new BasicStroke((float)BORDER_WIDTH));
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int y = minY; y < minY + height; y++) {
            for (int x = minX; x < minX + width; x++) {
                graphics.setColor(BORDER_COLOR);
                graphics.draw(new Rectangle(x, y, 1, 1));
                if (Thread.interrupted()) {
                    return;
                }
            }
        }

        // Render the gadgets.  First render all of the content, then render all of the borders.
        for (int layer = 0; layer < 2; layer++) {
            if (layer == 0) {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            } else {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
            for (Entry<IPlanarGadget, Point> entry : gadgets.entrySet()) {
                // Compute the matrix of tiles for the gadget
                Push1Gadget gadget = (Push1Gadget)entry.getKey();
                Point point = entry.getValue();
                int gadgetMinX = Math.max(0, minX - point.x);
                int gadgetMinY = Math.max(0, minY - point.y);
                int gadgetMaxX = Math.min(gadget.width(), minX + width - point.x);
                int gadgetMaxY = Math.min(gadget.height(), minY + height - point.y);
                Push1Tile[][] tiles = gadget.tiles(
                    gadgetMinX, gadgetMinY, gadgetMaxX - gadgetMinX, gadgetMaxY - gadgetMinY);

                for (int y = gadgetMinY; y < gadgetMaxY; y++) {
                    Push1Tile[] row = tiles[y - gadgetMinY];
                    for (int x = gadgetMinX; x < gadgetMaxX; x++) {
                        Push1Tile tile = row[x - gadgetMinX];
                        switch (tile) {
                            case BLOCK:
                            case EFFECTIVELY_IMMOVABLE_BLOCK:
                                if (layer == 1) {
                                    graphics.setColor(BORDER_COLOR);
                                    graphics.draw(new Rectangle(point.x + x, point.y + y, 1, 1));
                                } else {
                                    Color color;
                                    if (tile == Push1Tile.BLOCK) {
                                        color = BLOCK_COLOR;
                                    } else {
                                        color = EFFECTIVELY_IMMOVABLE_BLOCK_COLOR;
                                    }
                                    graphics.setColor(color);
                                    graphics.fillRect(point.x + x, point.y + y, 1, 1);
                                }
                                break;
                            case FINISH:
                                if (layer == 0) {
                                    graphics.setColor(FINISH_COLOR);
                                    graphics.fillRect(point.x + x, point.y + y, 1, 1);
                                }
                                break;
                            case GROUND:
                                if (layer == 0) {
                                    graphics.setColor(GROUND_COLOR);
                                    graphics.fillRect(point.x + x, point.y + y, 1, 1);
                                }
                                break;
                            case ROBOT:
                            {
                                Shape shape = new Ellipse2D.Double(
                                    point.x + x + 0.5 - ROBOT_SIZE / 2, point.y + y + 0.5 - ROBOT_SIZE / 2,
                                    ROBOT_SIZE, ROBOT_SIZE);
                                if (layer == 0) {
                                    graphics.setColor(GROUND_COLOR);
                                    graphics.fillRect(point.x + x, point.y + y, 1, 1);
                                    graphics.setColor(ROBOT_COLOR);
                                    graphics.fill(shape);
                                } else {
                                    graphics.setColor(BORDER_COLOR);
                                    graphics.draw(shape);
                                }
                                break;
                            }
                            default:
                                throw new RuntimeException("Unhandled tile " + tile);
                        }
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
}
