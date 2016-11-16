package com.github.btrekkie.reductions.planar.test;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.Point;

/** A horizontal or vertical wire gadget for testing.  See IPlanarWireFactory. */
class TestWireGadget implements IPlanarGadget {
    private final int width;

    private final int height;

    /** Whether this is a horizontal wire rather than a vertical wire. */
    private final boolean isHorizontal;

    public TestWireGadget(int width, int height, boolean isHorizontal) {
        this.width = width;
        this.height = height;
        this.isHorizontal = isHorizontal;
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
        if (isHorizontal) {
            return Arrays.asList(new Point(0, height / 3), new Point(width, height / 3));
        } else {
            return Arrays.asList(new Point(width / 2, 0), new Point(width / 2, height));
        }
    }
}
