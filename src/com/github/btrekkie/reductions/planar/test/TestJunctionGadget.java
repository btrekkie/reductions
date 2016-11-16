package com.github.btrekkie.reductions.planar.test;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.Point;

/** A test gadget for I3SatPlanarGadgetFactory.createJunction().  See the comments for that method. */
class TestJunctionGadget implements IPlanarGadget {
    @Override
    public int width() {
        return 30;
    }

    @Override
    public int height() {
        return 20;
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 10), new Point(15, 0), new Point(20, 20));
    }
}
