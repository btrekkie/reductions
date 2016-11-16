package com.github.btrekkie.reductions.planar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.ec.EcNode;
import com.github.btrekkie.graph.ec.EcPlanarEmbeddingWithCrossings;
import com.github.btrekkie.graph.planar.PlanarEmbedding;
import com.github.btrekkie.graph.planar.PlanarEmbeddingWithCrossings;
import com.github.btrekkie.reductions.bool.Literal;
import com.github.btrekkie.reductions.bool.ThreeSat;
import com.github.btrekkie.reductions.bool.ThreeSatClause;
import com.github.btrekkie.reductions.bool.Variable;

/**
 * Creates and positions IPlanarGadgets in order to form a reduction from a 3-SAT problem.  This uses gadgets with
 * certain functions produced by an I3SatPlanarGadgetFactory, an IPlanarWireFactory, and an IPlanarBarrierFactory.  See
 * "layout".
 */
/* This class is implemented by producing a graph similar to the one described in https://arxiv.org/pdf/1203.1895.pdf
 * (Aloupis, Demaine, Guo, Viglietta (2012): Classic Nintendo Games are (Computationally) Hard), and then handing the
 * graph off to PlanarGadgetLayout.layout to produce the desired layout.  To be precise, in addition to any crossover
 * gadgets, the graph consists of the following:
 *
 * - One variable gadget per variable and one clause gadget per clause.
 * - An edge from the start gadget to the first variable.
 * - For each literal, one junction gadget per clause in which it appears, with edges from each junction to its clause,
 *   from each junction to the next, from the variable for the literal to the first junction, and from the last junction
 *   to the next variable.
 * - An additional variable gadget with edges from the last junctions of the literals for the last variable and to the
 *   first clause gadget.
 * - An edge from each clause to the next and from the last clause to the finish gadget.
 *
 * We need to make sure that there are no crossovers on the edges from the clauses to the corresponding literal
 * junctions.  This is because depending on the I3SatPlanarGadgetFactory, crossover gadgets might only permit travel in
 * one direction (e.g. from the first entry point to the first exit point but not vice versa).  To accomplish this, we
 * start with a graph that is the same as the above graph, except that rather than having one junction for each
 * literal's appearance in a clause, we add edges directly to the clause gadgets without any intermediate junctions.
 * This is not a valid gadget graph, because some ports of the clause gadgets have multiple edges.  However, we can use
 * this to obtain a PlanarEmbeddingWithCrossings, at which point we can proceed to add in the required junction gadgets.
 * (Note that there are also junction gadgets in the middle of some of the edges described in the above bullet points,
 * to ensure that the initial graph doesn't have multiple edges between a pair of gadgets.)
 */
