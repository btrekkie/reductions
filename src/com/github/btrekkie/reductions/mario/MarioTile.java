package com.github.btrekkie.reductions.mario;

/**
 * The contents of a 1 x 1 cell in a MarioProblem.  The contents of a tile may leak into other cells.  For example, the
 * castle occupies not only its own cell, but also cells above it and to the right.  However, we still use just one
 * castle tile (per castle).  MarioTile objects represent movable objects such as goombas, because this representation
 * is more useful for the purposes of MarioProblem.
 */
public enum MarioTile {
    /** An air (unoccupied) tile. */
    AIR,

    /** A tile consisting of a "standard" block. */
    BLOCK,

    /** A tile consisting of a breakable block, which Mario can break by hitting it from below when he is large. */
    BRICK,

    /** A castle tile.  A castle fills a castle tile cell as well as a few cells above and to the right. */
    CASTLE,

    /**
     * A down-left fire bar tile.  This consists of a fire bar block and a fire bar extending down and to the left from
     * the cell.
     */
    FIRE_BAR_DOWN_LEFT,

    /**
     * An up-right fire bar tile.  This consists of a fire bar block and a fire bar extending up and to the right from
     * the cell.
     */
    FIRE_BAR_UP_RIGHT,

    /**
     * A flag (goal) tile.  A flag fills a flag tile cell as well as a few cells above and to the left.  A flag includes
     * a block that is the base of the flag.
     */
    FLAG,

    /** A goomba tile, consisting of an air block with a goomba in it. */
    GOOMBA,

    /** A tile consisting of a block styled for Mario to walk on. */
    GROUND_BLOCK,

    /** A Mario tile, consisting of an air block with a small Mario in it.  This indicates Mario's starting location. */
    MARIO,

    /** A tile consisting of a question mark block with a mushroom in it. */
    QUESTION_MARK_MUSHROOM,

    /** A tile consisting of a question mark block with an invincibility star in it. */
    QUESTION_MARK_STAR,
}
