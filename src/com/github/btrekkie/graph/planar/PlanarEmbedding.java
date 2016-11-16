package com.github.btrekkie.graph.planar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.btrekkie.graph.Vertex;

/**
 * A description of a planar drawing of a connected planar graph.  It consists of a clockwise ordering of the edges
 * around each vertex along with an external face, a sequence of the edges on the outside of the embedding.
 * PlanarEmbeddings only apply to graphs with at least one vertex.
 */
/* Computing a planar embedding for a graph is implemented using the algorithm described in
 * https://www.emis.de/journals/JGAA/accepted/2004/BoyerMyrvold2004.8.3.pdf (Boyer and Myrvold (2004): On the Cutting
 * Edge: Simplified O(n) Planarity by Edge Addition).  However, the data structures differ a little from those in
 * Appendix A, and the particulars of flipping components are a little different.
 *
 * The paper gives a good explanation of determining whether a graph is planar, but it is brief when it comes to
 * computing the planar embedding.  We compute the embedding from the faces of the graph.  Each time we add an edge, we
 * add the internal face containing that edge to the collection of faces.
 *
 * The term "out" refers to the "forward" direction: the reverse of the direction used to reach the current location.
 * The term "in" is the opposite.
 */
public class PlanarEmbedding {
    /**
     * A map from each vertex in the graph to the adjacent vertices, in clockwise order of the edges to those vertices.
     */
    public Map<Vertex, List<Vertex>> clockwiseOrder;

    /**
     * The sequence of vertices in the polygon on the outside of the embedding, in clockwise order.  This may repeat
     * vertices.  For example, if the graph consists a hub vertex, three spoke vertices, and three edges from the hub to
     * the spokes, then the external face consists of an alternation between the hub vertex and the spoke vertices.
     */
    public List<Vertex> externalFace;

    /**
     * Constructs a new PlanarEmbedding.
     * @param clockwiseOrder A map from each vertex in the graph to the adjacent vertices, in clockwise order of the
     *     edges to those vertices.
     * @param externalFace The external face, as in the externalFace field.
     * @param isPartial Whether the embedding is (potentially) for a subgraph of a connected component, as opposed to an
     *     entire connected component.
     */
    private PlanarEmbedding(Map<Vertex, List<Vertex>> clockwiseOrder, List<Vertex> externalFace, boolean isPartial) {
        validatePlanarEmbedding(clockwiseOrder, externalFace, isPartial);
        this.clockwiseOrder = clockwiseOrder;
        this.externalFace = externalFace;
    }

    /** Constructs a new PlanarEmbedding for a connected component.  See also createPartial. */
    public PlanarEmbedding(Map<Vertex, List<Vertex>> clockwiseOrder, List<Vertex> externalFace) {
        this(clockwiseOrder, externalFace, false);
    }

    /**
     * Returns a new PlanarEmbedding that is (potentially) for a subgraph of a connected component, as opposed to an
     * entire connected component.  See also the PlanarEmbedding constructor.
     * @param clockwiseOrder A map from each vertex in the graph to the adjacent vertices, in clockwise order of the
     *     edges to those vertices.
     * @param externalFace The external face, as in the externalFace field.
     * @return The PlanarEmbedding.
     */
    public static PlanarEmbedding createPartial(Map<Vertex, List<Vertex>> clockwiseOrder, List<Vertex> externalFace) {
        return new PlanarEmbedding(clockwiseOrder, externalFace, true);
    }

