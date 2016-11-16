package com.github.btrekkie.reductions.push1;

/** The contents of a 1 x 1 cell in a Push1Problem. */
public enum Push1Tile {
    /** A tile containing a block. */
    BLOCK,

    /**
     * A tile containing a block that, we assert, the robot can never move.  The robot would never be able to move the
     * block even if the robot started at any unoccupied tile.
     */
    EFFECTIVELY_IMMOVABLE_BLOCK,

    /** A goal tile, for the area the robot is trying to reach. */
    FINISH,

    /** A ground (unobstructed) tile. */
    GROUND,

    /** A robot tile indicating the robot's starting location. */
    ROBOT,
}
