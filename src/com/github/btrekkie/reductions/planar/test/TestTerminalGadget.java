package com.github.btrekkie.reductions.planar.test;

import java.util.Collections;
import java.util.List;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.Point;

/** A test gadget having one port. */
class TestTerminalGadget implements IPlanarGadget {
    @Override
    public int width() {
        return 10;
    }

    @Override
    public int height() {
        return 20;
    }

    @Override
    public List<Point> ports() {
        return Collections.singletonList(new Point(0, 10));
    }
}
