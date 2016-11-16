package com.github.btrekkie.reductions.zelda;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A variable gadget for ZeldaProblem, as in I3SatPlanarGadgetFactory.createVariable(). */
public class ZeldaVariableGadget extends ZeldaGadget {
    /** The minimum index of the entry ports, as in I3SatPlanarGadgetFactory.minVariableEntryPort(). */
    public static final int MIN_ENTRY_PORT = 0;

    /** The maximum index of the entry ports, as in I3SatPlanarGadgetFactory.maxVariableEntryPort(). */
    public static final int MAX_ENTRY_PORT = 1;

    /** The minimum index of the exit ports, as in I3SatPlanarGadgetFactory.minVariableExitPort(). */
    public static final int MIN_EXIT_PORT = 2;

    /** The maximum index of the exit ports, as in I3SatPlanarGadgetFactory.minVariableExitPort(). */
    public static final int MAX_EXIT_PORT = 3;

    @Override
    public ZeldaTile[][] tiles() {
        String[] rows = new String[]{
            "****  *****",
            "***** *****",
            "  o *******",
            "** ********",
            "** ********",
            "**       **",
            "******** **",
            "****  ** **",
            "***** ** **",
            "  o **** **",
            "** ***** **",
            "** ***** **",
            "**       **",
            "******** **",
            "******** **",
            "*****    **",
            "***** ** **",
            "***** ** **",
            "**   o   **",
            "   ** *****",
            "***** *****",
            "*****  ****",
            "****** ****",
            "****** ****"};
        return tiles(rows);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 9), new Point(0, 2), new Point(6, 24), new Point(0, 19));
    }
}
