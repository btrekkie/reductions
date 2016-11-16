package com.github.btrekkie.reductions.push1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.btrekkie.reductions.planar.Point;

/** A crossover gadget for Push1Problem, as in I3SatPlanarGadgetFactory.createCrossover(). */
public class Push1CrossoverGadget extends Push1Gadget {
    /**
     * Whether the gadget is flipped horizontally, so that the arc from the first entry port to the second entry port
     * and then to the first exit port is clockwise.
     */
    private final boolean isFlipped;

    public Push1CrossoverGadget(boolean isFlipped) {
        this.isFlipped = isFlipped;
    }

    public Push1Tile[][] tiles() {
        String[] rows = new String[]{
            "**********************                                       ",
            "**********************                                       ",
            "                    **                                       ",
            "******************* **                                       ",
            "******************* **                                       ",
            "                 ** **                                       ",
            "                 ** **                                       ",
            "                 ** **                                       ",
            "                 ** **                                       ",
            "                 ** **                                       ",
            "                 ** **                                       ",
            "                 ** **                                       ",
            "                 ** **                                   ****",
            "                **  **                                  *****",
            "                ** ***                                 **    ",
            "               ** o************************************** ***",
            "               **    ************************************ ***",
            "***************** **                                     o **",
            "***************** ************************************** *** ",
            "               **  ************************************* **  ",
            "********* ****   o ***                                ** **  ",
            "********* ********  **                                ** **  ",
            "       ** **  ***** **                                ** **  ",
            "       ** **     ** **                                ** **  ",
            "       ** **     ** **                                ** **  ",
            "       ** **     ** **                                ** **  ",
            "       ** **     ** **                                ** **  ",
            "       ** **     ** **                                ** **  ",
            "       ** **     ** **                                ** **  ",
            "       ** **     ** **                                ** **  ",
            "       ** ********* **                                ** **  ",
            "       ** ********* ***           ****                ** **  ",
            "       **       *** *************************         ** **  ",
            "       ******oo   oo   *********** o ********         ** **  ",
            "       ****** oo oo **          ** o        **        ** **  ",
            "           *** ooo ******* ****   o ******* **        ** **  ",
            "           **** o ***   ** ******   ******* **        ** **  ",
            "           ***** ***    ** **  ******    ** ************ **  ",
            "           ***** **     ** **    ***     ** ************ **  ",
            "           **    **     ** **            ** o ****       **  ",
            "           ** ** **     ** **            ** oo *** ********  ",
            "           ** ****      ** **            **  oo ** ********  ",
            "           ** ***       ** **     ****    **  oo    **       ",
            "           ** ************ ***************** oo ******       ",
            "           ** ************ ******* o *******oo *******       ",
            "           **                   ** o        o ***            ",
            "           ********************   o ******** ***             ",
            "           **********************   ******** ***             ",
            "                               ******     **  **             ",
            "                                *****     *** **             ",
            "                                           ** **             ",
            "                                           ** **             ",
            "                                           ** **             ",
            "                                           ** ***************",
            "                                           ** ***************",
            "                                           **                ",
            "                                           ******************",
            "                                           ******************"};
        Push1Tile[][] tiles = tiles(rows);

        if (isFlipped) {
            for (int y = 0; y < tiles.length; y++) {
                Push1Tile[] row = tiles[y];
                for (int x = 0; x < row.length / 2; x++) {
                    Push1Tile tile = row[x];
                    row[x] = row[row.length - x - 1];
                    row[row.length - x - 1] = tile;
                }
            }
        }
        return tiles;
    }

    @Override
    public List<Point> ports() {
        List<Point> ports = Arrays.asList(new Point(0, 19), new Point(0, 3), new Point(61, 14), new Point(61, 55));
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
        return 1;
    }

    /** Returns the index of the first entry port, as in I3SatPlanarGadgetFactory.firstCrossoverExitPort. */
    public int firstExitPort() {
        return 3;
    }

    /** Returns the index of the second entry port, as in I3SatPlanarGadgetFactory.secondCrossoverEntryPort. */
    public int secondEntryPort() {
        return isFlipped ? 2 : 0;
    }

    /** Returns the index of the second exit port, as in I3SatPlanarGadgetFactory.secondCrossoverExitPort. */
    public int secondExitPort() {
        return isFlipped ? 0 : 2;
    }
}
