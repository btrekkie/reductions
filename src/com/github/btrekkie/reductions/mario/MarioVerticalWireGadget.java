package com.github.btrekkie.reductions.mario;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A vertical wire gadget for MarioProblem, as in IPlanarWireFactory.verticalWire. */
public class MarioVerticalWireGadget extends MarioGadget {
    /** The width of vertical wires. */
    public static final int WIDTH = 4;

    private final int height;

    public MarioVerticalWireGadget(int height) {
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
    public MarioTile[][] tiles(int minX, int minY, int width, int height) {
        // A vertical wire consists of mostly air, with a "level" at (roughly) every third row.  A "level" consists of a
        // two blocks, either at the edges or in the middle, alternating between levels.
        assertIsInBounds(minX, minY, width, height);
        MarioTile[][] tiles = new MarioTile[height][];
        int levelCount = this.height / 3;
        MarioTile[] fullRow = new MarioTile[WIDTH];
        for (int y = minY; y < minY + height; y++) {
            // The level with index i is at y = this.height * (i + 1) / levelCount - 1
            int levelIndex = ((y + 2) * levelCount - 1) / this.height;
            int prevLevelIndex = ((y + 1) * levelCount - 1) / this.height;
            if (levelIndex == prevLevelIndex) {
                for (int x = 0; x < 4; x++) {
                    fullRow[x] = MarioTile.AIR;
                }
            } else if (levelIndex % 2 == 0) {
                fullRow[0] = MarioTile.AIR;
                fullRow[1] = MarioTile.BLOCK;
                fullRow[2] = MarioTile.BLOCK;
                fullRow[3] = MarioTile.AIR;
            } else {
                fullRow[0] = MarioTile.BLOCK;
                fullRow[1] = MarioTile.AIR;
                fullRow[2] = MarioTile.AIR;
                fullRow[3] = MarioTile.BLOCK;
            }

            // Store a subarray of fullRow in tiles[y - minY]
            MarioTile[] row = new MarioTile[width];
            for (int x = minX; x < minX + width; x++) {
                row[x - minX] = fullRow[x];
            }
            tiles[y - minY] = row;
        }
        return tiles;
    }

    @Override
    public MarioTile[][] tiles() {
        return tiles(0, 0, WIDTH, height);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 0), new Point(0, height));
    }
}
