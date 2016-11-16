package com.github.btrekkie.reductions.pokemon;

import java.util.Collections;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A finish gadget for PokemonProblem, which we must reach to beat the game. */
public class PokemonFinishGadget extends PokemonGadget {
    @Override
    public PokemonTile[][] tiles() {
        String[] rows = new String[]{
            "     ",
            " FFF ",
            " FFF ",
            " FFF ",
            "     "};
        return tiles(rows);
    }

    @Override
    public List<Point> ports() {
        return Collections.singletonList(new Point(0, 2));
    }
}