    /**
     * Throws an IllegalArgumentException if the specified arguments do not suggest a valid planar embedding.
     * @param clockwiseOrder A map from each vertex in the graph to the adjacent vertices, in clockwise order of the
     *     edges to those vertices.
     * @param externalFace The external face, as in the externalFace field.
     * @param isPartial Whether the embedding is (potentially) for a subgraph of a connected component, as opposed to an
     *     entire connected component.
     */
    private static void validatePlanarEmbedding(
            Map<Vertex, List<Vertex>> clockwiseOrder, List<Vertex> externalFace, boolean isPartial) {
        // Compute nextClockwise, a map from each vertex V to a map from each adjacent vertex W to the next vertex
        // clockwise from W
        Map<Vertex, Map<Vertex, Vertex>> nextClockwise = new HashMap<Vertex, Map<Vertex, Vertex>>();
        for (Entry<Vertex, List<Vertex>> entry : clockwiseOrder.entrySet()) {
            Vertex vertex = entry.getKey();
            List<Vertex> vertexClockwiseOrder = entry.getValue();
            if (vertexClockwiseOrder.isEmpty()) {
                if (clockwiseOrder.size() != 1) {
                    throw new IllegalArgumentException("The clockwiseOrder entry for " + vertex + " is empty");
                }
                nextClockwise.put(vertex, Collections.<Vertex, Vertex>emptyMap());
            } else {
                Map<Vertex, Vertex> vertexNextClockwise = new HashMap<Vertex, Vertex>();
                Vertex prevAdjVertex = vertexClockwiseOrder.get(vertexClockwiseOrder.size() - 1);
                for (Vertex adjVertex : vertexClockwiseOrder) {
                    if (adjVertex == vertex) {
                        throw new IllegalArgumentException(
                            "The clockwiseOrder entry for " + vertex + " contains that vertex");
                    }
                    vertexNextClockwise.put(prevAdjVertex, adjVertex);
                    prevAdjVertex = adjVertex;
                }
                nextClockwise.put(vertex, vertexNextClockwise);
            }
        }

        // Verify clockwiseOrder
        if (isPartial) {
            for (Entry<Vertex, List<Vertex>> entry : clockwiseOrder.entrySet()) {
                Vertex vertex = entry.getKey();
                for (Vertex adjVertex : entry.getValue()) {
                    Map<Vertex, Vertex> vertexNextClockwise = nextClockwise.get(adjVertex);
                    if (vertexNextClockwise == null || !vertexNextClockwise.containsKey(vertex)) {
                        throw new IllegalArgumentException(
                            "The clockwiseOrder entry for " + vertex + " contains " + adjVertex +
                            ", but not vice versa");
                    }
                }
            }
        } else {
            for (Entry<Vertex, List<Vertex>> entry : clockwiseOrder.entrySet()) {
                Vertex vertex = entry.getKey();
                List<Vertex> vertexClockwiseOrder = entry.getValue();
                if (vertexClockwiseOrder.size() != vertex.edges.size() ||
                        !new HashSet<Vertex>(vertexClockwiseOrder).equals(vertex.edges)) {
                    throw new IllegalArgumentException(
                        "The clockwiseOrder entry for " + vertex +
                        " does not contain all adjacent vertices exactly once.  If part of the graph is " +
                        "intentionally omitted, create the PlanarEmbedding using PlanarEmbedding.createPartial.");
                }
                for (Vertex adjVertex : vertex.edges) {
                    if (!clockwiseOrder.containsKey(adjVertex)) {
                        throw new IllegalArgumentException(
                            "clockwiseOrder does not have an entry for " + adjVertex + ".  If part of the graph is " +
                            "intentionally omitted, create the PlanarEmbedding using PlanarEmbedding.createPartial.");
                    }
                }
            }
        }

        // Vertify the external face
        if (externalFace.isEmpty()) {
            throw new IllegalArgumentException("The external face may not be empty");
        } else if (externalFace.size() == 1) {
            if (clockwiseOrder.size() != 1 || !clockwiseOrder.containsKey(externalFace.get(0))) {
                throw new IllegalArgumentException("externalFace is not a face in the embedding");
            }
        } else {
            Vertex prevVertex = externalFace.get(externalFace.size() - 1);
            if (!clockwiseOrder.containsKey(prevVertex)) {
                throw new IllegalArgumentException(
                    "externalFace contains a vertex " + prevVertex + " that does not appear in clockwiseOrder");
            }
            for (int i = 0; i < externalFace.size(); i++) {
                Vertex vertex = externalFace.get(i);
                Vertex nextVertex;
                if (i + 1 < externalFace.size()) {
                    nextVertex = externalFace.get(i + 1);
                } else {
                    nextVertex = externalFace.get(0);
                }
                Map<Vertex, Vertex> vertexNextClockwise = nextClockwise.get(vertex);
                if (vertexNextClockwise == null) {
                    throw new IllegalArgumentException(
                        "externalFace contains a vertex " + vertex + " that does not appear in clockwiseOrder");
                }

                Vertex expectedVertex = vertexNextClockwise.get(prevVertex);
                if (expectedVertex == null) {
                    throw new IllegalArgumentException(
                        "externalFace contains an edge (" + prevVertex + ", " + vertex +
                        ") that does not appear in clockwiseOrder");
                } else if (nextVertex != expectedVertex) {
                    throw new IllegalArgumentException(
                        "externalFace is not a face in the embedding or is not specified in clockwise order");
                }

                prevVertex = vertex;
                vertex = nextVertex;
            }
        }
    }

