package com.github.btrekkie.graph.spqr;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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

import com.github.btrekkie.graph.MultiGraph;
import com.github.btrekkie.graph.MultiVertex;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.util.UnorderedPair;

/**
 * A node in an SPQR tree.  SPQR trees are useful for problems related to planar embedding.  An SPQR tree decomposes a
 * biconnected Graph (a graph that remains connected after removing any vertex) into MultiGraphs called "skeletons",
 * after first adding pairs of edges called "virtual edges".  The edges in a pair of virtual edges connect the same pair
 * of vertices and appear in different skeletons.  See the picture at the top of https://en.wikipedia.org/wiki/SPQR_tree
 * for an example.
 *
 * An SPQR tree has one node per skeleton.  There is an edge between two nodes if their skeletons contain a matching
 * pair of virtual edges.  There are three types of nodes:
 *
 * S (series) node: A node whose skeleton is a cycle consisting of at least three edges.
 * P (parallel) node: A node whose skeleton is a bond (two or more edges between the same pair of vertices).
 * R (rigid) node: A node whose skeleton is a triconnected graph (a graph that remains connected after
 *     removing any vertex or pair of vertices) with at least four or exactly two vertices that does not have multiple
 *     edges between any pair of vertices.
 *
 * (In another variant of SPQR trees, each non-virtual or "real" edge appears in a special leaf node called a Q node;
 * hence the name "SPQR tree".)  Of all trees satisfying the above conditions, we only regard trees with a mimimum
 * number of virtual edges as valid SPQR trees.  Each biconnected graph has a unique SPQR tree, apart from the choice of
 * a root node.  For a different and inscrutable definition of SPQR trees, see
 * http://ls11-www.cs.uni-dortmund.de/_media/techreports/tr09-09.pdf (Zey (2009): Algorithms for Planar Graph
 * Augmentation).
 *
 * SPQR trees are useful because there is a one-to-one correspondence between a biconnected graph's combinatorial
 * embeddings (the clockwise order of the edges at each vertex in a planar drawing) and a collection of combinatorial
 * embeddings for its SPQR tree's skeletons.  Each S node has only one combinatorial embedding.  For a P node, the
 * combinatorial embeddings are defined by the permutations of the edges.  Each R node has either zero combinatorial
 * embeddings or two combinatorial embeddings that are mirror images.
 */
/* We compute the SPQR tree for a graph using the algorithm described in
 * http://link.springer.com/content/pdf/10.1007%2F3-540-44541-2_8.pdf (Gutwenger and Mutzel (2001): A Linear Time
 * Implementation of SPQR-Trees), subject to the following correction, clarifications, and notes:
 *
 * - Note: https://en.wikipedia.org/wiki/SPQR_tree gives a good summary of the algorithm.
 * - Note: I don't understand why algorithms 4, 5, and 6 work; I just translated them into Java code.
 * - Clarification: In algorithm 2, when pairing virtual edges from different components, we pair a virtual edge in a
 *   polygon component with a virtual edge in a bond component in preference to pairing it with a virtual edge in
 *   another polygon component.
 * - Clarification: In line 2.3 of algorithm 2, we subtract both occurrences of "e" from "C_i U C_j".
 * - Clarification: When we first construct the palm tree, we assign each vertex a preliminary number
 *   PalmVertex.dfsNumber.  Then, we order the edges in each adjacency list in ascending order of phi(e), using this
 *   preliminary numbering scheme.  Then, we compute the final numbering scheme PalmVertex.number using the algorithm in
 *   "Hopcroft and Tarjan (1973): Dividing a graph into triconnected components", as Gutwenger and Mutzel suggests.  (At
 *   this point, there is no need to reorder the adjacency lists a second time.)
 * - Note: When finding the split components, the paper says to maintain a graph G_c and a palm tree P_c of G_c.  In
 *   fact, we do not need to maintain G_c, because it is not used anywhere; we only maintain P_c.
 * - Clarification: Technically, the graph P_c is not a palm tree, but rather a palm forest (a collection of palm
 *   trees).
 * - Clarification: Whenever we add an edge to P_c, we add it as either a tree edge or as a frond, depending on whether
 *   there is a proximate call to make_tree_edge.  For those edges on which we call make_tree_edge, we do not really add
 *   them as fronds and then convert them to tree edges, as this would result in a temporarily invalid palm forest.
 * - Clarification: The depth-first search over the palm tree does not traverse any edges added after algorithm 4
 *   starts.  To accomplish this, we add such edges to the beginning of their respective adjacency lists.
 * - Clarification: The adjacency lists are linked lists.
 * - Clarification: The term "degree" refers to the sum of a vertex's in-degree and its out-degree.
 * - Clarification: The term "first visited" in the definition of high(w) refers to the first of the edges traversed in
 *   a depth-first search of the original palm tree (in adjacency list order).  If there is no such edge, it refers to
 *   the first of the edges added in algorithms 4, 5, and 6.
 * - Clarification: Lines 37 - 39 of algorithm 4 (starting with "C := new_component(e, w -> v)") have a confusing way of
 *   indicating that we should mark the tree edge w -> v as a virtual edge, without adding or removing any edges from
 *   P_c.  Unlike with other edges added during algorithm 4, the depth-first search should not skip over w -> v.
 * - Clarification: The phrase "pop all (h, a, b) with a > lowpt1(w) from TSTACK" in algorithm 4 indicates to keep
 *   popping entries until the top entry fails to meet the condition.
 * - Correction: Line 10 of algorithm 5 should read "e' = new_virtual_edge(v, b, C)" instead of "e' =
 *   new_virtual_edge(v, x, C)".
 * - Clarification: The code for algorithm 6 has a confusing way of indicating that if the condition of the last if
 *   statement is true, we should add the frond v /-> lowpt1(w), and if it is false, we should not add or remove any
 *   edge between v and lowpt1(w).
 * - Clarification: When adding a frond x /-> y after initially creating the palm tree, we must add the frond in the
 *   correct position in the frond list for y.  We only add a frond in one part of the code, in algorithm 6.  We put a
 *   new frond in the former position of the frond ending at the same vertex that we just removed from the palm forest.
 * - Clarification: The term "not yet visited" in the code for algorithm 6 refers to the edges that were in the initial
 *   palm tree that algorithm 4 has not yet reached.
 */