public class ThreeSatPlanarGadgetLayout {
    /**
     * Returns the initial graph for computing the reduction from the specified 3-SAT problem, as described in the
     * comments for the implementation of this class.
     * @param threeSat The 3-SAT problem.
     * @param threeSatFactory The factory to use to create gadgets for the reduction.
     * @param startGadget The starting gadget for the reduction.
     * @param startPort The index in startGadget.ports() of the starting gadget port to use for the reduction.
     * @param finishGadget The ending gadget for the reduction.
     * @param finishPort The index in finishGadget.ports() of the ending gadget port to use for the reduction.
     * @param gadgets A map to which to add mappings from the vertices of the graph to the gadgets at those vertices.
     * @param minPorts A map to which to add mappings from each vertex V to a map from each adjacent vertex W to the
     *     index in gadgets.get(V).ports() of the minimum port to connect to W.  We may connect any port whose index is
     *     at least the minimum and at most the maximum.  The port ranges for a vertex may not overlap.
     * @param maxPorts A map to which to add mappings from each vertex V to a map from each adjacent vertex W to the
     *     index in gadgets.get(V).ports() of the maximum port to connect to W.  We may connect any port whose index is
     *     at least the minimum and at most the maximum.  The port ranges for a vertex may not overlap.
     * @param clauseEdges A map to which to add mappings from each clause vertex to the adjacent vertices.  The adjacent
     *     vertices are ordered so that when we add in the literal-clause junctions, the first two vertices will be
     *     adjacent to the same junction, the next two vertices will be adjacent to the same junction, and so on.
     * @param paths A list to which to add the sequence of paths that comprise the edges in the graph.  We represent
     *     each path as a sequence of its vertices.  The edges are ordered so that the sequence of edges in any path
     *     from the start to the finish is a sublist of the sequence of edges in "paths".  (For the purposes of this
     *     description, when we enter the "entry" port of the first clause gadgets, we may treat all clause gadgets as
     *     unlocked.)
     * @return The graph.
     */
    private static Graph initialGraph(
            ThreeSat threeSat, I3SatPlanarGadgetFactory threeSatFactory, IPlanarGadget startGadget, int startPort,
            IPlanarGadget finishGadget, int finishPort, Map<Vertex, IPlanarGadget> gadgets,
            Map<Vertex, Map<Vertex, Integer>> minPorts, Map<Vertex, Map<Vertex, Integer>> maxPorts,
            Map<Vertex, List<Vertex>> clauseEdges, List<List<Vertex>> paths) {
        // Create the clause vertices
        Graph graph = new Graph();
        Map<ThreeSatClause, Vertex> clauseVertices = new LinkedHashMap<ThreeSatClause, Vertex>();
        for (ThreeSatClause clause : threeSat.clauses) {
            Vertex vertex = graph.createVertex();
            clauseVertices.put(clause, vertex);
            gadgets.put(vertex, threeSatFactory.createClause());
            minPorts.put(vertex, new HashMap<Vertex, Integer>());
            maxPorts.put(vertex, new HashMap<Vertex, Integer>());
            clauseEdges.put(vertex, new ArrayList<Vertex>(6));
        }

        // Add the variable vertices and the literal-clause edges
        Map<Literal, Set<ThreeSatClause>> literalClauses = threeSat.literalClauses();
        Vertex start = graph.createVertex();
        gadgets.put(start, startGadget);
        minPorts.put(start, new HashMap<Vertex, Integer>());
        maxPorts.put(start, new HashMap<Vertex, Integer>());
        List<Vertex> prevVertices = Collections.singletonList(start);
        List<Integer> prevMinPorts = Collections.singletonList(startPort);
        List<Integer> prevMaxPorts = Collections.singletonList(startPort);
        List<Vertex> prevPrevVertices = Collections.singletonList(null);
        for (Variable variable : threeSat.variables()) {
            // Create the variable vertex
            Vertex vertex = graph.createVertex();
            gadgets.put(vertex, threeSatFactory.createVariable());
            Map<Vertex, Integer> vertexMinPorts = new HashMap<Vertex, Integer>();
            Map<Vertex, Integer> vertexMaxPorts = new HashMap<Vertex, Integer>();
            minPorts.put(vertex, vertexMinPorts);
            maxPorts.put(vertex, vertexMaxPorts);

            // Add an edge from the previous vertices to the variable vertex
            for (int i = 0; i < prevVertices.size(); i++) {
                Vertex prevVertex = prevVertices.get(i);
                prevVertex.addEdge(vertex);
                paths.add(Arrays.asList(prevVertex, vertex));
                minPorts.get(prevVertex).put(vertex, prevMinPorts.get(i));
                maxPorts.get(prevVertex).put(vertex, prevMaxPorts.get(i));
                vertexMinPorts.put(prevVertex, threeSatFactory.minVariableEntryPort());
                vertexMaxPorts.put(prevVertex, threeSatFactory.maxVariableEntryPort());
                if (prevPrevVertices.get(i) != null) {
                    List<Vertex> curClauseEdges = clauseEdges.get(prevVertex);
                    curClauseEdges.add(prevPrevVertices.get(i));
                    curClauseEdges.add(vertex);
                }
            }

            // Add the edges for the literals
            prevVertices = new ArrayList<Vertex>(2);
            prevMinPorts = new ArrayList<Integer>(2);
            prevMaxPorts = new ArrayList<Integer>(2);
            prevPrevVertices = new ArrayList<Vertex>(2);
            for (int i = 0; i < 2; i++) {
                Literal literal = new Literal(variable, i != 0);
                Vertex prevVertex = vertex;
                int prevMinPort = threeSatFactory.minVariableExitPort();
                int prevMaxPort = threeSatFactory.maxVariableExitPort();
                Vertex prevPrevVertex = null;
                Set<ThreeSatClause> clauses = literalClauses.get(literal);
                if (clauses == null) {
                    clauses = Collections.emptySet();
                }
                for (ThreeSatClause clause : clauses) {
                    // Go from the previous vertex to the clause.  We visit a junction vertex first in order to avoid
                    // the possibility of multiple edges between the a pair of clauses.

                    // Add an edge from the previous vertex to the junction
                    Vertex clauseVertex = clauseVertices.get(clause);
                    Vertex junction = graph.createVertex();
                    IPlanarGadget junctionGadget = threeSatFactory.createJunction();
                    gadgets.put(junction, junctionGadget);
                    prevVertex.addEdge(junction);
                    Map<Vertex, Integer> junctionMinPorts = new HashMap<Vertex, Integer>();
                    Map<Vertex, Integer> junctionMaxPorts = new HashMap<Vertex, Integer>();
                    minPorts.get(prevVertex).put(junction, prevMinPort);
                    maxPorts.get(prevVertex).put(junction, prevMaxPort);
                    junctionMinPorts.put(prevVertex, 0);
                    junctionMaxPorts.put(prevVertex, junctionGadget.ports().size() - 1);
                    if (prevPrevVertex != null) {
                        List<Vertex> curClauseEdges = clauseEdges.get(prevVertex);
                        curClauseEdges.add(prevPrevVertex);
                        curClauseEdges.add(junction);
                    }

                    // Add an edge from the junction to the clause
                    junction.addEdge(clauseVertex);
                    junctionMinPorts.put(clauseVertex, 0);
                    junctionMaxPorts.put(clauseVertex, junctionGadget.ports().size() - 1);
                    minPorts.get(clauseVertex).put(junction, threeSatFactory.minClausePort());
                    maxPorts.get(clauseVertex).put(junction, threeSatFactory.maxClausePort());

                    paths.add(Arrays.asList(prevVertex, junction, clauseVertex));
                    minPorts.put(junction, junctionMinPorts);
                    maxPorts.put(junction, junctionMaxPorts);

                    prevVertex = clauseVertex;
                    prevMinPort = threeSatFactory.minClausePort();
                    prevMaxPort = threeSatFactory.maxClausePort();
                    prevPrevVertex = junction;
                }
                prevVertices.add(prevVertex);
                prevMinPorts.add(prevMinPort);
                prevMaxPorts.add(prevMaxPort);
                prevPrevVertices.add(prevPrevVertex);
            }
        }

        // Add the variable vertex gating access to the first clause
        Vertex variableEnd = graph.createVertex();
        gadgets.put(variableEnd, threeSatFactory.createVariable());
        Map<Vertex, Integer> variableEndMinPorts = new HashMap<Vertex, Integer>();
        Map<Vertex, Integer> variableEndMaxPorts = new HashMap<Vertex, Integer>();
        for (int i = 0; i < prevVertices.size(); i++) {
            Vertex prevVertex = prevVertices.get(i);
            if (prevPrevVertices.get(i) == null) {
                // Add an edge from the previous vertex to the variable vertex
                prevVertex.addEdge(variableEnd);
                paths.add(Arrays.asList(prevVertex, variableEnd));
                minPorts.get(prevVertex).put(variableEnd, prevMinPorts.get(i));
                maxPorts.get(prevVertex).put(variableEnd, prevMaxPorts.get(i));
                variableEndMinPorts.put(prevVertex, threeSatFactory.minVariableEntryPort());
                variableEndMaxPorts.put(prevVertex, threeSatFactory.maxVariableEntryPort());
            } else {
                // We need to include a junction vertex between the previous vertex and the variable vertex, to avoid
                // the possibility of multiple edges between the variable and the first clause

                // Add an edge from the previous vertex to the junction
                Vertex junction = graph.createVertex();
                IPlanarGadget junctionGadget = threeSatFactory.createJunction();
                gadgets.put(junction, junctionGadget);
                prevVertex.addEdge(junction);
                Map<Vertex, Integer> junctionMinPorts = new HashMap<Vertex, Integer>();
                Map<Vertex, Integer> junctionMaxPorts = new HashMap<Vertex, Integer>();
                minPorts.get(prevVertex).put(junction, prevMinPorts.get(i));
                maxPorts.get(prevVertex).put(junction, prevMaxPorts.get(i));
                junctionMinPorts.put(prevVertex, 0);
                junctionMaxPorts.put(prevVertex, junctionGadget.ports().size() - 1);

                // Add a vertex from the junction to the variable
                junction.addEdge(variableEnd);
                junctionMinPorts.put(variableEnd, 0);
                junctionMaxPorts.put(variableEnd, junctionGadget.ports().size() - 1);
                variableEndMinPorts.put(junction, threeSatFactory.minVariableEntryPort());
                variableEndMaxPorts.put(junction, threeSatFactory.maxVariableEntryPort());

                List<Vertex> curClauseEdges = clauseEdges.get(prevVertex);
                curClauseEdges.add(prevPrevVertices.get(i));
                curClauseEdges.add(junction);
                paths.add(Arrays.asList(prevVertex, junction, variableEnd));
                minPorts.put(junction, junctionMinPorts);
                maxPorts.put(junction, junctionMaxPorts);
            }
        }
        minPorts.put(variableEnd, variableEndMinPorts);
        maxPorts.put(variableEnd, variableEndMaxPorts);

        // Add edges from the variable vertex to the first clause vertex and from each clause to the next
        Vertex prevVertex = variableEnd;
        int prevMinPort = threeSatFactory.minVariableExitPort();
        int prevMaxPort = threeSatFactory.maxVariableExitPort();
        List<Vertex> path = new ArrayList<Vertex>(threeSat.clauses.size() + 2);
        path.add(variableEnd);
        for (Vertex vertex : clauseVertices.values()) {
            prevVertex.addEdge(vertex);
            path.add(vertex);
            minPorts.get(prevVertex).put(vertex, prevMinPort);
            maxPorts.get(prevVertex).put(vertex, prevMaxPort);
            minPorts.get(vertex).put(prevVertex, threeSatFactory.clauseEntryPort());
            maxPorts.get(vertex).put(prevVertex, threeSatFactory.clauseEntryPort());
            prevVertex = vertex;
            prevMinPort = threeSatFactory.clauseExitPort();
            prevMaxPort = threeSatFactory.clauseExitPort();
        }

        // Add an edge from the last clause vertex to the finish vertex
        Vertex finish = graph.createVertex();
        gadgets.put(finish, finishGadget);
        prevVertex.addEdge(finish);
        path.add(finish);
        minPorts.get(prevVertex).put(finish, prevMinPort);
        maxPorts.get(prevVertex).put(finish, prevMaxPort);
        minPorts.put(finish, Collections.singletonMap(prevVertex, finishPort));
        maxPorts.put(finish, Collections.singletonMap(prevVertex, finishPort));
        paths.add(path);
        return graph;
    }

