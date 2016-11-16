package com.github.btrekkie.graph.ec;

import java.util.ArrayList;
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

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.MultiVertex;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.dual.DualGraph;
import com.github.btrekkie.graph.ec.EcNode.Type;
import com.github.btrekkie.graph.planar.PlanarEmbedding;
import com.github.btrekkie.graph.planar.PlanarEmbeddingWithCrossings;
import com.github.btrekkie.util.UnorderedPair;

/**
 * Produces PlanarEmbeddingWithCrossings for graphs that have constraints specified using EcNodes.
 *
 * I haven't done a good analysis of the number of crossings that the current implementation of
 * EcPlanarEmbeddingWithCrossings produces, but I get the sense that it's rather large.  If you require a small number
 * of crossings, you may want to investigate the recommendations in
 * http://link.springer.com/content/pdf/10.1007%2F978-3-540-24595-7_2.pdf (Gutwenger and Mutzel (2003): An Experimental
 * Study of Crossing Minimization Heuristics).  See also the note in the comments for the implementation.
 */
/* The basic approach of EcPlanarEmbeddingWithCrossings is to keep adding edges from the input graph as long as the
 * graph remains ec-planar, and then to add each of the remaining edges along with suitable crossings.  To determine
 * which crossings to add, we find a shortest path in the dual graph (see DualGraph; derived from an arbitrary ec-planar
 * embedding) from a dual vertex corresponding to a starting face that satisfies the embedding constraints for the start
 * vertex to a dual vertex corresponding to an ending face that satisfies the embedding constraints for the end vertex.
 * The crossings consist of the edges in the primal graph corresponding to the edges in the dual graph that comprise the
 * path.
 *
 * The paper http://jgaa.info/accepted/2008/GutwengerKleinMutzel2008.12.1.pdf (Gutwenger, Klien, and Mutzel (2008):
 * Planarity Testing and Optimal Edge Insertion with Embedding Constraints) gives an algorithm for adding an edge
 * respecting embedding constraints with a minimum number of crossings.  For ease of implementation, we use the above,
 * simpler approach instead.
 */
public class EcPlanarEmbeddingWithCrossings {
    /**
     * Returns the root of a subtree that is the same as the subtree rooted at "node", but with all nodes with one child
     * replaced with the nearest descendants with multiple children.  For example, given a node of type
     * EcNode.Type.MIRROR with one child of EcNode.Type.ORIENTED that has four children of type EcNode.Type.VERTEX, this
     * returns a node of type EcNode.Type.ORIENTED that has four children of type EcNode.Type.VERTEX.  The resulting
     * tree is equivalent, and its height is at most the number of leaf nodes.
     * @param node The node from which to remove non-branching nodes.
     * @param parent The parent of the resulting node.  This method adds the resulting node as the last child of the
     *     parent.
     * @return The resulting node.
     */
    private static EcNode removeNonBranchingNodes(EcNode node, EcNode parent) {
        while (node.children.size() == 1) {
            node = node.children.get(0);
        }
        if (node.type == EcNode.Type.VERTEX) {
            return EcNode.createVertex(parent, node.vertex);
        } else {
            EcNode newNode = EcNode.create(parent, node.type);
            for (EcNode child : node.children) {
                removeNonBranchingNodes(child, newNode);
            }
            return newNode;
        }
    }

    /**
     * Adds a map from the vertex of each node of type EcNode.Type.VERTEX in the subtree rooted at "node" to the node to
     * "constraints".
     */
    private static void addLeafConstraints(EcNode node, Map<Vertex, EcNode> constraints) {
        if (node.type == Type.VERTEX) {
            constraints.put(node.vertex, node);
        } else {
            for (EcNode child : node.children) {
                addLeafConstraints(child, constraints);
            }
        }
    }

    /** Adds the vertices of the nodes of type EcNode.Type.VERTEX in the subtree rooted at "node" to "vertices". */
    private static void addLeafVertices(EcNode node, Collection<Vertex> vertices) {
        if (node.type == Type.VERTEX) {
            vertices.add(node.vertex);
        } else {
            for (EcNode child : node.children) {
                addLeafVertices(child, vertices);
            }
        }
    }

    /**
     * Returns the root of a subtree that is the same as the subtree rooted at "node", but with vertex nodes altered
     * according to the specified replacements.  Each vertex node with a vertex that is a key in "replacements" is
     * replaced with a vertex node whose vertex is the corresponding value.  Each vertex node with a vertex that is not
     * a key in "replacements" is removed (and so are any ancestors that have no children as a result).  This returns
     * null if none of the leaf nodes in the subtree rooted at "node" have vertices that are keys in "replacements".
     * @param node The node in which to replace vertices.
     * @param parent The parent of the resulting node.  This method adds the resulting node as the last child of the
     *     parent.
     * @param replacements A map from each vertex to its replacement.
     * @return The resulting node.
     */
    private static EcNode replaceVertices(EcNode node, EcNode parent, Map<Vertex, Vertex> replacements) {
        if (node.type == EcNode.Type.VERTEX) {
            Vertex replacement = replacements.get(node.vertex);
            if (replacement != null) {
                return EcNode.createVertex(parent, replacement);
            } else {
                return null;
            }
        } else {
            EcNode newNode = EcNode.create(parent, node.type);
            for (EcNode child : node.children) {
                replaceVertices(child, newNode, replacements);
            }
            if (!newNode.children.isEmpty()) {
                return newNode;
            } else {
                if (parent != null) {
                    parent.children.remove(parent.children.size() - 1);
                }
                return null;
            }
        }
    }

    /** Equivalent implementation is contractual. */
    private static void replaceVertices(
            Vertex start, EcNode node, Map<Vertex, EcNode> constraints, Map<Vertex, Vertex> replacements) {
        EcNode newNode;
        if (node != null) {
            newNode = replaceVertices(node, null, replacements);
        } else {
            newNode = null;
        }
        if (newNode != null) {
            constraints.put(start, newNode);
        } else {
            constraints.remove(start);
        }
    }

    /**
     * Adds mappings from each node in the subtree rooted at "node" to the number of leaf nodes in the subtree rooted
     * at the node whose vertices do not appear in excludeVertices.  This does not add any mappings for subtrees without
     * any such leaf vertices.
     * @param node The node.
     * @param excludeVertices The vertices to exclude.
     * @param leafCounts The map to which to add the leaf count mappings.
     * @return The number of leaf nodes in the subtree whose vertices do not appear in excludeVertices in the subtree
     *     rooted at "node".
     */
    private static int addLeafCounts(EcNode node, Set<Vertex> excludeVertices, Map<EcNode, Integer> leafCounts) {
        if (node.type == EcNode.Type.VERTEX) {
            if (excludeVertices.contains(node.vertex)) {
                return 0;
            } else {
                leafCounts.put(node, 1);
                return 1;
            }
        } else {
            int leafCount = 0;
            for (EcNode child : node.children) {
                leafCount += addLeafCounts(child, excludeVertices, leafCounts);
            }
            if (leafCount == 0) {
                return 0;
            } else {
                leafCounts.put(node, leafCount);
                return leafCount;
            }
        }
    }

