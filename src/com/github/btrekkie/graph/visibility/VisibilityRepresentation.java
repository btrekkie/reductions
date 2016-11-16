package com.github.btrekkie.graph.visibility;

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

import com.github.btrekkie.graph.MultiVertex;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.dual.DualGraph;
import com.github.btrekkie.graph.planar.PlanarAugmentation;
import com.github.btrekkie.graph.planar.PlanarEmbedding;
import com.github.btrekkie.util.UnorderedPair;

/**
 * A weak visibility representation: a drawing of a planar Graph where vertices are horizontal line segments and edges
 * are vertical line segments.  An edges' endpoints lie on the vertices to which it is incident.  Other than this,
 * vertices and edges do not intersect or overlap.  The coordinate system we use for VisibilityRepresentation has y
 * coordinates increasing in the downward direction.
 */
/* This is implemented using a variant of the algorithm described in
 * http://cs.brown.edu/courses/cs252/misc/resources/lectures/pdf/notes15.pdf - lecture notes scribed by Loughlin, Hoang,
 * and Karon.  To summarize, the algorithm described therein, which takes a biconnected graph as input, starts by
 * selecting an arbitrary edge S to T on the external face.  It establishes a st-orientation - an assignment of a
 * direction to each edge so that the graph forms a directed, acyclic graph, including an edge from S to T.  It assigns
 * each vertex a number equal to the number of edges in a longest path from S to the vertex.  It computes the dual graph
 * (as in DualGraph), and orients its edges from left to right.  That is, for each edge from V to W, we include an edge
 * from the dual vertex corresponding to the face immediately counterclockwise from W relative to V to the dual vertex
 * corresponding to the face immediately clockwise from W relative to V.  However, it does not include a dual edge for
 * the edge from S to T.  The algorithm assigns each face a number equal to the number of edges in a longest path in the
 * dual graph from the dual vertex corresponding to the non-external face containing the edge from S to T to the dual
 * vertex corresponding to the face.  A vertex's y coordinate is its number, its minimum x coordinate is the number of
 * its left face, and its maximum x coordinate is a half unit less than the number of its right face.  (A vertex's "left
 * face" is the face adjacent to it that passes through the vertex using two edges both oriented in the counterclockwise
 * direction.  A vertex's "right face" is the face adjacent to it that passes through the vertex using two edges both
 * oriented in the clockwise direction.)  An edge's x coordinate is roughly equal to the x coordinates of the adjacent
 * faces.  However, there are special rules for S and T.  Their x coordinates span the interval from 0 to the number of
 * the external face, and the x coordinate of the edge connecting them is equal to the number of the external face.
 *
 * The algorithm implemented here differs from the one described in the lecture notes in the following ways:
 *
 * - When computing the longest path lengths for the vertex and face numbers, we assign weights to each of the edges.
 *   This allows us to respect various minimum distances passed in as arguments, such as a minimum distance between the
 *   edges adjacent to a vertex.
 * - The edge from S to T is on the left side of the drawing rather than the right side.  That way, the drawing respects
 *   the given planar embedding.  To accomplish this, the number for dual vertex coresponding to the non-external face
 *   containing the edge from S to T is greater than 0.
 * - For each vertex, we include an edge connecting the dual vertices corresponding to the left and right faces.  This
 *   allows us to guarantee minimum vertex widths passed in as arguments, when we use the proper weight for such edges.
 * - The precise details of computing x coordinates from face numbers are different.
 * - The y coordinate of each vertex is equal to the number of T minus the number of the vertex.  This makes it so that
 *   the y coordinate is increasing in the downward direction.
 * - The algorithm for computing an st-orientation is different.
 *
 * The x coordinate of an edge other than the one from S to T is equal to the number of the face to the left of the
 * edge.  To make room for a margin and padding around each vertex (which are defined in the comments for "compute"), we
 * use the following minimum edge weights:
 *
 * - If a face consists of multiple left edges and multiple right edges, the weights of the dual edges corresponding to
 *   the left edges are equal to the margin plus twice the padding.  The maximum x coordinate of vertices that have the
 *   face as a right face is equal to the number of the face minus the margin minus the padding.  The minimum x
 *   coordinate of vertices that have the face as a left face is equal to the number of the face minus the padding.
 * - If a face consists of multiple left edges and one right edge, the weights of the dual edges corresponding to
 *   the left edges are equal to the margin plus the padding.  The maximum x coordinate of vertices that have the
 *   face as a right face is equal to the number of the face minus the margin.  There are no vertices that have the face
 *   as a left face.
 * - If a face consists of one left edge and multiple right edges, the weights of the dual edges corresponding to the
 *   left edges are equal to the margin plus the padding.  The minimum x coordinate of vertices that have the face as a
 *   left face is equal to the number of the face minus the padding.  There are no vertices that have the face as a
 *   right face.
 */
