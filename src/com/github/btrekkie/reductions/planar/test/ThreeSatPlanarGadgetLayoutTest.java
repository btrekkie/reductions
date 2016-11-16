package com.github.btrekkie.reductions.planar.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

import com.github.btrekkie.reductions.bool.Literal;
import com.github.btrekkie.reductions.bool.ThreeSat;
import com.github.btrekkie.reductions.bool.ThreeSatClause;
import com.github.btrekkie.reductions.bool.Variable;
import com.github.btrekkie.reductions.planar.IPlanarBarrierFactory;
import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.IPlanarWireFactory;
import com.github.btrekkie.reductions.planar.Point;
import com.github.btrekkie.reductions.planar.ThreeSatPlanarGadgetLayout;

public class ThreeSatPlanarGadgetLayoutTest {
    /** Equivalent implementation is contractual. */
    private void addEdge(
            GadgetLocation start, GadgetLocation end, Map<GadgetLocation, Collection<GadgetLocation>> locationGraph) {
        Collection<GadgetLocation> adjLocations = locationGraph.get(start);
        if (adjLocations == null) {
            adjLocations = new ArrayList<GadgetLocation>();
            locationGraph.put(start, adjLocations);
        }
        adjLocations.add(end);
    }

    /**
     * Adds all of the GadgetLocations reachable from "start" without passing through locations in "visited" and without
     * unlocking any clause gadgets to "visited".
     * @param start The location at which to start.
     * @param graph A map from each location adjacent to at least one location to the adjacent locations, excluding the
     *     adjacencies suggested by unlockedClauses.  The graph is directed.
     * @param unlockedClauses The clause gadgets that are unlocked, as elaborated in the comments for
     *     I3SatPlanarGadgetFactory.createClause().
     * @param visited The visited locations.
     */
    private void depthFirstSearch(
            GadgetLocation start, Map<GadgetLocation, Collection<GadgetLocation>> graph,
            Set<TestClauseGadget> unlockedClauses, Set<GadgetLocation> visited) {
        if (visited.add(start)) {
            Collection<GadgetLocation> adjLocations = graph.get(start);
            if (adjLocations != null) {
                for (GadgetLocation adjLocation : adjLocations) {
                    depthFirstSearch(adjLocation, graph, unlockedClauses, visited);
                }
            }
            if (unlockedClauses.contains(start.gadget) && start.port == TestClauseGadget.ENTRY_PORT) {
                depthFirstSearch(
                    new GadgetLocation(start.gadget, TestClauseGadget.EXIT_PORT), graph, unlockedClauses, visited);
            }
        }
    }