public class SpqrNode {
    /** The type of an SPQR tree node. */
    public static enum Type {
        /** The type of an S (series) node. */
        S,

        /** The type of a P (parallel) node. */
        P,

        /** The type of an R (rigid) node. */
        R};

    /** The parent of this node, if any. */
    public final SpqrNode parent;

    /** The children of this node. */
    public Collection<SpqrNode> children = new ArrayList<SpqrNode>();

    /** The type of this node. */
    public final Type type;

    /** The skeleton graph for this node.  See skeletonVertexToVertex and realEdges. */
    public final MultiGraph skeleton;

    /** A map from each vertex in "skeleton" to the Vertex to which it corresponds in the original Graph. */
    public Map<MultiVertex, Vertex> skeletonVertexToVertex;

    /** The edges in "skeleton" that are real edges.  Each edge is represented as a pair of its endpoints. */
    public Set<UnorderedPair<MultiVertex>> realEdges;

    /** Constructs a new SpqrNode and adds it to parent.children. */
    public SpqrNode(
            SpqrNode parent, Type type, MultiGraph skeleton, Map<MultiVertex, Vertex> skeletonVertexToVertex,
            Set<UnorderedPair<MultiVertex>> realEdges) {
        this.parent = parent;
        this.type = type;
        this.skeleton = skeleton;
        this.skeletonVertexToVertex = skeletonVertexToVertex;
        this.realEdges = realEdges;
        if (parent != null) {
            parent.children.add(this);
        }
    }

    /**
     * Reorders the adjacency lists (the lists starting at PalmVertex.edgesHead) of vertices in the specified palm tree,
     * according to the sort order phi(e).
     * @param vertices The vertices in the palm tree.
     */
    private static void orderEdges(Collection<PalmVertex> vertices) {
        // Order the edges using bucket sort
        @SuppressWarnings("unchecked")
        Collection<PalmEdge>[] edges = new Collection[3 * vertices.size() + 6];
        for (PalmVertex vertex : vertices) {
            for (PalmEdge edge = vertex.edgesHead; edge != null; edge = edge.next) {
                int index;
                if (edge.isFrond) {
                    index = 3 * edge.end.dfsNumber + 1;
                } else if (edge.end.lowpoint2.dfsNumber < edge.start.dfsNumber) {
                    index = 3 * edge.end.lowpoint1.dfsNumber;
                } else {
                    index = 3 * edge.end.lowpoint1.dfsNumber + 2;
                }
                if (edges[index] == null) {
                    edges[index] = new ArrayList<PalmEdge>();
                }
                edges[index].add(edge);
            }
            vertex.edgesHead = null;
            vertex.edgesTail = null;
        }

        for (Collection<PalmEdge> bucketEdges : edges) {
            if (bucketEdges != null) {
                for (PalmEdge edge : bucketEdges) {
                    edge.prev = edge.start.edgesTail;
                    edge.next = null;
                    if (edge.start.edgesHead == null) {
                        edge.start.edgesHead = edge;
                    } else {
                        edge.start.edgesTail.next = edge;
                    }
                    edge.start.edgesTail = edge;
                }
            }
        }
    }

    /**
     * Computes PalmVertex.number, PalmVertex.isStart, and the fronds lists (starting at PalmVertex.frondsHead) for the
     * vertices in the specified palm tree.  Assumes the adjacency lists are ordered according to phi(e).
     * @param vertices The vertices in the palm tree.
     */
    private static void computeNumbersFrondsAndIsStart(Collection<PalmVertex> vertices) {
        // Compute isStart
        PalmVertex root = null;
        for (PalmVertex vertex : vertices) {
            if (vertex.parent == null) {
                root = vertex;
            }
            if (vertex.edgesHead != null) {
                for (PalmEdge edge = vertex.edgesHead.next; edge != null; edge = edge.next) {
                    edge.isStart = true;
                }
            }
        }
        root.edgesHead.isStart = true;

        // Compute PalmVertex.number and the fronds lists using an iterative implementation of depth-first search
        List<PalmEdge> path = new ArrayList<PalmEdge>();
        path.add(root.edgesHead);
        root.number = 1;
        int subtreeMax = vertices.size();
        while (!path.isEmpty()) {
            PalmEdge edge = path.get(path.size() - 1);
            if (edge == null) {
                path.remove(path.size() - 1);
                subtreeMax--;
            } else {
                path.set(path.size() - 1, edge.next);
                if (!edge.isFrond) {
                    edge.end.number = subtreeMax - edge.end.descendantCount + 1;
                    path.add(edge.end.edgesHead);
                } else {
                    edge.prevFrond = edge.end.frondsTail;
                    if (edge.end.frondsHead == null) {
                        edge.end.frondsHead = edge;
                    } else {
                        edge.end.frondsTail.nextFrond = edge;
                    }
                    edge.end.frondsTail = edge;
                }
            }
        }
    }