public class VisibilityRepresentation {
    /** A map from each vertex in the graph to the VisibilityVertex in the visibility representation drawing. */
    public Map<Vertex, VisibilityVertex> vertices;

    public VisibilityRepresentation(Map<Vertex, VisibilityVertex> vertices) {
        this.vertices = vertices;
    }

    /**
     * Returns a topological ordering of an st-orientation of the graph using the specified reference edge.  An
     * st-orientation is an assignment of a direction to each edge so that the graph forms a directed, acyclic graph,
     * including an edge from "source" to "sink".  A topological ordering is an ordering in which each vertex appears
     * before all of the vertices to which it has an outgoing edge.
     */
    private static List<Vertex> stOrdering(Vertex source, Vertex sink) {
        // This is implemented using an algorithm based on depth-first search described in
        // https://en.wikipedia.org/wiki/Bipolar_orientation

        // Use an iterative implementation of depth-first search
        List<Vertex> sourceEdges = new ArrayList<Vertex>(source.edges.size());
        sourceEdges.add(sink);
        for (Vertex vertex : source.edges)  {
            if (vertex != sink) {
                sourceEdges.add(vertex);
            }
        }
        List<StVertex> stVertices = new ArrayList<StVertex>();
        Map<Vertex, StVertex> vertexToStVertex = new HashMap<Vertex, StVertex>();
        StVertex sourceStVertex = new StVertex(source, null, 0);
        stVertices.add(sourceStVertex);
        vertexToStVertex.put(source, sourceStVertex);
        List<StVertex> path = new ArrayList<StVertex>();
        List<Iterator<Vertex>> pathIters = new ArrayList<Iterator<Vertex>>();
        path.add(sourceStVertex);
        pathIters.add(sourceEdges.iterator());
        int depth = 0;
        while (!path.isEmpty()) {
            if (!pathIters.get(pathIters.size() - 1).hasNext()) {
                pathIters.remove(pathIters.size() - 1);
                StVertex vertex = path.remove(path.size() - 1);

                // Compute the lowpoint
                StVertex lowpoint = vertex;
                for (Vertex adjVertex : vertex.vertex.edges) {
                    StVertex adjStVertex = vertexToStVertex.get(adjVertex);
                    if (adjStVertex.depth > vertex.depth) {
                        if (adjStVertex.lowpoint.depth < lowpoint.depth) {
                            lowpoint = adjStVertex.lowpoint;
                        }
                    } else if (adjStVertex.depth != depth - 1 && adjStVertex.depth < lowpoint.depth) {
                        lowpoint = adjStVertex;
                    }
                }
                vertex.lowpoint = lowpoint;

                depth--;
            } else {
                StVertex stStart = path.get(path.size() - 1);
                Vertex end = pathIters.get(pathIters.size() - 1).next();
                if (!vertexToStVertex.containsKey(end)) {
                    // Tree edge
                    depth++;
                    StVertex stEnd = new StVertex(end, stStart, depth);
                    stVertices.add(stEnd);
                    vertexToStVertex.put(end, stEnd);
                    path.add(stEnd);
                    pathIters.add(end.edges.iterator());
                }
            }
        }

        StVertex sinkStVertex = vertexToStVertex.get(sink);
        sourceStVertex.isInverted = true;
        sourceStVertex.next = sinkStVertex;
        sinkStVertex.prev = sourceStVertex;
        for (StVertex vertex : stVertices.subList(2, stVertices.size())) {
            if (!vertex.lowpoint.isInverted) {
                vertex.prev = vertex.parent;
                vertex.next = vertex.parent.next;
            } else {
                vertex.next = vertex.parent;
                vertex.prev = vertex.parent.prev;
            }
            vertex.prev.next = vertex;
            vertex.next.prev = vertex;
            vertex.parent.isInverted = !vertex.lowpoint.isInverted;
        }

        List<Vertex> stOrdering = new ArrayList<Vertex>();
        for (StVertex vertex = sourceStVertex; vertex != null; vertex = vertex.next) {
            stOrdering.add(vertex.vertex);
        }
        return stOrdering;
    }