    /**
     * Returns the EcNode constraints for the initial graph, as described in the comments for the implementation of this
     * class.
     * @param minPorts A map from each vertex V to a map from each adjacent vertex W to the index in
     *     gadgets.get(V).ports() of the minimum port to connect to W.  We may connect any port whose index is at least
     *     the minimum and at most the maximum.  The port ranges for a vertex may not overlap.
     * @param maxPorts A map from each vertex V to a map from each adjacent vertex W to the index in
     *     gadgets.get(V).ports() of the maximum port to connect to W.  We may connect any port whose index is at least
     *     the minimum and at most the maximum.  The port ranges for a vertex may not overlap.
     * @param clauseEdges A map from each clause vertex to the adjacent vertices.  The adjacent vertices are ordered so
     *     that when we add in the literal-clause junctions, the first two vertices will be adjacent to the same
     *     junction, the next two vertices will be adjacent to the same junction, and so on.
     * @param minPortGroups A map to which to add mappings from each vertex to a map from the minimum port of each port
     *     range for the vertex to the adjacent vertices in that port range.  Each adjacent vertex belongs to a port
     *     range given by minPorts and maxPorts.
     * @return A map from each constrained vertex to the root node of its constraint tree.  It is okay for a vertex not
     *     to have a constraint tree.
     */
    private static Map<Vertex, EcNode> constraints(
            Map<Vertex, Map<Vertex, Integer>> minPorts, Map<Vertex, Map<Vertex, Integer>> maxPorts,
            Map<Vertex, List<Vertex>> clauseEdges, Map<Vertex, SortedMap<Integer, Collection<Vertex>>> minPortGroups) {
        Map<Vertex, EcNode> constraints = new HashMap<Vertex, EcNode>();
        for (Vertex vertex : minPorts.keySet()) {
            // Compute minPortGroups.get(vertex)
            Map<Vertex, Integer> vertexMinPorts = minPorts.get(vertex);
            Map<Vertex, Integer> vertexMaxPorts = maxPorts.get(vertex);
            SortedMap<Integer, Collection<Vertex>> minPortToVertices = new TreeMap<Integer, Collection<Vertex>>();
            Map<Integer, Integer> minPortToMaxPort = new HashMap<Integer, Integer>();
            for (Vertex adjVertex : vertex.edges) {
                int minPort = vertexMinPorts.get(adjVertex);
                int maxPort = vertexMaxPorts.get(adjVertex);
                if (minPortToVertices.containsKey(minPort)) {
                    minPortToVertices.get(minPort).add(adjVertex);
                } else {
                    minPortToMaxPort.put(minPort, maxPort);
                    Collection<Vertex> vertices = new ArrayList<Vertex>();
                    vertices.add(adjVertex);
                    minPortToVertices.put(minPort, vertices);
                }
            }
            minPortGroups.put(vertex, minPortToVertices);

            // Compute constraints.get(vertex)
            List<Vertex> curClauseEdges = clauseEdges.get(vertex);
            EcNode rootNode = EcNode.create(null, EcNode.Type.ORIENTED);
            for (Collection<Vertex> vertices : minPortToVertices.values()) {
                EcNode node = EcNode.create(rootNode, EcNode.Type.GROUP);
                if (curClauseEdges == null || !curClauseEdges.contains(vertices.iterator().next())) {
                    for (Vertex adjVertex : vertices) {
                        EcNode.createVertex(node, adjVertex);
                    }
                } else {
                    for (int i = 0; i < curClauseEdges.size(); i += 2) {
                        EcNode group = EcNode.create(node, EcNode.Type.GROUP);
                        EcNode.createVertex(group, curClauseEdges.get(i));
                        EcNode.createVertex(group, curClauseEdges.get(i + 1));
                    }
                }
            }
            constraints.put(vertex, rootNode);
        }
        return constraints;
    }

