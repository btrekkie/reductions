package com.github.btrekkie.reductions.pokemon;

import com.github.btrekkie.reductions.planar.IPlanarBarrierFactory;
import com.github.btrekkie.reductions.planar.IPlanarGadget;

/** An IPlanarBarrierFactory for PokemonProblem. */
public class PokemonBarrierFactory implements IPlanarBarrierFactory {
    /** The singleton instance of PokemonBarrierFactory. */
    public static final PokemonBarrierFactory instance = new PokemonBarrierFactory();

    private PokemonBarrierFactory() {

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
        return new PokemonBarrierGadget(width, height);
    }
}
