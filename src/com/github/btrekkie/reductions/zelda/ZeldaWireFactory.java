package com.github.btrekkie.reductions.zelda;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.IPlanarWireFactory;

/** An IPlanarWireFactory for ZeldaProblem. */
public class ZeldaWireFactory implements IPlanarWireFactory {
    /** The singleton instance of ZeldaWireFactory. */
    public static final ZeldaWireFactory instance = new ZeldaWireFactory();

    private ZeldaWireFactory() {

    }

    @Override
    public int width() {
        return ZeldaVerticalWireGadget.WIDTH;
    }

    @Override
    public int height() {
        return ZeldaHorizontalWireGadget.HEIGHT;
    }

    @Override
    public IPlanarGadget createHorizontalWire(int width) {
        return new ZeldaHorizontalWireGadget(width);
    }

    @Override
    public IPlanarGadget createVerticalWire(int height) {
        return new ZeldaVerticalWireGadget(height);
    }

    @Override
    public IPlanarGadget createTurnWire() {
        return new ZeldaTurnWireGadget();
    }
}