    /**
     * Returns the final graph for computing the reduction from the 3-SAT problem.
     * @param embedding The PlanarEmbeddingWithCrossings for the initial graph, as described in the comments for the
     *     implementation of this class.
     * @param clauseEdges A map from each clause vertex to the adjacent vertices.  The adjacent vertices are ordered so
     *     that when we add in the literal-clause junctions, the first two vertices will be adjacent to the same
     *     junction, the next two vertices will be adjacent to the same junction, and so on.
     * @param embeddingVertexToNewVertex A map to which to add mappings from each vertex in embedding.graph to the
     *     corresponding vertex in the returned graph.
     * @param clauseJunctionVertices A map to which to add mappings from each clause vertex in embedding.graph to a map
     *     from each adjacent vertex where we interpose a junction vertex for the final graph to the junction vertex.
     *     The keys of the maps are vertices in embedding.graph, while the values are vertices in the returned graph.
     * @return The graph.
     */
    private static Graph newGraph(
            PlanarEmbeddingWithCrossings embedding, Map<Vertex, List<Vertex>> clauseEdges,
            Map<Vertex, Vertex> embeddingVertexToNewVertex, Map<Vertex, Map<Vertex, Vertex>> clauseJunctionVertices) {
        Graph newGraph = new Graph();
        for (Vertex embeddingVertex : embedding.graph.vertices) {
            embeddingVertexToNewVertex.put(embeddingVertex, newGraph.createVertex());
        }
        Map<Vertex, Vertex> vertexToEmbeddingVertex = embedding.originalVertexToVertex;

        // Create the junction vertices and add them to clauseJunctionVertices
        for (Entry<Vertex, List<Vertex>> entry : clauseEdges.entrySet()) {
            Vertex clauseVertex = entry.getKey();
            List<Vertex> curClauseEdges = entry.getValue();
            Map<Vertex, Vertex> junctionVertices = new LinkedHashMap<Vertex, Vertex>();
            for (int i = 0; i < curClauseEdges.size(); i += 2) {
                Vertex junction = newGraph.createVertex();
                for (int j = 0; j < 2; j++) {
                    Vertex adjVertex = curClauseEdges.get(i + j);
                    List<Vertex> addedVertices = embedding.addedVertices(clauseVertex, adjVertex);
                    if (addedVertices.isEmpty()) {
                        junctionVertices.put(vertexToEmbeddingVertex.get(adjVertex), junction);
                    } else {
                        junctionVertices.put(addedVertices.get(0), junction);
                    }
                }
            }
            clauseJunctionVertices.put(vertexToEmbeddingVertex.get(clauseVertex), junctionVertices);
        }

        // Add the edges between the non-clause, non-junction vertices
        for (Vertex embeddingVertex : embedding.graph.vertices) {
            if (!clauseJunctionVertices.containsKey(embeddingVertex)) {
                Vertex newVertex = embeddingVertexToNewVertex.get(embeddingVertex);
                for (Vertex adjEmbeddingVertex : embeddingVertex.edges) {
                    if (!clauseJunctionVertices.containsKey(adjEmbeddingVertex)) {
                        Vertex adjNewVertex = embeddingVertexToNewVertex.get(adjEmbeddingVertex);
                        newVertex.addEdge(adjNewVertex);
                    }
                }
            }
        }

        // Add the edges to the clause and junction vertices
        for (Entry<Vertex, Map<Vertex, Vertex>> entry : clauseJunctionVertices.entrySet()) {
            Vertex embeddingVertex = entry.getKey();
            Map<Vertex, Vertex> vertices = entry.getValue();
            Vertex newVertex = embeddingVertexToNewVertex.get(embeddingVertex);
            for (Vertex adjEmbeddingVertex : embeddingVertex.edges) {
                Vertex adjNewVertex = embeddingVertexToNewVertex.get(adjEmbeddingVertex);
                Vertex newJunction = vertices.get(adjEmbeddingVertex);
                if (newJunction == null) {
                    newVertex.addEdge(adjNewVertex);
                } else {
                    newJunction.addEdge(adjNewVertex);
                }
            }
            for (Vertex newJunction : new LinkedHashSet<Vertex>(vertices.values())) {
                newVertex.addEdge(newJunction);
            }
        }
        return newGraph;
    }

