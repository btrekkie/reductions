package com.github.btrekkie.reductions.mario;

import java.util.Collections;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A barrier gadget for MarioProblem, as in IPlanarBarrierFactory. */
public class MarioBarrierGadget extends MarioGadget {
    private final int width;

    private final int height;

    public MarioBarrierGadget(int width, int height) {
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
    public MarioTile[][] tiles(int minX, int minY, int width, int height) {
        assertIsInBounds(minX, minY, width, height);
        MarioTile[][] tiles = new MarioTile[height][];
        for (int y = 0; y < height; y++) {
            MarioTile[] row = new MarioTile[width];
            for (int x = 0; x < width; x++) {
                row[x] = MarioTile.BLOCK;
            }
            tiles[y] = row;
        }
        return tiles;
    }

    @Override
    public MarioTile[][] tiles() {
        return tiles(0, 0, width, height);
    }

    @Override
    public List<Point> ports() {
        return Collections.emptyList();
    }
}
