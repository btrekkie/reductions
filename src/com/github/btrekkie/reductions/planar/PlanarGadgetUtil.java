package com.github.btrekkie.reductions.planar;

import java.util.Map;

/** Provides utility methods for IPlanarGadget implementations. */
public class PlanarGadgetUtil {
    /**
     * Sets "tiles" to match the matrix of tiles represented by the specified string array.  tiles[y][x] is given by
     * tileChars.get(rows[y].charAt(x)).  If we use this method to compute a gadget's tiles, then we have in the "rows"
     * argument a visual representation of the gadget.
     * @param rows The strings.  This must not be empty.
     * @param tileChars A map from each character in "rows" to the corresponding tile.  The tiles may not be null.
     * @param tiles The matrix in which to store the results.
     */
    public static <T> void tiles(String[] rows, Map<Character, T> tileChars, T[][] tiles) {
        int rowIndex = 0;
        for (String row : rows) {
            T[] rowTiles = tiles[rowIndex];
            for (int columnIndex = 0; columnIndex < row.length(); columnIndex++) {
                char c = row.charAt(columnIndex);
                T tile = tileChars.get(c);
                if (tile == null) {
                    throw new IllegalArgumentException("Missing entry for tile \"" + c + "\"");
                }
                rowTiles[columnIndex] = tile;
            }
            rowIndex++;
        }
    }

    /**
     * Stores the tiles that comprise the specified rectangular region of the gadget in "tiles", so that tiles[y][x] is
     * given by allTiles[minY + y][minX + x].  "width" and "height" must be positive.
     * @param allTiles The tiles.
     * @param minX The x coordinate of the left edge of the rectangle.
     * @param minY The y coordinate of the top edge of the rectangle.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     * @param tiles The matrix in which to store the results.
     */
    public static <T> void tiles(T[][] allTiles, int minX, int minY, int width, int height, T[][] tiles) {
        if (minX < 0 || minY < 0 || minX + width > allTiles[0].length || minY + height > allTiles.length) {
            throw new IllegalArgumentException("The specified rectangle is out of bounds");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("The width and height must be positive");
        }
        for (int y = minY; y < minY + height; y++) {
            T[] allTilesRow = allTiles[y];
            T[] row = tiles[y - minY];
            for (int x = minX; x < minX + width; x++) {
                row[x - minX] = allTilesRow[x];
            }
        }
    }
}
