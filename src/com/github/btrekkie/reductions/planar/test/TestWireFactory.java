package com.github.btrekkie.reductions.planar.test;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.IPlanarWireFactory;

/** An IPlanarWireFactory for testing. */
class TestWireFactory implements IPlanarWireFactory {
    /** The width of vertical wires. */
    private final int width;

    /** The height of horizontal wires. */
    private final int height;

    public TestWireFactory(int width, int height) {
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
    public IPlanarGadget createHorizontalWire(int wireWidth) {
        if (wireWidth < width) {
            throw new IllegalArgumentException("The specified width must be at least width()");
        }
        return new TestWireGadget(wireWidth, height, true);
    }

    @Override
    public IPlanarGadget createVerticalWire(int wireHeight) {
        if (wireHeight < height) {
            throw new IllegalArgumentException("The specified height must be at least height()");
        }
        return new TestWireGadget(width, wireHeight, false);
    }

    @Override
    public IPlanarGadget createTurnWire() {
        return new TestTurnWireGadget(width, height);
    }
}
