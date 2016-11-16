package com.github.btrekkie.reductions.zelda;

import com.github.btrekkie.reductions.planar.IPlanarBarrierFactory;
import com.github.btrekkie.reductions.planar.IPlanarGadget;

/** An IPlanarBarrierFactory for ZeldaProblem. */
public class ZeldaBarrierFactory implements IPlanarBarrierFactory {
    /** The singleton instance of ZeldaBarrierFactory. */
    public static final ZeldaBarrierFactory instance = new ZeldaBarrierFactory();

    private ZeldaBarrierFactory() {

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
        return new ZeldaBarrierGadget(width, height);
    }
}
