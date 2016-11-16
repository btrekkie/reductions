package com.github.btrekkie.reductions.pokemon;

import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A variable gadget for PokemonProblem, as in I3SatPlanarGadgetFactory.createVariable(). */
public class PokemonVariableGadget extends PokemonGadget {
    /** The minimum index of the entry ports, as in I3SatPlanarGadgetFactory.minVariableEntryPort(). */
    public static final int MIN_ENTRY_PORT = 0;

    /** The maximum index of the entry ports, as in I3SatPlanarGadgetFactory.maxVariableEntryPort(). */
    public static final int MAX_ENTRY_PORT = 1;

    /** The minimum index of the exit ports, as in I3SatPlanarGadgetFactory.minVariableExitPort(). */
    public static final int MIN_EXIT_PORT = 2;

    /** The maximum index of the exit ports, as in I3SatPlanarGadgetFactory.minVariableExitPort(). */
    public static final int MAX_EXIT_PORT = 3;

    @Override
    public PokemonTile[][] tiles() {
        String[] rows = new String[]{
            "****** *",
            "  **** *",
            "r  *  d*",
            "**   *  ",
            "r  *   *",
            "  ******",
            "********"};
        return tiles(rows);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 5), new Point(0, 2), new Point(6, 0), new Point(8, 3));
    }
}