    /**
     * Returns a map from each vertex to its number.
     * @param stOrdering A topological ordering of the st-orientation of the graph, as returned by stOrdering.
     * @param minVertexVerticalSpace A map from each vertex to the minimum space below it.  See the comments for
     *     "compute".
     * @return The vertex numbers.
     */
    private static Map<Vertex, Integer> vertexNumbers(
            List<Vertex> stOrdering, Map<Vertex, Integer> minVertexVerticalSpace) {
        Map<Vertex, Integer> vertexNumbers = new HashMap<Vertex, Integer>();
        vertexNumbers.put(stOrdering.get(0), 0);
        for (Vertex vertex : stOrdering.subList(1, stOrdering.size())) {
            int space = minVertexVerticalSpace.get(vertex);
            int longestPathLength = 0;
            for (Vertex adjVertex : vertex.edges) {
                Integer predPathLength = vertexNumbers.get(adjVertex);
                if (predPathLength != null) {
                    int pathLength = predPathLength + space;
                    if (pathLength > longestPathLength) {
                        longestPathLength = pathLength;
                    }
                }
            }
            vertexNumbers.put(vertex, longestPathLength);
        }
        return vertexNumbers;
    }

    /**
     * Adds the dual vertices corresponding to the faces that are the right face of one edge to oneLeftFaces and those
     * corresponding to the faces that are the left face of one edge to oneRightFaces.
     * @param embedding The planar embedding for the graph.
     * @param dual The dual of the graph.
     * @param stIndices A map from each vertex to its index in the topological ordering of the st-orienation of the
     *     graph, as returned by stOrdering.
     * @param oneLeftFaces The collection to which to add the "one left" faces.
     * @param oneRightFaces The collection to which to add the "one right" faces.
     */
    private static void oneFaces(
            PlanarEmbedding embedding, DualGraph dual, Map<Vertex, Integer> stIndices,
            Collection<MultiVertex> oneLeftFaces, Collection<MultiVertex> oneRightFaces) {
        // Compute the number of edges adjacent to each face on each side
        Map<MultiVertex, Integer> leftCounts = new HashMap<MultiVertex, Integer>();
        Map<MultiVertex, Integer> rightCounts = new HashMap<MultiVertex, Integer>();
        for (MultiVertex vertex : dual.graph.vertices) {
            leftCounts.put(vertex, 0);
            rightCounts.put(vertex, 0);
        }
        for (Vertex vertex : embedding.clockwiseOrder.keySet()) {
            int index = stIndices.get(vertex);
            for (Vertex adjVertex : vertex.edges) {
                if (stIndices.get(adjVertex) > index) {
                    MultiVertex leftFace = dual.leftFace(vertex, adjVertex);
                    rightCounts.put(leftFace, rightCounts.get(leftFace) + 1);
                    MultiVertex rightFace = dual.rightFace(vertex, adjVertex);
                    leftCounts.put(rightFace, leftCounts.get(rightFace) + 1);
                }
            }
        }

        for (Entry<MultiVertex, Integer> entry : leftCounts.entrySet()) {
            if (entry.getValue() == 1) {
                oneLeftFaces.add(entry.getKey());
            }
        }
        for (Entry<MultiVertex, Integer> entry : rightCounts.entrySet()) {
            if (entry.getValue() == 1) {
                oneRightFaces.add(entry.getKey());
            }
        }
    }

