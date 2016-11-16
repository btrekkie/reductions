package com.github.btrekkie.reductions.planar.test;

import com.github.btrekkie.reductions.planar.I3SatPlanarGadgetFactory;
import com.github.btrekkie.reductions.planar.IPlanarGadget;

/** A I3SatPlanarGadgetFactory implementation for testing. */
class Test3SatPlanarGadgetFactory implements I3SatPlanarGadgetFactory {
    /** The singleton instance of Test3SatPlanarGadgetFactory. */
    public static final Test3SatPlanarGadgetFactory instance = new Test3SatPlanarGadgetFactory();

    public Test3SatPlanarGadgetFactory() {

    }

    @Override
    public IPlanarGadget createVariable() {
        return new TestVariableGadget();
    }

    @Override
    public IPlanarGadget createClause() {
        return new TestClauseGadget();
    }

    @Override
    public IPlanarGadget createCrossover(boolean isClockwise) {
        return new TestCrossoverGadget();
    }

    @Override
    public IPlanarGadget createJunction() {
        return new TestJunctionGadget();
    }

    @Override
    public int minVariableEntryPort() {
        return TestVariableGadget.MIN_ENTRY_PORT;
    }

    @Override
    public int maxVariableEntryPort() {
        return TestVariableGadget.MAX_ENTRY_PORT;
    }

    @Override
    public int minVariableExitPort() {
        return TestVariableGadget.MIN_EXIT_PORT;
    }

    @Override
    public int maxVariableExitPort() {
        return TestVariableGadget.MAX_EXIT_PORT;
    }

    @Override
    public int minClausePort() {
        return TestClauseGadget.MIN_CLAUSE_PORT;
    }

    @Override
    public int maxClausePort() {
        return TestClauseGadget.MAX_CLAUSE_PORT;
    }

    @Override
    public int clauseEntryPort() {
        return TestClauseGadget.ENTRY_PORT;
    }

    @Override
    public int clauseExitPort() {
        return TestClauseGadget.EXIT_PORT;
    }

    @Override
    public int firstCrossoverEntryPort(IPlanarGadget gadget, boolean isClockwise) {
        return isClockwise ? TestCrossoverGadget.ENTRY_PORT1 : TestCrossoverGadget.ENTRY_PORT2;
    }

    @Override
    public int firstCrossoverExitPort(IPlanarGadget gadget, boolean isClockwise) {
        return isClockwise ? TestCrossoverGadget.EXIT_PORT1 : TestCrossoverGadget.EXIT_PORT2;
    }

    @Override
    public int secondCrossoverEntryPort(IPlanarGadget gadget, boolean isClockwise) {
        return isClockwise ? TestCrossoverGadget.ENTRY_PORT2 : TestCrossoverGadget.ENTRY_PORT1;
    }

    @Override
    public int secondCrossoverExitPort(IPlanarGadget gadget, boolean isClockwise) {
        return isClockwise ? TestCrossoverGadget.EXIT_PORT2 : TestCrossoverGadget.EXIT_PORT1;
    }
}