    /**
     * Returns the planar embedding for the final graph for computing the reduction from the 3-SAT problem, derived from
     * the planar embedding for the PlanarEmbeddingWithCrossings used to create the final graph.
     * @param embedding The PlanarEmbeddingWithCrossings for the initial graph, as described in the comments for the
     *     implementation of this class.
     * @param embeddingVertexToNewVertex A map from each vertex in embedding.graph to the corresponding vertex in the
     *     final graph.
     * @param clauseJunctionVertices A map from each clause vertex in embedding.graph to a map from each adjacent vertex
     *     where we interpose a junction vertex for the final graph to the junction vertex.  The keys of the maps are
     *     vertices in embedding.graph, while the values are vertices in the returned graph.
     * @return The embedding.
     */
    private static PlanarEmbedding newEmbedding(
            PlanarEmbeddingWithCrossings embedding, Map<Vertex, Vertex> embeddingVertexToNewVertex,
            Map<Vertex, Map<Vertex, Vertex>> clauseJunctionVertices) {
        // Compute the clockwise order
        Map<Vertex, List<Vertex>> newClockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        for (Vertex embeddingVertex : embedding.graph.vertices) {
            Vertex newVertex = embeddingVertexToNewVertex.get(embeddingVertex);
            List<Vertex> embeddingVertexClockwiseOrder = embedding.embedding.clockwiseOrder.get(embeddingVertex);
            List<Vertex> newVertexClockwiseOrder;
            Map<Vertex, Vertex> vertices = clauseJunctionVertices.get(embeddingVertex);
            if (vertices == null) {
                // Not a clause vertex
                newVertexClockwiseOrder = new ArrayList<Vertex>(embeddingVertexClockwiseOrder.size());
                for (Vertex adjEmbeddingVertex : embeddingVertexClockwiseOrder) {
                    vertices = clauseJunctionVertices.get(adjEmbeddingVertex);
                    Vertex newJunction;
                    if (vertices == null) {
                        newJunction = null;
                    } else {
                        newJunction = vertices.get(embeddingVertex);
                    }
                    if (newJunction == null) {
                        newVertexClockwiseOrder.add(embeddingVertexToNewVertex.get(adjEmbeddingVertex));
                    } else {
                        newVertexClockwiseOrder.add(newJunction);
                    }
                }
            } else {
                // Clause vertex
                newVertexClockwiseOrder = new ArrayList<Vertex>(5);
                Vertex prevAdjEmbeddingVertex = embeddingVertexClockwiseOrder.get(
                    embeddingVertexClockwiseOrder.size() - 1);
                Vertex prevNewJunction = vertices.get(prevAdjEmbeddingVertex);
                Vertex prevAdjNewVertex = embeddingVertexToNewVertex.get(prevAdjEmbeddingVertex);
                for (Vertex adjEmbeddingVertex : embeddingVertexClockwiseOrder) {
                    Vertex adjNewVertex = embeddingVertexToNewVertex.get(adjEmbeddingVertex);
                    Vertex newJunction = vertices.get(adjEmbeddingVertex);
                    if (newJunction == null) {
                        newVertexClockwiseOrder.add(adjNewVertex);
                    } else if (newJunction != prevNewJunction) {
                        newVertexClockwiseOrder.add(newJunction);
                    } else {
                        newClockwiseOrder.put(newJunction, Arrays.asList(newVertex, prevAdjNewVertex, adjNewVertex));
                    }
                    prevNewJunction = newJunction;
                    prevAdjNewVertex = adjNewVertex;
                }
            }
            newClockwiseOrder.put(newVertex, newVertexClockwiseOrder);
        }

        // Compute the external face
        List<Vertex> embeddingExternalFace = embedding.embedding.externalFace;
        List<Vertex> newExternalFace = new ArrayList<Vertex>();
        Vertex prevEmbeddingVertex = embeddingExternalFace.get(embeddingExternalFace.size() - 1);
        for (Vertex embeddingVertex : embeddingExternalFace) {
            Vertex newVertex = embeddingVertexToNewVertex.get(embeddingVertex);

            // Insert the junction vertex succeeding prevEmbeddingVertex if any
            Map<Vertex, Vertex> vertices = clauseJunctionVertices.get(prevEmbeddingVertex);
            if (vertices != null) {
                Vertex newJunction = vertices.get(embeddingVertex);
                if (newJunction != null) {
                    List<Vertex> newVertexClockwiseOrder = newClockwiseOrder.get(newJunction);
                    int index = newVertexClockwiseOrder.indexOf(newVertex);
                    int nextIndex;
                    if (index + 1 < newVertexClockwiseOrder.size()) {
                        nextIndex = index + 1;
                    } else {
                        nextIndex = 0;
                    }
                    if (newVertexClockwiseOrder.get(nextIndex) != embeddingVertexToNewVertex.get(prevEmbeddingVertex)) {
                        newExternalFace.add(newJunction);
                    }
                }
            }

            // Insert the junction vertex preceding embeddingVertex if any
            vertices = clauseJunctionVertices.get(embeddingVertex);
            boolean skipVertex;
            if (vertices == null) {
                skipVertex = false;
            } else {
                Vertex newJunction = vertices.get(prevEmbeddingVertex);
                if (newJunction == null) {
                    skipVertex = false;
                } else {
                    newExternalFace.add(newJunction);
                    List<Vertex> newVertexClockwiseOrder = newClockwiseOrder.get(newJunction);
                    int index = newVertexClockwiseOrder.indexOf(newVertex);
                    int nextIndex;
                    if (index + 1 < newVertexClockwiseOrder.size()) {
                        nextIndex = index + 1;
                    } else {
                        nextIndex = 0;
                    }
                    skipVertex =
                        newVertexClockwiseOrder.get(nextIndex) == embeddingVertexToNewVertex.get(prevEmbeddingVertex);
                }
            }

            if (skipVertex) {
                // The external face visits the clause junction adjacent to newVertex, but does not (immediately) visit
                // the clause vertex newVertex
            } else {
                newExternalFace.add(newVertex);
            }
            prevEmbeddingVertex = embeddingVertex;
        }
        return new PlanarEmbedding(newClockwiseOrder, newExternalFace);
    }