    /**
     * Returns a map from each vertex reachable from "start" to the length of the longest path from "start" to the
     * vertex, using the specified edges rather than MultiVertex.edges.  The specified edges form a directed, acyclic
     * graph.
     * @param start The starting vertex.
     * @param edges A map from each vertex to all of the vertices to which it has an outgoing edge.
     * @param weights A map from each edge to its weight (or length).  Each edge is represented as a pair of its
     *     endpoints.
     * @return The longest path lengths.
     */
    private static Map<MultiVertex, Integer> longestPathLengths(
            MultiVertex start, Map<MultiVertex, Set<MultiVertex>> edges,
            Map<UnorderedPair<MultiVertex>, Integer> weights) {
        // Use an iterative implementation of depth-first search to compute a topological ordering.  The topological
        // ordering is the reverse of a post-order traversal.
        List<MultiVertex> topologicalOrder = new ArrayList<MultiVertex>(edges.size());
        Set<MultiVertex> visited = new HashSet<MultiVertex>();
        visited.add(start);
        List<MultiVertex> path = new ArrayList<MultiVertex>();
        path.add(start);
        List<Iterator<MultiVertex>> pathIters = new ArrayList<Iterator<MultiVertex>>();
        pathIters.add(edges.get(start).iterator());
        while (!pathIters.isEmpty()) {
            if (!pathIters.get(pathIters.size() - 1).hasNext()) {
                pathIters.remove(pathIters.size() - 1);
                MultiVertex vertex = path.remove(path.size() - 1);
                topologicalOrder.add(vertex);
            } else {
                MultiVertex vertex = pathIters.get(pathIters.size() - 1).next();
                if (visited.add(vertex)) {
                    path.add(vertex);
                    pathIters.add(edges.get(vertex).iterator());
                }
            }
        }
        Collections.reverse(topologicalOrder);

        // Compute the incoming edges: the inverse of "edges"
        Map<MultiVertex, Collection<MultiVertex>> incomingEdges = new HashMap<MultiVertex, Collection<MultiVertex>>();
        for (MultiVertex vertex : edges.keySet()) {
            incomingEdges.put(vertex, new ArrayList<MultiVertex>());
        }
        for (Entry<MultiVertex, Set<MultiVertex>> entry : edges.entrySet()) {
            MultiVertex vertex = entry.getKey();
            for (MultiVertex adjVertex : entry.getValue()) {
                incomingEdges.get(adjVertex).add(vertex);
            }
        }

        // Compute the longest path lengths
        Map<MultiVertex, Integer> longestPathLengths = new HashMap<MultiVertex, Integer>();
        longestPathLengths.put(topologicalOrder.get(0), 0);
        for (MultiVertex vertex : topologicalOrder.subList(1, topologicalOrder.size())) {
            int longestPathLength = 0;
            for (MultiVertex adjVertex : incomingEdges.get(vertex)) {
                Integer predPathLength = longestPathLengths.get(adjVertex);
                if (predPathLength != null) {
                    UnorderedPair<MultiVertex> edge = new UnorderedPair<MultiVertex>(vertex, adjVertex);
                    int pathLength = predPathLength + weights.get(edge);
                    if (pathLength > longestPathLength) {
                        longestPathLength = pathLength;
                    }
                }
            }
            longestPathLengths.put(vertex, longestPathLength);
        }
        return longestPathLengths;
    }