    /**
     * Computes a palm tree representation of the connected component containing the specified vertex, rooted at that
     * vertex.
     * @param root The root.
     * @return The vertices in the palm tree.
     */
    private static Collection<PalmVertex> createPalmTree(Vertex root) {
        // Use an iterative implementation of depth-first search to construct the palm tree's vertices and edges and
        // compute the vertices' parentEdge, lowpoint1, lowpoint2, and descendantCount fields
        Collection<PalmVertex> palmVertices = new ArrayList<PalmVertex>();
        PalmVertex palmRoot = new PalmVertex(root, null, 1, root.edges.size());
        palmVertices.add(palmRoot);
        List<PalmVertex> path = new ArrayList<PalmVertex>();
        path.add(palmRoot);
        List<Iterator<Vertex>> pathIters = new ArrayList<Iterator<Vertex>>();
        pathIters.add(root.edges.iterator());
        Map<Vertex, PalmVertex> vertexToPalmVertex = new HashMap<Vertex, PalmVertex>();
        vertexToPalmVertex.put(root, palmRoot);
        int nextDfsNumber = 2;
        while (!path.isEmpty()) {
            if (!pathIters.get(pathIters.size() - 1).hasNext()) {
                PalmVertex palmVertex = path.remove(path.size() - 1);
                pathIters.remove(pathIters.size() - 1);
                if (palmVertex.parent != null) {
                    // Finish visiting palmVertex.parentEdge: update lowpoint1, lowpoint2, and descendantCount
                    if (palmVertex.lowpoint1.dfsNumber < palmVertex.parent.lowpoint1.dfsNumber) {
                        if (palmVertex.parent.lowpoint1.dfsNumber < palmVertex.lowpoint2.dfsNumber) {
                            palmVertex.parent.lowpoint2 = palmVertex.parent.lowpoint1;
                        } else {
                            palmVertex.parent.lowpoint2 = palmVertex.lowpoint2;
                        }
                        palmVertex.parent.lowpoint1 = palmVertex.lowpoint1;
                    } else if (palmVertex.lowpoint1 == palmVertex.parent.lowpoint1) {
                        if (palmVertex.lowpoint2.dfsNumber < palmVertex.parent.lowpoint2.dfsNumber) {
                            palmVertex.parent.lowpoint2 = palmVertex.lowpoint2;
                        }
                    } else if (palmVertex.lowpoint1.dfsNumber < palmVertex.parent.lowpoint2.dfsNumber) {
                        palmVertex.parent.lowpoint2 = palmVertex.lowpoint1;
                    }
                    palmVertex.parent.descendantCount += palmVertex.descendantCount;
                }
            } else {
                PalmVertex palmStart = path.get(path.size() - 1);
                Vertex end = pathIters.get(pathIters.size() - 1).next();
                PalmVertex palmEnd = vertexToPalmVertex.get(end);
                PalmEdge edge;
                if (palmEnd == null) {
                    // Create a tree edge
                    palmEnd = new PalmVertex(end, palmStart, nextDfsNumber, end.edges.size());
                    nextDfsNumber++;
                    edge = PalmEdge.createRealTreeEdge(palmStart, palmEnd);
                    palmEnd.parentEdge = edge;
                    palmVertices.add(palmEnd);
                    vertexToPalmVertex.put(end, palmEnd);
                    path.add(palmEnd);
                    pathIters.add(end.edges.iterator());
                } else if (palmEnd.dfsNumber > palmStart.dfsNumber || palmStart.parent == palmEnd) {
                    // We are visiting a frond, but from the wrong direction
                    continue;
                } else {
                    // Create a frond
                    edge = PalmEdge.createRealFrond(palmStart, palmEnd);
                    if (palmEnd.dfsNumber < palmStart.lowpoint1.dfsNumber) {
                        palmStart.lowpoint2 = palmStart.lowpoint1;
                        palmStart.lowpoint1 = palmEnd;
                    } else if (palmEnd != palmStart.lowpoint1 && palmEnd.dfsNumber < palmStart.lowpoint2.dfsNumber) {
                        palmStart.lowpoint2 = palmEnd;
                    }
                }

                edge.prev = palmStart.edgesTail;
                if (palmStart.edgesHead == null) {
                    palmStart.edgesHead = edge;
                } else {
                    palmStart.edgesTail.next = edge;
                }
                palmStart.edgesTail = edge;
            }
        }

        orderEdges(palmVertices);
        computeNumbersFrondsAndIsStart(palmVertices);
        return palmVertices;
    }

    /**
     * Adds the specified edge to the palm tree.  However, this does not add the edge to the appropriate fronds list
     * (starting at PalmVertex.frondsHead) if it is a frond, because "add" does not know where to add it to the list.
     * This must be called after initially constructing the palm tree, i.e. when we are finding split components.
     */
    private static void add(PalmEdge edge) {
        if (!edge.isFrond) {
            edge.end.parentEdge = edge;
            edge.end.parent = null;
        }

        // Add the edge to the adjacency lists
        edge.next = edge.start.edgesHead;
        if (edge.start.edgesTail == null) {
            edge.start.edgesTail = edge;
        } else {
            edge.start.edgesHead.prev = edge;
        }
        edge.start.edgesHead = edge;

        edge.start.degree++;
        edge.end.degree++;
    }

    /**
     * Removes the specified edge from the palm tree.  This must be called after initially constructing the palm tree,
     * i.e. when we are finding split components.
     */
    private static void remove(PalmEdge edge) {
        // Remove the edge from the adjacency list
        if (edge.prev == null) {
            edge.start.edgesHead = edge.next;
        } else {
            edge.prev.next = edge.next;
        }
        if (edge.next == null) {
            edge.start.edgesTail = edge.prev;
        } else {
            edge.next.prev = edge.prev;
        }

        if (!edge.isFrond) {
            edge.end.parentEdge = null;
            edge.end.parent = edge.start;
        } else {
            // Remove the edge from the fronds list
            if (edge.prevFrond == null) {
                edge.end.frondsHead = edge.nextFrond;
            } else {
                edge.prevFrond.nextFrond = edge.nextFrond;
            }
            if (edge.nextFrond == null) {
                edge.end.frondsTail = edge.prevFrond;
            } else {
                edge.nextFrond.prevFrond = edge.prevFrond;
            }
        }

        edge.start.degree--;
        edge.end.degree--;
    }

