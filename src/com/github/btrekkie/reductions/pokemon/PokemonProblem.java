package com.github.btrekkie.reductions.pokemon;

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
 * A "Pokemon problem" instance, which is the problem of whether it is possible for the player to get from a given start
 * location to a given finish location.  This works for any version of Pokemon.
 */
public class PokemonProblem {
    /**
     * A map from each gadget to the position of its top-left corner.  We regard the region not covered by gadgets as
     * filled with tiles of type PokemonTile.ROCK.
     */
    public Map<PokemonGadget, Point> gadgets;

    public PokemonProblem(Map<PokemonGadget, Point> gadgets) {
        this.gadgets = gadgets;
    }

    /**
     * Returns a PokemonProblem that is equivalent to the specified 3-SAT problem, i.e. for which the player can reach
     * the finish iff the 3-SAT problem has a satisfying assignment.
     */
    public static PokemonProblem reduceFrom(ThreeSat threeSat) {
        Map<IPlanarGadget, Point> gadgets = ThreeSatPlanarGadgetLayout.layout(
            threeSat, Pokemon3SatPlanarGadgetFactory.instance, PokemonWireFactory.instance,
            PokemonBarrierFactory.instance, new PokemonStartGadget(), 0, new PokemonFinishGadget(), 0);
        Map<PokemonGadget, Point> pokemonGadgets = new LinkedHashMap<PokemonGadget, Point>();
        for (Entry<IPlanarGadget, Point> entry : gadgets.entrySet()) {
            pokemonGadgets.put((PokemonGadget)entry.getKey(), entry.getValue());
        }
        return new PokemonProblem(pokemonGadgets);
    }

    /** Displays a PokemonProblem created as a reduction from a 3-SAT problem. */
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

        PokemonProblem problem = PokemonProblem.reduceFrom(threeSat);
        new ProgrammaticImageFrame(new PokemonRenderer(problem)).setVisible(true);
    }
}
