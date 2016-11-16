package com.github.btrekkie.reductions.mario;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.btrekkie.reductions.planar.Point;

/** A clause gadget for MarioProblem, as in I3SatPlanarGadgetFactory.createClause(). */
public class MarioClauseGadget extends MarioGadget {
    /** The index of the entry port, as in I3SatPlanarGadgetFactory.clauseEntryPort(). */
    public static final int ENTRY_PORT = 0;

    /** The index of the entry port, as in I3SatPlanarGadgetFactory.clauseExitPort(). */
    public static final int EXIT_PORT = 1;

    /** The minimum index of the clause ports, as in I3SatPlanarGadgetFactory.minClausePort(). */
    public static final int MIN_CLAUSE_PORT = 2;

    /** The maximum index of the clause ports, as in I3SatPlanarGadgetFactory.minClausePort(). */
    public static final int MAX_CLAUSE_PORT = 4;

    @Override
    public MarioTile[][] tiles() {
        String[] rows = new String[]{
            "                              BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
            "                              B                                           B",
            "                              B                                           B",
            "                              B   BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB  B",
            "                              B     B   B   B   B   B   B   B          B  B",
            "                              B     B   B   B   B   B   B   B          B  B",
            "                              BBBB  B B B B B B B B B B B B B B        B  B",
            "                                 B    B   B   B   B   B   B   B        B   ",
            "                                 B    B   B   B   B   B   B   B        B   ",
            "B     B        B                 BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB     BBBB",
            "BB    B        B                 B                                     B   ",
            "BB    B        B                 B                                     B   ",
            "BBB   B        B                 B                                     B   ",
            "BBB   B        B  FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF   B   ",
            "BBBBBBBBQBBQBBQB                                                       B   ",
            "BBBBBBB  B  B  B                                                       B   ",
            "BBBBBBB  B  B  BBBffffffffffffffffffffffffffffffffffffffffffffffffffBBBB   ",
            "         B  B    B                                                         ",
            "         B  B    B                                                         ",
            "BBBBBBBBBB BBBB  BBBBB                                                     ",
            "            B        BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
            "            B                                                              ",
            "         BBBBBBBBB                                                         ",
            "        BBBBBBBBBB                                                         ",
            "       BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
            "      BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
            "     BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
            "    BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
            "   BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
            "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
            "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"};
        Map<Character, MarioTile> tileChars = new HashMap<Character, MarioTile>();
        tileChars.put(' ', MarioTile.AIR);
        tileChars.put('B', MarioTile.BLOCK);
        tileChars.put('F', MarioTile.FIRE_BAR_DOWN_LEFT);
        tileChars.put('f', MarioTile.FIRE_BAR_UP_RIGHT);
        tileChars.put('Q', MarioTile.QUESTION_MARK_STAR);
        return tiles(rows, tileChars);
    }

    @Override
    public List<Point> ports() {
        return Arrays.asList(new Point(0, 9), new Point(75, 9), new Point(75, 24), new Point(0, 29), new Point(0, 19));
    }
}
