package com.github.btrekkie.reductions.push1;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.IPlanarWireFactory;

/** An IPlanarWireFactory for Push1Problem. */
public class Push1WireFactory implements IPlanarWireFactory {
    /** The singleton instance of Push1WireFactory. */
    public static final Push1WireFactory instance = new Push1WireFactory();

    private Push1WireFactory() {

    }

    @Override
    public int width() {
        return Push1VerticalWireGadget.WIDTH;
    }

    @Override
    public int height() {
        return Push1HorizontalWireGadget.HEIGHT;
    }

    @Override
    public IPlanarGadget createHorizontalWire(int width) {
        return new Push1HorizontalWireGadget(width);
    }

    @Override
    public IPlanarGadget createVerticalWire(int height) {
        return new Push1VerticalWireGadget(height);
    }

    @Override
    public IPlanarGadget createTurnWire() {
        return new Push1TurnWireGadget();
    }
}
