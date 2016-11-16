package com.github.btrekkie.reductions.zelda;

import com.github.btrekkie.reductions.planar.I3SatPlanarGadgetFactory;
import com.github.btrekkie.reductions.planar.IPlanarGadget;

/** A I3SatPlanarGadgetFactory implementation for ZeldaProblem. */
/* The major gadgets are taken from https://arxiv.org/pdf/1203.1895.pdf (Aloupis, Demaine, Guo, Viglietta (2012):
 * Classic Nintendo Games are (Computationally) Hard) and the paper it references,
 * https://arxiv.org/pdf/cs/0007021v2.pdf (Demaine, Demaine, and O'Rourke (2000): PushPush and Push-1 are NP-hard in
 * 2D).
 */
public class Zelda3SatPlanarGadgetFactory implements I3SatPlanarGadgetFactory {
    /** The singleton instance of Zelda3SatPlanarGadgetFactory. */
    public static final Zelda3SatPlanarGadgetFactory instance = new Zelda3SatPlanarGadgetFactory();

    private Zelda3SatPlanarGadgetFactory() {

    }

    @Override
    public IPlanarGadget createVariable() {
        return new ZeldaVariableGadget();
    }

    @Override
    public IPlanarGadget createClause() {
        return new ZeldaClauseGadget();
    }

    @Override
    public IPlanarGadget createCrossover(boolean isClockwise) {
        return new ZeldaCrossoverGadget(isClockwise);
    }

    @Override
    public IPlanarGadget createJunction() {
        return new ZeldaJunctionGadget();
    }

    @Override
    public int minVariableEntryPort() {
        return ZeldaVariableGadget.MIN_ENTRY_PORT;
    }

    @Override
    public int maxVariableEntryPort() {
        return ZeldaVariableGadget.MAX_ENTRY_PORT;
    }

    @Override
    public int minVariableExitPort() {
        return ZeldaVariableGadget.MIN_EXIT_PORT;
    }

    @Override
    public int maxVariableExitPort() {
        return ZeldaVariableGadget.MAX_EXIT_PORT;
    }

    @Override
    public int minClausePort() {
        return ZeldaClauseGadget.MIN_CLAUSE_PORT;
    }

    @Override
    public int maxClausePort() {
        return ZeldaClauseGadget.MAX_CLAUSE_PORT;
    }

    @Override
    public int clauseEntryPort() {
        return ZeldaClauseGadget.ENTRY_PORT;
    }

    @Override
    public int clauseExitPort() {
        return ZeldaClauseGadget.EXIT_PORT;
    }

    @Override
    public int firstCrossoverEntryPort(IPlanarGadget gadget, boolean isClockwise) {
        return ((ZeldaCrossoverGadget)gadget).firstEntryPort();
    }

    @Override
    public int firstCrossoverExitPort(IPlanarGadget gadget, boolean isClockwise) {
        return ((ZeldaCrossoverGadget)gadget).firstExitPort();
    }

    @Override
    public int secondCrossoverEntryPort(IPlanarGadget gadget, boolean isClockwise) {
        return ((ZeldaCrossoverGadget)gadget).secondEntryPort();
    }

    @Override
    public int secondCrossoverExitPort(IPlanarGadget gadget, boolean isClockwise) {
        return ((ZeldaCrossoverGadget)gadget).secondExitPort();
    }
}