    /**
     * Computes a depth-first search tree for the component containing the specified vertex.
     * @param root The vertex.
     * @return The vertices in the tree, in reverse topological order (i.e. with each vertex appearing before its
     *     parent).
     */
    private static List<PlanarVertex> depthFirstSearch(Vertex root) {
        // Use an iterative implementation of depth-first search
        PlanarVertex planarRoot = new PlanarVertex(root, null, 0);
        List<PlanarVertex> vertices = new ArrayList<PlanarVertex>();
        List<PlanarVertex> path = new ArrayList<PlanarVertex>();
        path.add(planarRoot);
        List<Iterator<Vertex>> pathIters = new ArrayList<Iterator<Vertex>>();
        pathIters.add(root.edges.iterator());
        Map<Vertex, PlanarVertex> vertexToPlanarVertex = new HashMap<Vertex, PlanarVertex>();
        vertexToPlanarVertex.put(root, planarRoot);
        int nextIndex = 1;
        while (!path.isEmpty()) {
            if (!pathIters.get(pathIters.size() - 1).hasNext()) {
                PlanarVertex planarVertex = path.remove(path.size() - 1);
                pathIters.remove(pathIters.size() - 1);
                vertices.add(planarVertex);

                // Compute planarVertex.lowpoint
                if (planarVertex.leastAncestor != null) {
                    planarVertex.lowpoint = planarVertex.leastAncestor;
                } else {
                    planarVertex.lowpoint = planarVertex;
                }
                for (RootVertex child = planarVertex.separatedChildrenHead; child != null; child = child.next) {
                    if (child.child.lowpoint.index < planarVertex.lowpoint.index) {
                        planarVertex.lowpoint = child.child.lowpoint;
                    }
                }
            } else {
                PlanarVertex planarStart = path.get(path.size() - 1);
                Vertex end = pathIters.get(pathIters.size() - 1).next();
                PlanarVertex planarEnd = vertexToPlanarVertex.get(end);
                if (planarEnd == null) {
                    // Tree edge

                    planarEnd = new PlanarVertex(end, planarStart, nextIndex);
                    nextIndex++;
                    vertexToPlanarVertex.put(end, planarEnd);

                    // Add a new root vertex to planarStart.separatedChildrenHead
                    RootVertex rootVertex = new RootVertex(planarStart, planarEnd);
                    rootVertex.prev = planarStart.separatedChildrenTail;
                    if (planarStart.separatedChildrenHead == null) {
                        planarStart.separatedChildrenHead = rootVertex;
                    } else {
                        planarStart.separatedChildrenTail.next = rootVertex;
                    }
                    planarStart.separatedChildrenTail = rootVertex;

                    // Create the HalfEdges connecting rootVertex and planarEnd
                    HalfEdge edge1 = HalfEdge.create(planarEnd);
                    HalfEdge edge2 = HalfEdge.create(rootVertex);
                    edge1.twinEdge = edge2;
                    edge2.twinEdge = edge1;
                    rootVertex.setLink(false, edge1);
                    rootVertex.setLink(true, edge1);
                    planarEnd.setLink(false, edge2);
                    planarEnd.setLink(true, edge2);

                    path.add(planarEnd);
                    pathIters.add(end.edges.iterator());
                } else if (planarEnd.index > planarStart.index || planarStart.parent == planarEnd) {
                    // We are visiting a back edge, but from the wrong direction
                    continue;
                } else {
                    // Back edge
                    planarEnd.backEdges.add(planarStart);
                    if (planarStart.leastAncestor == null || planarStart.leastAncestor.index > planarEnd.index) {
                        planarStart.leastAncestor = planarEnd;
                    }
                }
            }
        }

        // Using bucket sort, order the separated children lists (as in PlanarVertex.separatedChildrenHead) in ascending
        // order of lowpoint
        @SuppressWarnings("unchecked")
        Collection<RootVertex>[] rootVertexBuckets = new Collection[vertices.size()];
        for (PlanarVertex vertex : vertices) {
            for (RootVertex rootVertex = vertex.separatedChildrenHead; rootVertex != null;
                    rootVertex = rootVertex.next) {
                int index = rootVertex.child.lowpoint.index;
                if (rootVertexBuckets[index] == null) {
                    rootVertexBuckets[index] = new ArrayList<RootVertex>();
                }
                rootVertexBuckets[index].add(rootVertex);
            }
            vertex.separatedChildrenHead = null;
            vertex.separatedChildrenTail = null;
        }
        for (Collection<RootVertex> bucket : rootVertexBuckets) {
            if (bucket != null) {
                for (RootVertex rootVertex : bucket) {
                    rootVertex.prev = rootVertex.vertex.separatedChildrenTail;
                    rootVertex.next = null;
                    if (rootVertex.vertex.separatedChildrenHead == null) {
                        rootVertex.vertex.separatedChildrenHead = rootVertex;
                    } else {
                        rootVertex.vertex.separatedChildrenTail.next = rootVertex;
                    }
                    rootVertex.vertex.separatedChildrenTail = rootVertex;
                }
            }
        }
        return vertices;
    }

    /**
     * Common implementation of successorOnExternalFace(PlanarVertex, boolean) and
     * successorOnExternalFace(RootVertex, boolean).
     * @param edge The edge to the successor.
     * @param isInBlack The isInBlack argument to successorOnExternalFace(PlanarVertex, boolean) or
     *     successorOnExternalFace(RootVertex, boolean).
     * @return The successor.
     */
    private static VertexAndDir successorOnExternalFace(HalfEdge edge, boolean isInBlack) {
        boolean nextIsInBlack;
        if (edge.endLink(isInBlack).twinEdge == edge) {
            nextIsInBlack = isInBlack;
        } else {
            nextIsInBlack = !isInBlack;
        }

        if (edge.endVertex != null) {
            return new VertexAndDir(edge.endVertex, nextIsInBlack);
        } else {
            return new VertexAndDir(edge.endRootVertex, nextIsInBlack);
        }
    }

    /**
     * Returns the point on the external face of some biconnected component after the specified point.  Assumes that
     * "vertex" is on the external face of some biconnected component.
     *
     * In order make the "successor" operation reversible, we keep isInBlack the same if possible.  By "reversible", I
     * mean that if s = successor(vertex, isInBlack), then successor(s.vertex, !s.isInBlack).isInBlack != isInBlack,
     * i.e. that color information is preserved.  Likewise for successor operations involving RootVertices rather than
     * PlanarVertices.  This comes up when the black and white links are the same, i.e. the biconnected component
     * consists of one edge.  The successor operation must be reversible in order for us to compute the internal face
     * created when adding an edge, and for the computation of the external face of the final embedding to work.
     *
     * @param vertex The vertex.
     * @param isInBlack Whether vertex.link(true) is in the inward direction, opposite the dierction to move along the
     *     external face.
     * @return The successor.
     */
    private static VertexAndDir successorOnExternalFace(PlanarVertex vertex, boolean isInBlack) {
        return successorOnExternalFace(vertex.link(!isInBlack), isInBlack);
    }

