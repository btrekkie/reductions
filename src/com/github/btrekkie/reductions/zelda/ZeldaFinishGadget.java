package com.github.btrekkie.reductions.zelda;

import java.util.Collections;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A finish gadget for ZeldaProblem, which we must reach to beat the game. */
public class ZeldaFinishGadget extends ZeldaGadget {
    @Override
    public ZeldaTile[][] tiles() {
        String[] rows = new String[]{
            "     ",
            " FFF ",
            " FFF ",
            " FFF ",
            "     "};
        return tiles(rows);
    }

    @Override
    public List<Point> ports() {
        return Collections.singletonList(new Point(0, 2));
    }
}
