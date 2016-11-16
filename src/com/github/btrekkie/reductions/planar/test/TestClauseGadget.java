package com.github.btrekkie.reductions.planar.test;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.Point;

/** A test gadget for I3SatPlanarGadgetFactory.createClause().  See the comments for that method. */
class TestClauseGadget implements IPlanarGadget {
    /** The index of the entry port, as in I3SatPlanarGadgetFactory.clauseEntryPort(). */
    public static final int ENTRY_PORT = 0;

    /** The index of the exit port, as in I3SatPlanarGadgetFactory.clauseExitPort(). */
    public static final int EXIT_PORT = 1;

    /** The minimum index of the clause ports, as in I3SatPlanarGadgetFactory.minClausePort(). */
    public static final int MIN_CLAUSE_PORT = 2;

    /** The maximum index of the clause ports, as in I3SatPlanarGadgetFactory.maxClausePort(). */
    public static final int MAX_CLAUSE_PORT = 4;

    @Override
    public int width() {
        return 60;
    }

    @Override
    public int height() {
        return 10;
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(
            new Point(10, 0), new Point(45, 0), new Point(45, 10), new Point(35, 10), new Point(15, 10));
    }
}
