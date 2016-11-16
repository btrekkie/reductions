package com.github.btrekkie.reductions.mario;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.btrekkie.reductions.planar.Point;

/** A finish gadget for MarioProblem, which we must reach to beat the level. */
public class MarioFinishGadget extends MarioGadget {
    @Override
    public MarioTile[][] tiles() {
        String[] rows = new String[]{
            "                    ",
            "                    ",
            "                    ",
            "                    ",
            "                    ",
            "                    ",
            "                    ",
            "BBBBBbB             ",
            "      B             ",
            "      B             ",
            "      B   F   C     ",
            "GGGGGGGGGGGGGGGGGGGG",
            "GGGGGGGGGGGGGGGGGGGG"};
        Map<Character, MarioTile> tileChars = new HashMap<Character, MarioTile>();
        tileChars.put(' ', MarioTile.AIR);
        tileChars.put('B', MarioTile.BLOCK);
        tileChars.put('b', MarioTile.BRICK);
        tileChars.put('C', MarioTile.CASTLE);
        tileChars.put('F', MarioTile.FLAG);
        tileChars.put('G', MarioTile.GROUND_BLOCK);
        return tiles(rows, tileChars);
    }

    @Override
    public List<Point> ports() {
        return Collections.singletonList(new Point(0, 11));
    }
}
