package com.github.btrekkie.reductions.pokemon;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.PlanarGadgetUtil;
import com.github.btrekkie.reductions.planar.Point;

/**
 * A gadget in a PokemonProblem.  The gadget must entirely contain the sight line of any trainer in the gadget (other
 * than the player).  For performance reasons, any type of PokemonGadget that may have a trainer must have a bounded
 * size.
 */
public abstract class PokemonGadget implements IPlanarGadget {
    /** The width of the gadget.  This is negative if we have not computed it yet. */
    private int width = -1;

    /** The height of the gadget.  This is negative if we have not computed it yet. */
    private int height = -1;

    /** The cached return value of hasTrainer().  This is unspecified if haveComputedHasTrainer is false. */
    private boolean hasTrainer;

    /** Whether we have computed the hasTrainer field. */
    private boolean haveComputedHasTrainer;

    /**
     * Returns the tiles that comprise the gadget, so that tiles()[y][x] is the tile whose top-left corner is at (x, y)
     * relative to the top-left corner of the gadget.
     */
    public abstract PokemonTile[][] tiles();

    /** Sets "width" and "height" if they are negative. */
    private void ensureSizeComputed() {
        if (width < 0) {
            PokemonTile[][] tiles = tiles();
            height = tiles.length;
            if (height > 0) {
                width = tiles[0].length;
            } else {
                width = 0;
            }
        }
    }

    public int width() {
        ensureSizeComputed();
        return width;
    }

    public int height() {
        ensureSizeComputed();
        return height;
    }

    /**
     * Throws an IllegalArgumentException if the specified rectangle is not a valid subrectangle of this gadget.  A
     * width or height of 0 is invalid.
     * @param minX The x coordinate of the left edge of the rectangle, relative to the top-left corner of the gadget.
     * @param minY The y coordinate of the top edge of the rectangle, relative to the top-left corner of the gadget.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     */
    protected final void assertIsInBounds(int minX, int minY, int width, int height) {
        if (minX < 0 || minY < 0 || minX + width > width() || minY + height > height()) {
            throw new IllegalArgumentException("The specified rectangle is out of bounds");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("The width and height must be positive");
        }
    }

    /**
     * Returns the tiles that comprise the specified rectangular region of the gadget, so that tiles[y][x] is the tile
     * whose top-left corner is at (minX + x, minY + y) relative to the top-left corner of the gadget, where "tiles" is
     * the return value.  The rectangle must be a subrectangle of this gadget.  "width" and "height" must be positive.
     * The default implementation obtains the tiles from the return value of tiles().
     * @param minX The x coordinate of the left edge of the rectangle, relative to the top-left corner of the gadget.
     * @param minY The y coordinate of the top edge of the rectangle, relative to the top-left corner of the gadget.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     * @return The tiles.
     */
    public PokemonTile[][] tiles(int minX, int minY, int width, int height) {
        assertIsInBounds(minX, minY, width, height);
        PokemonTile[][] tiles = new PokemonTile[height][width];
        PlanarGadgetUtil.tiles(tiles(), minX, minY, width, height, tiles);
        return tiles;
    }

    /**
     * Returns the matrix of tiles represented by the specified string array.  tiles[y][x] is given by
     * rows[y].charAt(x), where "tiles" is the return value.  This relationship between characters and tiles is as
     * follows:
     *
     * 'F': PokemonTile.FINISH
     * ' ': PokemonTile.GROUND
     * 'P': PokemonTile.PLAYER
     * '*': PokemonTile.ROCK
     * 'D': PokemonTile.STRONG_TRAINER_DOWN
     * 'L': PokemonTile.STRONG_TRAINER_LEFT
     * 'R': PokemonTile.STRONG_TRAINER_RIGHT
     * 'U': PokemonTile.STRONG_TRAINER_UP
     * 'd': PokemonTile.WEAK_TRAINER_DOWN
     * 'r': PokemonTile.WEAK_TRAINER_RIGHT
     *
     * If we use this method to compute a gadget's tiles, then we have in the "rows" argument a visual representation of
     * the gadget.
     *
     * @param rows The strings.  This must not be empty.
     * @return The tiles.
     */
    protected PokemonTile[][] tiles(String[] rows) {
        Map<Character, PokemonTile> tileChars = new HashMap<Character, PokemonTile>();
        tileChars.put('F', PokemonTile.FINISH);
        tileChars.put(' ', PokemonTile.GROUND);
        tileChars.put('P', PokemonTile.PLAYER);
        tileChars.put('*', PokemonTile.ROCK);
        tileChars.put('D', PokemonTile.STRONG_TRAINER_DOWN);
        tileChars.put('L', PokemonTile.STRONG_TRAINER_LEFT);
        tileChars.put('R', PokemonTile.STRONG_TRAINER_RIGHT);
        tileChars.put('U', PokemonTile.STRONG_TRAINER_UP);
        tileChars.put('d', PokemonTile.WEAK_TRAINER_DOWN);
        tileChars.put('r', PokemonTile.WEAK_TRAINER_RIGHT);
        PokemonTile[][] tiles = new PokemonTile[rows.length][rows[0].length()];
        PlanarGadgetUtil.tiles(rows, tileChars, tiles);
        return tiles;
    }

    /**
     * Returns whether the gadget contains a trainer (excluding the player).  The default implementation checks whether
     * sightLimits() is non-empty, then checks the return value of tiles().
     */
    public boolean hasTrainer() {
        if (!haveComputedHasTrainer) {
            if (!sightLimits().isEmpty()) {
                hasTrainer = true;
            } else {
                PokemonTile[][] tiles = tiles();
                for (PokemonTile[] row : tiles) {
                    for (PokemonTile tile : row) {
                        switch (tile) {
                            case STRONG_TRAINER_DOWN:
                            case STRONG_TRAINER_LEFT:
                            case STRONG_TRAINER_RIGHT:
                            case STRONG_TRAINER_UP:
                            case WEAK_TRAINER_DOWN:
                            case WEAK_TRAINER_RIGHT:
                                hasTrainer = true;
                                break;
                            default:
                                break;
                        }
                    }
                    if (hasTrainer) {
                        break;
                    }
                }
                haveComputedHasTrainer = true;
            }
        }
        return hasTrainer;
    }

    /**
     * Returns a map from the position of each trainer (other than the player) with a limited sight range to the maximum
     * number of tiles ahead of him that he can see (excluding the tile containing the trainer).  Any trainer not
     * identified in the map has an unlimited sight range, meaning he can see up to the next rock.  For such trainers,
     * the next rock must be contained in this gadget.  Positions are specified relative to the top-left corner of the
     * gadget.  The default implementation returns an empty map.
     */
    public Map<Point, Integer> sightLimits() {
        return Collections.emptyMap();
    }
}
