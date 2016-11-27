package com.github.btrekkie.reductions.mario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.btrekkie.reductions.planar.Point;

/** A crossover gadget for MarioProblem, as in I3SatPlanarGadgetFactory.createCrossover(). */
public class MarioCrossoverGadget extends MarioGadget {
    /**
     * Whether the gadget is flipped horizontally, so that the arc from the first entry port to the second entry port
     * and then to the first exit port is clockwise.
     */
    private final boolean isFlipped;

    public MarioCrossoverGadget(boolean isFlipped) {
        this.isFlipped = isFlipped;
    }

    @Override
    public MarioTile[][] tiles() {
        String[] rows = new String[]{
            "                           ",
            "                           ",
            "                           ",
            "                           ",
            "                           ",
            "                           ",
            "BBBBBBBBBBBBBBB            ",
            "              B            ",
            "              B            ",
            "BBBB          B            ",
            "    B         B            ",
            "     B        B            ",
            "      B       B            ",
            "       B      B            ",
            "        B     B            ",
            "         B    B            ",
            "BBBBBBBBBBB   BBBBBBBBBBB  ",
            "      B   B   B   B     B  ",
            "      B B B   B B B B Q BbB",
            "B     B B BbbbB B B B     B",
            "B     B B B   B B B B     B",
            "B G B   B       B   B     B",
            "BBBBBBBBBBBbbbBBBBBBBBBBBBB",
            "BBBBBBBBBBB   BBBBBBBBBBBBB",
            "BBBBBBBBBBB   BBBBBBBBBBBBB",
            "BBBBBBBBBBB  BBBBBBBBBBBBBB",
            "BBBBBBBBBBB   BBBBBBBBBBBBB",
            "       BBBB   BBBBBBBBBBBBB",
            "       BBBBB  BBBBBBBBBBBBB",
            "BBBBB BBBBB   BBBBBBBBBBBBB",
            "BBBB   BBBB   BBBBBBBBBBBBB",
            "BBBB   BBBB  BBBBBBBBBBBBBB",
            "BBBB   BBBB   BBBBBBBBBBBBB",
            "BBBB   BBBB   BBBBBBBBBBBBB",
            "BBBB   BBBBB  BBBBBBBBBBBBB",
            "BBBB          BBBBBBBBBBBBB",
            "BBBB          BBBBBBBBBBBBB",
            "BBBBBBBBBBBBBBBBBBBBBBBBBBB"};
        Map<Character, MarioTile> tileChars = new HashMap<Character, MarioTile>();
        tileChars.put(' ', MarioTile.AIR);
        tileChars.put('B', MarioTile.BLOCK);
        tileChars.put('b', MarioTile.BRICK);
        tileChars.put('G', MarioTile.GOOMBA);
        tileChars.put('Q', MarioTile.QUESTION_MARK_MUSHROOM);
        MarioTile[][] tiles = tiles(rows, tileChars);

        if (isFlipped) {
            for (int y = 0; y < tiles.length; y++) {
                MarioTile[] row = tiles[y];
                for (int x = 0; x < row.length / 2; x++) {
                    MarioTile tile = row[x];
                    row[x] = row[row.length - x - 1];
                    row[row.length - x - 1] = tile;
                }
            }
        }
        return tiles;
    }

    @Override
    public List<Point> ports() {
        List<Point> ports = Arrays.asList(new Point(0, 19), new Point(0, 9), new Point(27, 18), new Point(0, 29));
        if (!isFlipped) {
            return ports;
        } else {
            int width = width();
            List<Point> newPorts = new ArrayList<Point>(ports.size());
            for (Point port : ports) {
                newPorts.add(new Point(width - port.x, port.y));
            }
            return Arrays.asList(newPorts.get(2), newPorts.get(1), newPorts.get(0), newPorts.get(3));
        }
    }

    /** Returns the index of the first entry port, as in I3SatPlanarGadgetFactory.firstCrossoverEntryPort. */
    public int firstEntryPort() {
        return isFlipped ? 2 : 0;
    }

    /** Returns the index of the first entry port, as in I3SatPlanarGadgetFactory.firstCrossoverExitPort. */
    public int firstExitPort() {
        return isFlipped ? 0 : 2;
    }

    /** Returns the index of the second entry port, as in I3SatPlanarGadgetFactory.secondCrossoverEntryPort. */
    public int secondEntryPort() {
        return 3;
    }

    /** Returns the index of the second exit port, as in I3SatPlanarGadgetFactory.secondCrossoverExitPort. */
    public int secondExitPort() {
        return 1;
    }
}