    /**
     * Computes the first and last vertex for each node in the tree rooted at "node" in the specified clockwise
     * ordering.  That is, this finds a way of ordering the children of each node that satisfies the specified clockwise
     * ordering, and then it computes the vertex for the first and last leaf node for each node according to this
     * ordering.  It stores the results in constraintToFirstVertex and constraintToLastVertex.  This method assumes that
     * the clockwise ordering satisfies the constraints suggested by "node".
     *
     * The subtree rooted at "node" may have leaf nodes whose vertices do not appear in nextClockwise.  We ignore nodes
     * whose subtrees whose leaf nodes are limited to such nodes.
     *
     * Note that there may be multiple possible results.  For example, if the tree consists of a node of type
     * EcNode.Type.MIRROR with two leaf children, we may take either vertex to be the first vertex.
     *
     * @param node The root node.
     * @param nextClockwise A map from each vertex adjacent to the relevant vertex to the next adjacent vertex in the
     *     clockwise direction.
     * @param constraintToFirstVertex The map to which to add mappings from each node to the first vertex for that node.
     * @param constraintToLastVertex The map to which to add mappings from each node to the last vertex for that node.
     */
    private static void computeConstraintVertices(
            EcNode node, Map<Vertex, Vertex> nextClockwise,
            Map<EcNode, Vertex> constraintToFirstVertex, Map<EcNode, Vertex> constraintToLastVertex) {
        if (nextClockwise.isEmpty()) {
            return;
        }

        Map<Vertex, EcNode> leafConstraints = new HashMap<Vertex, EcNode>();
        addLeafConstraints(node, leafConstraints);

        Set<Vertex> missingVertexSet = new HashSet<Vertex>(leafConstraints.keySet());
        missingVertexSet.removeAll(nextClockwise.keySet());

        // Find the highest branching descendant of "node", i.e. the highest node with two children that have descendant
        // leaf nodes with vertices in nextClockwise.keySet().  To do so, we determine the depth of the descendant, then
        // identify an arbitrary leaf node, determine its height, and follow the appropriate number of parent links from
        // the leaf node.
        EcNode nodeWithoutMissingVertices = replaceVertices(node, null, nextClockwise);
        int depth;
        for (depth = 0; nodeWithoutMissingVertices.children.size() == 1; depth++) {
            nodeWithoutMissingVertices = nodeWithoutMissingVertices.children.get(0);
        }
        EcNode leafNode = null;
        for (EcNode curLeafNode : leafConstraints.values()) {
            if (!missingVertexSet.contains(curLeafNode.vertex)) {
                leafNode = curLeafNode;
                break;
            }
        }
        int height = 0;
        for (EcNode parent = leafNode; parent.parent != null; parent = parent.parent) {
            height++;
        }
        node = leafNode;
        for (int i = 0; i < height - depth; i++) {
            node = node.parent;
        }

        leafConstraints = new HashMap<Vertex, EcNode>();
        addLeafConstraints(node, leafConstraints);

        // Identify the first vertex of "node"
        Vertex startVertex;
        if (node.type == EcNode.Type.VERTEX) {
            startVertex = node.vertex;
        } else {
            // Find the first child of "node" that contains a vertex not in missingVertexSet
            Set<Vertex> childVertices = null;
            for (EcNode child : node.children) {
                childVertices = new HashSet<Vertex>();
                addLeafVertices(child, childVertices);
                childVertices.removeAll(missingVertexSet);
                if (!childVertices.isEmpty()) {
                    break;
                }
            }

            // Move to the vertex after the child
            startVertex = childVertices.iterator().next();
            while (childVertices.contains(startVertex)) {
                startVertex = nextClockwise.get(startVertex);
            }

            // Loop back around to the child's first vertex
            for (int i = 0; i < nextClockwise.size() - childVertices.size(); i++) {
                startVertex = nextClockwise.get(startVertex);
            }
        }

        // Compute the first and last vertex of each node by iterating over each vertex starting at startVertex and
        // "removing" it from its ancestors.  When we remove the first leaf node from a subtree, we add a mapping to
        // constraintToFirstVertex, and when we remove the last leaf node, we add a mapping to constraintToLastVertex.
        // "Removing" a leaf node consists of updating the remainingLeafCounts entries of its ancestors.
        // remainingLeafCounts is a map from each node to the total number of leaf nodes, including removed leaf nodes,
        // in the subtrees rooted at the children that have at least one non-removed leaf node left.
        Map<EcNode, Integer> leafCounts = new HashMap<EcNode, Integer>();
        addLeafCounts(node, missingVertexSet, leafCounts);
        int leafCount = leafCounts.get(node);
        for (EcNode parent = node; parent.parent != null; parent = parent.parent) {
            leafCounts.put(parent.parent, leafCount);
        }
        Map<EcNode, Integer> remainingLeafCounts = new HashMap<EcNode, Integer>(leafCounts);
        Vertex vertex = startVertex;
        do {
            // Add mappings to constraintToFirstVertex
            node = leafConstraints.get(vertex);
            for (EcNode parent = node;
                    parent != null && remainingLeafCounts.get(parent).equals(leafCounts.get(parent));
                    parent = parent.parent) {
                constraintToFirstVertex.put(parent, vertex);
            }

            // "Remove" the leaf node and add mappings to constraintToLastVertex
            remainingLeafCounts.put(node, 0);
            constraintToLastVertex.put(node, vertex);
            for (EcNode parent = node; parent.parent != null; parent = parent.parent) {
                int remainingLeafCount = remainingLeafCounts.get(parent.parent) - leafCounts.get(parent);
                remainingLeafCounts.put(parent.parent, remainingLeafCount);
                if (remainingLeafCount > 0) {
                    break;
                }
                constraintToLastVertex.put(parent.parent, vertex);
            }

            vertex = nextClockwise.get(vertex);
        } while (vertex != startVertex);
    }

    /**
     * Computes the positions in the clockwise ordering of edges aroung "start" for the edge from "start" to "end" that
     * satisfy the constraints for "start" described in the tree rooted at rootNode.  Returns a map from each vertex in
     * the dual graph that corresponds to a face for these positions to the vertex in the primal graph that is adjacent
     * to "start" and the face and is immediately clockwise relative to the other such vertex.
     * @param start The start vertex.
     * @param end The end vertex.
     * @param rootNode The constraint node.
     * @param nextClockwise A map from each vertex adjacent to the "start" to the next adjacent vertex in the clockwise
     *     direction, excluding "end".
     * @param rightFaces A map of the right faces in the dual graph, as in DualGraph.rightFaces.
     * @return The positions.
     */
    private static Map<DualVertex, Vertex> validStarts(
            Vertex start, Vertex end, EcNode rootNode, Map<Vertex, Map<Vertex, Vertex>> nextClockwise,
            Map<Vertex, Map<Vertex, DualVertex>> rightFaces) {
        // Compute the values of the map we will return
        Map<Vertex, Vertex> startNextClockwise = nextClockwise.get(start);
        Collection<Vertex> validSuccessors;
        if (rootNode == null) {
            validSuccessors = new ArrayList<Vertex>(start.edges);
        } else {
            Map<EcNode, Vertex> constraintToFirstVertex = new HashMap<EcNode, Vertex>();
            Map<EcNode, Vertex> constraintToLastVertex = new HashMap<EcNode, Vertex>();
            computeConstraintVertices(rootNode, startNextClockwise, constraintToFirstVertex, constraintToLastVertex);

            Map<Vertex, EcNode> leafConstraints = new HashMap<Vertex, EcNode>();
            addLeafConstraints(rootNode, leafConstraints);

            // Find the highest non-branching ancestor of leafConstraints.get(end)
            EcNode ancestor;
            for (ancestor = leafConstraints.get(end);
                    ancestor.parent != null && ancestor.parent.children.size() == 1; ancestor = ancestor.parent);

            // Determine whether ancestor.parent is the highest non-branching node
            boolean isRoot = startNextClockwise.get(constraintToLastVertex.get(ancestor.parent)) ==
                constraintToFirstVertex.get(ancestor.parent);

            // Compute validSuccessors by determining where we may order "ancestor" in ancestor.parent.children
            if (ancestor.parent.type == EcNode.Type.GROUP ||
                    (ancestor.parent.type == EcNode.Type.MIRROR &&
                        ((!isRoot && ancestor.parent.children.size() == 2) ||
                            (isRoot && ancestor.parent.children.size() == 3)))) {
                // We may order "ancestor" in any position in ancestor.parent.children
                validSuccessors = new ArrayList<Vertex>();
                for (EcNode sibling : ancestor.parent.children) {
                    if (sibling != ancestor) {
                        validSuccessors.add(constraintToFirstVertex.get(sibling));
                    }
                }
                if (!isRoot) {
                    validSuccessors.add(startNextClockwise.get(constraintToLastVertex.get(ancestor.parent)));
                }
            } else {
                int index = ancestor.parent.children.indexOf(ancestor);

                // Determine whether we must order ancestor.parent.children in reverse order
                EcNode firstChild;
                if (ancestor.parent.children.get(0) != ancestor) {
                    firstChild = ancestor.parent.children.get(0);
                } else {
                    firstChild = ancestor.parent.children.get(1);
                }
                boolean isFlipped = constraintToFirstVertex.get(ancestor.parent) !=
                    constraintToFirstVertex.get(firstChild);

                Vertex successor;
                if (!isFlipped) {
                    // We must order "ancestor" in the first position in ancestor.parent.children
                    if (index + 1 < ancestor.parent.children.size()) {
                        successor = constraintToFirstVertex.get(ancestor.parent.children.get(index + 1));
                    } else {
                        Vertex predecessor = constraintToLastVertex.get(ancestor.parent.children.get(index - 1));
                        successor = startNextClockwise.get(predecessor);
                    }
                } else {
                    // We must order "ancestor" in the last position in ancestor.parent.children
                    if (index > 0) {
                        successor = constraintToFirstVertex.get(ancestor.parent.children.get(index - 1));
                    } else {
                        Vertex predecessor = constraintToLastVertex.get(ancestor.parent.children.get(1));
                        successor = startNextClockwise.get(predecessor);
                    }
                }
                validSuccessors = Collections.singleton(successor);
            }
        }

        // Compute the return value from the successors
        Map<DualVertex, Vertex> validStarts = new LinkedHashMap<DualVertex, Vertex>();
        for (Vertex successor : validSuccessors) {
            validStarts.put(rightFaces.get(successor).get(start), successor);
        }
        return validStarts;
    }

    /**
     * Returns a shortest path from a vertex in "starts" to a vertex in "ends", if any, represented as a sequence of the
     * vertices in the path.  Assumes that "starts" and "ends" are disjoint.
     */
    private static List<DualVertex> findPath(Set<DualVertex> starts, Set<DualVertex> ends) {
        // Use breadth-first search to find a shortest path
        Map<DualVertex, DualVertex> predecessors = new HashMap<DualVertex, DualVertex>();
        for (DualVertex vertex : starts) {
            predecessors.put(vertex, null);
        }
        Collection<DualVertex> level = starts;
        DualVertex end = null;
        while (end == null && !level.isEmpty()) {
            Collection<DualVertex> nextLevel = new ArrayList<DualVertex>();
            for (DualVertex vertex : level) {
                for (DualVertex adjVertex : vertex.edgeCounts.keySet()) {
                    if (!predecessors.containsKey(adjVertex)) {
                        predecessors.put(adjVertex, vertex);
                        if (ends.contains(adjVertex)) {
                            end = adjVertex;
                            break;
                        }
                        nextLevel.add(adjVertex);
                    }
                }
                if (end != null) {
                    break;
                }
            }
            level = nextLevel;
        }

        if (end == null) {
            return null;
        } else {
            // Use "predecessors" to determine the vertices in the path
            List<DualVertex> path = new ArrayList<DualVertex>();
            for (DualVertex vertex = end; vertex != null; vertex = predecessors.get(vertex)) {
                path.add(vertex);
            }
            Collections.reverse(path);
            return path;
        }
    }

