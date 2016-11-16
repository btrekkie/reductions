package com.github.btrekkie.reductions.zelda;

/** The contents of a 1 x 1 cell in a ZeldaProblem. */
public enum ZeldaTile {
    /** A tile consisting of a barrier that obstructs the player's movement. */
    BARRIER,

    /** A block tile, containing a block that Link may push, but only once and only to an unobstructed tile. */
    BLOCK,

    /** A goal tile, for the area Link is trying to reach. */
    FINISH,

    /** A ground (unobstructed) tile. */
    GROUND,

    /** A Link tile indicating Link's starting location. */
    LINK,
}