    /**
     * Returns the point on the external face of the biconnected component rooted at rootVertex after the specified
     * point.
     *
     * In order make the "successor" operation reversible, we keep isInBlack the same if possible.  By "reversible", I
     * mean that if s = successor(rootVertex, isInBlack), then
     * successor(s.rootVertex, !s.isInBlack).isInBlack != isInBlack, i.e. that color information is preserved.  Likewise
     * for successor operations involving PlanarVertices rather than RootVertices.  This comes up when the black and
     * white links are the same, i.e. the biconnected component consists of one edge.  The successor operation must be
     * reversible in order for us to compute the internal face created when adding an edge, and for the computation of
     * the external face of the final embedding to work.
     *
     * @param isInBlack Whether rootVertex.link(true) is in the inward direction, opposite the dierction to move along
     *     the external face.
     * @return The successor.
     */
    private static VertexAndDir successorOnExternalFace(RootVertex rootVertex, boolean isInBlack) {
        return successorOnExternalFace(rootVertex.link(!isInBlack), isInBlack);
    }

    /**
     * Moves up the tree from "start" toward curVertex, in order to supply information relevant to calling walkDown on a
     * root vertex with rootVertex.vertex == curVertex.
     */
    private static void walkUp(PlanarVertex start, PlanarVertex curVertex) {
        start.backEdgeFlag = curVertex;
        PlanarVertex vertex1 = start;
        boolean isInBlack1 = false;
        PlanarVertex vertex2 = start;
        boolean isInBlack2 = true;
        while (vertex1 != curVertex && vertex1.visited != curVertex && vertex2.visited != curVertex) {
            vertex1.visited = curVertex;
            vertex2.visited = curVertex;
            VertexAndDir successor1 = successorOnExternalFace(vertex1, isInBlack1);
            VertexAndDir successor2 = successorOnExternalFace(vertex2, isInBlack2);

            RootVertex rootVertex;
            if (successor1.rootVertex != null) {
                rootVertex = successor1.rootVertex;
            } else if (successor2.rootVertex != null) {
                rootVertex = successor2.rootVertex;
            } else {
                rootVertex = null;
            }
            if (rootVertex == null) {
                vertex1 = successor1.vertex;
                isInBlack1 = successor1.isInBlack;
                vertex2 = successor2.vertex;
                isInBlack2 = successor2.isInBlack;
            } else {
                PlanarVertex vertex = rootVertex.vertex;
                if (vertex != curVertex &&
                        rootVertex.prevPertinent == null && vertex.pertinentRootsHead != rootVertex) {
                    // Add rootVertex to the pertinent roots list
                    if (rootVertex.child.lowpoint.index < curVertex.index) {
                        // Append rootVertex to the pertinent roots list
                        rootVertex.prevPertinent = vertex.pertinentRootsTail;
                        if (vertex.pertinentRootsHead == null) {
                            vertex.pertinentRootsHead = rootVertex;
                        } else {
                            vertex.pertinentRootsTail.nextPertinent = rootVertex;
                        }
                        vertex.pertinentRootsTail = rootVertex;
                    } else {
                        // Prepend rootVertex to the pertinent roots list
                        rootVertex.nextPertinent = vertex.pertinentRootsHead;
                        if (vertex.pertinentRootsTail == null) {
                            vertex.pertinentRootsTail = rootVertex;
                        } else {
                            vertex.pertinentRootsHead.prevPertinent = rootVertex;
                        }
                        vertex.pertinentRootsHead = rootVertex;
                    }
                }

                vertex1 = vertex;
                isInBlack1 = false;
                vertex2 = vertex;
                isInBlack2 = true;
            }
        }
    }

    /**
     * Returns whether "vertex" is pertinent relative to adding the back edges from curVertex to descendants of
     * curVertex, according to the definition of "pertinent" provided in the paper.
     */
    private static boolean isPertinent(PlanarVertex vertex, PlanarVertex curVertex) {
        return vertex.backEdgeFlag == curVertex || vertex.pertinentRootsHead != null;
    }

    /**
     * Returns whether "vertex" is externally active relative to adding the back edges from curVertex to descendants of
     * curVertex.
     */
    private static boolean isExternallyActive(PlanarVertex vertex, PlanarVertex curVertex) {
        return (vertex.leastAncestor != null && vertex.leastAncestor.index < curVertex.index) ||
            (vertex.separatedChildrenHead != null &&
                vertex.separatedChildrenHead.vertex.lowpoint.index < curVertex.index);
    }

    /**
     * Returns whether "vertex" is active relative to adding the back edges from curVertex to descendants of
     * curVertex, according to the definition of "active" provided in the paper.
     */
    private static boolean isActive(PlanarVertex vertex, PlanarVertex curVertex) {
        return isPertinent(vertex, curVertex) || isExternallyActive(vertex, curVertex);
    }

    /**
     * Returns whether "vertex" is internally active relative to adding the back edges from curVertex to descendants of
     * curVertex.
     */
    private static boolean isInternallyActive(PlanarVertex vertex, PlanarVertex curVertex) {
        return isPertinent(vertex, curVertex) && !isExternallyActive(vertex, curVertex);
    }

