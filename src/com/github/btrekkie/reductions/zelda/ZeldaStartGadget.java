package com.github.btrekkie.reductions.zelda;

import java.util.Collections;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A start gadget for ZeldaProblem, where Link starts the game. */
public class ZeldaStartGadget extends ZeldaGadget {
    @Override
    public ZeldaTile[][] tiles() {
        String[] rows = new String[]{
            "   ",
            " L ",
            "   ",
            "   "};
        return tiles(rows);
    }

    @Override
    public List<Point> ports() {
        return Collections.singletonList(new Point(3, 2));
    }
}
