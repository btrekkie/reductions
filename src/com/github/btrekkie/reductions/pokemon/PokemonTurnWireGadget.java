package com.github.btrekkie.reductions.pokemon;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A turn wire gadget for PokemonProblem, as in IPlanarWireFactory.turnWire. */
public class PokemonTurnWireGadget extends PokemonGadget {
    @Override
    public PokemonTile[][] tiles() {
        String[] rows = new String[]{
            " ",
            " "};
        return tiles(rows);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 1), new Point(0, 0), new Point(1, 1), new Point(0, 2));
    }
}
