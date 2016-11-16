package com.github.btrekkie.reductions.push1;

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
 * A "Push-1 problem" instance, which is the problem of whether it is possible for a robot to get from a given start
 * location to a given finish location in a grid.  The robot may move from a cell to an adjacent unoccupied cell.  The
 * robot may also push a block by moving into an adjacent cell occupied by a block and displacing it to the adjacent
 * cell in the same direction, provided that cell is unoccupied.
 */
public class Push1Problem {
    /**
     * A map from each gadget to the position of its top-left corner.  We regard the region not covered by gadgets as
     * filled with tiles of type Push1Tile.EFFECTIVELY_IMMOVABLE_BLOCK.
     */
    public Map<Push1Gadget, Point> gadgets;

    public Push1Problem(Map<Push1Gadget, Point> gadgets) {
        this.gadgets = gadgets;
    }

    /**
     * Returns a Push1Problem that is equivalent to the specified 3-SAT problem, i.e. for which the robot can reach the
     * finish iff the 3-SAT problem has a satisfying assignment.  As it happens, the resulting problem is also
     * equivalent to threeSat if we interpret it as a PushPush problem.  In PushPush, whenever the robot pushes a block,
     * it (instantaneously) continues moving in the direction the robot pushed it until obstructed by an occupied cell.
     */
    public static Push1Problem reduceFrom(ThreeSat threeSat) {
        Map<IPlanarGadget, Point> gadgets = ThreeSatPlanarGadgetLayout.layout(
            threeSat, Push13SatPlanarGadgetFactory.instance, Push1WireFactory.instance, Push1BarrierFactory.instance,
            new Push1StartGadget(), 0, new Push1FinishGadget(), 0);
        Map<Push1Gadget, Point> push1Gadgets = new LinkedHashMap<Push1Gadget, Point>();
        for (Entry<IPlanarGadget, Point> entry : gadgets.entrySet()) {
            push1Gadgets.put((Push1Gadget)entry.getKey(), entry.getValue());
        }
        return new Push1Problem(push1Gadgets);
    }

    /** Displays a Push1Problem created as a reduction from a 3-SAT problem. */
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

        Push1Problem problem = Push1Problem.reduceFrom(threeSat);
        new ProgrammaticImageFrame(new Push1Renderer(problem)).setVisible(true);
    }
}
