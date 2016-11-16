package com.github.btrekkie.reductions.planar.test;

import java.util.Collections;
import java.util.List;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.Point;

/** A barrier gadget for testing.  See IPlanarBarrierFactory. */
class TestBarrierGadget implements IPlanarGadget {
    private final int width;

    private final int height;

    public TestBarrierGadget(int width, int height) {
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
        return Collections.emptyList();
    }
}
