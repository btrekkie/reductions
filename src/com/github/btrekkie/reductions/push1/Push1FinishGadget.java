package com.github.btrekkie.reductions.push1;

import java.util.Collections;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A finish gadget for Push1Problem, which we must reach to beat the game. */
public class Push1FinishGadget extends Push1Gadget {
    @Override
    public Push1Tile[][] tiles() {
        String[] rows = new String[]{
            "     ",
            " FFF ",
            " FFF ",
            " FFF ",
            "     ",
            "     "};
        return tiles(rows);
    }

    @Override
    public List<Point> ports() {
        return Collections.singletonList(new Point(0, 3));
    }
}
