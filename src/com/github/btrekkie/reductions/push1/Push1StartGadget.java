package com.github.btrekkie.reductions.push1;

import java.util.Collections;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A start gadget for Push1Problem, where Link starts the game. */
public class Push1StartGadget extends Push1Gadget {
    @Override
    public Push1Tile[][] tiles() {
        String[] rows = new String[]{
            "   ",
            "   ",
            " R ",
            "   ",
            "   ",
            "   "};
        return tiles(rows);
    }

    @Override
    public List<Point> ports() {
        return Collections.singletonList(new Point(3, 3));
    }
}