    /**
     * Removes the edge corresponding to "edge" from the specified dual graph (see DualGraph), if any and then adds an
     * edge dualEdge corresponding to "edge".  We represent an edge as a pair of its endpoints.
     * @param edge The primal edge.
     * @param dualEdge The dual edge.  If this is null, we remove the edge corresponding to "edge" in the dual graph
     *     without adding a replacement dual edge.
     * @param edgeToDualEdge A map from each edge in the primal graph to the corresponding edge in the dual graph, as in
     *     DualGraph.edgeToDualEdge.
     * @param dualEdgeToEdges A map from each edge in the dual graph to a collection of the corresponding edges in the
     *     primal graph, as in DualGraph.dualEdgeToEdges.
     */
    private static void setDualEdge(
            UnorderedPair<Vertex> edge, UnorderedPair<DualVertex> dualEdge,
            Map<UnorderedPair<Vertex>, UnorderedPair<DualVertex>> edgeToDualEdge,
            Map<UnorderedPair<DualVertex>, Set<UnorderedPair<Vertex>>> dualEdgeToEdges) {
        UnorderedPair<DualVertex> oldDualEdge;
        if (dualEdge != null) {
            oldDualEdge = edgeToDualEdge.put(edge, dualEdge);
        } else {
            oldDualEdge = edgeToDualEdge.remove(edge);
        }
        if (oldDualEdge == dualEdge) {
            return;
        }
        if (oldDualEdge != null) {
            Set<UnorderedPair<Vertex>> edges = dualEdgeToEdges.get(oldDualEdge);
            edges.remove(edge);
            if (edges.isEmpty()) {
                dualEdgeToEdges.remove(oldDualEdge);
            }
            oldDualEdge.value1.removeEdge(oldDualEdge.value2);
        }

        if (dualEdge != null) {
            Set<UnorderedPair<Vertex>> edges = dualEdgeToEdges.get(dualEdge);
            if (edges == null) {
                edges = new LinkedHashSet<UnorderedPair<Vertex>>();
                dualEdgeToEdges.put(dualEdge, edges);
            }
            edges.add(edge);
            dualEdge.value1.addEdge(dualEdge.value2);
        }
    }

    /**
     * Changes the edge from graphVertex1 to graphVertex2 into an edge from graphVertex1 to the crossing vertex
     * crossVertex and an edge from crossVertex to graphVertex2, and updates the bookkeeping represented in the
     * arguments to reflect this change.  graphVertex1 and graphVertex2 may correspond to vertices in the input graph or
     * to added vertices.
     * @param graphVertex1 The first vertex.
     * @param graphVertex2 The second vertex.
     * @param crossVertex The crossing vertex.
     * @param crossings A map from each crossing vertex in the output graph to the corresponding Crossing object.
     * @param graphVertexToVertex A map from each vertex in the output graph that has a corresponding vertex in the
     *     input graph to the corresponding vertex.
     * @param replacements A map from each vertex V in the input graph to a map from each adjacent vertex W to the first
     *     vertex in the path in the output graph corresponding to the edge from V to W after the vertex corresponding
     *     to V.
     * @param replacementsInverse A map from each key of "replacements" to a map that is the same as the associated
     *     value in "replacements", but with keys and values reversed.  In other words, given V, W, and X, if
     *     replacements.get(V).get(W) == X, then replacmentsInverse.get(V).get(X) == W.
     * @param constraints A map from each constrained vertex in the input graph to the root node of its constraint tree.
     *     It is okay for a vertex not to have a constraint tree.
     * @param graphConstraints A map from each constrained vertex in the output graph to the root node of its constraint
     *     tree.  This is equivalent to "constraints", but it refers to vertices in the output graph rather than
     *     vertices in the input graph, and it excludes edges in the input graph that do not yet have a corresponding
     *     path in the output graph.
     * @param nextClockwise A map from each vertex in the output graph to a map from each adjacent vertex to the next
     *     adjacent vertex in the clockwise direction (in the combinatorial embedding we are maintaining).
     * @param nextCounterclockwise A map from each vertex in the output graph to a map from each adjacent vertex to the
     *     next adjacent vertex in the counterclockwise direction (in the combinatorial embedding we are maintaining).
     */
    private static void addCrossing(
            Vertex graphVertex1, Vertex graphVertex2, Vertex crossVertex, Map<Vertex, Crossing> crossings,
            Map<Vertex, Vertex> graphVertexToVertex, Map<Vertex, Map<Vertex, Vertex>> replacements,
            Map<Vertex, Map<Vertex, Vertex>> replacementsInverse, Map<Vertex, EcNode> constraints,
            Map<Vertex, EcNode> graphConstraints,
            Map<Vertex, Map<Vertex, Vertex>> nextClockwise, Map<Vertex, Map<Vertex, Vertex>> nextCounterclockwise) {
        graphVertex1.removeEdge(graphVertex2);
        graphVertex1.addEdge(crossVertex);
        graphVertex2.addEdge(crossVertex);

        for (int i = 0; i < 2; i++) {
            // In the first iteration, fix the bookkeeping for graphVertex1.  In the second iteration, fix graphVertex2.
            Vertex graphStart;
            Vertex graphEnd;
            if (i == 0) {
                graphStart = graphVertex1;
                graphEnd = graphVertex2;
            } else {
                graphStart = graphVertex2;
                graphEnd = graphVertex1;
            }

            Crossing crossing = crossings.get(graphStart);
            if (crossing == null) {
                Vertex start = graphVertexToVertex.get(graphStart);
                if (start == null) {
                    // graphStart is an added vertex other than a crossing vertex
                } else {
                    // Update replacements.get(start), replacementsInverse.get(start), and graphConstraints.get(start)
                    Map<Vertex, Vertex> startReplacements = replacements.get(start);
                    Map<Vertex, Vertex> startReplacementsInverse = replacementsInverse.get(start);
                    Vertex replacementVertex = startReplacementsInverse.remove(graphEnd);
                    startReplacements.put(replacementVertex, crossVertex);
                    startReplacementsInverse.put(crossVertex, replacementVertex);
                    replaceVertices(graphStart, constraints.get(start), graphConstraints, startReplacements);
                }
            } else {
                // Update "crossing"
                if (crossing.start1 == graphEnd) {
                    crossing.start1 = crossVertex;
                } else if (crossing.end1 == graphEnd) {
                    crossing.end1 = crossVertex;
                } else if (crossing.start2 == graphEnd) {
                    crossing.start2 = crossVertex;
                } else {
                    crossing.end2 = crossVertex;
                }
            }

            // Update nextClockwise.get(graphStart) and nextCounterclockwise.get(graphStart)
            Map<Vertex, Vertex> startNextClockwise = nextClockwise.get(graphStart);
            Map<Vertex, Vertex> startNextCounterclockwise = nextCounterclockwise.get(graphStart);
            Vertex predecessor = startNextCounterclockwise.remove(graphEnd);
            Vertex successor = startNextClockwise.remove(graphEnd);
            startNextClockwise.put(predecessor, crossVertex);
            startNextClockwise.put(crossVertex, successor);
            startNextCounterclockwise.put(successor, crossVertex);
            startNextCounterclockwise.put(crossVertex, predecessor);
        }
    }

