package com.github.btrekkie.reductions.zelda;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A junction gadget for ZeldaProblem, as in I3SatPlanarGadgetFactory.createJunction(). */
public class ZeldaJunctionGadget extends ZeldaGadget {
    @Override
    public ZeldaTile[][] tiles() {
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
