package com.github.btrekkie.reductions.pokemon;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A vertical wire gadget for PokemonProblem, as in IPlanarWireFactory.verticalWire. */
public class PokemonVerticalWireGadget extends PokemonGadget {
    /** The width of vertical wires. */
    public static final int WIDTH = 1;

    private final int height;

    public PokemonVerticalWireGadget(int height) {
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
    public PokemonTile[][] tiles(int minX, int minY, int width, int height) {
        assertIsInBounds(minX, minY, width, height);
        PokemonTile[][] tiles = new PokemonTile[height][];
        for (int y = minY; y < minY + height; y++) {
            PokemonTile[] row = new PokemonTile[width];
            for (int x = minX; x < minX + width; x++) {
                row[x - minX] = PokemonTile.GROUND;
            }
            tiles[y - minY] = row;
        }
        return tiles;
    }

    @Override
    public PokemonTile[][] tiles() {
        return tiles(0, 0, WIDTH, height);
    }

    @Override
    public boolean hasTrainer() {
        return false;
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 0), new Point(0, height));
    }
}