    /**
     * Adds the edges of the external faces of the biconnected components rooted at the RootVertices in the separated
     * children list of "vertex" (vertex.separatedChildrenHead to vertex.separatedChildrenTail) to "edges", so that the
     * edges of each external face are a sublist of "edges".  We include the external faces of any biconnected
     * components rooted at any vertices we encounter along the way, i.e. we take such components to be part of the
     * faces we are adding to "edges".  This method sets RootVertex.isFlippedRelativeToVertex and
     * PlanarVertex.isFlippedRelativeToParent as appropriate for if the biconnected components are on the outside of the
     * embedding.  It adds the external faces of the biconnected components in the order in which the components appear
     * in the separated children list.
     */
    private static void addSeparatedChildrenFace(PlanarVertex vertex, List<HalfEdge> edges) {
        // Compute the face.  The edges of the face of a separated biconnected component appear between the adjacent
        // edges of the adjacent biconnected component.
        VertexAndDir successor = new VertexAndDir(vertex, true);
        while (successor.rootVertex != vertex.separatedChildrenTail) {
            if (successor.vertex != null) {
                if (successor.vertex.separatedChildrenHead == null) {
                    // We reached the last separated child component.  Switch back to traversing the external face of
                    // the original biconnected component.
                    successor = successorOnExternalFace(successor.vertex, successor.isInBlack);
                } else {
                    // Traverse the external faces of the separated child components before continuing with the external
                    // face of the original biconnected component
                    RootVertex rootVertex = successor.vertex.separatedChildrenHead;

                    // Flip rootVertex as necessary so that the direction (clockwiseness) of rootVertex.link(false)
                    // matches that of rv.link(false) for the root vertex "rv" with rv.vertex == vertex that is an
                    // ancestor of rootVertex
                    rootVertex.isFlippedRelativeToVertex = !successor.isInBlack;
                    rootVertex.child.isFlippedRelativeToParent = !successor.isInBlack;

                    successor = successorOnExternalFace(rootVertex, successor.isInBlack);
                }
            } else {
                if (successor.rootVertex.next == null) {
                    successor = successorOnExternalFace(successor.rootVertex.vertex, successor.isInBlack);
                } else {
                    // Traverse the external face of the next separated child component before switching back to
                    // traversing the external face of the original biconnected component
                    RootVertex rootVertex = successor.rootVertex.next;

                    // Flip rootVertex as necessary so that the direction (clockwiseness) of rootVertex.link(false)
                    // matches that of rv.link(false) for the root vertex "rv" with rv.vertex == vertex that is an
                    // ancestor of rootVertex
                    rootVertex.isFlippedRelativeToVertex = !successor.isInBlack;
                    rootVertex.child.isFlippedRelativeToParent = !successor.isInBlack;

                    successor = successorOnExternalFace(rootVertex, successor.isInBlack);
                }
            }
            if (successor.vertex != null) {
                edges.add(successor.vertex.link(successor.isInBlack).twinEdge);
            } else {
                edges.add(successor.rootVertex.link(successor.isInBlack).twinEdge);
            }
        }
    }

    /**
     * Adds an edge from rootVertex.vertex to a descendant vertex "vertex".
     * @param rootVertex The root vertex.
     * @param vertex The vertex.
     * @param rootVertexIsOutBlack Whether the black link from rootVertex is an edge on the internal face that we will
     *     create when we add the edge.
     * @param vertexIsInBlack Whether the black link from "vertex" is an edge on the internal face that we will create
     *     when we add the edge.
     * @param isSynthetic Whether to add a synthetic edge, as in HalfEdge.isSynthetic.
     * @return The internal face that contains the new edge.
     */
    private static PlanarFace addEdge(
            RootVertex rootVertex, PlanarVertex vertex, boolean rootVertexIsOutBlack, boolean vertexIsInBlack,
            boolean isSynthetic) {
        // Create the edge
        HalfEdge edge1;
        HalfEdge edge2;
        if (!isSynthetic) {
            edge1 = HalfEdge.create(vertex);
            edge2 = HalfEdge.create(rootVertex);
        } else {
            edge1 = HalfEdge.createSynthetic(vertex);
            edge2 = HalfEdge.createSynthetic(rootVertex);
        }

        edge1.twinEdge = edge2;
        edge2.twinEdge = edge1;

        // Compute the internal face the new edge creates.  Note that we have to compute this edge by traveling up from
        // "vertex" rather than down from rootVertex, for reasons elaborated in the implementation of
        // mergeBiconnectedComponent.
        List<HalfEdge> edges = new ArrayList<HalfEdge>();
        edges.add(edge1);
        edges.add(vertex.link(vertexIsInBlack));
        VertexAndDir successor = successorOnExternalFace(vertex, !vertexIsInBlack);
        boolean isInBlack = successor.isInBlack;
        while (successor.rootVertex != rootVertex) {
            if (successor.vertex != null) {
                // We are "enclosing" the vertex.  Include any separated children in the face.
                addSeparatedChildrenFace(successor.vertex, edges);

                edges.add(successor.vertex.link(!isInBlack));
                successor = successorOnExternalFace(successor.vertex, isInBlack);
            } else {
                edges.add(successor.rootVertex.link(!isInBlack));
                successor = successorOnExternalFace(successor.rootVertex, isInBlack);
            }
            isInBlack = successor.isInBlack;
        }

        rootVertex.setLink(rootVertexIsOutBlack, edge1);
        vertex.setLink(vertexIsInBlack, edge2);

        return PlanarFace.createInternal(edges, rootVertex, rootVertexIsOutBlack);
    }