    /**
     * Returns a map from each dual vertex to the number of the corresponding face.
     * @param embedding The embedding for the graph.
     * @param dual The dual of the graph.
     * @param source The vertex S we are using for the st-orientation.
     * @param sink The vertex T we are using for the st-orientation.
     * @param stIndices A map from each vertex to its index in the topological ordering of the st-orienation of the
     *     graph, as returned by stOrdering.
     * @param minVertexWidths A map from each vertex to its minimum width in the visibility representation.  Each value
     *     should be positive.
     * @param edgeBorder The minimum distance between the edges adjacent to a vertex.  This should be positive.
     * @param horizontalVertexMargin The minimum margin around each vertex.  See the comments for "compute".
     * @param horizontalVertexPadding The minimum distance from an endpoint of a vertex to an adjacent edge.  This
     *     should be nonnegative.
     * @return The face numbers.
     */
    private static Map<MultiVertex, Integer> faceNumbers(
            PlanarEmbedding embedding, DualGraph dual, Vertex source, Vertex sink, Map<Vertex, Integer> stIndices,
            Map<Vertex, Integer> minVertexWidths,
            int edgeBorder, int horizontalVertexMargin, int horizontalVertexPadding) {
        Set<MultiVertex> oneLeftFaces = new HashSet<MultiVertex>();
        Set<MultiVertex> oneRightFaces = new HashSet<MultiVertex>();
        oneFaces(embedding, dual, stIndices, oneLeftFaces, oneRightFaces);

        // Compute the edges and edge weights for the vertex margin and padding
        MultiVertex zeroFace = dual.rightFace(source, sink);
        MultiVertex externalFace = dual.leftFace(source, sink);
        Map<UnorderedPair<MultiVertex>, Integer> weights = new LinkedHashMap<UnorderedPair<MultiVertex>, Integer>();
        Map<MultiVertex, Set<MultiVertex>> edges = new LinkedHashMap<MultiVertex, Set<MultiVertex>>();
        for (MultiVertex vertex : dual.graph.vertices) {
            edges.put(vertex, new LinkedHashSet<MultiVertex>());
        }
        for (Vertex vertex : embedding.clockwiseOrder.keySet()) {
            int index = stIndices.get(vertex);
            for (Vertex adjVertex : vertex.edges) {
                if (stIndices.get(adjVertex) > index && (vertex != source || adjVertex != sink)) {
                    MultiVertex start = dual.leftFace(vertex, adjVertex);
                    MultiVertex end = dual.rightFace(vertex, adjVertex);
                    edges.get(start).add(end);
                    int weight;
                    if (oneLeftFaces.contains(end) || oneRightFaces.contains(end)) {
                        weight = horizontalVertexMargin + horizontalVertexPadding;
                    } else {
                        weight = horizontalVertexMargin + 2 * horizontalVertexPadding;
                    }
                    weights.put(new UnorderedPair<MultiVertex>(start, end), weight);
                }
            }
        }

        // Include the edges and edge weights for the edge border and minimum vertex widths
        for (Entry<Vertex, List<Vertex>> entry : embedding.clockwiseOrder.entrySet()) {
            Vertex vertex = entry.getKey();
            List<Vertex> clockwiseOrder = entry.getValue();
            int index = stIndices.get(vertex);
            Vertex prevVertex = clockwiseOrder.get(clockwiseOrder.size() - 1);
            boolean prevIsUp = stIndices.get(prevVertex) > index;
            Vertex firstUpVertex = null;
            Vertex lastUpVertex = null;
            for (int i = 0; i < clockwiseOrder.size(); i++) {
                Vertex adjVertex = clockwiseOrder.get(i);
                boolean isUp = stIndices.get(adjVertex) > index;

                Vertex nextVertex;
                if (i + 1 < clockwiseOrder.size()) {
                    nextVertex = clockwiseOrder.get(i + 1);
                } else {
                    nextVertex = clockwiseOrder.get(0);
                }
                boolean nextIsUp = stIndices.get(nextVertex) > index;

                if (((isUp && nextIsUp) || (!isUp && !prevIsUp)) &&
                        (vertex != source || (adjVertex != sink && nextVertex != sink)) &&
                        (vertex != sink || (adjVertex != source && prevVertex != source))) {
                    MultiVertex start = dual.leftFace(vertex, adjVertex);
                    MultiVertex end = dual.rightFace(vertex, adjVertex);
                    UnorderedPair<MultiVertex> edge = new UnorderedPair<MultiVertex>(start, end);
                    if (edgeBorder > weights.get(edge)) {
                        weights.put(edge, edgeBorder);
                    }
                }

                if (isUp) {
                    if (!prevIsUp) {
                        firstUpVertex = adjVertex;
                    }
                    if (!nextIsUp) {
                        lastUpVertex = adjVertex;
                    }
                }
                prevVertex = adjVertex;
                prevIsUp = isUp;
            }

            // Include the edge and edge weight for the minimum vertex width
            MultiVertex start;
            MultiVertex end;
            if (vertex != source && vertex != sink) {
                start = dual.leftFace(vertex, firstUpVertex);
                end = dual.rightFace(vertex, lastUpVertex);
            } else {
                start = zeroFace;
                end = externalFace;
            }
            int weight;
            if (oneRightFaces.contains(end)) {
                weight = minVertexWidths.get(vertex) + horizontalVertexMargin - horizontalVertexPadding;
            } else {
                weight = minVertexWidths.get(vertex) + horizontalVertexMargin;
            }
            UnorderedPair<MultiVertex> edge = new UnorderedPair<MultiVertex>(start, end);
            Integer oldWeight = weights.get(edge);
            if (oldWeight == null || weight > oldWeight) {
                weights.put(edge, weight);
            }
            edges.get(start).add(end);
        }

        // Compute the face numbers.  Leave minFaceNumber space at the left for the edge from S to T.
        Map<MultiVertex, Integer> longestPathLengths = longestPathLengths(zeroFace, edges, weights);
        Map<MultiVertex, Integer> faceNumbers = new HashMap<MultiVertex, Integer>();
        int minFaceNumber = horizontalVertexPadding +
            Math.max(edgeBorder, horizontalVertexMargin + horizontalVertexPadding);
        for (Entry<MultiVertex, Integer> entry : longestPathLengths.entrySet()) {
            faceNumbers.put(entry.getKey(), entry.getValue() + minFaceNumber);
        }
        return faceNumbers;
    }

