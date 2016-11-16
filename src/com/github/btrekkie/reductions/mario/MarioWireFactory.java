package com.github.btrekkie.reductions.mario;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.IPlanarWireFactory;

/** An IPlanarWireFactory for MarioProblem. */
public class MarioWireFactory implements IPlanarWireFactory {
    /** The singleton instance of MarioWireFactory. */
    public static final MarioWireFactory instance = new MarioWireFactory();

    private MarioWireFactory() {

    }

    @Override
    public int width() {
        return MarioVerticalWireGadget.WIDTH;
    }

    @Override
    public int height() {
        return MarioHorizontalWireGadget.HEIGHT;
    }

    @Override
    public IPlanarGadget createHorizontalWire(int width) {
        if (width < width()) {
            throw new IllegalArgumentException("The specified width must be at least width()");
        }
        return new MarioHorizontalWireGadget(width);
    }

    @Override
    public IPlanarGadget createVerticalWire(int height) {
        if (height < height()) {
            throw new IllegalArgumentException("The specified height must be at least height()");
        }
        return new MarioVerticalWireGadget(height);
    }

    @Override
    public IPlanarGadget createTurnWire() {
        return new MarioTurnWireGadget();
    }
}
