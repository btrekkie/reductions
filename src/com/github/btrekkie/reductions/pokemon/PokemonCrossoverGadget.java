package com.github.btrekkie.reductions.pokemon;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.btrekkie.reductions.planar.Point;

/** A crossover gadget for PokemonProblem, as in I3SatPlanarGadgetFactory.createCrossover(). */
public class PokemonCrossoverGadget extends PokemonGadget {
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
    public PokemonTile[][] tiles() {
        String[] rows = new String[]{
            "********* *************",
            "********* *************",
            "*********       d******",
            "*********D ***** ******",
            "*******Rd  ***** ******",
            "******** * ***** ******",
            "**     *   ***** ******",
            "** R * * ***D*** L*****",
            "   *r      *r        **",
            "** *U*** * R * * *** **",
            "** ***   *     * *** **",
            "** *** D******** *** **",
            "** ***  dL****** *** **",
            "** *** * ******* *** **",
            "** ***   ******* *** **",
            "** ***** ******R D** **",
            "**r                  **",
            "*******U ******U L* ***",
            "******** ******* **    ",
            "******** *******   ****",
            "********         * ****",
            "****************** ****",
            "****************** ****"};
        return tiles(rows);
    }

    @Override
    public Map<Point, Integer> sightLimits() {
        Map<Point, Integer> sightLimits = new HashMap<Point, Integer>();
        sightLimits.put(new Point(8, 4), 2);
        sightLimits.put(new Point(4, 8), 2);
        sightLimits.put(new Point(12, 8), 2);
        sightLimits.put(new Point(8, 12), 2);
        return sightLimits;
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 8), new Point(9, 0), new Point(23, 18), new Point(18, 23));
    }
}
