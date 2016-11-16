package com.github.btrekkie.reductions.planar.test;

import com.github.btrekkie.reductions.planar.IPlanarBarrierFactory;
import com.github.btrekkie.reductions.planar.IPlanarGadget;

/** An IPlanarBarrierFactory for testing. */
class TestBarrierFactory implements IPlanarBarrierFactory {
    /** The minimum width of a barrier gadget. */
    private final int minWidth;

    /** The minimum height of a barrier gadget. */
    private final int minHeight;

    public TestBarrierFactory(int minWidth, int minHeight) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
    }

    @Override
    public int minWidth() {
        return minWidth;
    }

    @Override
    public int minHeight() {
        return minHeight;
    }

    @Override
    public IPlanarGadget createBarrier(int width, int height) {
        if (width < minWidth || height < minHeight) {
            throw new IllegalArgumentException("The specified size must be at least minWidth() x minHeight()");
        }
        return new TestBarrierGadget(width, height);
    }
}