    /**
     * Prepares the biconnected component with the specified root vertex to be merged with one or more other biconnected
     * components, in preparation for adding an edge spanning the components.
     * @param rootVertex The root vertex.
     * @param vertexIsInBlack Whether the black link from rootVertex.vertex is an edge on the internal face that we will
     *     create when we add the new edge.
     * @param rootVertexIsOutBlack Whether the black link from rootVertex is an edge on the internal face that we will
     *     create when we add the edge.
     * @return The internal face we created for the merge process, if any.  Sometimes, we need to add a synthetic edge,
     *     as elaborated in the comments in this method's implementation, resulting in the creation of an internal face.
     */
    private static PlanarFace mergeBiconnectedComponent(
            RootVertex rootVertex, boolean vertexIsInBlack, boolean rootVertexIsOutBlack) {
        if (vertexIsInBlack == rootVertexIsOutBlack) {
            // Flip rootVertex
            rootVertex.isFlippedRelativeToVertex = true;
            rootVertex.child.isFlippedRelativeToParent = true;
        }

        PlanarFace face;
        if (rootVertex.link(false) != rootVertex.link(true)) {
            face = null;
        } else {
            // The biconnected component rooted at rootVertex consists of the single edge between rootVertex and
            // rootVertex.child.  Create a second, synthetic edge between rootVertex and rootVertex.child.  We do this
            // so that when we add the edge that merges the biconnected component with other biconnected components, we
            // will be able to compute the internal face that edge creates.  To compute the internal face, the path from
            // the end of rootVertex.link(rootVertexIsOutBlack) to the end of rootVertex.vertex.link(vertexIsInBlack)
            // must remain intact.  To preserve the external face, the path from the end of
            // rootVertex.link(!rootVertexIsOutBlack) to the end of rootVertex.vertex.link(!vertexIsInBlack) must
            // likewise remain intact.  These two paths must pass through two different points: one through rootVertex,
            // and the other through rootVertex.vertex.  That is, there must be one edge from the end of
            // rootVertex.link(rootVertexIsOutBlack) to rootVertex and a separate edge from the end of
            // rootVertex.link(!rootVertexIsOutBlack) to rootVertex.vertex.  Since these cannot be the same edge, we
            // must create a second, synthetic edge here.
            //
            // To make the implementation slightly simpler, we do not also require rootVertex.vertex.link(false) to be a
            // different edge from rootVertex.vertex.link(true).  Consequently, we can only compute the new internal
            // face by moving up the tree through rootVertex; we cannot compute it by moving down the tree.
            face = addEdge(rootVertex, rootVertex.child, !rootVertexIsOutBlack, rootVertexIsOutBlack, true);
        }

        // Change the twin edge to point to the PlanarVertex rather than the RootVertex
        HalfEdge edge = rootVertex.link(!rootVertexIsOutBlack).twinEdge;
        edge.endRootVertex = null;
        edge.endVertex = rootVertex.vertex;

        // Remove rootVertex from the separated children list
        if (rootVertex.prev != null) {
            rootVertex.prev.next = rootVertex.next;
        } else {
            rootVertex.vertex.separatedChildrenHead = rootVertex.next;
        }
        if (rootVertex.next != null) {
            rootVertex.next.prev = rootVertex.prev;
        } else {
            rootVertex.vertex.separatedChildrenTail = rootVertex.prev;
        }
        rootVertex.prev = null;
        rootVertex.next = null;

        // Remove rootVertex from the pertinent children list
        if (rootVertex.prevPertinent != null) {
            rootVertex.prevPertinent.nextPertinent = rootVertex.nextPertinent;
        } else {
            rootVertex.vertex.pertinentRootsHead = rootVertex.nextPertinent;
        }
        if (rootVertex.nextPertinent != null) {
            rootVertex.nextPertinent.prevPertinent = rootVertex.prevPertinent;
        } else {
            rootVertex.vertex.pertinentRootsTail = rootVertex.prevPertinent;
        }
        rootVertex.prevPertinent = null;
        rootVertex.nextPertinent = null;

        // Circularly shift the edges:
        //
        // \   /         \        /
        //  \ /           \      /
        //   v             \    /
        //        --->      >  <
        //   ^             /    \
        //  / \           /      \
        // /   \         /        \
        PlanarVertex vertex = rootVertex.vertex;
        edge = vertex.link(vertexIsInBlack);
        vertex.setLink(vertexIsInBlack, rootVertex.link(!rootVertexIsOutBlack));
        rootVertex.setLink(!rootVertexIsOutBlack, edge);
        return face;
    }

