package com.github.btrekkie.reductions.pokemon;

/** The contents of a 1 x 1 cell in a PokemonProblem. */
public enum PokemonTile {
    /** A goal tile, for the area the player is trying to reach. */
    FINISH,

    /** A ground (unoccupied) tile. */
    GROUND,

    /** A player tile.  This indicates the player's starting location. */
    PLAYER,

    /** A tile consisting of a rock that obstructs the player's movement. */
    ROCK,

    /**
     * A strong trainer facing downward.  A strong trainer always defeats the player, per the construction described in
     * https://arxiv.org/pdf/1203.1895.pdf (Aloupis, Demaine, Guo, Viglietta (2012): Classic Nintendo Games are
     * (Computationally) Hard).
     */
    STRONG_TRAINER_DOWN,

    /**
     * A strong trainer facing left.  A strong trainer always defeats the player, per the construction described in
     * https://arxiv.org/pdf/1203.1895.pdf (Aloupis, Demaine, Guo, Viglietta (2012): Classic Nintendo Games are
     * (Computationally) Hard).
     */
    STRONG_TRAINER_LEFT,

    /**
     * A strong trainer facing right.  A strong trainer always defeats the player, per the construction described in
     * https://arxiv.org/pdf/1203.1895.pdf (Aloupis, Demaine, Guo, Viglietta (2012): Classic Nintendo Games are
     * (Computationally) Hard).
     */
    STRONG_TRAINER_RIGHT,

    /**
     * A strong trainer facing upward.  A strong trainer always defeats the player, per the construction described in
     * https://arxiv.org/pdf/1203.1895.pdf (Aloupis, Demaine, Guo, Viglietta (2012): Classic Nintendo Games are
     * (Computationally) Hard).
     */
    STRONG_TRAINER_UP,

    /**
     * A weak trainer facing downward.  A weak trainer never defeats the player, per the construction described in
     * https://arxiv.org/pdf/1203.1895.pdf (Aloupis, Demaine, Guo, Viglietta (2012): Classic Nintendo Games are
     * (Computationally) Hard).
     */
    WEAK_TRAINER_DOWN,

    /**
     * A weak trainer facing right.  A weak trainer never defeats the player, per the construction described in
     * https://arxiv.org/pdf/1203.1895.pdf (Aloupis, Demaine, Guo, Viglietta (2012): Classic Nintendo Games are
     * (Computationally) Hard).
     */
    WEAK_TRAINER_RIGHT,
}
