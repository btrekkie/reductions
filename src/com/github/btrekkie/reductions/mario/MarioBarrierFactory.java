package com.github.btrekkie.reductions.mario;

import com.github.btrekkie.reductions.planar.IPlanarBarrierFactory;
import com.github.btrekkie.reductions.planar.IPlanarGadget;

/** An IPlanarBarrierFactory for MarioProblem. */
public class MarioBarrierFactory implements IPlanarBarrierFactory {
    /** The singleton instance of MarioBarrierFactory. */
    public static final MarioBarrierFactory instance = new MarioBarrierFactory();

    private MarioBarrierFactory() {

    }

    @Override
    public int minWidth() {
        return 1;
    }

    @Override
    public int minHeight() {
        return 1;
    }

    @Override
    public IPlanarGadget createBarrier(int width, int height) {
        if (width < minWidth() || height < minHeight()) {
            throw new IllegalArgumentException("The specified size must be at least minWidth() x minHeight()");
        }
        return new MarioBarrierGadget(width, height);
    }
}