    /**
     * Adds back edges from curRootVertex.vertex to descendants of curRootVertex.vertex encountered along the path on
     * the external face starting with curRootVertex.link(curRootVertexIsOutBlack).  Adds the internal faces created in
     * the process to "faces".
     * @param curRootVertex The root vertex.
     * @param curRootVertexIsOutBlack Whether to initially follow curRootVertex.link(true).
     * @param faces The collection to which to add any internal faces we create.
     * @return The number of back edges we added.
     */
    private static int walkDown(
            RootVertex curRootVertex, boolean curRootVertexIsOutBlack, Collection<PlanarFace> faces) {
        List<MergeStackEntry> mergeStack = new ArrayList<MergeStackEntry>();
        VertexAndDir successor = successorOnExternalFace(curRootVertex, !curRootVertexIsOutBlack);
        PlanarVertex vertex = successor.vertex;
        boolean isInBlack = successor.isInBlack;
        PlanarVertex curVertex = curRootVertex.vertex;
        int backEdgeCount = 0;
        while (vertex != curVertex) {
            if (vertex.backEdgeFlag == curVertex) {
                // Add the back edge from curVertex to vertex
                while (!mergeStack.isEmpty()) {
                    MergeStackEntry entry = mergeStack.remove(mergeStack.size() - 1);
                    PlanarFace face = mergeBiconnectedComponent(
                        entry.rootVertex, entry.vertexIsInBlack, entry.rootVertexIsOutBlack);
                    if (face != null) {
                        faces.add(face);
                    }
                }
                faces.add(addEdge(curRootVertex, vertex, curRootVertexIsOutBlack, isInBlack, false));
                vertex.backEdgeFlag = null;
                backEdgeCount++;
            }

            if (vertex.pertinentRootsHead != null) {
                // Continue walking from the first pertinent root
                RootVertex rootVertex = vertex.pertinentRootsHead;
                VertexAndDir successor1 = successorOnExternalFace(rootVertex, isInBlack);
                VertexAndDir successor2 = successorOnExternalFace(rootVertex, !isInBlack);
                if (isInternallyActive(successor1.vertex, curVertex)) {
                    successor = successor1;
                } else if (isInternallyActive(successor2.vertex, curVertex)) {
                    successor = successor2;
                } else if (isPertinent(successor1.vertex, curVertex)) {
                    successor = successor1;
                } else {
                    successor = successor2;
                }

                // Add the root vertex to mergeStack.  In order make the traversal reversible, we keep isInBlack the
                // same if possible.  See the comments for successorOnExternalFace(PlanarVertex, boolean).
                boolean rootVertexIsOutBlack;
                if (successor == successor1) {
                    rootVertexIsOutBlack = !isInBlack;
                } else {
                    rootVertexIsOutBlack = isInBlack;
                }
                mergeStack.add(new MergeStackEntry(rootVertex, isInBlack, rootVertexIsOutBlack));

                if (successor.vertex != null) {
                    vertex = successor.vertex;
                } else {
                    vertex = successor.rootVertex.vertex;
                }
                isInBlack = successor.isInBlack;
            } else if (!isActive(vertex, curVertex)) {
                // Advance to the successor
                successor = successorOnExternalFace(vertex, isInBlack);
                if (successor.vertex != null) {
                    vertex = successor.vertex;
                } else {
                    vertex = successor.rootVertex.vertex;
                }
                isInBlack = successor.isInBlack;
            } else {
                if (curRootVertex.child.lowpoint.index < curVertex.index && mergeStack.isEmpty() &&
                        vertex.link(isInBlack).endRootVertex != curRootVertex) {
                    // Add a short circuit edge
                    faces.add(addEdge(curRootVertex, vertex, curRootVertexIsOutBlack, isInBlack, true));
                }
                break;
            }
        }
        return backEdgeCount;
    }

    /**
     * Returns a traversal of the same face traversed by "edges", but in the opposite direction.  We represent a
     * traversal by a sequence of edges, each starting at the end of the previous edge, with the start of the first edge
     * matching the end of the last edge.
     */
    private static List<HalfEdge> reverse(List<HalfEdge> edges) {
        List<HalfEdge> reversed = new ArrayList<HalfEdge>(edges.size());
        for (HalfEdge edge : edges) {
            reversed.add(edge.twinEdge);
        }
        Collections.reverse(reversed);
        return reversed;
    }

