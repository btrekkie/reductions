package com.github.btrekkie.reductions.planar.test;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.Point;

/** A test gadget for I3SatPlanarGadgetFactory.createVariable().  See the comments for that method. */
class TestVariableGadget implements IPlanarGadget {
    /** The minimum index of the entry ports, as in I3SatPlanarGadgetFactory.minVariableEntryPort(). */
    public static final int MIN_ENTRY_PORT = 0;

    /** The maximum index of the entry ports, as in I3SatPlanarGadgetFactory.maxVariableEntryPort(). */
    public static final int MAX_ENTRY_PORT = 1;

    /** The minimum index of the exit ports, as in I3SatPlanarGadgetFactory.minVariableExitPort(). */
    public static final int MIN_EXIT_PORT = 2;

    /** The maximum index of the exit ports, as in I3SatPlanarGadgetFactory.minVariableExitPort(). */
    public static final int MAX_EXIT_PORT = 3;

    @Override
    public int width() {
        return 40;
    }

    @Override
    public int height() {
        return 30;
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 20), new Point(0, 10), new Point(15, 0), new Point(20, 30));
    }
}
