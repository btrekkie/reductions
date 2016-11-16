package com.github.btrekkie.reductions.zelda;

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
 * A "Zelda problem" instance, which is the problem of whether it is possible for Link to get from a given start
 * location to a given finish location, in the original Legend of Zelda game.
 */
public class ZeldaProblem {
    /**
     * A map from each gadget to the position of its top-left corner.  We regard the region not covered by gadgets as
     * filled with tiles of type ZeldaTile.BARRIER.
     */
    public Map<ZeldaGadget, Point> gadgets;

    public ZeldaProblem(Map<ZeldaGadget, Point> gadgets) {
        this.gadgets = gadgets;
    }

    /**
     * Returns a ZeldaProblem that is equivalent to the specified 3-SAT problem, i.e. for which Link can reach the
     * finish iff the 3-SAT problem has a satisfying assignment.
     */
    public static ZeldaProblem reduceFrom(ThreeSat threeSat) {
        Map<IPlanarGadget, Point> gadgets = ThreeSatPlanarGadgetLayout.layout(
            threeSat, Zelda3SatPlanarGadgetFactory.instance, ZeldaWireFactory.instance, ZeldaBarrierFactory.instance,
            new ZeldaStartGadget(), 0, new ZeldaFinishGadget(), 0);
        Map<ZeldaGadget, Point> zeldaGadgets = new LinkedHashMap<ZeldaGadget, Point>();
        for (Entry<IPlanarGadget, Point> entry : gadgets.entrySet()) {
            zeldaGadgets.put((ZeldaGadget)entry.getKey(), entry.getValue());
        }
        return new ZeldaProblem(zeldaGadgets);
    }

    /** Displays a ZeldaProblem created as a reduction from a 3-SAT problem. */
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

        ZeldaProblem problem = ZeldaProblem.reduceFrom(threeSat);
        new ProgrammaticImageFrame(new ZeldaRenderer(problem)).setVisible(true);
    }
}
