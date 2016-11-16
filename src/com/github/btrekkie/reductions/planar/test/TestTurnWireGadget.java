package com.github.btrekkie.reductions.planar.test;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.Point;

/** A turn wire gadget for testing, as in IPlanarWireFactory.turnWire. */
class TestTurnWireGadget implements IPlanarGadget {
    private final int width;

    private final int height;

    public TestTurnWireGadget(int width, int height) {
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
    public List<Point> ports() {
        return Arrays.asList(
            new Point(0, height / 3), new Point(width / 2, 0),
            new Point(width, height / 3), new Point(width / 2, height));
    }
}