    /**
     * Updates the specified dual graph (as in DualGraph) so that the edges in the dual graph corresponding to the edges
     * in the primal graph of the face that contains the edges in faceBorder are made to be adjacent to newFace rather
     * than oldFace.  This is except for the edges in faceBorder, whose corresponding edges in the dual graph are made
     * to be adjacent to the vertices in borderLeftFaces and borderRightFaces.  This method assumes that the second or
     * second-to-last vertex in faceBorder has degree greater than two.
     * @param oldFace The old dual vertex.
     * @param newFace The new dual vertex.
     * @param faceBorder The face border path, represented as a sequence of its vertices.  The last vertex may be the
     *     same as the first vertex.  The edges in the path might not be in the dual graph yet, in which case this
     *     method adds them to the dual graph.
     * @param borderLeftFaces The dual vertices corresponding to the faces to the left of the edges in faceBorder, in
     *     order.  In other words, the face immediately counterclockwise relative to the edge from faceBorder.get(i) to
     *     faceBorder.get(i + 1) corresponds to borderLeftFaces.get(i).
     * @param borderRightFaces The dual vertices corresponding to the faces to the right of the edges in faceBorder, in
     *     order.  In other words, the face immediately clockwise relative to the edge from faceBorder.get(i) to
     *     faceBorder.get(i + 1) corresponds to borderLeftFaces.get(i).
     * @param edgeToDualEdge A map from each edge in the primal graph to the corresponding edge in the dual graph, as in
     *     DualGraph.edgeToDualEdge.
     * @param dualEdgeToEdges A map from each edge in the dual graph to a collection of the corresponding edges in the
     *     primal graph, as in DualGraph.dualEdgeToEdges.
     * @param rightFaces A map of the right faces in the dual graph, as in DualGraph.rightFaces.
     * @param nextClockwise A map from each vertex to a map from each adjacent vertex to the next adjacent vertex in the
     *     clockwise direction (in the combinatorial embedding we are maintaining).  This method does not alter
     *     nextClockwise.  The entries need only be current for those vertices in the face corresponding to newFace.
     */
    private static void splitFace(
            DualVertex oldFace, DualVertex newFace, List<Vertex> faceBorder, List<DualVertex> borderLeftFaces,
            List<DualVertex> borderRightFaces, Map<UnorderedPair<Vertex>, UnorderedPair<DualVertex>> edgeToDualEdge,
            Map<UnorderedPair<DualVertex>, Set<UnorderedPair<Vertex>>> dualEdgeToEdges,
            Map<Vertex, Map<Vertex, DualVertex>> rightFaces, Map<Vertex, Map<Vertex, Vertex>> nextClockwise) {
        // Update the dual edges corresponding to the edges in faceBorder
        for (int i = 0; i < faceBorder.size() - 1; i++) {
            Vertex vertex1 = faceBorder.get(i);
            Vertex vertex2 = faceBorder.get(i + 1);
            DualVertex leftFace = borderLeftFaces.get(i);
            DualVertex rightFace = borderRightFaces.get(i);
            Map<Vertex, DualVertex> rightFaces1 = rightFaces.get(vertex1);
            if (rightFaces1 == null) {
                rightFaces1 = new HashMap<Vertex, DualVertex>();
                rightFaces.put(vertex1, rightFaces1);
            }
            Map<Vertex, DualVertex> rightFaces2 = rightFaces.get(vertex2);
            if (rightFaces2 == null) {
                rightFaces2 = new HashMap<Vertex, DualVertex>();
                rightFaces.put(vertex2, rightFaces2);
            }
            rightFaces2.put(vertex1, leftFace);
            rightFaces1.put(vertex2, rightFace);
            setDualEdge(
                new UnorderedPair<Vertex>(vertex1, vertex2), new UnorderedPair<DualVertex>(leftFace, rightFace),
                edgeToDualEdge, dualEdgeToEdges);
        }

        // Determine the last edge in faceBorder in the counterclockwise direction
        Vertex start;
        Vertex end;
        Vertex endSuccessor;
        Vertex prevVertex;
        boolean isFlipped;
        if (faceBorder.get(1).edges.size() > 2) {
            isFlipped = nextClockwise.get(faceBorder.get(1)).get(faceBorder.get(2)) == faceBorder.get(0);
        } else {
            isFlipped =
                nextClockwise.get(faceBorder.get(faceBorder.size() - 2)).get(faceBorder.get(faceBorder.size() - 1)) ==
                faceBorder.get(faceBorder.size() - 3);
        }
        if (!isFlipped) {
            start = faceBorder.get(faceBorder.size() - 1);
            end = faceBorder.get(0);
            endSuccessor = faceBorder.get(1);
            prevVertex = faceBorder.get(faceBorder.size() - 2);
        } else {
            start = faceBorder.get(0);
            end = faceBorder.get(faceBorder.size() - 1);
            endSuccessor = faceBorder.get(faceBorder.size() - 2);
            prevVertex = faceBorder.get(1);
        }

        // Update the remaining dual edges
        Vertex vertex = start;
        while (vertex != end || nextClockwise.get(vertex).get(prevVertex) != endSuccessor) {
            // Advance to the next dual edge
            Vertex nextVertex = nextClockwise.get(vertex).get(prevVertex);
            prevVertex = vertex;
            vertex = nextVertex;

            // Update the dual edge
            UnorderedPair<Vertex> edge = new UnorderedPair<Vertex>(prevVertex, vertex);
            UnorderedPair<DualVertex> oldDualEdge = edgeToDualEdge.get(edge);
            UnorderedPair<DualVertex> newDualEdge;
            if (oldDualEdge.value1 == oldFace) {
                newDualEdge = new UnorderedPair<DualVertex>(oldDualEdge.value2, newFace);
            } else {
                newDualEdge = new UnorderedPair<DualVertex>(oldDualEdge.value1, newFace);
            }
            setDualEdge(edge, newDualEdge, edgeToDualEdge, dualEdgeToEdges);
            rightFaces.get(vertex).put(prevVertex, newFace);
        }
    }

    /**
     * Updates the specified dual graph to reflect splitting a face in the primal graph in two, due to the introduction
     * of an edge that crossed through the face (or, in the case of an added vertex that is not a crossing vertex), the
     * introduction of two edges).  This is for the addition of a path corresponding to an edge in the
     * input vertex, which we take have a certain direction.  We assume the path splits at least two faces.
     * @param index The index of the face in the list of faces the path splits.
     * @param face The dual vertex corresponding to the face we are splitting.
     * @param faces1 The dual vertices corresponding to the first smaller face into which we are splitting each face.
     *     This is parallel to the list of edges in the path, and refers to the smaller faces that are
     *     immediately counterclockwise relative to the edges.
     * @param faces2 The dual vertices corresponding to the second smaller face into which we are splitting each face.
     *     This is parallel to the list of edges in the path, and refers to the smaller faces that are
     *     immediately clockwise relative to the edges.
     * @param crossVertex The vertex of the edge(s) that split the face that is furthest in the direction of the path.
     * @param crossingStart The vertex on the path that is immediately adjacent to crossVertex in the opposite of the
     *     direction of the path.  If we are splitting the last face the path splits, this is the vertex of the edge(s)
     *     that split the face that is furthest in the direction opposite that of the path instead.
     * @param firstAddedVertex The second vertex in the path.
     * @param lastAddedVertex The second-to-last vertex in the path.  If we are not currently splitting the last face
     *     split by the path, then this is unspecified and may be inaccurate.
     * @param end The last vertex in the path.
     * @param edgeToDualEdge A map from each edge in the primal graph to the corresponding edge in the dual graph, as in
     *     DualGraph.edgeToDualEdge.
     * @param dualEdgeToEdges A map from each edge in the dual graph to a collection of the corresponding edges in the
     *     primal graph, as in DualGraph.dualEdgeToEdges.
     * @param rightFaces A map of the right faces in the dual graph, as in DualGraph.rightFaces.
     * @param nextClockwise A map from each vertex in the output graph to a map from each adjacent vertex to the next
     *     adjacent vertex in the clockwise direction (in the combinatorial embedding we are using).  This method does
     *     not alter nextClockwise.  The entries need only be current for those vertices in the face we are splitting.
     * @param nextCounterclockwise A map from each vertex in the output graph to a map from each adjacent vertex to the
     *     next adjacent vertex in the counterclockwise direction (in the combinatorial embedding we are using).  This
     *     method does not alter nextCounterclockwise.  The entries need only be current for those vertices in the face
     *     we are splitting.
     */
    private static void splitFace(
            int index, DualVertex face, List<DualVertex> faces1, List<DualVertex> faces2,
            Vertex crossVertex, Vertex crossingStart, Vertex firstAddedVertex, Vertex lastAddedVertex, Vertex end,
            Map<UnorderedPair<Vertex>, UnorderedPair<DualVertex>> edgeToDualEdge,
            Map<UnorderedPair<DualVertex>, Set<UnorderedPair<Vertex>>> dualEdgeToEdges,
            Map<Vertex, Map<Vertex, DualVertex>> rightFaces, Map<Vertex, Map<Vertex, Vertex>> nextClockwise,
            Map<Vertex, Map<Vertex, Vertex>> nextCounterclockwise) {
        // Compute the edges adjacent to crossingStart that are "perpendicular" to the path
        Vertex prevEndpoint1;
        Vertex prevEndpoint2;
        if (index == 0) {
            prevEndpoint1 = null;
            prevEndpoint2 = null;
        } else if (index + 1 < faces1.size()) {
            prevEndpoint1 = nextCounterclockwise.get(crossingStart).get(crossVertex);
            prevEndpoint2 = nextClockwise.get(crossingStart).get(crossVertex);
        } else {
            if (lastAddedVertex.edges.size() > 2) {
                prevEndpoint1 = nextCounterclockwise.get(crossingStart).get(end);
                prevEndpoint2 = nextClockwise.get(crossingStart).get(end);
            } else {
                prevEndpoint1 = nextCounterclockwise.get(crossingStart).get(lastAddedVertex);
                prevEndpoint2 = nextClockwise.get(crossingStart).get(lastAddedVertex);
            }
        }

        // Compute the edges adjacent to crossVertex that are "perpendicular" to the path
        Vertex endpoint1;
        Vertex endpoint2;
        if (index + 1 < faces1.size()) {
            endpoint1 = nextClockwise.get(crossVertex).get(crossingStart);
            endpoint2 = nextCounterclockwise.get(crossVertex).get(crossingStart);
        } else {
            endpoint1 = null;
            endpoint2 = null;
        }
        DualVertex face1 = faces1.get(index);
        DualVertex face2 = faces2.get(index);

        // Compute the first face border
        List<Vertex> faceBorder1 = new ArrayList<Vertex>();
        List<DualVertex> borderLeftFaces1 = new ArrayList<DualVertex>();
        List<DualVertex> borderRightFaces1 = new ArrayList<DualVertex>();
        if (index > 0) {
            faceBorder1.add(prevEndpoint1);
            borderLeftFaces1.add(face1);
            borderRightFaces1.add(faces1.get(index - 1));
        } else if (firstAddedVertex != crossVertex) {
            Iterator<Vertex> iterator = firstAddedVertex.edges.iterator();
            Vertex adjVertex = iterator.next();
            if (adjVertex == crossVertex) {
                adjVertex = iterator.next();
            }
            faceBorder1.add(adjVertex);
            borderLeftFaces1.add(face1);
            borderRightFaces1.add(face2);
        }
        faceBorder1.add(crossingStart);
        borderLeftFaces1.add(face1);
        borderRightFaces1.add(face2);
        if (index + 1 < faces1.size()) {
            faceBorder1.add(crossVertex);
            borderLeftFaces1.add(face1);
            borderRightFaces1.add(faces1.get(index + 1));
            faceBorder1.add(endpoint1);
        } else {
            if (lastAddedVertex.edges.size() == 2) {
                faceBorder1.add(lastAddedVertex);
                borderLeftFaces1.add(face1);
                borderRightFaces1.add(face2);
            }
            faceBorder1.add(end);
        }

        splitFace(
            face, face1, faceBorder1, borderLeftFaces1, borderRightFaces1, edgeToDualEdge, dualEdgeToEdges, rightFaces,
            nextClockwise);

        // Compute the second face border
        List<Vertex> faceBorder2 = new ArrayList<Vertex>();
        List<DualVertex> borderLeftFaces2 = new ArrayList<DualVertex>();
        List<DualVertex> borderRightFaces2 = new ArrayList<DualVertex>();
        if (index > 0) {
            faceBorder2.add(prevEndpoint2);
            borderLeftFaces2.add(faces2.get(index - 1));
            borderRightFaces2.add(face2);
        } else if (firstAddedVertex != crossVertex) {
            Iterator<Vertex> iterator = firstAddedVertex.edges.iterator();
            Vertex adjVertex = iterator.next();
            if (adjVertex == crossVertex) {
                adjVertex = iterator.next();
            }
            faceBorder2.add(adjVertex);
            borderLeftFaces2.add(face1);
            borderRightFaces2.add(face2);
        }
        faceBorder2.add(crossingStart);
        borderLeftFaces2.add(face1);
        borderRightFaces2.add(face2);
        if (index + 1 < faces1.size()) {
            faceBorder2.add(crossVertex);
            borderLeftFaces2.add(faces2.get(index + 1));
            borderRightFaces2.add(face2);
            faceBorder2.add(endpoint2);
        } else {
            if (lastAddedVertex.edges.size() == 2) {
                faceBorder2.add(lastAddedVertex);
                borderLeftFaces2.add(face1);
                borderRightFaces2.add(face2);
            }
            faceBorder2.add(end);
        }

        splitFace(
            face, face2, faceBorder2, borderLeftFaces2, borderRightFaces2, edgeToDualEdge, dualEdgeToEdges, rightFaces,
            nextClockwise);
    }