    /**
     * Returns whether it is possible to get from "location" to "finish".
     * @param location The start location.
     * @param finish The end location.
     * @param graph A map from each location adjacent to at least one location to the adjacent locations, excluding the
     *     adjacencies suggested by unlockedClauses.  The graph is directed.
     * @param unlockedClauses The clause gadgets that are initially unlocked, as elaborated in the comments for
     *     I3SatPlanarGadgetFactory.createClause().  This method may temporarily alter unlockedClauses, but when the
     *     method is finished, unlockedClauses will be the same as it was initially.
     * @return Whether there is a solution.
     */
    private boolean hasSolution(
            GadgetLocation location, IPlanarGadget finish, Map<GadgetLocation, Collection<GadgetLocation>> graph,
            Set<TestClauseGadget> unlockedClauses) {
        Set<GadgetLocation> reachableLocations = new HashSet<GadgetLocation>();
        depthFirstSearch(location, graph, unlockedClauses, reachableLocations);
        for (GadgetLocation reachableLocation : reachableLocations) {
            if (reachableLocation.gadget == finish) {
                return true;
            }
        }
        for (GadgetLocation reachableLocation : reachableLocations) {
            if (reachableLocation.gadget instanceof TestClauseGadget &&
                    reachableLocation.port >= TestClauseGadget.MIN_CLAUSE_PORT &&
                    reachableLocation.port <= TestClauseGadget.MAX_CLAUSE_PORT &&
                    !unlockedClauses.contains(reachableLocation.gadget)) {
                // Recurse with reachableLocation.gadget unlocked
                unlockedClauses.add((TestClauseGadget)reachableLocation.gadget);
                boolean hasSolution = hasSolution(reachableLocation, finish, graph, unlockedClauses);
                unlockedClauses.remove(reachableLocation.gadget);

                if (hasSolution) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns whether the specified gadget is a test wire gadget. */
    private static boolean isWire(IPlanarGadget gadget) {
        return gadget instanceof TestWireGadget || gadget instanceof TestTurnWireGadget;
    }

    /**
     * Returns whether it is possible to get from "start" to "finish".
     * @param graph A map from each non-barrier gadget G adjacent to at least one non-barrier gadget to a map from
     *     each adjacent non-barrier gadget to the indices in G.ports() of the port at which those gadgets connect.
     * @param start The start location.
     * @param finish The end location.
     * @return Whether there is a solution.
     */
    private boolean hasSolution(
            Map<IPlanarGadget, Map<IPlanarGadget, Integer>> graph, IPlanarGadget start, IPlanarGadget finish) {
        Map<GadgetLocation, Collection<GadgetLocation>> locationGraph =
            new HashMap<GadgetLocation, Collection<GadgetLocation>>();
        for (Entry<IPlanarGadget, Map<IPlanarGadget, Integer>> entry : graph.entrySet()) {
            // Add the edges suggested by "graph".  As an optimization, skip over any sequences of wires or junctions.
            IPlanarGadget gadget = entry.getKey();
            Map<IPlanarGadget, Integer> edgePorts = entry.getValue();
            if ((isWire(gadget) || gadget instanceof TestJunctionGadget) && edgePorts.size() == 2) {
                continue;
            }
            for (Entry<IPlanarGadget, Integer> adjEntry : edgePorts.entrySet()) {
                int port = adjEntry.getValue();
                IPlanarGadget adjGadget = adjEntry.getKey();
                IPlanarGadget prevGadget = gadget;
                Map<IPlanarGadget, Integer> adjEdgePorts = graph.get(adjGadget);
                while ((isWire(adjGadget) || adjGadget instanceof TestJunctionGadget) && adjEdgePorts.size() == 2) {
                    Iterator<IPlanarGadget> iterator = adjEdgePorts.keySet().iterator();
                    IPlanarGadget adjGadget1 = iterator.next();
                    IPlanarGadget adjGadget2 = iterator.next();
                    IPlanarGadget nextGadget;
                    if (adjGadget1 == prevGadget) {
                        nextGadget = adjGadget2;
                    } else {
                        nextGadget = adjGadget1;
                    }
                    prevGadget = adjGadget;
                    adjGadget = nextGadget;
                    adjEdgePorts = graph.get(adjGadget);
                }
                int adjPort = adjEdgePorts.get(prevGadget);
                addEdge(new GadgetLocation(gadget, port), new GadgetLocation(adjGadget, adjPort), locationGraph);
            }

            // Add the edges from one port of "gadget" to another port of "gadget", based on the logic of the gadget
            // type
            if (gadget instanceof TestVariableGadget) {
                for (int i = TestVariableGadget.MIN_ENTRY_PORT; i <= TestVariableGadget.MAX_ENTRY_PORT; i++) {
                    for (int j = TestVariableGadget.MIN_EXIT_PORT; j <= TestVariableGadget.MAX_EXIT_PORT; j++) {
                        addEdge(new GadgetLocation(gadget, i), new GadgetLocation(gadget, j), locationGraph);
                    }
                }
            } else if (gadget instanceof TestCrossoverGadget) {
                addEdge(
                    new GadgetLocation(gadget, TestCrossoverGadget.ENTRY_PORT1),
                    new GadgetLocation(gadget, TestCrossoverGadget.EXIT_PORT1), locationGraph);
                addEdge(
                    new GadgetLocation(gadget, TestCrossoverGadget.EXIT_PORT1),
                    new GadgetLocation(gadget, TestCrossoverGadget.ENTRY_PORT1), locationGraph);
                addEdge(
                    new GadgetLocation(gadget, TestCrossoverGadget.ENTRY_PORT2),
                    new GadgetLocation(gadget, TestCrossoverGadget.EXIT_PORT2), locationGraph);
                addEdge(
                    new GadgetLocation(gadget, TestCrossoverGadget.EXIT_PORT2),
                    new GadgetLocation(gadget, TestCrossoverGadget.ENTRY_PORT2), locationGraph);
            } else if (isWire(gadget) || gadget instanceof TestJunctionGadget) {
                for (int port1 : edgePorts.values()) {
                    GadgetLocation location = new GadgetLocation(gadget, port1);
                    for (int port2 : edgePorts.values()) {
                        if (port1 != port2) {
                            addEdge(location, new GadgetLocation(gadget, port2), locationGraph);
                        }
                    }
                }
            }
        }
        return hasSolution(new GadgetLocation(start, 0), finish, locationGraph, new HashSet<TestClauseGadget>());
    }

    /**
     * Returns whether the specified 3-CNF expression has a satisfying assignment.  This method takes super-polynomial
     * time, because I haven't solved P vs. NP and won $1,000,000 (at least, not yet).
     */
    private boolean isSatisfiable(ThreeSat threeSat) {
        List<Variable> variables = new ArrayList<Variable>(threeSat.variables());
        Set<Variable> trueVariables = new HashSet<Variable>();
        do {
            // Check whether trueVariables is a satisfying assignment
            boolean found = false;
            for (ThreeSatClause clause : threeSat.clauses) {
                boolean foundLiteral = false;
                for (Literal literal : clause.literals()) {
                    if (literal.isInverted != trueVariables.contains(literal.variable)) {
                        foundLiteral = true;
                        break;
                    }
                }
                if (!foundLiteral) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return true;
            }

            // Iterate to the next assignment
            for (Variable variable : variables) {
                if (!trueVariables.remove(variable)) {
                    trueVariables.add(variable);
                    break;
                }
            }
        } while (!trueVariables.isEmpty());
        return false;
    }

    /**
     * Asserts that the return value of ThreeSatPlanarGadgetLayout.layout is correct when passed the specified
     * arguments.  We take gadgets with no ports to be barrier gadgets, and gadgets of type TestWire and TestTurnWire to
     * be wires.
     */
    private void checkLayout(ThreeSat threeSat, IPlanarWireFactory wireFactory, IPlanarBarrierFactory barrierFactory) {
        IPlanarGadget start = new TestTerminalGadget();
        IPlanarGadget finish = new TestTerminalGadget();
        Map<IPlanarGadget, Point> gadgets = ThreeSatPlanarGadgetLayout.layout(
            threeSat, Test3SatPlanarGadgetFactory.instance, wireFactory, barrierFactory, start, 0, finish, 0);
        Map<IPlanarGadget, Map<IPlanarGadget, Integer>> graph = PlanarGadgetLayoutTest.gadgetGraph(
            gadgets, barrierFactory.minWidth(), barrierFactory.minHeight());
        assertEquals(isSatisfiable(threeSat), hasSolution(graph, start, finish));
    }

    @Test
    public void testLayout() {
        ThreeSat threeSat = new ThreeSat(Collections.<ThreeSatClause>emptyList());
        checkLayout(threeSat, new TestWireFactory(2, 2), new TestBarrierFactory(4, 4));

        Variable variable1 = new Variable();
        Variable variable2 = new Variable();
        Variable variable3 = new Variable();
        ThreeSatClause clause1 = new ThreeSatClause(
            new Literal(variable1, false), new Literal(variable2, true), new Literal(variable3, false));
        threeSat = new ThreeSat(Collections.singletonList(clause1));
        checkLayout(threeSat, new TestWireFactory(3, 3), new TestBarrierFactory(1, 1));

        clause1 = new ThreeSatClause(
            new Literal(variable1, false), new Literal(variable2, true), new Literal(variable3, false));
        ThreeSatClause clause2 = new ThreeSatClause(
            new Literal(variable1, false), new Literal(variable2, false), new Literal(variable2, true));
        ThreeSatClause clause3 = new ThreeSatClause(
            new Literal(variable1, true), new Literal(variable2, true), new Literal(variable3, true));
        ThreeSatClause clause4 = new ThreeSatClause(
            new Literal(variable1, true), new Literal(variable2, true), new Literal(variable3, true));
        threeSat = new ThreeSat(Arrays.asList(clause1, clause2, clause3, clause4));
        checkLayout(threeSat, new TestWireFactory(2, 2), new TestBarrierFactory(4, 4));

        Variable variable4 = new Variable();
        clause1 = new ThreeSatClause(
            new Literal(variable1, true), new Literal(variable3, true), new Literal(variable4, true));
        clause2 = new ThreeSatClause(
            new Literal(variable2, false), new Literal(variable3, false), new Literal(variable4, true));
        clause3 = new ThreeSatClause(
            new Literal(variable1, false), new Literal(variable2, true), new Literal(variable4, false));
        clause4 = new ThreeSatClause(
            new Literal(variable1, false), new Literal(variable3, false), new Literal(variable4, false));
        ThreeSatClause clause5 = new ThreeSatClause(
            new Literal(variable1, true), new Literal(variable2, false), new Literal(variable3, true));
        threeSat = new ThreeSat(Arrays.asList(clause1, clause2, clause3, clause4, clause5));
        checkLayout(threeSat, new TestWireFactory(6, 1), new TestBarrierFactory(1, 4));

        clause1 = new ThreeSatClause(
            new Literal(variable1, false), new Literal(variable1, false), new Literal(variable1, false));
        clause2 = new ThreeSatClause(
            new Literal(variable1, true), new Literal(variable1, true), new Literal(variable1, true));
        threeSat = new ThreeSat(Arrays.asList(clause1, clause2));
        checkLayout(threeSat, new TestWireFactory(3, 3), new TestBarrierFactory(1, 1));

        clause1 = new ThreeSatClause(
            new Literal(variable1, false), new Literal(variable2, false), new Literal(variable2, false));
        clause2 = new ThreeSatClause(
            new Literal(variable1, false), new Literal(variable2, true), new Literal(variable2, true));
        clause3 = new ThreeSatClause(
            new Literal(variable1, true), new Literal(variable2, false), new Literal(variable2, false));
        clause4 = new ThreeSatClause(
            new Literal(variable1, true), new Literal(variable2, true), new Literal(variable2, true));
        threeSat = new ThreeSat(Arrays.asList(clause1, clause2, clause3, clause4));
        checkLayout(threeSat, new TestWireFactory(2, 2), new TestBarrierFactory(4, 4));

        clause1 = new ThreeSatClause(
            new Literal(variable3, false), new Literal(variable3, false), new Literal(variable3, false));
        clause2 = new ThreeSatClause(
            new Literal(variable1, false), new Literal(variable2, false), new Literal(variable3, true));
        clause3 = new ThreeSatClause(
            new Literal(variable1, false), new Literal(variable2, true), new Literal(variable3, true));
        clause4 = new ThreeSatClause(
            new Literal(variable1, true), new Literal(variable2, false), new Literal(variable3, true));
        clause5 = new ThreeSatClause(
            new Literal(variable1, true), new Literal(variable2, true), new Literal(variable3, true));
        threeSat = new ThreeSat(Arrays.asList(clause1, clause2, clause3, clause4, clause5));
        checkLayout(threeSat, new TestWireFactory(3, 3), new TestBarrierFactory(1, 1));
    }
}
