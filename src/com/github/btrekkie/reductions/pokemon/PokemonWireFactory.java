package com.github.btrekkie.reductions.pokemon;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.IPlanarWireFactory;

/** An IPlanarWireFactory for PokemonProblem. */
public class PokemonWireFactory implements IPlanarWireFactory {
    /** The singleton instance of PokemonWireFactory. */
    public static final PokemonWireFactory instance = new PokemonWireFactory();

    private PokemonWireFactory() {

    }

    @Override
    public int width() {
        return PokemonVerticalWireGadget.WIDTH;
    }

    @Override
    public int height() {
        return PokemonHorizontalWireGadget.HEIGHT;
    }

    @Override
    public IPlanarGadget createHorizontalWire(int width) {
        return new PokemonHorizontalWireGadget(width);
    }

    @Override
    public IPlanarGadget createVerticalWire(int height) {
        return new PokemonVerticalWireGadget(height);
    }

    @Override
    public IPlanarGadget createTurnWire() {
        return new PokemonTurnWireGadget();
    }
}