    /**
     * Returns a map from each vertex V in the final graph that corresponds to a vertex in the initial graph, as
     * described in the comments for the implementation of this class, to a map from each adjacent vertex W (in the
     * final graph) to the index in IPlanarGadget.ports() for the gadget for V of the port to connect to W.
     * @param embedding The PlanarEmbeddingWithCrossings for the initial graph.
     * @param minPortGroups A map from each vertex to a map from the minimum port of each port range for the vertex to
     *     the adjacent vertices in that port range.  Each adjacent vertex belongs to a port range.
     * @param embeddingVertexToNewVertex A map from each vertex in embedding.graph to the corresponding vertex in the
     *     final graph.
     * @param clauseJunctionVertices A map from each clause vertex in embedding.graph to a map from each adjacent vertex
     *     where we interpose a junction vertex for the final graph to the junction vertex.  The keys of the maps are
     *     vertices in embedding.graph, while the values are vertices in the returned graph.
     * @return The ports.
     */
    private static Map<Vertex, Map<Vertex, Integer>> newPortsForOriginalVertices(
            PlanarEmbeddingWithCrossings embedding, Map<Vertex, SortedMap<Integer, Collection<Vertex>>> minPortGroups,
            Map<Vertex, Vertex> embeddingVertexToNewVertex, Map<Vertex, Map<Vertex, Vertex>> clauseJunctionVertices) {
        Map<Vertex, Map<Vertex, Integer>> newPorts = new HashMap<Vertex, Map<Vertex, Integer>>();
        Map<Vertex, Vertex> vertexToEmbeddingVertex = embedding.originalVertexToVertex;
        for (Entry<Vertex, SortedMap<Integer, Collection<Vertex>>> entry : minPortGroups.entrySet()) {
            Vertex vertex = entry.getKey();
            Vertex embeddingVertex = vertexToEmbeddingVertex.get(vertex);
            SortedMap<Integer, Collection<Vertex>> minPortToVertices = entry.getValue();

            // Compute a map from each adjacent vertex to the corresponding adjacent embedding vertex
            Map<Vertex, Vertex> adjVertexToAdjEmbeddingVertex = new HashMap<Vertex, Vertex>();
            for (Vertex adjVertex : vertex.edges) {
                List<Vertex> addedVertices = embedding.addedVertices(vertex, adjVertex);
                if (addedVertices.isEmpty()) {
                    adjVertexToAdjEmbeddingVertex.put(adjVertex, vertexToEmbeddingVertex.get(adjVertex));
                } else {
                    adjVertexToAdjEmbeddingVertex.put(adjVertex, addedVertices.get(0));
                }
            }

            List<Vertex> embeddingVertexClockwiseOrder = embedding.embedding.clockwiseOrder.get(embeddingVertex);

            // Rotate embeddingVertexClockwiseOrder so that the first vertex is from the minimum port range and the last
            // vertex is from the maximum port range
            List<Vertex> shiftedEmbeddingClockwiseOrder;
            if (minPortToVertices.size() == 1) {
                shiftedEmbeddingClockwiseOrder = embeddingVertexClockwiseOrder;
            } else {
                Set<Vertex> embeddingVertices = new HashSet<Vertex>();
                for (Vertex groupVertex : minPortToVertices.get(minPortToVertices.lastKey())) {
                    embeddingVertices.add(adjVertexToAdjEmbeddingVertex.get(groupVertex));
                }
                int index = embeddingVertexClockwiseOrder.indexOf(embeddingVertices.iterator().next());
                while (embeddingVertices.contains(embeddingVertexClockwiseOrder.get(index))) {
                    if (index + 1 < embeddingVertexClockwiseOrder.size()) {
                        index++;
                    } else {
                        index = 0;
                    }
                }
                shiftedEmbeddingClockwiseOrder = new ArrayList<Vertex>(embeddingVertexClockwiseOrder.size());
                shiftedEmbeddingClockwiseOrder.addAll(
                    embeddingVertexClockwiseOrder.subList(index, embeddingVertexClockwiseOrder.size()));
                shiftedEmbeddingClockwiseOrder.addAll(embeddingVertexClockwiseOrder.subList(0, index));
            }

            // Compute newPorts.get(newVertex)
            Map<Vertex, Vertex> vertices = clauseJunctionVertices.get(embeddingVertex);
            Map<Vertex, Integer> newVertexPorts = new HashMap<Vertex, Integer>();
            int index = 0;
            Vertex prevAdjEmbeddingVertex = shiftedEmbeddingClockwiseOrder.get(
                shiftedEmbeddingClockwiseOrder.size() - 1);
            for (Entry<Integer, Collection<Vertex>> groupEntry : minPortToVertices.entrySet()) {
                int port = groupEntry.getKey();
                Vertex prevNewJunction;
                if (vertices != null) {
                    prevNewJunction = vertices.get(prevAdjEmbeddingVertex);
                } else {
                    prevNewJunction = null;
                }
                for (int i = 0; i < groupEntry.getValue().size(); i++) {
                    Vertex adjEmbeddingVertex = shiftedEmbeddingClockwiseOrder.get(index);
                    Vertex adjNewVertex = embeddingVertexToNewVertex.get(adjEmbeddingVertex);
                    Vertex newJunction;
                    if (vertices != null) {
                        newJunction = vertices.get(adjEmbeddingVertex);
                    } else {
                        newJunction = null;
                    }
                    if (newJunction == null) {
                        Map<Vertex, Vertex> adjVertices = clauseJunctionVertices.get(adjEmbeddingVertex);
                        Vertex adjNewJunction;
                        if (adjVertices == null) {
                            adjNewJunction = null;
                        } else {
                            adjNewJunction = adjVertices.get(embeddingVertex);
                        }
                        if (adjNewJunction == null) {
                            newVertexPorts.put(adjNewVertex, port);
                        } else {
                            newVertexPorts.put(adjNewJunction, port);
                        }
                        port++;
                    } else if (newJunction != prevNewJunction) {
                        newVertexPorts.put(newJunction, port);
                        port++;
                    }
                    index++;
                    prevAdjEmbeddingVertex = adjEmbeddingVertex;
                    prevNewJunction = newJunction;
                }
            }
            newPorts.put(embeddingVertexToNewVertex.get(embeddingVertex), newVertexPorts);
        }
        return newPorts;
    }

