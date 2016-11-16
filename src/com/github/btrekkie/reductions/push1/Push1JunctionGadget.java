package com.github.btrekkie.reductions.push1;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A junction gadget for Push1Problem, as in I3SatPlanarGadgetFactory.createJunction(). */
public class Push1JunctionGadget extends Push1Gadget {
    @Override
    public Push1Tile[][] tiles() {
        String[] rows = new String[]{
            "** **",
            "** **",
            "     ",
            "     ",
            "** **",
            "** **",};
        return tiles(rows);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 3), new Point(2, 0), new Point(5, 3), new Point(2, 6));
    }
}
