package com.github.btrekkie.reductions.push1;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A horizontal wire gadget for Push1Problem, as in IPlanarWireFactory.horizontalWire. */
public class Push1HorizontalWireGadget extends Push1Gadget {
    /** The height of horizontal wires. */
    public static final int HEIGHT = 2;

    private final int width;

    public Push1HorizontalWireGadget(int width) {
        this.width = width;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public Push1Tile[][] tiles(int minX, int minY, int width, int height) {
        assertIsInBounds(minX, minY, width, height);
        Push1Tile[][] tiles = new Push1Tile[height][];
        for (int y = minY; y < minY + height; y++) {
            Push1Tile[] row = new Push1Tile[width];
            for (int x = minX; x < minX + width; x++) {
                row[x - minX] = Push1Tile.GROUND;
            }
            tiles[y - minY] = row;
        }
        return tiles;
    }

    @Override
    public Push1Tile[][] tiles() {
        return tiles(0, 0, width, HEIGHT);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 1), new Point(width, 1));
    }
}