    /**
     * Returns a VisibilityRepresentation for the specified biconnected Graph.  See "compute".
     * @param embedding The embedding for the graph.
     * @param minVertexWidths A map from each vertex to its minimum width in the visibility representation.  Each value
     *     should be positive.
     * @param minVertexVerticalSpace A map from each vertex to the minimum space below it.  See the comments for
     *     "compute".
     * @param edgeBorder The minimum distance between the edges adjacent to a vertex.  This should be positive.
     * @param horizontalVertexMargin The minimum margin around each vertex.  See the comments for "compute".
     * @param horizontalVertexPadding The minimum distance from an endpoint of a vertex to an adjacent edge.  This
     *     should be nonnegative.
     * @return The VisibilityRepresentation.
     */
    private static VisibilityRepresentation computeFromBiconnectedGraph(
            PlanarEmbedding embedding, Map<Vertex, Integer> minVertexWidths,
            Map<Vertex, Integer> minVertexVerticalSpace, int edgeBorder, int horizontalVertexMargin,
            int horizontalVertexPadding) {
        // Special cases for one or two vertices
        if (embedding.clockwiseOrder.size() == 1) {
            Vertex vertex = embedding.clockwiseOrder.keySet().iterator().next();
            VisibilityVertex visibilityVertex = new VisibilityVertex(vertex, 0, 0, minVertexWidths.get(vertex));
            return new VisibilityRepresentation(Collections.singletonMap(vertex, visibilityVertex));
        } else if (embedding.clockwiseOrder.size() == 2) {
            Iterator<Vertex> iterator = embedding.clockwiseOrder.keySet().iterator();
            Vertex vertex1 = iterator.next();
            Vertex vertex2 = iterator.next();
            int width = Math.max(
                Math.max(minVertexWidths.get(vertex1), minVertexWidths.get(vertex2)), 2 * horizontalVertexPadding);
            int space = minVertexVerticalSpace.get(vertex1);
            VisibilityVertex visibilityVertex1 = new VisibilityVertex(vertex1, 0, 0, width);
            VisibilityVertex visibilityVertex2 = new VisibilityVertex(vertex2, space, 0, width);
            VisibilityEdge visibilityEdge = new VisibilityEdge(visibilityVertex1, visibilityVertex2, width / 2);
            visibilityVertex1.edges.add(visibilityEdge);
            visibilityVertex2.edges.add(visibilityEdge);
            Map<Vertex, VisibilityVertex> vertices = new HashMap<Vertex, VisibilityVertex>();
            vertices.put(vertex1, visibilityVertex1);
            vertices.put(vertex2, visibilityVertex2);
            return new VisibilityRepresentation(vertices);
        }

        // Compute the st-orientation
        Vertex source = embedding.externalFace.get(0);
        Vertex sink = embedding.externalFace.get(1);
        List<Vertex> stOrdering = stOrdering(source, sink);
        Map<Vertex, Integer> stIndices = new HashMap<Vertex, Integer>();
        int index = 0;
        for (Vertex vertex : stOrdering) {
            stIndices.put(vertex, index);
            index++;
        }

        // Compute the vertex and face numbers
        Map<Vertex, Integer> vertexNumbers = vertexNumbers(stOrdering, minVertexVerticalSpace);
        DualGraph dual = DualGraph.compute(embedding);
        Map<MultiVertex, Integer> faceNumbers = faceNumbers(
            embedding, dual, source, sink, stIndices, minVertexWidths,
            edgeBorder, horizontalVertexMargin, horizontalVertexPadding);

        // Create the VisibilityVertex objects
        MultiVertex externalFace = dual.leftFace(source, sink);
        Set<MultiVertex> oneRightFaces = new HashSet<MultiVertex>();
        oneFaces(embedding, dual, stIndices, new ArrayList<MultiVertex>(), oneRightFaces);
        int maxVertexNumber = vertexNumbers.get(sink);
        Map<Vertex, VisibilityVertex> visibilityVertices = new HashMap<Vertex, VisibilityVertex>();
        for (Entry<Vertex, List<Vertex>> entry : embedding.clockwiseOrder.entrySet()) {
            Vertex vertex = entry.getKey();
            int minX;
            int maxX;
            if (vertex == source || vertex == sink) {
                minX = 0;
                maxX = faceNumbers.get(externalFace) - horizontalVertexMargin;
            } else {
                minX = -1;
                maxX = -1;
                List<Vertex> clockwiseOrder = entry.getValue();
                index = stIndices.get(vertex);
                boolean prevIsUp = stIndices.get(clockwiseOrder.get(clockwiseOrder.size() - 1)) > index;
                for (Vertex adjVertex : clockwiseOrder) {
                    boolean isUp = stIndices.get(adjVertex) > index;
                    if (isUp && !prevIsUp) {
                        // Left face of "vertex"
                        minX = faceNumbers.get(dual.leftFace(vertex, adjVertex)) - horizontalVertexPadding;
                        if (maxX >= 0) {
                            break;
                        }
                    } else if (!isUp && prevIsUp) {
                        // Right face of "vertex"
                        MultiVertex face = dual.leftFace(vertex, adjVertex);
                        if (oneRightFaces.contains(face)) {
                            maxX = faceNumbers.get(face) - horizontalVertexMargin;
                        } else {
                            maxX = faceNumbers.get(face) - horizontalVertexMargin - horizontalVertexPadding;
                        }
                        if (minX >= 0) {
                            break;
                        }
                    }
                    prevIsUp = isUp;
                }
            }

            VisibilityVertex visibilityVertex = new VisibilityVertex(
                vertex, maxVertexNumber - vertexNumbers.get(vertex), minX, maxX);
            visibilityVertices.put(vertex, visibilityVertex);
        }

        // Create the VisibilityEdge objects
        VisibilityVertex visibilitySource = visibilityVertices.get(source);
        VisibilityVertex visibilitySink = visibilityVertices.get(sink);
        VisibilityEdge edge = new VisibilityEdge(visibilitySource, visibilitySink, horizontalVertexPadding);
        visibilitySource.edges.add(edge);
        visibilitySink.edges.add(edge);
        for (Entry<Vertex, List<Vertex>> entry : embedding.clockwiseOrder.entrySet()) {
            Vertex vertex = entry.getKey();
            VisibilityVertex visibilityVertex = visibilityVertices.get(vertex);
            index = stIndices.get(vertex);
            for (Vertex adjVertex : entry.getValue()) {
                if (stIndices.get(adjVertex) > index && (vertex != source || adjVertex != sink)) {
                    VisibilityVertex adjVisibilityVertex = visibilityVertices.get(adjVertex);
                    MultiVertex face = dual.leftFace(vertex, adjVertex);
                    edge = new VisibilityEdge(visibilityVertex, adjVisibilityVertex, faceNumbers.get(face));
                    visibilityVertex.edges.add(edge);
                    adjVisibilityVertex.edges.add(edge);
                }
            }
        }

        return new VisibilityRepresentation(visibilityVertices);
    }

