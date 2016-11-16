package com.github.btrekkie.reductions.push1;

import java.util.Collections;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A barrier gadget for Push1Problem, as in IPlanarBarrierFactory. */
public class Push1BarrierGadget extends Push1Gadget {
    private final int width;

    private final int height;

    public Push1BarrierGadget(int width, int height) {
        if (width < 2 || height < 2) {
            throw new IllegalArgumentException("Push-1 barrier gadgets must be at least 2 x 2");
        }
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
    public Push1Tile[][] tiles(int minX, int minY, int width, int height) {
        assertIsInBounds(minX, minY, width, height);
        Push1Tile[][] tiles = new Push1Tile[height][];
        for (int y = 0; y < height; y++) {
            Push1Tile[] row = new Push1Tile[width];
            for (int x = 0; x < width; x++) {
                row[x] = Push1Tile.EFFECTIVELY_IMMOVABLE_BLOCK;
            }
            tiles[y] = row;
        }
        return tiles;
    }

    @Override
    public Push1Tile[][] tiles() {
        return tiles(0, 0, width, height);
    }

    @Override
    public List<Point> ports() {
        return Collections.emptyList();
    }
}
