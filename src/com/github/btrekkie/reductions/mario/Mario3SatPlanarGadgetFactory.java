package com.github.btrekkie.reductions.mario;

import com.github.btrekkie.reductions.planar.I3SatPlanarGadgetFactory;
import com.github.btrekkie.reductions.planar.IPlanarGadget;

/** A I3SatPlanarGadgetFactory implementation for MarioProblem. */
/* The major gadgets are taken from https://arxiv.org/pdf/1203.1895.pdf (Aloupis, Demaine, Guo, Viglietta (2012):
 * Classic Nintendo Games are (Computationally) Hard).
 */
public class Mario3SatPlanarGadgetFactory implements I3SatPlanarGadgetFactory {
    /** The singleton instance of Mario3SatPlanarGadgetFactory. */
    public static final Mario3SatPlanarGadgetFactory instance = new Mario3SatPlanarGadgetFactory();

    private Mario3SatPlanarGadgetFactory() {

    }

    @Override
    public IPlanarGadget createVariable() {
        return new MarioVariableGadget();
    }

    @Override
    public IPlanarGadget createClause() {
        return new MarioClauseGadget();
    }

    @Override
    public IPlanarGadget createCrossover(boolean isClockwise) {
        return new MarioCrossoverGadget(isClockwise);
    }

    @Override
    public IPlanarGadget createJunction() {
        return new MarioJunctionGadget();
    }

    @Override
    public int minVariableEntryPort() {
        return MarioVariableGadget.MIN_ENTRY_PORT;
    }

    @Override
    public int maxVariableEntryPort() {
        return MarioVariableGadget.MAX_ENTRY_PORT;
    }

    @Override
    public int minVariableExitPort() {
        return MarioVariableGadget.MIN_EXIT_PORT;
    }

    @Override
    public int maxVariableExitPort() {
        return MarioVariableGadget.MAX_EXIT_PORT;
    }

    @Override
    public int minClausePort() {
        return MarioClauseGadget.MIN_CLAUSE_PORT;
    }

    @Override
    public int maxClausePort() {
        return MarioClauseGadget.MAX_CLAUSE_PORT;
    }

    @Override
    public int clauseEntryPort() {
        return MarioClauseGadget.ENTRY_PORT;
    }

    @Override
    public int clauseExitPort() {
        return MarioClauseGadget.EXIT_PORT;
    }

    @Override
    public int firstCrossoverEntryPort(IPlanarGadget gadget, boolean isClockwise) {
        return ((MarioCrossoverGadget)gadget).firstEntryPort();
    }

    @Override
    public int firstCrossoverExitPort(IPlanarGadget gadget, boolean isClockwise) {
        return ((MarioCrossoverGadget)gadget).firstExitPort();
    }

    @Override
    public int secondCrossoverEntryPort(IPlanarGadget gadget, boolean isClockwise) {
        return ((MarioCrossoverGadget)gadget).secondEntryPort();
    }

    @Override
    public int secondCrossoverExitPort(IPlanarGadget gadget, boolean isClockwise) {
        return ((MarioCrossoverGadget)gadget).secondExitPort();
    }
}
