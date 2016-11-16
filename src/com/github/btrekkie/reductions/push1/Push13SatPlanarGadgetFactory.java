package com.github.btrekkie.reductions.push1;

import com.github.btrekkie.reductions.planar.I3SatPlanarGadgetFactory;
import com.github.btrekkie.reductions.planar.IPlanarGadget;

/** A I3SatPlanarGadgetFactory implementation for Push1Problem. */
/* The major gadgets are taken from https://arxiv.org/pdf/cs/0007021v2.pdf (Demaine, Demaine, and O'Rourke (2000):
 * PushPush and Push-1 are NP-hard in 2D).
 */
public class Push13SatPlanarGadgetFactory implements I3SatPlanarGadgetFactory {
    /** The singleton instance of Push13SatPlanarGadgetFactory. */
    public static final Push13SatPlanarGadgetFactory instance = new Push13SatPlanarGadgetFactory();

    private Push13SatPlanarGadgetFactory() {

    }

    @Override
    public IPlanarGadget createVariable() {
        return new Push1VariableGadget();
    }

    @Override
    public IPlanarGadget createClause() {
        return new Push1ClauseGadget();
    }

    @Override
    public IPlanarGadget createCrossover(boolean isClockwise) {
        return new Push1CrossoverGadget(isClockwise);
    }

    @Override
    public IPlanarGadget createJunction() {
        return new Push1JunctionGadget();
    }

    @Override
    public int minVariableEntryPort() {
        return Push1VariableGadget.MIN_ENTRY_PORT;
    }

    @Override
    public int maxVariableEntryPort() {
        return Push1VariableGadget.MAX_ENTRY_PORT;
    }

    @Override
    public int minVariableExitPort() {
        return Push1VariableGadget.MIN_EXIT_PORT;
    }

    @Override
    public int maxVariableExitPort() {
        return Push1VariableGadget.MAX_EXIT_PORT;
    }

    @Override
    public int minClausePort() {
        return Push1ClauseGadget.MIN_CLAUSE_PORT;
    }

    @Override
    public int maxClausePort() {
        return Push1ClauseGadget.MAX_CLAUSE_PORT;
    }

    @Override
    public int clauseEntryPort() {
        return Push1ClauseGadget.ENTRY_PORT;
    }

    @Override
    public int clauseExitPort() {
        return Push1ClauseGadget.EXIT_PORT;
    }

    @Override
    public int firstCrossoverEntryPort(IPlanarGadget gadget, boolean isClockwise) {
        return ((Push1CrossoverGadget)gadget).firstEntryPort();
    }

    @Override
    public int firstCrossoverExitPort(IPlanarGadget gadget, boolean isClockwise) {
        return ((Push1CrossoverGadget)gadget).firstExitPort();
    }

    @Override
    public int secondCrossoverEntryPort(IPlanarGadget gadget, boolean isClockwise) {
        return ((Push1CrossoverGadget)gadget).secondEntryPort();
    }

    @Override
    public int secondCrossoverExitPort(IPlanarGadget gadget, boolean isClockwise) {
        return ((Push1CrossoverGadget)gadget).secondExitPort();
    }
}