    /**
     * Returns a map from each crossing vertex in "embedding" to the adjacent vertices in the order they are traversed
     * in "paths".  Each value consists of four elements: respectively, the first vertex used to enter the crossing
     * vertex, the first vertex used to exit the crossing vertex, the second vertex used to enter the crossing vertex,
     * and the second vertex used to exit the crossing vertex.
     * @param embedding The PlanarEmbeddingWithCrossings for the initial graph.
     * @param paths The sequence of paths, as in the "paths" argument to initialGraph.
     * @return The crossing vertex order.
     */
    private static Map<Vertex, List<Vertex>> embeddingCrossVertexOrder(
            PlanarEmbeddingWithCrossings embedding, List<List<Vertex>> paths) {
        Map<Vertex, List<Vertex>> embeddingCrossVertexOrder = new HashMap<Vertex, List<Vertex>>();
        Map<Vertex, Vertex> vertexToEmbeddingVertex = embedding.originalVertexToVertex;
        for (List<Vertex> curPath : paths) {
            Vertex prevVertex = curPath.get(0);
            Vertex prevEmbeddingVertex = vertexToEmbeddingVertex.get(prevVertex);
            for (int i = 1; i < curPath.size(); i++) {
                Vertex vertex = curPath.get(i);
                Vertex embeddingVertex = vertexToEmbeddingVertex.get(vertex);
                List<Vertex> addedVertices = embedding.addedVertices(prevVertex, vertex);
                Vertex prevAddedVertex = prevEmbeddingVertex;
                for (int j = 0; j < addedVertices.size(); j++) {
                    Vertex addedVertex = addedVertices.get(j);
                    if (addedVertex.edges.size() > 2) {
                        List<Vertex> order = embeddingCrossVertexOrder.get(addedVertex);
                        if (order == null) {
                            order = new ArrayList<Vertex>(4);
                            embeddingCrossVertexOrder.put(addedVertex, order);
                        }
                        order.add(prevAddedVertex);
                        if (j + 1 < addedVertices.size()) {
                            order.add(addedVertices.get(j + 1));
                        } else {
                            order.add(embeddingVertex);
                        }
                    }
                    prevAddedVertex = addedVertex;
                }
                prevVertex = vertex;
                prevEmbeddingVertex = embeddingVertex;
            }
        }
        return embeddingCrossVertexOrder;
    }

