package com.github.btrekkie.reductions.planar.test;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.Point;

/** A test gadget for I3SatPlanarGadgetFactory.createCrossover().  See the comments for that method. */
class TestCrossoverGadget implements IPlanarGadget {
    /**
     * The index of one of the entry ports - corresponding to EXIT_PORT1 and immediately counterclockwise from
     * ENTRY_PORT2 - as in I3SatPlanarGadgetFactory.firstCrossoverEntryPort or
     * I3SatPlanarGadgetFactory.secondCrossoverEntryPort.
     */
    public static final int ENTRY_PORT1 = 0;

    /**
     * The index of one of the exit ports - corresponding to ENTRY_PORT1 and immediately counterclockwise from
     * EXIT_PORT2 - as in I3SatPlanarGadgetFactory.firstCrossoverExitPort or
     * I3SatPlanarGadgetFactory.secondCrossoverExitPort.
     */
    public static final int EXIT_PORT1 = 2;

    /**
     * The index of one of the entry ports - corresponding to EXIT_PORT2 and immediately clockwise from ENTRY_PORT1 - as
     * in I3SatPlanarGadgetFactory.firstCrossoverEntryPort or I3SatPlanarGadgetFactory.secondCrossoverEntryPort.
     */
    public static final int ENTRY_PORT2 = 1;

    /**
     * The index of one of the exit ports - corresponding to ENTRY_PORT2 and immediately clockwise from EXIT_PORT1 - as
     * in I3SatPlanarGadgetFactory.firstCrossoverExitPort or I3SatPlanarGadgetFactory.secondCrossoverExitPort.
     */
    public static final int EXIT_PORT2 = 3;

    @Override
    public int width() {
        return 20;
    }

    @Override
    public int height() {
        return 20;
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 10), new Point(10, 0), new Point(20, 10), new Point(10, 20));
    }
}