    /**
     * Adds any SpqrComponents for type 2 pairs for the specified edge to "components".  This implements algorithm 5 of
     * the paper.
     * @param edge The edge.
     * @param eStack The edge stack.
     * @param tStack The triple stack.
     * @param components The collection to which to add the components.
     */
    private static void checkForType2Pairs(
            PalmEdge edge, List<PalmEdge> eStack, List<TStackEntry> tStack, Collection<SpqrComponent> components) {
        PalmVertex start = edge.start;
        if (start.parent == null) {
            return;
        }
        PalmVertex end = edge.end;
        TStackEntry lastEntry;
        if (tStack.isEmpty()) {
            lastEntry = null;
        } else {
            lastEntry = tStack.get(tStack.size() - 1);
        }
        while ((lastEntry != null && lastEntry.start == start) ||
                (end.degree == 2 && end.edgesHead.end.number > end.number)) {
            if (lastEntry != null && lastEntry.start == start && lastEntry.end.parent == lastEntry.start) {
                tStack.remove(tStack.size() - 1);
            } else {
                PalmEdge eab = null;
                PalmVertex newEdgeStart;
                PalmVertex newEdgeEnd;
                if (end.degree == 2 && end.edgesHead.end.number > end.number) {
                    PalmEdge stackEdge1 = eStack.remove(eStack.size() - 1);
                    PalmEdge stackEdge2 = eStack.remove(eStack.size() - 1);
                    if (stackEdge1.end == stackEdge2.start) {
                        newEdgeStart = stackEdge1.start;
                        newEdgeEnd = stackEdge2.end;
                    } else {
                        newEdgeStart = stackEdge2.start;
                        newEdgeEnd = stackEdge1.end;
                    }
                    SpqrComponent component = new SpqrComponent();
                    component.addEdge(stackEdge1);
                    component.addEdge(stackEdge2);
                    component.addVirtualEdge(newEdgeStart, newEdgeEnd);
                    components.add(component);
                    if (!eStack.isEmpty()) {
                        PalmEdge lastEdge = eStack.get(eStack.size() - 1);
                        if (lastEdge.start == newEdgeStart && lastEdge.end == newEdgeEnd) {
                            eab = eStack.remove(eStack.size() - 1);
                        }
                    }

                    remove(stackEdge1);
                    remove(stackEdge2);
                } else {
                    tStack.remove(tStack.size() - 1);
                    SpqrComponent component = new SpqrComponent();
                    while (!eStack.isEmpty()) {
                        PalmEdge lastEdge = eStack.get(eStack.size() - 1);
                        if (lastEdge.start.number < lastEntry.start.number ||
                                lastEdge.start.number > lastEntry.high.number ||
                                lastEdge.end.number < lastEntry.start.number ||
                                lastEdge.end.number > lastEntry.high.number) {
                            break;
                        }
                        eStack.remove(eStack.size() - 1);
                        if (lastEdge.end == lastEntry.start && lastEdge.start == lastEntry.end) {
                            eab = lastEdge;
                        } else {
                            component.addEdge(lastEdge);
                            remove(lastEdge);
                        }
                    }
                    component.addVirtualEdge(lastEntry.start, lastEntry.end);
                    components.add(component);
                    newEdgeStart = lastEntry.start;
                    newEdgeEnd = lastEntry.end;
                }

                if (eab != null) {
                    SpqrComponent component = new SpqrComponent();
                    component.addEdge(eab);
                    component.addVirtualEdge(newEdgeStart, newEdgeEnd);
                    component.addVirtualEdge(start, lastEntry.end);
                    components.add(component);
                    remove(eab);
                    newEdgeStart = start;
                    newEdgeEnd = lastEntry.end;
                }
                PalmEdge newEdge = PalmEdge.createVirtualTreeEdge(newEdgeStart, newEdgeEnd);
                eStack.add(newEdge);
                add(newEdge);
                end = newEdge.end;
            }

            if (tStack.isEmpty()) {
                lastEntry = null;
            } else {
                lastEntry = tStack.get(tStack.size() - 1);
            }
        }
    }

