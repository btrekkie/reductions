package com.github.btrekkie.reductions.push1;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A vertical wire gadget for Push1Problem, as in IPlanarWireFactory.verticalWire. */
public class Push1VerticalWireGadget extends Push1Gadget {
    /** The width of vertical wires. */
    public static final int WIDTH = 1;

    private final int height;

    public Push1VerticalWireGadget(int height) {
        this.height = height;
    }

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return height;
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
        return tiles(0, 0, WIDTH, height);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 0), new Point(0, height));
    }
}