    /**
     * Returns a VisibilityRepresentation for the specified Graph.  The drawing of the graph conforms to the specified
     * planar embedding.  This method attempts to produce a compact drawing, i.e. one with a small axis-aligned bounding
     * box.  The vertex with the lowest x coordinate has a minimum x coordinate equal to 0, and the vertex with the
     * lowest y coordinate has a y coordinate of 0.
     *
     * minVertexVerticalSpace and horizontalVertexMargin identify rectangles below each vertex.  If "vertex" is a
     * VisibilityVertex and "space" is its entry in minVertexVerticalSpace, then the axis-aligned rectangle with corners
     * (vertex.minX - horizontalVertexMargin, vertex.y) and (vertex.maxX + horizontalVertexMargin, vertex.y + space) may
     * only strictly overlap edges adjacent to the vertex.  It may not strictly overlap any other vertices or edges.  By
     * "strictly overlap", I mean an overlap at more than just the edges of the rectangle.
     *
     * @param embedding The embedding for the graph.
     * @param minVertexWidths A map from each vertex to its minimum width in the visibility representation.  Each value
     *     should be positive.
     * @param minVertexVerticalSpace A map from each vertex to the minimum space below it.  Each value should be
     *   positive.
     * @param edgeBorder The minimum distance between the edges adjacent to a vertex.  This should be positive.
     * @param horizontalVertexMargin The minimum margin around each vertex. This should be positive.
     * @param horizontalVertexPadding The minimum distance from an endpoint of a vertex to an adjacent edge.  This
     *     should be nonnegative.
     */
    public static VisibilityRepresentation compute(
            PlanarEmbedding embedding, Map<Vertex, Integer> minVertexWidths,
            Map<Vertex, Integer> minVertexVerticalSpace, int edgeBorder, int horizontalVertexMargin,
            int horizontalVertexPadding) {
        // Add edges to make the graph biconnected
        PlanarAugmentation augmentation = PlanarAugmentation.makeBiconnected(embedding);
        Map<Vertex, Vertex> vertexToOriginalVertex = augmentation.vertexToOriginalVertex;
        Map<Vertex, Integer> minAugmentedVertexWidths = new HashMap<Vertex, Integer>();
        Map<Vertex, Integer> minAugmentedVertexVerticalSpace = new HashMap<Vertex, Integer>();
        for (Vertex augmentedVertex : augmentation.graph.vertices) {
            Vertex vertex = vertexToOriginalVertex.get(augmentedVertex);
            minAugmentedVertexWidths.put(augmentedVertex, minVertexWidths.get(vertex));
            minAugmentedVertexVerticalSpace.put(augmentedVertex, minVertexVerticalSpace.get(vertex));
        }

        // Compute the VisibilityRepresentation
        VisibilityRepresentation visibilityRepresentation = computeFromBiconnectedGraph(
            augmentation.embedding, minAugmentedVertexWidths, minAugmentedVertexVerticalSpace, edgeBorder,
            horizontalVertexMargin, horizontalVertexPadding);

        // Translate the vertices according to vertexToOriginalVertex
        Map<VisibilityVertex, VisibilityVertex> augmentedVertexToVertex =
            new HashMap<VisibilityVertex, VisibilityVertex>();
        Map<Vertex, VisibilityVertex> vertexToVisibilityVertex = new HashMap<Vertex, VisibilityVertex>();
        for (VisibilityVertex augmentedVertex : visibilityRepresentation.vertices.values()) {
            Vertex origVertex = vertexToOriginalVertex.get(augmentedVertex.vertex);
            VisibilityVertex vertex = new VisibilityVertex(
                origVertex, augmentedVertex.y, augmentedVertex.minX, augmentedVertex.maxX);
            augmentedVertexToVertex.put(augmentedVertex, vertex);
            vertexToVisibilityVertex.put(origVertex, vertex);
        }
        Set<VisibilityEdge> visited = new HashSet<VisibilityEdge>();
        for (VisibilityVertex augmentedVertex : visibilityRepresentation.vertices.values()) {
            for (VisibilityEdge augmentedEdge : augmentedVertex.edges) {
                if (visited.add(augmentedEdge)) {
                    VisibilityVertex vertex1 = augmentedVertexToVertex.get(augmentedEdge.vertex1);
                    VisibilityVertex vertex2 = augmentedVertexToVertex.get(augmentedEdge.vertex2);
                    if (vertex1.vertex.edges.contains(vertex2.vertex)) {
                        VisibilityEdge edge = new VisibilityEdge(vertex1, vertex2, augmentedEdge.x);
                        vertex1.edges.add(edge);
                        vertex2.edges.add(edge);
                    }
                }
            }
        }
        return new VisibilityRepresentation(vertexToVisibilityVertex);
    }