    /**
     * Adds an edge from crossEdge.value1 to crossEdge.value2 with crossings that maintain an ec-planar embedding, and
     * updates the bookkeeping represented in the arguments to reflect this change.  Assumes that it is impossible to
     * add such an edge without crossings.
     * @param crossEdge The edge to add.  The vertices are output graph vertices.
     * @param graph The output graph.
     * @param crossings A map from each crossing vertex in the output graph to the corresponding Crossing object.
     * @param edgeToDualEdge A map from each edge in the primal graph to the corresponding edge in the dual graph, as in
     *     DualGraph.edgeToDualEdge.
     * @param dualEdgeToEdges A map from each edge in the dual graph to a collection of the corresponding edges in the
     *     primal graph, as in DualGraph.dualEdgeToEdges.
     * @param rightFaces A map of the right faces in the dual graph, as in DualGraph.rightFaces.
     * @param nextClockwise A map from each vertex in the output graph to a map from each adjacent vertex to the next
     *     adjacent vertex in the clockwise direction (in the combinatorial embedding we are maintaining).
     * @param nextCounterclockwise A map from each vertex in the output graph to a map from each adjacent vertex to the
     *     next adjacent vertex in the counterclockwise direction (in the combinatorial embedding we are maintaining).
     * @param graphVertexToVertex A map from each vertex in the output graph that has a corresponding vertex in the
     *     input graph to the corresponding vertex.
     * @param replacements A map from each vertex V in the input graph to a map from each adjacent vertex W to the first
     *     vertex in the path in the output graph corresponding to the edge from V to W after the vertex corresponding
     *     to V.
     * @param replacementsInverse A map from each key of "replacements" to a map that is the same as the associated
     *     value in "replacements", but with keys and values reversed.  In other words, given V, W, and X, if
     *     replacements.get(V).get(W) == X, then replacmentsInverse.get(V).get(X) == W.
     * @param constraints A map from each constrained vertex in the input graph to the root node of its constraint tree.
     *     It is okay for a vertex not to have a constraint tree.
     * @param graphConstraints A map from each constrained vertex in the output graph to the root node of its constraint
     *     tree.  This is equivalent to "constraints", but it refers to vertices in the output graph rather than
     *     vertices in the input graph, and it excludes edges in the input graph that do not yet have a corresponding
     *     path in the output graph.
     */
    private static void addCrossEdge(
            UnorderedPair<Vertex> crossEdge, Graph graph, Map<Vertex, Crossing> crossings,
            Map<UnorderedPair<Vertex>, UnorderedPair<DualVertex>> edgeToDualEdge,
            Map<UnorderedPair<DualVertex>, Set<UnorderedPair<Vertex>>> dualEdgeToEdges,
            Map<Vertex, Map<Vertex, DualVertex>> rightFaces, Map<Vertex, Map<Vertex, Vertex>> nextClockwise,
            Map<Vertex, Map<Vertex, Vertex>> nextCounterclockwise, Map<Vertex, Vertex> graphVertexToVertex,
            Map<Vertex, Map<Vertex, Vertex>> replacements, Map<Vertex, Map<Vertex, Vertex>> replacementsInverse,
            Map<Vertex, EcNode> constraints, Map<Vertex, EcNode> graphConstraints) {
        // Add the edge to graphConstraints
        Vertex vertex1 = graphVertexToVertex.get(crossEdge.value1);
        Vertex vertex2 = graphVertexToVertex.get(crossEdge.value2);
        Map<Vertex, Vertex> replacements1 = replacements.get(vertex1);
        Map<Vertex, Vertex> replacements2 = replacements.get(vertex2);
        replacements1.put(vertex2, crossEdge.value2);
        replacements2.put(vertex1, crossEdge.value1);
        replaceVertices(crossEdge.value1, constraints.get(vertex1), graphConstraints, replacements1);
        replaceVertices(crossEdge.value2, constraints.get(vertex2), graphConstraints, replacements2);

        // Compute the path in the dual graph that crossEdge will take
        Map<DualVertex, Vertex> starts = validStarts(
            crossEdge.value1, crossEdge.value2, graphConstraints.get(crossEdge.value1), nextClockwise, rightFaces);
        Map<DualVertex, Vertex> ends = validStarts(
            crossEdge.value2, crossEdge.value1, graphConstraints.get(crossEdge.value2), nextClockwise, rightFaces);
        List<DualVertex> path = findPath(starts.keySet(), ends.keySet());

        // Create the vertices we will add to the primal and dual graphs, in order from crossEdge.value1 to
        // crossEdge.value2
        List<Vertex> crossVertices = new ArrayList<Vertex>(path.size() - 1);
        for (int i = 0; i < path.size() - 1; i++) {
            crossVertices.add(graph.createVertex());
        }
        List<DualVertex> faces1 = new ArrayList<DualVertex>(path.size());
        List<DualVertex> faces2 = new ArrayList<DualVertex>(path.size());
        for (int i = 0; i < path.size(); i++) {
            faces1.add(new DualVertex());
            faces2.add(new DualVertex());
        }

        // Add the crossings
        Vertex firstAddedVertex = crossVertices.get(0);
        Vertex lastAddedVertex = crossVertices.get(crossVertices.size() - 1);
        for (int i = 0; i < path.size() - 1; i++) {
            // Compute the vertices on the path that are adjacent to crossVertices.get(i)
            Vertex crossingStart;
            if (i > 0) {
                crossingStart = crossVertices.get(i - 1);
            } else {
                crossingStart = crossEdge.value1;
            }
            Vertex crossingEnd;
            if (i + 2 < path.size()) {
                crossingEnd = crossVertices.get(i + 1);
            } else {
                crossingEnd = crossEdge.value2;
            }

            // Compute the edge to cross
            Vertex crossVertex = crossVertices.get(i);
            DualVertex face = path.get(i);
            UnorderedPair<DualVertex> dualEdge = new UnorderedPair<DualVertex>(face, path.get(i + 1));
            UnorderedPair<Vertex> edge = null;
            for (UnorderedPair<Vertex> curEdge : dualEdgeToEdges.get(dualEdge)) {
                if ((i > 0 || (curEdge.value1 != crossEdge.value1 && curEdge.value2 != crossEdge.value1)) &&
                        (i + 2 < path.size() ||
                            (curEdge.value1 != crossEdge.value2 && curEdge.value2 != crossEdge.value2))) {
                    edge = curEdge;
                    break;
                }
            }
            if (edge == null) {
                edge = dualEdgeToEdges.get(dualEdge).iterator().next();

                // Add a non-crossing vertex
                if (i == 0 && (edge.value1 == crossEdge.value1 || edge.value2 == crossEdge.value1)) {
                    firstAddedVertex = graph.createVertex();
                    crossEdge.value1.addEdge(firstAddedVertex);
                    crossingStart = firstAddedVertex;

                    // Add the nextClockwise.get(firstAddedVertex) and nextCounterclockwise.get(firstAddedVertex)
                    // entries
                    Map<Vertex, Vertex> addedVertexNextClockwise = new HashMap<Vertex, Vertex>();
                    addedVertexNextClockwise.put(crossEdge.value1, crossVertex);
                    addedVertexNextClockwise.put(crossVertex, crossEdge.value1);
                    nextClockwise.put(firstAddedVertex, addedVertexNextClockwise);
                    Map<Vertex, Vertex> addedVertexNextCounterclockwise = new HashMap<Vertex, Vertex>(
                        addedVertexNextClockwise);
                    nextCounterclockwise.put(firstAddedVertex, addedVertexNextCounterclockwise);
                }
                if (i + 2 >= path.size() && (edge.value1 == crossEdge.value2 || edge.value2 == crossEdge.value2)) {
                    lastAddedVertex = graph.createVertex();
                    crossVertex.addEdge(lastAddedVertex);
                    crossingEnd = lastAddedVertex;

                    // Add the nextClockwise.get(lastAddedVertex) and nextCounterclockwise.get(lastAddedVertex) entries
                    Map<Vertex, Vertex> addedVertexNextClockwise = new HashMap<Vertex, Vertex>();
                    addedVertexNextClockwise.put(crossVertex, crossEdge.value2);
                    addedVertexNextClockwise.put(crossEdge.value2, crossVertex);
                    nextClockwise.put(lastAddedVertex, addedVertexNextClockwise);
                    Map<Vertex, Vertex> addedVertexNextCounterclockwise = new HashMap<Vertex, Vertex>(
                        addedVertexNextClockwise);
                    nextCounterclockwise.put(lastAddedVertex, addedVertexNextCounterclockwise);
                }
            }

            // Add the crossing
            Crossing crossing = new Crossing(crossingStart, crossingEnd, edge.value1, edge.value2);
            crossings.put(crossVertex, crossing);
            crossingStart.addEdge(crossVertex);
            addCrossing(
                edge.value1, edge.value2, crossVertex, crossings, graphVertexToVertex, replacements,
                replacementsInverse, constraints, graphConstraints, nextClockwise, nextCounterclockwise);

            if (i == 0) {
                // Add the edge from crossEdge.value1 to firstAddedVertex to nextClockwise and nextCounterclockwise
                Map<Vertex, Vertex> nextClockwise1 = nextClockwise.get(crossEdge.value1);
                Map<Vertex, Vertex> nextCounterclockwise1 = nextCounterclockwise.get(crossEdge.value1);
                Vertex successor1 = starts.get(path.get(0));
                if (firstAddedVertex != crossVertex && (edge.value1 == successor1 || edge.value2 == successor1)) {
                    successor1 = crossVertex;
                }
                Vertex predecessor1 = nextCounterclockwise1.get(successor1);
                nextClockwise1.put(predecessor1, firstAddedVertex);
                nextClockwise1.put(firstAddedVertex, successor1);
                nextCounterclockwise1.put(successor1, firstAddedVertex);
                nextCounterclockwise1.put(firstAddedVertex, predecessor1);
            }
            if (i == path.size() - 2) {
                // Add the edge from crossEdge.value2 to lastAddedVertex to nextClockwise and nextCounterclockwise
                Map<Vertex, Vertex> nextClockwise2 = nextClockwise.get(crossEdge.value2);
                Map<Vertex, Vertex> nextCounterclockwise2 = nextCounterclockwise.get(crossEdge.value2);
                Vertex successor2 = ends.get(path.get(path.size() - 1));
                if (lastAddedVertex != crossVertex && (edge.value1 == successor2 || edge.value2 == successor2)) {
                    successor2 = crossVertex;
                }
                Vertex predecessor2 = nextCounterclockwise2.get(successor2);
                nextClockwise2.put(predecessor2, lastAddedVertex);
                nextClockwise2.put(lastAddedVertex, successor2);
                nextCounterclockwise2.put(successor2, lastAddedVertex);
                nextCounterclockwise2.put(lastAddedVertex, predecessor2);
            }

            // Add the edges from crossVertex to nextClockwise and nextCounterclockwise
            Vertex endpoint1;
            Vertex endpoint2;
            if (rightFaces.get(edge.value1).get(edge.value2) == face) {
                endpoint1 = edge.value1;
                endpoint2 = edge.value2;
            } else {
                endpoint1 = edge.value2;
                endpoint2 = edge.value1;
            }
            Map<Vertex, Vertex> crossVertexNextClockwise = new HashMap<Vertex, Vertex>();
            crossVertexNextClockwise.put(crossingStart, endpoint1);
            crossVertexNextClockwise.put(endpoint1, crossingEnd);
            crossVertexNextClockwise.put(crossingEnd, endpoint2);
            crossVertexNextClockwise.put(endpoint2, crossingStart);
            nextClockwise.put(crossVertex, crossVertexNextClockwise);
            Map<Vertex, Vertex> crossVertexNextCounterclockwise = new HashMap<Vertex, Vertex>();
            for (Entry<Vertex, Vertex> entry : crossVertexNextClockwise.entrySet()) {
                crossVertexNextCounterclockwise.put(entry.getValue(), entry.getKey());
            }
            nextCounterclockwise.put(crossVertex, crossVertexNextCounterclockwise);

            // Remove "edge" from the dual graph
            setDualEdge(edge, null, edgeToDualEdge, dualEdgeToEdges);
            rightFaces.get(edge.value1).remove(edge.value2);
            rightFaces.get(edge.value2).remove(edge.value1);

            // Split the face preceding the crossing in the dual graph
            splitFace(
                i, face, faces1, faces2, crossVertex, crossingStart, firstAddedVertex, lastAddedVertex,
                crossEdge.value2, edgeToDualEdge, dualEdgeToEdges, rightFaces, nextClockwise, nextCounterclockwise);
        }

        crossEdge.value2.addEdge(lastAddedVertex);

        // Update the entries in "replacements", replacementsInverse, and graphConstraints for crossEdge.value1 and
        // crossEdge.value2
        vertex1 = graphVertexToVertex.get(crossEdge.value1);
        vertex2 = graphVertexToVertex.get(crossEdge.value2);
        replacements.get(vertex1).put(vertex2, firstAddedVertex);
        replacementsInverse.get(vertex1).put(firstAddedVertex, vertex2);
        replacements.get(vertex2).put(vertex1, lastAddedVertex);
        replacementsInverse.get(vertex2).put(lastAddedVertex, vertex1);
        replaceVertices(crossEdge.value1, constraints.get(vertex1), graphConstraints, replacements1);
        replaceVertices(crossEdge.value2, constraints.get(vertex2), graphConstraints, replacements2);

        // Split the last face in the dual graph
        splitFace(
            path.size() - 1, path.get(path.size() - 1), faces1, faces2, lastAddedVertex,
            crossVertices.get(crossVertices.size() - 1), firstAddedVertex, lastAddedVertex, crossEdge.value2,
            edgeToDualEdge, dualEdgeToEdges, rightFaces, nextClockwise, nextCounterclockwise);
    }

