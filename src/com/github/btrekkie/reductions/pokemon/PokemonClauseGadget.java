package com.github.btrekkie.reductions.pokemon;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.btrekkie.reductions.planar.Point;

/** A clause gadget for PokemonProblem, as in I3SatPlanarGadgetFactory.createClause(). */
public class PokemonClauseGadget extends PokemonGadget {
    /** The index of the entry port, as in I3SatPlanarGadgetFactory.clauseEntryPort(). */
    public static final int ENTRY_PORT = 3;

    /** The index of the entry port, as in I3SatPlanarGadgetFactory.clauseExitPort(). */
    public static final int EXIT_PORT = 4;

    /** The minimum index of the clause ports, as in I3SatPlanarGadgetFactory.minClausePort(). */
    public static final int MIN_CLAUSE_PORT = 0;

    /** The maximum index of the clause ports, as in I3SatPlanarGadgetFactory.minClausePort(). */
    public static final int MAX_CLAUSE_PORT = 2;

    @Override
    public PokemonTile[][] tiles() {
        String[] rows = new String[]{
            "**** * * **",
            "**** * * **",
            "    d d dL*",
            "** U * * **",
            "**         ",
            "***********"};
        return tiles(rows);
    }

    @Override
    public Map<Point, Integer> sightLimits() {
        return Collections.singletonMap(new Point(9, 2), 7);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(4, 0), new Point(6, 0), new Point(8, 0), new Point(11, 4), new Point(0, 3));
    }
}