    /**
     * Returns a VisibilityRepresentation for the specified Graph.  The drawing of the graph conforms to the specified
     * planar embedding.  This method attempts to produce a compact drawing, i.e. one with a small axis-aligned bounding
     * box.  The vertex with the lowest x coordinate has a minimum x coordinate equal to 0, and the vertex with the
     * lowest y coordinate has a y coordinate of 0.
     * @param embedding The embedding for the graph.
     */
    public static VisibilityRepresentation compute(PlanarEmbedding embedding) {
        Map<Vertex, Integer> minVertexWidths = new HashMap<Vertex, Integer>();
        Map<Vertex, Integer> minVertexVerticalSpace = new HashMap<Vertex, Integer>();
        for (Vertex vertex : embedding.clockwiseOrder.keySet()) {
            minVertexWidths.put(vertex, 1);
            minVertexVerticalSpace.put(vertex, 1);
        }
        return compute(embedding, minVertexWidths, minVertexVerticalSpace, 1, 1, 0);
    }

    /** Returns a string representation of the visibility representation for debugging purposes. */
    public String debugStr() {
        // Determine the maximum x and y coordinates
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (VisibilityVertex vertex : vertices.values()) {
            if (vertex.maxX > maxX) {
                maxX = vertex.maxX;
            }
            if (vertex.y > maxY) {
                maxY = vertex.y;
            }
        }

        // Determine the type of entity or entities at each position, at 2x scale
        final byte EMPTY = 0;
        final byte VERTEX = 1;
        final byte EDGE = 2;
        final byte VERTEX_AND_EDGE = 3;
        int width = maxX + 1;
        byte[] matrix = new byte[4 * width * (maxY + 1)];
        for (VisibilityVertex vertex : vertices.values()) {
            for (int x = 2 * vertex.minX; x <= 2 * vertex.maxX; x++) {
                matrix[4 * width * vertex.y + x] = VERTEX;
            }
            for (VisibilityEdge edge : vertex.edges) {
                matrix[4 * vertex.y * width + 2 * edge.x] = VERTEX_AND_EDGE;
                VisibilityVertex adjVertex = edge.adjVertex(vertex);
                for (int y = 2 * Math.min(vertex.y, adjVertex.y) + 1; y < 2 * Math.max(vertex.y, adjVertex.y); y++) {
                    matrix[2 * (width * y + edge.x)] = EDGE;
                }
            }
        }

        // Build the string
        StringBuilder builder = new StringBuilder((2 * maxX + 2) * (2 * maxY + 1));
        for (int y = 0; y <= 2 * maxY; y++) {
            for (int x = 0; x <= 2 * maxX; x++) {
                switch (matrix[2 * width * y + x]) {
                    case EMPTY:
                        builder.append(' ');
                        break;
                    case VERTEX:
                        builder.append('-');
                        break;
                    case EDGE:
                        builder.append('|');
                        break;
                    case VERTEX_AND_EDGE:
                        builder.append('+');
                        break;
                }
            }
            builder.append('\n');
        }
        return builder.toString();
    }
}
