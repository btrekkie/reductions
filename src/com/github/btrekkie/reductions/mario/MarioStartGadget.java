package com.github.btrekkie.reductions.mario;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.btrekkie.reductions.planar.Point;

/** A start gadget for MarioProblem, where Mario starts the level. */
public class MarioStartGadget extends MarioGadget {
    @Override
    public MarioTile[][] tiles() {
        String[] rows = new String[]{
            "BBBBBBB",
            "BBBBBBB",
            "       ",
            "       ",
            "       ",
            "    Q  ",
            "       ",
            "       ",
            "  M    ",
            "GGGGGGG",
            "GGGGGGG"};
        Map<Character, MarioTile> tileChars = new HashMap<Character, MarioTile>();
        tileChars.put(' ', MarioTile.AIR);
        tileChars.put('B', MarioTile.BLOCK);
        tileChars.put('G', MarioTile.GROUND_BLOCK);
        tileChars.put('M', MarioTile.MARIO);
        tileChars.put('Q', MarioTile.QUESTION_MARK_MUSHROOM);
        return tiles(rows, tileChars);
    }

    @Override
    public List<Point> ports() {
        return Collections.singletonList(new Point(7, 9));
    }
}