    /**
     * Adds the edges suggested by crossEdges with crossings that maintain an ec-planar embedding, and updates
     * graphConstraints to reflect this change.  Assumes that it is impossible to add each edge in crossEdges without
     * crossings.  Assumes that we have not added any crossings prior to calling addCrossings.
     * @param graph The output graph.
     * @param crossEdges The edges to add.  The vertices are output graph vertices.
     * @param graphVertexToVertex A map from each vertex in the output graph that has a corresponding vertex in the
     *     input graph to the corresponding vertex.
     * @param constraints A map from each constrained vertex in the input graph to the root node of its constraint tree.
     *     It is okay for a vertex not to have a constraint tree.
     * @param graphConstraints A map from each constrained vertex in the output graph to the root node of its constraint
     *     tree.  This is equivalent to "constraints", but it refers to vertices in the output graph rather than
     *     vertices in the input graph, and it excludes edges in the input graph that do not yet have a corresponding
     *     path in the output graph.
     * @param crossings A map from each crossing vertex we added to the output graph to the corresponding Crossing
     *     object.
     */
    private static Map<Vertex, Crossing> addCrossings(
            Graph graph, Collection<UnorderedPair<Vertex>> crossEdges, Map<Vertex, Vertex> graphVertexToVertex,
            Map<Vertex, EcNode> constraints, Map<Vertex, EcNode> graphConstraints) {
        if (crossEdges.isEmpty()) {
            return Collections.emptyMap();
        }

        // Initialize "replacements" and replacementsInverse
        Map<Vertex, Map<Vertex, Vertex>> replacements = new HashMap<Vertex, Map<Vertex, Vertex>>();
        Map<Vertex, Map<Vertex, Vertex>> replacementsInverse = new HashMap<Vertex, Map<Vertex, Vertex>>();
        for (Vertex graphVertex : graphVertexToVertex.keySet()) {
            Map<Vertex, Vertex> vertexReplacements = new HashMap<Vertex, Vertex>();
            Map<Vertex, Vertex> vertexReplacementsInverse = new HashMap<Vertex, Vertex>();
            for (Vertex graphAdjVertex : graphVertex.edges) {
                Vertex adjVertex = graphVertexToVertex.get(graphAdjVertex);
                vertexReplacements.put(adjVertex, graphAdjVertex);
                vertexReplacementsInverse.put(graphAdjVertex, adjVertex);
            }
            Vertex vertex = graphVertexToVertex.get(graphVertex);
            replacements.put(vertex, vertexReplacements);
            replacementsInverse.put(vertex, vertexReplacementsInverse);
        }

        PlanarEmbedding embedding = EcPlanarEmbedding.embed(graph.vertices.iterator().next(), graphConstraints);
        DualGraph dual = DualGraph.compute(embedding);

        // Compute nextClockwise and nextCounterclockwise from embedding.clockwiseOrder
        Map<Vertex, Map<Vertex, Vertex>> nextClockwise = new HashMap<Vertex, Map<Vertex, Vertex>>();
        Map<Vertex, Map<Vertex, Vertex>> nextCounterclockwise = new HashMap<Vertex, Map<Vertex, Vertex>>();
        for (Entry<Vertex, List<Vertex>> entry : embedding.clockwiseOrder.entrySet()) {
            List<Vertex> clockwiseOrder = entry.getValue();
            Map<Vertex, Vertex> vertexNextClockwise = new HashMap<Vertex, Vertex>();
            for (int i = 0; i < clockwiseOrder.size() - 1; i++) {
                vertexNextClockwise.put(clockwiseOrder.get(i), clockwiseOrder.get(i + 1));
            }
            vertexNextClockwise.put(clockwiseOrder.get(clockwiseOrder.size() - 1), clockwiseOrder.get(0));

            Map<Vertex, Vertex> vertexNextCounterclockwise = new HashMap<Vertex, Vertex>();
            vertexNextCounterclockwise.put(clockwiseOrder.get(0), clockwiseOrder.get(clockwiseOrder.size() - 1));
            for (int i = 0; i < clockwiseOrder.size() - 1; i++) {
                vertexNextCounterclockwise.put(clockwiseOrder.get(i + 1), clockwiseOrder.get(i));
            }

            Vertex vertex = entry.getKey();
            nextClockwise.put(vertex, vertexNextClockwise);
            nextCounterclockwise.put(vertex, vertexNextCounterclockwise);
        }

        // Compute edgeToDualEdge and dualEdgeToEdges from "dual"
        Map<MultiVertex, DualVertex> dualVertices = new HashMap<MultiVertex, DualVertex>();
        for (MultiVertex multiVertex : dual.graph.vertices) {
            dualVertices.put(multiVertex, new DualVertex());
        }
        Map<UnorderedPair<Vertex>, UnorderedPair<DualVertex>> edgeToDualEdge =
            new HashMap<UnorderedPair<Vertex>, UnorderedPair<DualVertex>>();
        Map<UnorderedPair<DualVertex>, Set<UnorderedPair<Vertex>>> dualEdgeToEdges =
            new HashMap<UnorderedPair<DualVertex>, Set<UnorderedPair<Vertex>>>();
        for (Entry<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>> entry : dual.edgeToDualEdge.entrySet()) {
            UnorderedPair<Vertex> edge = entry.getKey();
            UnorderedPair<MultiVertex> multiVertexEdge = entry.getValue();
            UnorderedPair<DualVertex> dualEdge = new UnorderedPair<DualVertex>(
                dualVertices.get(multiVertexEdge.value1), dualVertices.get(multiVertexEdge.value2));
            setDualEdge(edge, dualEdge, edgeToDualEdge, dualEdgeToEdges);
        }

        // Compute rightFaces from "dual"
        Map<Vertex, Map<Vertex, DualVertex>> rightFaces = new HashMap<Vertex, Map<Vertex, DualVertex>>();
        for (Vertex vertex : graph.vertices) {
            Map<Vertex, DualVertex> vertexRightFaces = new HashMap<Vertex, DualVertex>();
            for (Vertex adjVertex : vertex.edges) {
                vertexRightFaces.put(adjVertex, dualVertices.get(dual.rightFace(vertex, adjVertex)));
            }
            rightFaces.put(vertex, vertexRightFaces);
        }

        // Add the edges
        Map<Vertex, Crossing> crossings = new HashMap<Vertex, Crossing>();
        for (UnorderedPair<Vertex> crossEdge : crossEdges) {
            addCrossEdge(
                crossEdge, graph, crossings, edgeToDualEdge, dualEdgeToEdges, rightFaces,
                nextClockwise, nextCounterclockwise, graphVertexToVertex, replacements, replacementsInverse,
                constraints, graphConstraints);
        }
        return crossings;
    }

