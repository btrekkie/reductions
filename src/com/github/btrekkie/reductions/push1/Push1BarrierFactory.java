package com.github.btrekkie.reductions.push1;

import com.github.btrekkie.reductions.planar.IPlanarBarrierFactory;
import com.github.btrekkie.reductions.planar.IPlanarGadget;

/** An IPlanarBarrierFactory for Push1Problem. */
public class Push1BarrierFactory implements IPlanarBarrierFactory {
    /** The singleton instance of Push1BarrierFactory. */
    public static final Push1BarrierFactory instance = new Push1BarrierFactory();

    private Push1BarrierFactory() {

    }

    @Override
    public int minWidth() {
        // Barriers must be at least 2 x 2, so that the robot can't push the blocks that comprise them
        return 2;
    }

    @Override
    public int minHeight() {
        // Barriers must be at least 2 x 2, so that the robot can't push the blocks that comprise them
        return 2;
    }

    @Override
    public IPlanarGadget createBarrier(int width, int height) {
        return new Push1BarrierGadget(width, height);
    }
}
