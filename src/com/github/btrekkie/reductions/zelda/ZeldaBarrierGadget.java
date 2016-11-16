package com.github.btrekkie.reductions.zelda;

import java.util.Collections;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A barrier gadget for ZeldaProblem, as in IPlanarBarrierFactory. */
public class ZeldaBarrierGadget extends ZeldaGadget {
    private final int width;

    private final int height;

    public ZeldaBarrierGadget(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public ZeldaTile[][] tiles(int minX, int minY, int width, int height) {
        assertIsInBounds(minX, minY, width, height);
        ZeldaTile[][] tiles = new ZeldaTile[height][];
        for (int y = 0; y < height; y++) {
            ZeldaTile[] row = new ZeldaTile[width];
            for (int x = 0; x < width; x++) {
                row[x] = ZeldaTile.BARRIER;
            }
            tiles[y] = row;
        }
        return tiles;
    }

    @Override
    public ZeldaTile[][] tiles() {
        return tiles(0, 0, width, height);
    }

    @Override
    public List<Point> ports() {
        return Collections.emptyList();
    }
}
