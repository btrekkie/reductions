package com.github.btrekkie.reductions.mario;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.btrekkie.reductions.planar.Point;

/** A turn wire gadget for MarioProblem, as in IPlanarWireFactory.turnWire. */
public class MarioTurnWireGadget extends MarioGadget {
    @Override
    public MarioTile[][] tiles() {
        String[] rows = new String[]{
            "    ",
            "    ",
            "B  B",
            "    ",
            "    ",
            " BB ",
            "    ",
            "    ",
            "B  B"};
        Map<Character, MarioTile> tileChars = new HashMap<Character, MarioTile>();
        tileChars.put(' ', MarioTile.AIR);
        tileChars.put('B', MarioTile.BLOCK);
        return tiles(rows, tileChars);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 8), new Point(0, 0), new Point(4, 8), new Point(0, 9));
    }
}