    /**
     * Adds any SpqrComponents for the type 1 pair for the specified edge to "components", if there is such a type 1
     * pair.  This implements algorithm 6 of the paper.
     * @param edge The edge.
     * @param eStack The edge stack.
     * @param components The collection to which to add the components.
     */
    private static void checkForType1Pair(PalmEdge edge, List<PalmEdge> eStack, Collection<SpqrComponent> components) {
        PalmVertex start = edge.start;
        PalmVertex end = edge.end;
        if (end.lowpoint2.number < start.number || end.lowpoint1.number >= start.number) {
            return;
        }
        if (start.parent != null && start.parent.parent == null) {
            // Check whether "start" is adjacent to a tree arc we have not yet visited
            boolean found = false;
            for (PalmEdge startEdge = edge.next; startEdge != null; startEdge = startEdge.next) {
                if (!startEdge.isFrond) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (PalmEdge startEdge = start.parentEdge.next; startEdge != null; startEdge = startEdge.next) {
                    if (!startEdge.isFrond) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                return;
            }
        }

        SpqrComponent component = new SpqrComponent();
        PalmEdge lastEdge;
        if (eStack.isEmpty()) {
            lastEdge = null;
        } else {
            lastEdge = eStack.get(eStack.size() - 1);
        }
        PalmEdge prevFrond = null;
        PalmEdge nextFrond = null;
        while (lastEdge != null &&
                ((lastEdge.start.number >= end.number && lastEdge.start.number < end.number + end.descendantCount) ||
                    (lastEdge.end.number >= end.number && lastEdge.end.number < end.number + end.descendantCount))) {
            if (lastEdge.isFrond && lastEdge.end == end.lowpoint1) {
                prevFrond = lastEdge.prevFrond;
                nextFrond = lastEdge.nextFrond;
            }
            component.addEdge(lastEdge);
            remove(lastEdge);
            eStack.remove(eStack.size() - 1);
            if (eStack.isEmpty()) {
                lastEdge = null;
            } else {
                lastEdge = eStack.get(eStack.size() - 1);
            }
        }
        PalmVertex newEdgeStart = start;
        PalmVertex newEdgeEnd = end.lowpoint1;
        component.addVirtualEdge(newEdgeStart, newEdgeEnd);
        components.add(component);

        if (lastEdge != null && lastEdge.start == newEdgeStart && lastEdge.end == newEdgeEnd) {
            component = new SpqrComponent();
            component.addEdge(lastEdge);
            for (int i = 0; i < 2; i++) {
                component.addVirtualEdge(newEdgeStart, newEdgeEnd);
            }
            components.add(component);
            if (lastEdge == prevFrond) {
                prevFrond = prevFrond.prevFrond;
            } else if (lastEdge == nextFrond) {
                nextFrond = nextFrond.nextFrond;
            }
            remove(lastEdge);
            eStack.remove(eStack.size() - 1);
        }

        if (end.lowpoint1 != start.parent) {
            PalmEdge newEdge = PalmEdge.createVirtualFrond(newEdgeStart, newEdgeEnd);
            eStack.add(newEdge);
            add(newEdge);

            // Add newEdge to the appropriate position in the fronds list
            newEdge.prevFrond = prevFrond;
            newEdge.nextFrond = nextFrond;
            if (prevFrond != null) {
                prevFrond.nextFrond = newEdge;
            } else {
                newEdgeEnd.frondsHead = newEdge;
            }
            if (nextFrond != null) {
                nextFrond.prevFrond = newEdge;
            } else {
                newEdgeEnd.frondsTail = newEdge;
            }
        } else {
            component = new SpqrComponent();
            component.addEdge(start.parentEdge);
            for (int i = 0; i < 2; i++) {
                component.addVirtualEdge(newEdgeStart, newEdgeEnd);
            }
            components.add(component);
            start.parentEdge.isVirtual = true;
        }
    }

    /**
     * Sets the "type" fields of the specified components to the appropriate values.
     * @param components The components.
     * @param vertexCount The number of vertices in the graph for which we are constructing an SPQR tree.
     */
    private static void computeComponentTypes(Collection<SpqrComponent> components, int vertexCount) {
        int[] degree = new int[vertexCount + 1];
        for (SpqrComponent component : components) {
            // Compute the degree of each vertex and the number of edges
            int edgeCount = 0;
            for (SpqrEdge edge = component.head; edge != null; edge = edge.next) {
                degree[edge.vertex1.number]++;
                degree[edge.vertex2.number]++;
                edgeCount++;
            }

            Type type;
            if (edgeCount == 1) {
                type = Type.R;
            } else if (degree[component.head.vertex1.number] == edgeCount &&
                    degree[component.head.vertex2.number] == edgeCount) {
                type = Type.P;
            } else {
                // A graph is a cycle iff every vertex has degree 2
                boolean found = false;
                for (SpqrEdge edge = component.head; edge != null; edge = edge.next) {
                    if (degree[edge.vertex1.number] != 2 || degree[edge.vertex2.number] != 2) {
                        found = true;
                        break;
                    }
                }
                type = found ? Type.R : Type.S;
            }
            component.type = type;

            // Reset "degree" so that every element is 0
            for (SpqrEdge edge = component.head; edge != null; edge = edge.next) {
                degree[edge.vertex1.number] = 0;
                degree[edge.vertex2.number] = 0;
            }
        }
    }

    /**
     * Moves all of the edges in "src" to "dest", removing destEdge and srcEdge.
     * @param dest The destination component.
     * @param src The source component.
     * @param destEdge The edge in "dest" to remove.
     * @param srcEdge The edge in "src" to remove.
     */
    private static void combine(SpqrComponent dest, SpqrComponent src, SpqrEdge destEdge, SpqrEdge srcEdge) {
        // Combine the lists
        dest.tail.next = src.head;
        src.head.prev = dest.tail;
        dest.tail = src.tail;

        // Remove destEdge
        if (destEdge.prev == null) {
            dest.head = dest.head.next;
        } else {
            destEdge.prev.next = destEdge.next;
        }
        if (destEdge.next == null) {
            dest.tail = dest.tail.prev;
        } else {
            destEdge.next.prev = destEdge.prev;
        }

        // Remove srcEdge
        if (srcEdge.prev == null) {
            dest.head = dest.head.next;
        } else {
            srcEdge.prev.next = srcEdge.next;
        }
        if (srcEdge.next == null) {
            dest.tail = dest.tail.prev;
        } else {
            srcEdge.next.prev = srcEdge.prev;
        }

        // Clear "src"
        src.head = null;
        src.tail = null;
    }

    /**
     * Merges the specified components at common virtual edges as in algorithm 2 in the paper.  This may destroy some of
     * the components.
     * @param components The components to merge.
     * @return The resulting components.
     */
    private static Collection<SpqrComponent> combineBondsAndPolygons(Collection<SpqrComponent> components) {
        // Note that this implementation is a little different from the one described in algorithm 2

        // Merge the bonds
        Map<UnorderedPair<PalmVertex>, SpqrComponent> pairToBondComponent =
            new LinkedHashMap<UnorderedPair<PalmVertex>, SpqrComponent>();
        for (SpqrComponent component : components) {
            if (component.type == Type.P) {
                SpqrEdge edge;
                if (component.head.isVirtual) {
                    edge = component.head;
                } else {
                    edge = component.head.next;
                }
                UnorderedPair<PalmVertex> pair = new UnorderedPair<PalmVertex>(edge.vertex1, edge.vertex2);
                SpqrComponent matchingComponent = pairToBondComponent.get(pair);
                if (matchingComponent == null) {
                    pairToBondComponent.put(pair, component);
                } else {
                    // Merge "component" into matchingComponent
                    SpqrEdge matchingEdge;
                    if (matchingComponent.head.isVirtual) {
                        matchingEdge = matchingComponent.head;
                    } else {
                        matchingEdge = matchingComponent.head.next;
                    }
                    combine(matchingComponent, component, matchingEdge, edge);
                }
            }
        }

        // Merge the polygons, and add the rigid components to rigidComponents
        Map<UnorderedPair<PalmVertex>, SpqrComponent> pairToPolygonComponent =
            new HashMap<UnorderedPair<PalmVertex>, SpqrComponent>();
        Map<UnorderedPair<PalmVertex>, SpqrEdge> pairToPolygonEdge =
            new HashMap<UnorderedPair<PalmVertex>, SpqrEdge>();
        Set<SpqrComponent> polygonComponents = new LinkedHashSet<SpqrComponent>();
        Collection<SpqrComponent> rigidComponents = new ArrayList<SpqrComponent>();
        for (SpqrComponent component : components) {
            switch (component.type) {
                case P:
                    break;
                case S:
                {
                    // Compute the components to merge into "component" and the virtual edges at which to merge
                    List<SpqrComponent> matchingComponents = new ArrayList<SpqrComponent>();
                    List<SpqrEdge> edges = new ArrayList<SpqrEdge>();
                    List<SpqrEdge> matchingEdges = new ArrayList<SpqrEdge>();
                    for (SpqrEdge edge = component.head; edge != null; edge = edge.next) {
                        if (edge.isVirtual) {
                            UnorderedPair<PalmVertex> pair = new UnorderedPair<PalmVertex>(edge.vertex1, edge.vertex2);
                            if (!pairToBondComponent.containsKey(pair)) {
                                SpqrComponent matchingComponent = pairToPolygonComponent.get(pair);
                                if (matchingComponent == null) {
                                    pairToPolygonComponent.put(pair, component);
                                    pairToPolygonEdge.put(pair, edge);
                                } else {
                                    edges.add(edge);
                                    matchingComponents.add(matchingComponent);
                                    matchingEdges.add(pairToPolygonEdge.get(pair));
                                }
                            }
                        }
                    }

                    for (int i = 0; i < matchingComponents.size(); i++) {
                        combine(component, matchingComponents.get(i), edges.get(i), matchingEdges.get(i));
                    }
                    polygonComponents.add(component);
                    polygonComponents.removeAll(matchingComponents);
                    break;
                }
                default:
                    rigidComponents.add(component);
                    break;
            }
        }

        // Collect the merged components
        Collection<SpqrComponent> combinedComponents = new ArrayList<SpqrComponent>(
            pairToBondComponent.size() + polygonComponents.size() + rigidComponents.size());
        combinedComponents.addAll(pairToBondComponent.values());
        combinedComponents.addAll(polygonComponents);
        combinedComponents.addAll(rigidComponents);
        return combinedComponents;
    }

    /**
     * Returns components containing the skeletons of the graph corresponding to the specified palm tree.  This executes
     * algorithms 3 and 2 in the paper.
     * @param vertices The vertices in the palm tree.
     * @return The components.
     */
    private static Collection<SpqrComponent> computeComponents(Collection<PalmVertex> vertices) {
        PalmVertex[] numberToVertex = new PalmVertex[vertices.size() + 1];
        for (PalmVertex vertex : vertices) {
            numberToVertex[vertex.number] = vertex;
        }

        // Execute algorithm 4 using an iterative implementation of depth-first search
        Collection<SpqrComponent> components = new ArrayList<SpqrComponent>();
        PalmVertex root = numberToVertex[1];
        List<PalmVertex> path = new ArrayList<PalmVertex>();
        path.add(root);
        List<PalmEdge> pathEdges = new ArrayList<PalmEdge>();
        pathEdges.add(root.edgesHead);
        List<PalmEdge> eStack = new ArrayList<PalmEdge>();
        List<TStackEntry> tStack = new ArrayList<TStackEntry>();
        while (!path.isEmpty()) {
            PalmEdge edge = pathEdges.get(pathEdges.size() - 1);
            if (edge == null) {
                PalmVertex vertex = path.remove(path.size() - 1);
                pathEdges.remove(pathEdges.size() - 1);
                if (vertex.parent != null) {
                    // Finish visiting vertex.parentEdge
                    PalmEdge parentEdge = vertex.parentEdge;
                    PalmVertex parent = vertex.parent;
                    eStack.add(parentEdge);
                    checkForType2Pairs(parentEdge, eStack, tStack, components);
                    checkForType1Pair(parentEdge, eStack, components);
                    if (parentEdge.isStart) {
                        while (tStack.remove(tStack.size() - 1) != null);
                    }
                    while (!tStack.isEmpty()) {
                        TStackEntry entry = tStack.get(tStack.size() - 1);
                        if (entry == null || entry.start == parent || entry.end == parent ||
                                parent.frondsHead == null || entry.high.number >= parent.frondsHead.start.number) {
                            break;
                        }
                        tStack.remove(tStack.size() - 1);
                    }
                }
            } else {
                pathEdges.set(pathEdges.size() - 1, edge.next);
                PalmVertex start = edge.start;
                PalmVertex end = edge.end;
                if (!edge.isFrond) {
                    if (edge.isStart) {
                        PalmVertex high = null;
                        TStackEntry lastEntry = null;
                        while (!tStack.isEmpty() && tStack.get(tStack.size() - 1) != null &&
                                tStack.get(tStack.size() - 1).start.number > end.lowpoint1.number) {
                            lastEntry = tStack.remove(tStack.size() - 1);
                            if (high == null || lastEntry.high.number > high.number) {
                                high = lastEntry.high;
                            }
                        }
                        PalmVertex lastSubtreeVertex = numberToVertex[end.number + end.descendantCount - 1];
                        if (lastEntry == null) {
                            tStack.add(new TStackEntry(lastSubtreeVertex, end.lowpoint1, start));
                        } else {
                            if (lastSubtreeVertex.number > high.number) {
                                high = lastSubtreeVertex;
                            }
                            tStack.add(new TStackEntry(high, end.lowpoint1, lastEntry.end));
                        }
                        tStack.add(null);
                    }

                    path.add(end);
                    pathEdges.add(end.edgesHead);
                } else {
                    if (edge.isStart) {
                        PalmVertex high = null;
                        TStackEntry lastEntry = null;
                        while (!tStack.isEmpty() && tStack.get(tStack.size() - 1) != null &&
                                tStack.get(tStack.size() - 1).start.number > end.number) {
                            lastEntry = tStack.remove(tStack.size() - 1);
                            if (high == null || lastEntry.high.number > high.number) {
                                high = lastEntry.high;
                            }
                        }
                        if (lastEntry == null) {
                            tStack.add(new TStackEntry(start, end, start));
                        } else {
                            tStack.add(new TStackEntry(high, end, lastEntry.end));
                        }
                    }

                    if (end != start.parent) {
                        eStack.add(edge);
                    } else {
                        SpqrComponent component = new SpqrComponent();
                        component.addEdge(edge);
                        component.addVirtualEdge(start, end);
                        PalmEdge newEdge = PalmEdge.createVirtualTreeEdge(end, start);
                        component.addEdge(newEdge);
                        components.add(component);
                        remove(edge);
                        add(newEdge);
                    }
                }
            }
        }

        if (!eStack.isEmpty()) {
            SpqrComponent component = new SpqrComponent();
            for (PalmEdge edge : eStack) {
                component.addEdge(edge);
            }
            components.add(component);
        }
        computeComponentTypes(components, vertices.size());
        return combineBondsAndPolygons(components);
    }

    /**
     * Returns a new SpqrNode for the specified component, adding it to parent.children.
     * @param parent The parent node.
     * @param component The component.
     * @return The node.
     */
    private static SpqrNode create(SpqrNode parent, SpqrComponent component) {
        MultiGraph skeleton = new MultiGraph();
        Map<PalmVertex, MultiVertex> palmVertexToMultiVertex = new LinkedHashMap<PalmVertex, MultiVertex>();
        Set<UnorderedPair<MultiVertex>> realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        for (SpqrEdge edge = component.head; edge != null; edge = edge.next) {
            MultiVertex multiVertex1 = palmVertexToMultiVertex.get(edge.vertex1);
            if (multiVertex1 == null) {
                multiVertex1 = skeleton.createVertex();
                palmVertexToMultiVertex.put(edge.vertex1, multiVertex1);
            }
            MultiVertex multiVertex2 = palmVertexToMultiVertex.get(edge.vertex2);
            if (multiVertex2 == null) {
                multiVertex2 = skeleton.createVertex();
                palmVertexToMultiVertex.put(edge.vertex2, multiVertex2);
            }
            multiVertex1.addEdge(multiVertex2);
            if (!edge.isVirtual) {
                realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
            }
        }

        Map<MultiVertex, Vertex> skeletonVertexToVertex = new LinkedHashMap<MultiVertex, Vertex>();
        for (Entry<PalmVertex, MultiVertex> entry : palmVertexToMultiVertex.entrySet()) {
            skeletonVertexToVertex.put(entry.getValue(), entry.getKey().vertex);
        }
        return new SpqrNode(parent, component.type, skeleton, skeletonVertexToVertex, realEdges);
    }

    /**
     * Returns the root of an SPQR tree for a graph consisting of the specified components.
     * @param components The components.
     * @param reference1 The first endpoint of the reference edge, as in the first argument to create(Vertex, Vertex).
     * @param reference2 The second endpoint of the reference edge, as in the second argument to create(Vertex, Vertex).
     * @return The root node.
     */
    private static SpqrNode createSpqrTree(Collection<SpqrComponent> components, Vertex reference1, Vertex reference2) {
        // Find the root component
        SpqrComponent rootComponent = null;
        for (SpqrComponent component : components) {
            for (SpqrEdge edge = component.head; edge != null; edge = edge.next) {
                if (!edge.isVirtual) {
                    if ((edge.vertex1.vertex == reference1 && edge.vertex2.vertex == reference2) ||
                            (edge.vertex1.vertex == reference2 && edge.vertex2.vertex == reference1)) {
                        rootComponent = component;
                        break;
                    }
                }
            }
            if (rootComponent != null) {
                break;
            }
        }

        // Compute maps from pairs of endpoints of virtual edges to the components that contain them
        Map<UnorderedPair<PalmVertex>, SpqrComponent> pComponents =
            new HashMap<UnorderedPair<PalmVertex>, SpqrComponent>();
        Map<UnorderedPair<PalmVertex>, Collection<SpqrComponent>> nonPComponents =
            new HashMap<UnorderedPair<PalmVertex>, Collection<SpqrComponent>>();
        for (SpqrComponent component : components) {
            for (SpqrEdge edge = component.head; edge != null; edge = edge.next) {
                if (edge.isVirtual) {
                    UnorderedPair<PalmVertex> pair = new UnorderedPair<PalmVertex>(edge.vertex1, edge.vertex2);
                    if (component.type == Type.P) {
                        pComponents.put(pair, component);
                    } else {
                        Collection<SpqrComponent> matchingComponents = nonPComponents.get(pair);
                        if (matchingComponents == null) {
                            matchingComponents = new ArrayList<SpqrComponent>();
                            nonPComponents.put(pair, matchingComponents);
                        }
                        matchingComponents.add(component);
                    }
                }
            }
        }

        // Use breadth-first search from the root node to construct the SPQR tree level by level
        Set<SpqrComponent> visited = new HashSet<SpqrComponent>();
        SpqrNode rootNode = create(null, rootComponent);
        List<SpqrComponent> levelComponents = Collections.singletonList(rootComponent);
        List<SpqrNode> levelNodes = Collections.singletonList(rootNode);
        while (!levelComponents.isEmpty()) {
            List<SpqrComponent> nextLevelComponents = new ArrayList<SpqrComponent>();
            List<SpqrNode> nextLevelNodes = new ArrayList<SpqrNode>();
            for (int i = 0; i < levelComponents.size(); i++) {
                SpqrComponent component = levelComponents.get(i);
                SpqrNode node = levelNodes.get(i);
                visited.add(component);

                // Find the components that share a virtual edge with "component"
                Set<SpqrComponent> matchingComponents = new LinkedHashSet<SpqrComponent>();
                for (SpqrEdge edge = component.head; edge != null; edge = edge.next) {
                    if (edge.isVirtual) {
                        matchingComponents.add(component);
                        UnorderedPair<PalmVertex> pair = new UnorderedPair<PalmVertex>(edge.vertex1, edge.vertex2);
                        if (component.type == Type.P) {
                            matchingComponents.addAll(nonPComponents.get(pair));
                            break;
                        } else {
                            SpqrComponent pComponent = pComponents.get(pair);
                            if (pComponent != null) {
                                matchingComponents.add(pComponent);
                            } else {
                                matchingComponents.addAll(nonPComponents.get(pair));
                            }
                        }
                    }
                }

                for (SpqrComponent matchingComponent : matchingComponents) {
                    if (matchingComponent != component && !visited.contains(matchingComponent)) {
                        SpqrNode childNode = create(node, matchingComponent);
                        nextLevelComponents.add(matchingComponent);
                        nextLevelNodes.add(childNode);
                    }
                }
            }

            levelComponents = nextLevelComponents;
            levelNodes = nextLevelNodes;
        }

        return rootNode;
    }

    /**
     * Returns the root of the SPQR tree for the graph containing the specified vertices.  The root node is the one
     * containing the real edge with the specified endpoints.  Assumes there is an edge with the specified endpoints.
     * Assumes the graph is biconnected.
     */
    public static SpqrNode create(Vertex reference1, Vertex reference2) {
        Collection<PalmVertex> palmVertices = createPalmTree(reference1);
        Collection<SpqrComponent> components = computeComponents(palmVertices);
        return createSpqrTree(components, reference1, reference2);
    }

    /**
     * Writes DOT code ( https://en.wikipedia.org/wiki/DOT_(graph_description_language) ) for the vertices and edges in
     * the skeleton graphs in the subtree rooted at this node to "writer".
     * @param writer The Writer to which to write the code.
     * @param nextMultiVertexId The next integer to use to uniquely identify a vertex.
     * @return The resulting next integer to use to uniquely identify a vertex.
     * @throws IOException If there was an I/O error writing to "writer".
     */
    private int writeDot(Writer writer, int nextMultiVertexId) throws IOException {
        // Write the vertices
        Map<MultiVertex, Integer> multiVertexIds = new HashMap<MultiVertex, Integer>();
        for (MultiVertex multiVertex : skeleton.vertices) {
            writer.write(
                "    vertex" + nextMultiVertexId +
                " [label=\"" + skeletonVertexToVertex.get(multiVertex).debugId + "\"];\n");
            multiVertexIds.put(multiVertex, nextMultiVertexId);
            nextMultiVertexId++;
        }

        // Write the edges
        Set<UnorderedPair<MultiVertex>> edges = new HashSet<UnorderedPair<MultiVertex>>();
        Set<UnorderedPair<MultiVertex>> realEdgesRemaining = new HashSet<UnorderedPair<MultiVertex>>(realEdges);
        for (MultiVertex multiVertex : skeleton.vertices) {
            for (MultiVertex adjMultiVertex : multiVertex.edges) {
                UnorderedPair<MultiVertex> edge = new UnorderedPair<MultiVertex>(multiVertex, adjMultiVertex);
                if (edges.contains(edge)) {
                    // Refrain from writing the same edge twice: once from each endpoint
                    edges.remove(edge);
                } else {
                    edges.add(edge);
                    writer.write(
                        "    vertex" + multiVertexIds.get(multiVertex) + " -- vertex" + multiVertexIds.get(adjMultiVertex));
                    if (!realEdgesRemaining.remove(edge)) {
                        writer.write(" [style=dotted]");
                    }
                    writer.write(";\n");
                }
            }
        }

        // Recurse on the children
        for (SpqrNode child : children) {
            nextMultiVertexId = child.writeDot(writer, nextMultiVertexId);
        }
        return nextMultiVertexId;
    }

    /**
     * Creates a PDF file depicting the SPQR tree rooted at this node.  Assumes the presence of the UNIX command-line
     * program /usr/local/bin/dot.  This method is intended for debugging.
     * @param filename The filename of the PDF file.
     * @throws IOException If there was an I/O exception writing the file.
     */
    public void writePdf(String filename) throws IOException {
        File file = File.createTempFile("graph", ".dot");
        try {
            Writer writer = new FileWriter(file);
            try {
                writer.write("graph {\n");
                writeDot(writer, 0);
                writer.write("}\n");
            } finally {
                writer.close();
            }
            try {
                Runtime.getRuntime().exec(
                    new String[]{"/usr/local/bin/dot", "-Tpdf", file.getAbsolutePath(), "-o", filename}).waitFor();
            } catch (InterruptedException exception) {
                throw new RuntimeException(exception);
            }
        } finally {
            file.delete();
        }
    }
}
