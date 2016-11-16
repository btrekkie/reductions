package com.github.btrekkie.reductions.zelda;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A vertical wire gadget for ZeldaProblem, as in IPlanarWireFactory.verticalWire. */
public class ZeldaVerticalWireGadget extends ZeldaGadget {
    /** The width of vertical wires. */
    public static final int WIDTH = 1;

    private final int height;

    public ZeldaVerticalWireGadget(int height) {
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
    public ZeldaTile[][] tiles(int minX, int minY, int width, int height) {
        assertIsInBounds(minX, minY, width, height);
        ZeldaTile[][] tiles = new ZeldaTile[height][];
        for (int y = minY; y < minY + height; y++) {
            ZeldaTile[] row = new ZeldaTile[width];
            for (int x = minX; x < minX + width; x++) {
                row[x - minX] = ZeldaTile.GROUND;
            }
            tiles[y - minY] = row;
        }
        return tiles;
    }

    @Override
    public ZeldaTile[][] tiles() {
        return tiles(0, 0, WIDTH, height);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 0), new Point(0, height));
    }
}
