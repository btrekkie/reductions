package com.github.btrekkie.reductions.zelda;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A clause gadget for ZeldaProblem, as in I3SatPlanarGadgetFactory.createClause(). */
public class ZeldaClauseGadget extends ZeldaGadget {
    /** The index of the entry port, as in I3SatPlanarGadgetFactory.clauseEntryPort(). */
    public static final int ENTRY_PORT = 4;

    /** The index of the entry port, as in I3SatPlanarGadgetFactory.clauseExitPort(). */
    public static final int EXIT_PORT = 3;

    /** The minimum index of the clause ports, as in I3SatPlanarGadgetFactory.minClausePort(). */
    public static final int MIN_CLAUSE_PORT = 0;

    /** The maximum index of the clause ports, as in I3SatPlanarGadgetFactory.minClausePort(). */
    public static final int MAX_CLAUSE_PORT = 2;

    @Override
    public ZeldaTile[][] tiles() {
        String[] rows = new String[]{
            "****************",
            "     ***********",
            "**** ***********",
            "  ** ***********",
            "  ** ***********",
            "**** ***********",
            "****o   ********",
            "   o ** ********",
            "*** *** ********",
            "*** *** ********",
            "***     ********",
            "  ** ***********",
            "  ** ***********",
            "**** ***********",
            "****o   ********",
            "   o ** ********",
            "*** *** ********",
            "*** *** ********",
            "***            *",
            "******** ***** *",
            "******** ***** *",
            "   ****  ***** *",
            "**   **o ***** *",
            "**oo   oo   ** *",
            "** oo oo **    *",
            "*** ooo ********",
            "****   *********",
            "***** **********",
            "***  o**********",
            "*** * **********",
            "*** ************"};
        return tiles(rows);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 15), new Point(0, 7), new Point(0, 2), new Point(3, 31), new Point(0, 21));
    }
}
