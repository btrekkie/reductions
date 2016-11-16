package com.github.btrekkie.reductions.zelda;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A horizontal wire gadget for ZeldaProblem, as in IPlanarWireFactory.horizontalWire. */
public class ZeldaHorizontalWireGadget extends ZeldaGadget {
    /** The height of horizontal wires. */
    public static final int HEIGHT = 2;

    private final int width;

    public ZeldaHorizontalWireGadget(int width) {
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
        return tiles(0, 0, width, HEIGHT);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 1), new Point(width, 1));
    }
}
