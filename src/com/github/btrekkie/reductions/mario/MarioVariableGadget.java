package com.github.btrekkie.reductions.mario;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.btrekkie.reductions.planar.Point;

/** A variable gadget for MarioProblem, as in I3SatPlanarGadgetFactory.createVariable(). */
public class MarioVariableGadget extends MarioGadget {
    /** The minimum index of the entry ports, as in I3SatPlanarGadgetFactory.minVariableEntryPort(). */
    public static final int MIN_ENTRY_PORT = 0;

    /** The maximum index of the entry ports, as in I3SatPlanarGadgetFactory.maxVariableEntryPort(). */
    public static final int MAX_ENTRY_PORT = 1;

    /** The minimum index of the exit ports, as in I3SatPlanarGadgetFactory.minVariableExitPort(). */
    public static final int MIN_EXIT_PORT = 2;

    /** The maximum index of the exit ports, as in I3SatPlanarGadgetFactory.minVariableExitPort(). */
    public static final int MAX_EXIT_PORT = 3;

    @Override
    public MarioTile[][] tiles() {
        String[] rows = new String[]{
            "BBBBBBBBBBBBBBB",
            "BBBBBBBBBBBBBBB",
            "BBBBBBBBBBBBBBB",
            "BBBBBBBBBBBBBBB",
            "BBBBBBBBBBBBBBB",
            "BBBBBBBBBBBBBBB",
            "BBBBBBBBBBBBBBB",
            "               ",
            "               ",
            "BBB       BBBBB",
            "B           BBB",
            "B           BBB",
            "B           BBB",
            "B           BBB",
            "B           BBB",
            "B           BBB",
            "B  BBBBBBB  BBB",
            "B  B     B  BBB",
            "B  B     B  BBB",
            "B  B     B  BBB",
            "B  B     B  BBB",
            "B  B     B  BBB",
            "B  BBBBBBB  BBB"};
        Map<Character, MarioTile> tileChars = new HashMap<Character, MarioTile>();
        tileChars.put(' ', MarioTile.AIR);
        tileChars.put('B', MarioTile.BLOCK);
        return tiles(rows, tileChars);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 9), new Point(15, 9), new Point(10, 23), new Point(1, 23));
    }
}