    /**
     * Returns a PlanarEmbedding for the graph, having added all back edges and having computed the internal faces.
     * @param internalFaces The internal faces.
     * @return The vertices in the graph, in reverse topological order (i.e. with each vertex appearing before its
     *     parent).
     * @return The embedding.
     */
    private static PlanarEmbedding embedding(Collection<PlanarFace> internalFaces, List<PlanarVertex> vertices) {
        // Compute the external face.  The edges of the external face of a separated biconnected component appear
        // between the adjacent edges of the adjacent biconnected component.
        List<HalfEdge> edges = new ArrayList<HalfEdge>();
        PlanarVertex treeRoot = vertices.get(vertices.size() - 1);
        addSeparatedChildrenFace(treeRoot, edges);

        Collection<PlanarFace> faces = new ArrayList<PlanarFace>(internalFaces.size() + 1);
        faces.addAll(internalFaces);
        faces.add(PlanarFace.createExternal(edges));

        // Compute PlanarVertex.isFlippedOverall
        for (int i = vertices.size() - 2; i >= 0; i--) {
            PlanarVertex vertex = vertices.get(i);
            vertex.isFlippedOverall = vertex.parent.isFlippedOverall != vertex.isFlippedRelativeToParent;
        }

        // Compute HalfEdge.nextClockwise.  We take rootVertex.link(false) to be in the clockwise direction for all
        // values rootVertex with rootVertex.vertex == treeRoot.
        for (PlanarFace face : faces) {
            if (!face.isFlippedOverall() && face.isInternal()) {
                edges = face.edges;
            } else {
                edges = reverse(face.edges);
            }
            edges.get(0).nextClockwise = edges.get(edges.size() - 1).twinEdge;
            for (int i = 0; i < edges.size() - 1; i++) {
                edges.get(i + 1).nextClockwise = edges.get(i).twinEdge;
            }
        }

        // Compute the clockwiseOrder map
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        for (PlanarVertex vertex : vertices) {
            // Select an arbitrary edge starting at "vertex"
            HalfEdge start;
            if (vertex != treeRoot) {
                start = vertex.link(true);
            } else {
                start = vertex.separatedChildrenHead.link(true);
            }

            List<Vertex> vertexClockwiseOrder = new ArrayList<Vertex>();
            if (!start.isSynthetic) {
                if (start.endVertex != null) {
                    vertexClockwiseOrder.add(start.endVertex.vertex);
                } else {
                    vertexClockwiseOrder.add(start.endRootVertex.vertex.vertex);
                }
            }
            for (HalfEdge edge = start.nextClockwise; edge != start; edge = edge.nextClockwise) {
                if (!edge.isSynthetic) {
                    if (edge.endVertex != null) {
                        vertexClockwiseOrder.add(edge.endVertex.vertex);
                    } else {
                        vertexClockwiseOrder.add(edge.endRootVertex.vertex.vertex);
                    }
                }
            }
            clockwiseOrder.put(vertex.vertex, vertexClockwiseOrder);
        }

        // Compute externalFace.  We iterated over the external face in the call to addSeparatedChildrenFace, but that
        // may have included synthetic edges, which we are able to skip over here.
        List<Vertex> externalFace = new ArrayList<Vertex>();
        for (RootVertex rootVertex = treeRoot.separatedChildrenHead; rootVertex != null; rootVertex = rootVertex.next) {
            HalfEdge externalEdge = rootVertex.link(false);
            externalFace.add(treeRoot.vertex);
            while (externalEdge.endRootVertex != rootVertex) {
                if (externalEdge.isSynthetic) {
                    // Move clockwise toward the next non-synthetic edge
                    externalEdge = externalEdge.nextClockwise;
                } else {
                    if (externalEdge.endVertex != null) {
                        externalFace.add(externalEdge.endVertex.vertex);
                    } else {
                        externalFace.add(externalEdge.endRootVertex.vertex.vertex);
                    }

                    // Move to the next edge on the face
                    externalEdge = externalEdge.twinEdge.nextClockwise;
                }
            }
        }

        return new PlanarEmbedding(clockwiseOrder, externalFace);
    }

    /**
     * Returns an arbitrary planar embedding of the connected component containing the specified vertex.  Returns null
     * if the connected component is not a planar graph.
     */
    public static PlanarEmbedding compute(Vertex start) {
        if (start.edges.isEmpty()) {
            return new PlanarEmbedding(
                Collections.singletonMap(start, Collections.<Vertex>emptyList()), Collections.singletonList(start));
        }

        List<PlanarVertex> vertices = depthFirstSearch(start);

        // All planar graphs with at least one edge have E <= 3V - 5.  Return early if this is not the case.
        int twiceEdgeCount = 0;
        for (PlanarVertex vertex : vertices) {
            twiceEdgeCount += vertex.vertex.edges.size();
        }
        if (twiceEdgeCount / 2 > 3 * vertices.size() - 5) {
            return null;
        }

        Collection<PlanarFace> faces = new ArrayList<PlanarFace>();
        for (PlanarVertex curVertex : vertices) {
            for (PlanarVertex backEdge : curVertex.backEdges) {
                walkUp(backEdge, curVertex);
            }
            List<RootVertex> children = new ArrayList<RootVertex>();
            for (RootVertex child = curVertex.separatedChildrenHead; child != null; child = child.next) {
                children.add(child);
            }
            int backEdgeCount = 0;
            for (RootVertex child : children) {
                backEdgeCount += walkDown(child, false, faces);
                backEdgeCount += walkDown(child, true, faces);
            }
            if (backEdgeCount < curVertex.backEdges.size()) {
                return null;
            }
        }
        return embedding(faces, vertices);
    }

    /** Returns the mirror image of this. */
    public PlanarEmbedding flip() {
        Map<Vertex, List<Vertex>> flippedClockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        for (Entry<Vertex, List<Vertex>> entry : clockwiseOrder.entrySet()) {
            List<Vertex> vertexClockwiseOrder = new ArrayList<Vertex>(entry.getValue());
            Collections.reverse(vertexClockwiseOrder);
            flippedClockwiseOrder.put(entry.getKey(), vertexClockwiseOrder);
        }
        List<Vertex> flippedExternalFace = new ArrayList<Vertex>(externalFace);
        Collections.reverse(flippedExternalFace);
        return PlanarEmbedding.createPartial(flippedClockwiseOrder, flippedExternalFace);
    }
}