    /**
     * Creates and positions IPlanarGadgets in order to form a reduction from a 3-SAT problem.  This uses gadgets with
     * certain functions produced by the specified I3SatPlanarGadgetFactory, IPlanarWireFactory, and
     * IPlanarBarrierFactory.  The reduction has to do with moving around in the plane.  The 3-SAT problem has a
     * satisfying assignment if and only if it is possible to get from the start gadget to the finish gadget.  The
     * resulting layout will have the characteristics described in the comments for PlanarGadgetLayout.layout.  See also
     * I3SatPlanarGadgetFactory.
     * @param threeSat The 3-SAT problem.
     * @param threeSatFactory The factory to use to create gadgets for the reduction.
     * @param wireFactory The wire factory to use to create the wires.
     * @param barrierFactory The barrier factory to use to create the barriers.
     * @param startGadget The starting gadget for the reduction.
     * @param startPort The index in startGadget.ports() of the starting gadget port to use for the reduction.
     * @param finishGadget The ending gadget for the reduction.
     * @param finishPort The index in finishGadget.ports() of the ending gadget port to use for the reduction.
     * @return A map from each gadget to its top-left corner.
     */
    public static Map<IPlanarGadget, Point> layout(
            ThreeSat threeSat, I3SatPlanarGadgetFactory threeSatFactory,
            IPlanarWireFactory wireFactory, IPlanarBarrierFactory barrierFactory,
            IPlanarGadget startGadget, int startPort, IPlanarGadget finishGadget, int finishPort) {
        // Create the initial graph
        Map<Vertex, IPlanarGadget> gadgets = new HashMap<Vertex, IPlanarGadget>();
        Map<Vertex, Map<Vertex, Integer>> minPorts = new HashMap<Vertex, Map<Vertex, Integer>>();
        Map<Vertex, Map<Vertex, Integer>> maxPorts = new HashMap<Vertex, Map<Vertex, Integer>>();
        Map<Vertex, List<Vertex>> clauseEdges = new LinkedHashMap<Vertex, List<Vertex>>();
        List<List<Vertex>> paths = new ArrayList<List<Vertex>>();
        Graph graph = initialGraph(
            threeSat, threeSatFactory, startGadget, startPort, finishGadget, finishPort, gadgets, minPorts, maxPorts,
            clauseEdges, paths);

        // Compute the PlanarEmbeddingWithCrossings for the initial graph
        Map<Vertex, SortedMap<Integer, Collection<Vertex>>> minPortGroups =
            new HashMap<Vertex, SortedMap<Integer, Collection<Vertex>>>();
        Map<Vertex, EcNode> constraints = constraints(minPorts, maxPorts, clauseEdges, minPortGroups);
        PlanarEmbeddingWithCrossings embedding = EcPlanarEmbeddingWithCrossings.embed(
            graph.vertices.iterator().next(), constraints);
        Map<Vertex, Vertex> vertexToEmbeddingVertex = embedding.originalVertexToVertex;

        // Compute the final graph
        Map<Vertex, Vertex> embeddingVertexToNewVertex = new HashMap<Vertex, Vertex>();
        Map<Vertex, Map<Vertex, Vertex>> clauseJunctionVertices = new LinkedHashMap<Vertex, Map<Vertex, Vertex>>();
        newGraph(embedding, clauseEdges, embeddingVertexToNewVertex, clauseJunctionVertices);

        PlanarEmbedding newEmbedding = newEmbedding(embedding, embeddingVertexToNewVertex, clauseJunctionVertices);

        // Compute the gadgets for the vertices in the original graph and for the junction vertices
        Map<Vertex, IPlanarGadget> newGadgets = new HashMap<Vertex, IPlanarGadget>();
        for (Entry<Vertex, IPlanarGadget> entry : gadgets.entrySet()) {
            newGadgets.put(
                embeddingVertexToNewVertex.get(vertexToEmbeddingVertex.get(entry.getKey())), entry.getValue());
        }
        for (Map<Vertex, Vertex> vertices : clauseJunctionVertices.values()) {
            for (Vertex newVertex : new HashSet<Vertex>(vertices.values())) {
                newGadgets.put(newVertex, threeSatFactory.createJunction());
            }
        }

        // Compute the ports for the vertices in the original graph and for the junction vertices
        Map<Vertex, Map<Vertex, Integer>> newPorts = newPortsForOriginalVertices(
            embedding, minPortGroups, embeddingVertexToNewVertex, clauseJunctionVertices);
        for (Map<Vertex, Vertex> vertices : clauseJunctionVertices.values()) {
            for (Vertex newVertex : new HashSet<Vertex>(vertices.values())) {
                Map<Vertex, Integer> vertexNewPorts = new HashMap<Vertex, Integer>();
                int port = 0;
                for (Vertex adjNewVertex : newEmbedding.clockwiseOrder.get(newVertex)) {
                    vertexNewPorts.put(adjNewVertex, port);
                    port++;
                }
                newPorts.put(newVertex, vertexNewPorts);
            }
        }

        // Add the ports and gadgets for the crossing vertices
        Map<Vertex, List<Vertex>> embeddingCrossVertexOrder = embeddingCrossVertexOrder(embedding, paths);
        for (Entry<Vertex, List<Vertex>> entry : embeddingCrossVertexOrder.entrySet()) {
            Vertex embeddingVertex = entry.getKey();
            Vertex newVertex = embeddingVertexToNewVertex.get(embeddingVertex);
            List<Vertex> embeddingOrder = entry.getValue();
            List<Vertex> embeddingVertexClockwiseOrder = embedding.embedding.clockwiseOrder.get(embeddingVertex);

            int index = embeddingVertexClockwiseOrder.indexOf(embeddingOrder.get(0));
            int nextIndex;
            if (index + 1 < embeddingVertexClockwiseOrder.size()) {
                nextIndex = index + 1;
            } else {
                nextIndex = 0;
            }
            boolean isClockwise = embeddingVertexClockwiseOrder.get(nextIndex) == embeddingOrder.get(2);
            IPlanarGadget gadget = threeSatFactory.createCrossover(isClockwise);
            newGadgets.put(newVertex, gadget);

            Map<Vertex, Integer> vertexNewPorts = new HashMap<Vertex, Integer>();
            int[] ports = new int[]{
                threeSatFactory.firstCrossoverEntryPort(gadget, isClockwise),
                threeSatFactory.firstCrossoverExitPort(gadget, isClockwise),
                threeSatFactory.secondCrossoverEntryPort(gadget, isClockwise),
                threeSatFactory.secondCrossoverExitPort(gadget, isClockwise)};
            for (int i = 0; i < embeddingOrder.size(); i++) {
                Vertex adjEmbeddingVertex = embeddingOrder.get(i);
                Map<Vertex, Vertex> vertices = clauseJunctionVertices.get(adjEmbeddingVertex);
                Vertex newJunction;
                if (vertices != null) {
                    newJunction = vertices.get(embeddingVertex);
                } else {
                    newJunction = null;
                }
                if (newJunction == null) {
                    vertexNewPorts.put(embeddingVertexToNewVertex.get(adjEmbeddingVertex), ports[i]);
                } else {
                    vertexNewPorts.put(newJunction, ports[i]);
                }
            }
            newPorts.put(newVertex, vertexNewPorts);
        }

        // Add the ports and gadgets for the added non-crossing vertices in "embedding"
        for (Vertex vertex : graph.vertices) {
            for (Vertex adjVertex : vertex.edges) {
                for (Vertex addedVertex : embedding.addedVertices(vertex, adjVertex)) {
                    if (addedVertex.edges.size() == 2) {
                        Vertex newAddedVertex = embeddingVertexToNewVertex.get(addedVertex);
                        if (!newGadgets.containsKey(newAddedVertex)) {
                            newGadgets.put(newAddedVertex, threeSatFactory.createJunction());
                            Iterator<Vertex> iterator = newAddedVertex.edges.iterator();
                            Vertex newAdjVertex1 = iterator.next();
                            Vertex newAdjVertex2 = iterator.next();
                            Map<Vertex, Integer> vertexNewPorts = new HashMap<Vertex, Integer>();
                            vertexNewPorts.put(newAdjVertex1, 0);
                            vertexNewPorts.put(newAdjVertex2, 1);
                            newPorts.put(newAddedVertex, vertexNewPorts);
                        }
                    }
                }
            }
        }

        return PlanarGadgetLayout.layout(newEmbedding, newGadgets, newPorts, wireFactory, barrierFactory);
    }
}
