package com.github.btrekkie.reductions.mario;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A horizontal wire gadget for MarioProblem, as in IPlanarWireFactory.horizontalWire. */
public class MarioHorizontalWireGadget extends MarioGadget {
    /** The height of horizontal wires. */
    public static final int HEIGHT = 9;

    private final int width;

    public MarioHorizontalWireGadget(int width) {
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
    public MarioTile[][] tiles(int minX, int minY, int width, int height) {
        // The wire consists of height - 3 rows of blocks, then two rows of air, then one row of blocks
        assertIsInBounds(minX, minY, width, height);
        MarioTile[][] tiles = new MarioTile[height][];
        for (int y = minY; y < minY + height; y++) {
            MarioTile[] row = new MarioTile[width];
            MarioTile tile;
            if (y >= HEIGHT - 3 && y < HEIGHT - 1) {
                tile = MarioTile.AIR;
            } else {
                tile = MarioTile.BLOCK;
            }
            for (int x = minX; x < minX + width; x++) {
                row[x - minX] = tile;
            }
            tiles[y - minY] = row;
        }
        return tiles;
    }

    @Override
    public MarioTile[][] tiles() {
        return tiles(0, 0, width, HEIGHT);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, HEIGHT - 1), new Point(width, HEIGHT - 1));
    }
}
