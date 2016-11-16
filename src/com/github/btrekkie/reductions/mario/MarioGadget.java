package com.github.btrekkie.reductions.mario;

import java.util.Map;

import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.PlanarGadgetUtil;

/**
 * A gadget in a MarioProblem.  If the contents of a MarioTile leaks into other cells, as with MarioTile.CASTLE, then
 * the cells MarioTile must be in the same gadget as the cells into which the contents leak.
 */
public abstract class MarioGadget implements IPlanarGadget {
    /** The width of the gadget.  This is negative if we have not computed it yet. */
    private int width = -1;

    /** The height of the gadget.  This is negative if we have not computed it yet. */
    private int height = -1;

    /**
     * Returns the tiles that comprise the gadget, so that tiles()[y][x] is the tile whose top-left corner is at (x, y)
     * relative to the top-left corner of the gadget.
     */
    public abstract MarioTile[][] tiles();

    /** Sets "width" and "height" if they are negative. */
    private void ensureSizeComputed() {
        if (width < 0) {
            MarioTile[][] tiles = tiles();
            height = tiles.length;
            if (height > 0) {
                width = tiles[0].length;
            } else {
                width = 0;
            }
        }
    }

    /**
     * Returns the width of the gadget.  This must be positive.  The default implementation obtains the width from the
     * return value of tiles().
     */
    public int width() {
        ensureSizeComputed();
        return width;
    }

    /**
     * Returns the height of the gadget.  This must be positive.  The default implementation obtains the height from the
     * return value of tiles().
     */
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
    public MarioTile[][] tiles(int minX, int minY, int width, int height) {
        assertIsInBounds(minX, minY, width, height);
        MarioTile[][] tiles = new MarioTile[height][width];
        PlanarGadgetUtil.tiles(tiles(), minX, minY, width, height, tiles);
        return tiles;
    }

    /**
     * Returns the matrix of tiles represented by the specified string array.  tiles[y][x] is given by
     * tileChars.get(rows[y].charAt(x)), where "tiles" is the return value.  If we use this method to compute a gadget's
     * tiles, then we have in the "rows" argument a visual representation of the gadget.
     * @param rows The strings.  This must not be empty.
     * @param tileChars A map from each character in "rows" to the corresponding tile.  The tiles may not be null.
     * @return The tiles.
     */
    protected MarioTile[][] tiles(String[] rows, Map<Character, MarioTile> tileChars) {
        MarioTile[][] tiles = new MarioTile[rows.length][rows[0].length()];
        PlanarGadgetUtil.tiles(rows, tileChars, tiles);
        return tiles;
    }
}
