package com.github.btrekkie.reductions.mario;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.btrekkie.reductions.planar.Point;

/** A junction gadget for MarioProblem, as in I3SatPlanarGadgetFactory.createJunction(). */
public class MarioJunctionGadget extends MarioGadget {
    @Override
    public MarioTile[][] tiles() {
        String[] rows = new String[]{
            "      ",
            "      ",
            "BB  BB",
            "      ",
            "      ",
            "      ",
            "  BB  ",
            "      ",
            "      ",
            "B    B",
            "BB  BB"};
        Map<Character, MarioTile> tileChars = new HashMap<Character, MarioTile>();
        tileChars.put(' ', MarioTile.AIR);
        tileChars.put('B', MarioTile.BLOCK);
        return tiles(rows, tileChars);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 9), new Point(1, 0), new Point(6, 9), new Point(1, 11));
    }
}