    /**
     * Returns the longest path starting with the edge from firstVertex to secondPath that is a subpath of a path in the
     * output graph corresponding to an edge in the input graph.  Assumes there is such an edge.
     * @param firstVertex The start of the edge.
     * @param secondVertex The end of the edge.
     * @param crossings A map from each crossing vertex we added to the output graph to the corresponding Crossing
     *     object.
     * @param nonAddedVertices The vertices in the output graph that have corresponding vertices in the input graph.
     * @param visited1 The vertices for whose corresponding crossings we visited an edge to Crossing.start1 or
     *     Crossing.end1.  This method adds to "visited1" whenever is visits such an edge.
     * @param visited2 The vertices for whose corresponding crossings we visited an edge to Crossing.start2 or
     *     Crossing.end2.  This method adds to "visited2" whenever is visits such an edge.
     * @return The vertices of the path, starting with firstVertex and secondVertex.
     */
    private static List<Vertex> crossingPath(
            Vertex firstVertex, Vertex secondVertex, Map<Vertex, Crossing> crossings, Set<Vertex> nonAddedVertices,
            Set<Vertex> visited1, Set<Vertex> visited2) {
        List<Vertex> path = new ArrayList<Vertex>();
        path.add(firstVertex);
        path.add(secondVertex);
        Vertex prevVertex = firstVertex;
        Vertex vertex = secondVertex;
        while (!nonAddedVertices.contains(vertex)) {
            Vertex nextVertex;
            Crossing crossing = crossings.get(vertex);
            if (crossing == null) {
                Iterator<Vertex> iterator = vertex.edges.iterator();
                nextVertex = iterator.next();
                if (nextVertex == prevVertex) {
                    nextVertex = iterator.next();
                }
            } else if (crossing.start1 == prevVertex) {
                nextVertex = crossing.end1;
                visited1.add(vertex);
            } else if (crossing.end1 == prevVertex) {
                nextVertex = crossing.start1;
                visited1.add(vertex);
            } else if (crossing.start2 == prevVertex) {
                nextVertex = crossing.end2;
                visited2.add(vertex);
            } else {
                nextVertex = crossing.start2;
                visited2.add(vertex);
            }
            prevVertex = vertex;
            vertex = nextVertex;
            path.add(vertex);
        }
        return path;
    }

    /**
     * Returns the added vertices map as in PlanarEmbeddingWithCrossings.addedVertices.
     * @param crossings A map from each crossing vertex we added to the output graph to the corresponding Crossing
     *     object.
     * @param graphVertexToVertex A map from each vertex in the output graph that has a corresponding vertex in the
     *     input graph to the corresponding vertex.
     * @param orphanedAddedVertices A collection to which to add all orphaned added vertices: non-crossing added
     *     vertices that are not adjacent to non-added vertices.  We may contract each such vertex with an adjacent
     *     vertex, because these vertices are no longer useful.  However, this method does not perform such
     *     contractions, and the return value includes such vertices.
     * @return The added vertices.
     */
    private static Map<UnorderedPair<Vertex>, List<Vertex>> addedVertices(
            Map<Vertex, Crossing> crossings, Map<Vertex, Vertex> graphVertexToVertex,
            Collection<Vertex> orphanedAddedVertices) {
        Map<UnorderedPair<Vertex>, List<Vertex>> addedVertices = new HashMap<UnorderedPair<Vertex>, List<Vertex>>();
        Set<Vertex> visited1 = new HashSet<Vertex>();
        Set<Vertex> visited2 = new HashSet<Vertex>();
        for (Entry<Vertex, Crossing> entry : crossings.entrySet()) {
            Vertex vertex = entry.getKey();
            Crossing crossing = entry.getValue();
            for (int i = 0; i < 2; i++) {
                // In the first iteration, find the path containing the edge from crossing.start1 to "vertex".  In the
                // second iteration, find the path containing the edge from crossing.start2 to "vertex".
                Set<Vertex> curVisited;
                Vertex curStart;
                Vertex curEnd;
                if (i == 0) {
                    curVisited = visited1;
                    curStart = crossing.start1;
                    curEnd = crossing.end1;
                } else {
                    curVisited = visited2;
                    curStart = crossing.start2;
                    curEnd = crossing.end2;
                }

                if (curVisited.add(vertex)) {
                    // Compute the path
                    List<Vertex> path1 = crossingPath(
                        vertex, curStart, crossings, graphVertexToVertex.keySet(), visited1, visited2);
                    List<Vertex> path2 = crossingPath(
                        vertex, curEnd, crossings, graphVertexToVertex.keySet(), visited1, visited2);
                    Collections.reverse(path1);
                    List<Vertex> path = new ArrayList<Vertex>(path1.size() + path2.size() - 1);
                    path.addAll(path1);
                    path.addAll(path2.subList(1, path2.size()));

                    // Add to orphanedAddedVertices
                    if (path.size() >= 5) {
                        for (Vertex curVertex : path.subList(2, path.size() - 2)) {
                            if (curVertex.edges.size() == 2) {
                                orphanedAddedVertices.add(curVertex);
                            }
                        }
                    }

                    // Add to addedVertices
                    UnorderedPair<Vertex> edge = new UnorderedPair<Vertex>(
                        graphVertexToVertex.get(path.get(0)), graphVertexToVertex.get(path.get(path.size() - 1)));
                    addedVertices.put(edge, path);
                }
            }
        }
        return addedVertices;
    }

