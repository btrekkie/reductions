package com.github.btrekkie.reductions.pokemon;

import com.github.btrekkie.reductions.planar.I3SatPlanarGadgetFactory;
import com.github.btrekkie.reductions.planar.IPlanarGadget;

/** A I3SatPlanarGadgetFactory implementation for PokemonProblem. */
/* The major gadgets are taken from https://arxiv.org/pdf/1203.1895.pdf (Aloupis, Demaine, Guo, Viglietta (2012):
 * Classic Nintendo Games are (Computationally) Hard).
 */
public class Pokemon3SatPlanarGadgetFactory implements I3SatPlanarGadgetFactory {
    /** The singleton instance of Pokemon3SatPlanarGadgetFactory. */
    public static final Pokemon3SatPlanarGadgetFactory instance = new Pokemon3SatPlanarGadgetFactory();

    private Pokemon3SatPlanarGadgetFactory() {

    }

    @Override
    public IPlanarGadget createVariable() {
        return new PokemonVariableGadget();
    }

    @Override
    public IPlanarGadget createClause() {
        return new PokemonClauseGadget();
    }

    @Override
    public IPlanarGadget createCrossover(boolean isClockwise) {
        return new PokemonCrossoverGadget();
    }

    @Override
    public IPlanarGadget createJunction() {
        return new PokemonJunctionGadget();
    }

    @Override
    public int minVariableEntryPort() {
        return PokemonVariableGadget.MIN_ENTRY_PORT;
    }

    @Override
    public int maxVariableEntryPort() {
        return PokemonVariableGadget.MAX_ENTRY_PORT;
    }

    @Override
    public int minVariableExitPort() {
        return PokemonVariableGadget.MIN_EXIT_PORT;
    }

    @Override
    public int maxVariableExitPort() {
        return PokemonVariableGadget.MAX_EXIT_PORT;
    }

    @Override
    public int minClausePort() {
        return PokemonClauseGadget.MIN_CLAUSE_PORT;
    }

    @Override
    public int maxClausePort() {
        return PokemonClauseGadget.MAX_CLAUSE_PORT;
    }

    @Override
    public int clauseEntryPort() {
        return PokemonClauseGadget.ENTRY_PORT;
    }

    @Override
    public int clauseExitPort() {
        return PokemonClauseGadget.EXIT_PORT;
    }

    @Override
    public int firstCrossoverEntryPort(IPlanarGadget gadget, boolean isClockwise) {
        return isClockwise ? PokemonCrossoverGadget.ENTRY_PORT1 : PokemonCrossoverGadget.ENTRY_PORT2;
    }

    @Override
    public int firstCrossoverExitPort(IPlanarGadget gadget, boolean isClockwise) {
        return isClockwise ? PokemonCrossoverGadget.EXIT_PORT1 : PokemonCrossoverGadget.EXIT_PORT2;
    }

    @Override
    public int secondCrossoverEntryPort(IPlanarGadget gadget, boolean isClockwise) {
        return isClockwise ? PokemonCrossoverGadget.ENTRY_PORT2 : PokemonCrossoverGadget.ENTRY_PORT1;
    }

    @Override
    public int secondCrossoverExitPort(IPlanarGadget gadget, boolean isClockwise) {
        return isClockwise ? PokemonCrossoverGadget.EXIT_PORT2 : PokemonCrossoverGadget.EXIT_PORT1;
    }
}
