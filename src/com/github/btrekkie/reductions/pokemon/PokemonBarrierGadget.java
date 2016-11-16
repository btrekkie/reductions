package com.github.btrekkie.reductions.pokemon;

import java.util.Collections;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A barrier gadget for PokemonProblem, as in IPlanarBarrierFactory. */
public class PokemonBarrierGadget extends PokemonGadget {
    private final int width;

    private final int height;

    public PokemonBarrierGadget(int width, int height) {
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
    public PokemonTile[][] tiles(int minX, int minY, int width, int height) {
        assertIsInBounds(minX, minY, width, height);
        PokemonTile[][] tiles = new PokemonTile[height][];
        for (int y = 0; y < height; y++) {
            PokemonTile[] row = new PokemonTile[width];
            for (int x = 0; x < width; x++) {
                row[x] = PokemonTile.ROCK;
            }
            tiles[y] = row;
        }
        return tiles;
    }

    @Override
    public PokemonTile[][] tiles() {
        return tiles(0, 0, width, height);
    }

    @Override
    public boolean hasTrainer() {
        return false;
    }

    @Override
    public List<Point> ports() {
        return Collections.emptyList();
    }
}
