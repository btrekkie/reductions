package com.github.btrekkie.reductions.pokemon;

import java.util.Collections;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A start gadget for PokemonProblem, where the player starts the game. */
public class PokemonStartGadget extends PokemonGadget {
    @Override
    public PokemonTile[][] tiles() {
        String[] rows = new String[]{
            "   ",
            " P ",
            "   ",
            "   "};
        return tiles(rows);
    }

    @Override
    public List<Point> ports() {
        return Collections.singletonList(new Point(3, 2));
    }
}
