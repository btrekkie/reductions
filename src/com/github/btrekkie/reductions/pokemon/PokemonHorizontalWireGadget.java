package com.github.btrekkie.reductions.pokemon;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A horizontal wire gadget for PokemonProblem, as in IPlanarWireFactory.horizontalWire. */
public class PokemonHorizontalWireGadget extends PokemonGadget {
    /** The height of horizontal wires. */
    public static final int HEIGHT = 2;

    private final int width;

    public PokemonHorizontalWireGadget(int width) {
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
        return tiles(0, 0, width, HEIGHT);
    }

    @Override
    public boolean hasTrainer() {
        return false;
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 1), new Point(width, 1));
    }
}
