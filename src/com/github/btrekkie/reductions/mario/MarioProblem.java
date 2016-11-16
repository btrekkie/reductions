package com.github.btrekkie.reductions.mario;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.github.btrekkie.programmatic_image.ProgrammaticImageFrame;
import com.github.btrekkie.reductions.bool.Literal;
import com.github.btrekkie.reductions.bool.ThreeSat;
import com.github.btrekkie.reductions.bool.ThreeSatClause;
import com.github.btrekkie.reductions.bool.Variable;
import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.Point;
import com.github.btrekkie.reductions.planar.ThreeSatPlanarGadgetLayout;

/**
 * A "Mario problem" instance, which is the problem of whether it is possible for Mario to get from a given start
 * location to the finish in the original Super Mario Bros. game.
 */
public class MarioProblem {
    /**
     * A map from each gadget to the position of its top-left corner.  We regard the region not covered by gadgets as
     * filled with tiles of type MarioTile.BLOCK.
     */
    public Map<MarioGadget, Point> gadgets;

    public MarioProblem(Map<MarioGadget, Point> gadgets) {
        this.gadgets = gadgets;
    }

    /**
     * Returns a MarioProblem that is equivalent to the specified 3-SAT problem, i.e. for which Mario can reach the
     * finish iff the 3-SAT problem has a satisfying assignment.
     */
    public static MarioProblem reduceFrom(ThreeSat threeSat) {
        Map<IPlanarGadget, Point> gadgets = ThreeSatPlanarGadgetLayout.layout(
            threeSat, Mario3SatPlanarGadgetFactory.instance, MarioWireFactory.instance, MarioBarrierFactory.instance,
            new MarioStartGadget(), 0, new MarioFinishGadget(), 0);
        Map<MarioGadget, Point> marioGadgets = new LinkedHashMap<MarioGadget, Point>();
        for (Entry<IPlanarGadget, Point> entry : gadgets.entrySet()) {
            marioGadgets.put((MarioGadget)entry.getKey(), entry.getValue());
        }
        return new MarioProblem(marioGadgets);
    }

    /** Displays a MarioProblem created as a reduction from a 3-SAT problem. */
    public static void main(String[] args) {
        Variable variable1 = new Variable();
        Variable variable2 = new Variable();
        Variable variable3 = new Variable();
        ThreeSatClause clause1 = new ThreeSatClause(
            new Literal(variable1, false), new Literal(variable2, true), new Literal(variable3, false));
        ThreeSatClause clause2 = new ThreeSatClause(
            new Literal(variable1, false), new Literal(variable2, false), new Literal(variable2, true));
        ThreeSatClause clause3 = new ThreeSatClause(
            new Literal(variable1, true), new Literal(variable2, true), new Literal(variable3, true));
        ThreeSatClause clause4 = new ThreeSatClause(
            new Literal(variable1, true), new Literal(variable2, true), new Literal(variable3, true));
        ThreeSat threeSat = new ThreeSat(Arrays.asList(clause1, clause2, clause3, clause4));

        MarioProblem problem = MarioProblem.reduceFrom(threeSat);
        new ProgrammaticImageFrame(new MarioRenderer(problem)).setVisible(true);
    }
}
