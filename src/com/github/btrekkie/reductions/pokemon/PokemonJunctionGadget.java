package com.github.btrekkie.reductions.pokemon;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A junction gadget for PokemonProblem, as in I3SatPlanarGadgetFactory.createJunction(). */
public class PokemonJunctionGadget extends PokemonGadget {
    @Override
    public PokemonTile[][] tiles() {
        String[] rows = new String[]{
            "* *",
            "   ",
            "   ",
            "* *"};
        return tiles(rows);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 2), new Point(1, 0), new Point(3, 2), new Point(1, 4));
    }
}