    /**
     * Returns a PlanarEmbeddingWithCrossings that gives an ec-planar embedding of the connected component containing
     * "start", after adding crossings.  If possible, this does not add any crossings or other vertices.
     * @param start The vertex.
     * @param constraints A map from each constrained vertex to the root node of its constraint tree.  It is okay for a
     *     vertex not to have a constraint tree.
     * @return The ec-planar embedding.
     */
    public static PlanarEmbeddingWithCrossings embed(Vertex start, Map<Vertex, EcNode> constraints) {
        EcPlanarEmbedding.assertValid(constraints);

        // Remove non-branching nodes to avoid asymptotically worse performance
        Map<Vertex, EcNode> newConstraints = new HashMap<Vertex, EcNode>();
        for (Entry<Vertex, EcNode> entry : constraints.entrySet()) {
            newConstraints.put(entry.getKey(), removeNonBranchingNodes(entry.getValue(), null));
        }
        constraints = newConstraints;

        Graph graph = new Graph();
        Vertex graphStart = graph.createVertex();
        Map<Vertex, Vertex> vertexToGraphVertex = new HashMap<Vertex, Vertex>();
        Map<Vertex, Vertex> graphVertexToVertex = new HashMap<Vertex, Vertex>();
        vertexToGraphVertex.put(start, graphStart);
        graphVertexToVertex.put(graphStart, start);

        // Iterate over the edges in the connected component using breadth-first search.  Add any edges that do not make
        // an ec-planar embedding impossible without crossings.
        Map<Vertex, EcNode> graphConstraints = new HashMap<Vertex, EcNode>();
        Map<Vertex, Map<Vertex, Vertex>> replacements = new HashMap<Vertex, Map<Vertex, Vertex>>();
        replacements.put(start, new HashMap<Vertex, Vertex>());
        Set<UnorderedPair<Vertex>> visited = new HashSet<UnorderedPair<Vertex>>();
        Collection<UnorderedPair<Vertex>> crossEdges = new ArrayList<UnorderedPair<Vertex>>();
        Collection<Vertex> level = Collections.singleton(start);
        while (!level.isEmpty()) {
            Collection<Vertex> nextLevel = new ArrayList<Vertex>();
            for (Vertex vertex : level) {
                Vertex graphVertex = vertexToGraphVertex.get(vertex);
                Map<Vertex, Vertex> vertexReplacements = replacements.get(vertex);
                for (Vertex adjVertex : vertex.edges) {
                    Vertex graphAdjVertex = vertexToGraphVertex.get(adjVertex);
                    if (graphAdjVertex == null) {
                        graphAdjVertex = graph.createVertex();
                        vertexToGraphVertex.put(adjVertex, graphAdjVertex);
                        graphVertexToVertex.put(graphAdjVertex, adjVertex);
                    }

                    UnorderedPair<Vertex> edge = new UnorderedPair<Vertex>(graphVertex, graphAdjVertex);
                    if (!visited.add(edge)) {
                        continue;
                    }
                    nextLevel.add(adjVertex);

                    // Update the entries in "replacements" and graphConstraints for "vertex" and adjVertex
                    Map<Vertex, Vertex> adjVertexReplacements = replacements.get(adjVertex);
                    if (adjVertexReplacements == null) {
                        adjVertexReplacements = new HashMap<Vertex, Vertex>();
                        replacements.put(adjVertex, adjVertexReplacements);
                    }
                    vertexReplacements.put(adjVertex, graphAdjVertex);
                    adjVertexReplacements.put(vertex, graphVertex);
                    replaceVertices(graphVertex, constraints.get(vertex), graphConstraints, vertexReplacements);
                    replaceVertices(
                        graphAdjVertex, constraints.get(adjVertex), graphConstraints, adjVertexReplacements);

                    graphVertex.addEdge(graphAdjVertex);
                    PlanarEmbedding embedding = EcPlanarEmbedding.embed(graphStart, graphConstraints);
                    if (embedding == null) {
                        crossEdges.add(edge);
                        graphVertex.removeEdge(graphAdjVertex);

                        // Update the entries in "replacements" and graphConstraints for "vertex" and adjVertex
                        vertexReplacements.remove(adjVertex);
                        adjVertexReplacements.remove(vertex);
                        replaceVertices(graphVertex, constraints.get(vertex), graphConstraints, vertexReplacements);
                        replaceVertices(
                            graphAdjVertex, constraints.get(adjVertex), graphConstraints, adjVertexReplacements);
                    }
                }
            }
            level = nextLevel;
        }

        Map<Vertex, Crossing> crossings = addCrossings(
            graph, crossEdges, graphVertexToVertex, constraints, graphConstraints);

        Set<Vertex> orphanedAddedVertices = new HashSet<Vertex>();
        Map<UnorderedPair<Vertex>, List<Vertex>> addedVertices = addedVertices(
            crossings, graphVertexToVertex, orphanedAddedVertices);
        if (!orphanedAddedVertices.isEmpty()) {
            // Contract each orphaned added vertex with an adjacent vertex
            for (Vertex orphanedVertex : orphanedAddedVertices) {
                Iterator<Vertex> iterator = orphanedVertex.edges.iterator();
                Vertex adjVertex1 = iterator.next();
                Vertex adjVertex2 = iterator.next();
                orphanedVertex.removeEdge(adjVertex1);
                orphanedVertex.removeEdge(adjVertex2);
                graph.vertices.remove(orphanedVertex);
                adjVertex1.addEdge(adjVertex2);

                // Update the adjacent crossings
                Crossing crossing1 = crossings.get(adjVertex1);
                if (crossing1.start1 == orphanedVertex) {
                    crossing1.start1 = adjVertex2;
                } else if (crossing1.end1 == orphanedVertex) {
                    crossing1.end1 = adjVertex2;
                } else if (crossing1.start2 == orphanedVertex) {
                    crossing1.start2 = adjVertex2;
                } else {
                    crossing1.end2 = adjVertex2;
                }
                Crossing crossing2 = crossings.get(adjVertex2);
                if (crossing2.start1 == orphanedVertex) {
                    crossing2.start1 = adjVertex1;
                } else if (crossing2.end1 == orphanedVertex) {
                    crossing2.end1 = adjVertex1;
                } else if (crossing2.start2 == orphanedVertex) {
                    crossing2.start2 = adjVertex1;
                } else {
                    crossing2.end2 = adjVertex1;
                }
            }

            // Remove orphaned added vertices from addedVertices
            Map<UnorderedPair<Vertex>, List<Vertex>> newAddedVertices =
                new HashMap<UnorderedPair<Vertex>, List<Vertex>>();
            for (Entry<UnorderedPair<Vertex>, List<Vertex>> entry : addedVertices.entrySet()) {
                List<Vertex> path = new ArrayList<Vertex>();
                for (Vertex vertex : entry.getValue()) {
                    if (!orphanedAddedVertices.contains(vertex)) {
                        path.add(vertex);
                    }
                }
                newAddedVertices.put(entry.getKey(), path);
            }
            addedVertices = newAddedVertices;
        }

        // Add a mirror constraint for each crossing to ensure that it really is a crossing
        for (Entry<Vertex, Crossing> entry : crossings.entrySet()) {
            Crossing crossing = entry.getValue();
            EcNode node = EcNode.create(null, EcNode.Type.MIRROR);
            EcNode.createVertex(node, crossing.start1);
            EcNode.createVertex(node, crossing.start2);
            EcNode.createVertex(node, crossing.end1);
            EcNode.createVertex(node, crossing.end2);
            graphConstraints.put(entry.getKey(), node);
        }

        PlanarEmbedding embedding = EcPlanarEmbedding.embed(graphStart, graphConstraints);
        return new PlanarEmbeddingWithCrossings(graph, embedding, vertexToGraphVertex, addedVertices);
    }
}
