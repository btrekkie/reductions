package com.github.btrekkie.graph.visibility.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Test;

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.planar.PlanarEmbedding;
import com.github.btrekkie.graph.visibility.VisibilityEdge;
import com.github.btrekkie.graph.visibility.VisibilityRepresentation;
import com.github.btrekkie.graph.visibility.VisibilityVertex;

public class VisibilityRepresentationTest {
    /**
     * Returns whether any pair of rectangles in the specified collection overlaps in the "middle" of the rectangles.
     * That is, if the rectangle is degenerate (a line segment or a point), the intersection may not be limited to a
     * single point, and if not, the intersection may not be limited to a line segment or a single point.
     */
    private boolean overlaps(Collection<Rectangle> rectangles) {
        // This is implemented using a scan line algorithm.  We move from left to right, maintaining a record of the
        // intervals of y coordinates the scan line intersects.  If we ever add an interval that intersects an existing
        // interval, this returns true.

        // Sort the rectangles by minX and maxX
        List<Rectangle> rectanglesByMinX = new ArrayList<Rectangle>(rectangles);
        Collections.sort(rectanglesByMinX, new Comparator<Rectangle>() {
            @Override
            public int compare(Rectangle rectangle1, Rectangle rectangle2) {
                if (rectangle1.minX != rectangle2.minX) {
                    return rectangle1.minX - rectangle2.minX;
                } else {
                    return rectangle1.maxX - rectangle2.maxX;
                }
            }
        });
        List<Rectangle> rectanglesByMaxX = new ArrayList<Rectangle>(rectangles);
        Collections.sort(rectanglesByMaxX, new Comparator<Rectangle>() {
            @Override
            public int compare(Rectangle rectangle1, Rectangle rectangle2) {
                if (rectangle1.maxX != rectangle2.maxX) {
                    return rectangle1.maxX - rectangle2.maxX;
                } else {
                    return rectangle1.minX - rectangle2.minX;
                }
            }
        });

        // We maintain a map yIntervals from the minimum of each y interval to the corresponding maximum, and a set
        // horizontalLines of the y intervals consisting of a single y value.  Note that at any given time, we may have
        // two intervals [y1, y1] and [y1, y2], with y1 < y2.
        SortedMap<Integer, Integer> yIntervals = new TreeMap<Integer, Integer>();
        Set<Integer> horizontalLines = new HashSet<Integer>();
        int rectanglesByMinXIndex = 0;
        int rectanglesByMaxXIndex = 0;
        while (rectanglesByMinXIndex < rectanglesByMinX.size()) {
            Rectangle minXRectangle = rectanglesByMinX.get(rectanglesByMinXIndex);
            Rectangle maxXRectangle = rectanglesByMaxX.get(rectanglesByMaxXIndex);
            if (minXRectangle.minX < maxXRectangle.maxX ||
                    (minXRectangle.minX == maxXRectangle.maxX && minXRectangle.minX == minXRectangle.maxX &&
                        maxXRectangle.minX == maxXRectangle.maxX)) {
                // Start of rectangle
                Rectangle rectangle = minXRectangle;
                rectanglesByMinXIndex++;
                if (rectangle.minX == rectangle.maxX && rectangle.minY == rectangle.maxY) {
                    continue;
                }

                // Check whether the rectangle's y interval intersects yIntervals.
                SortedMap<Integer, Integer> subMap = yIntervals.headMap(rectangle.maxY);
                if (!subMap.isEmpty()) {
                    int maxY = subMap.get(subMap.lastKey());
                    if (maxY > rectangle.minY ||
                            (maxY == rectangle.minY && rectangle.minY == rectangle.maxY &&
                                horizontalLines.contains(rectangle.minY))) {
                        return true;
                    }
                }

                // Add rectangle to yIntervals
                if (rectangle.minY < rectangle.maxY) {
                    yIntervals.put(rectangle.minY, rectangle.maxY);
                } else {
                    if (!yIntervals.containsKey(rectangle.minY)) {
                        yIntervals.put(rectangle.minY, rectangle.maxY);
                    }
                    horizontalLines.add(rectangle.minY);
                }
            } else {
                // End of rectangle
                Rectangle rectangle = maxXRectangle;
                rectanglesByMaxXIndex++;
                if (rectangle.minX == rectangle.maxX && rectangle.minY == rectangle.maxY) {
                    continue;
                }

                // Remove rectangle from yIntervals
                if (rectangle.minY == rectangle.maxY) {
                    horizontalLines.remove(rectangle.minY);
                    if (yIntervals.get(rectangle.minY) == rectangle.minY) {
                        yIntervals.remove(rectangle.minY);
                    }
                } else if (horizontalLines.contains(rectangle.minY)) {
                    yIntervals.put(rectangle.minY, rectangle.minY);
                } else {
                    yIntervals.remove(rectangle.minY);
                }
            }
        }
        return false;
    }

    /**
     * Returns rectangles whose overlap implies a violation of the requirements of VisibilityRepresentation.compute with
     * respect to vertical space and margin.  See the comments for VisibilityRepresentation.compute.  It only returns
     * the rectangles for the margins on one side of the vertices, based on isLeft.  The rectangles are for the edges,
     * the vertices, and the rectangles below each vertex as described in the comments for
     * VisibilityRepresentation.compute, but limited to the side suggested by isLeft.
     * @param visibilityRepresentation The VisibilityRepresentation we are verifying.
     * @param embedding The embedding for the graph.
     * @param minVertexVerticalSpace A map from each vertex to the minimum space below it.  See the comments for
     *     VisibilityRepresentation.compute.
     * @param horizontalVertexMargin The minimum margin around each vertex.  See the comments for
     *     VisibilityRepresentation.compute.
     * @param isLeft Whether to exclude the right margin region from the resulting rectangles, as opposed to excluding
     *     the left margin region.
     * @return The rectangles.
     */
    private Collection<Rectangle> rectangles(
            VisibilityRepresentation visibilityRepresentation, PlanarEmbedding embedding,
            Map<Vertex, Integer> minVertexVerticalSpace, int horizontalVertexMargin, boolean isLeft) {
        Collection<Rectangle> rectangles = new ArrayList<Rectangle>();
        for (VisibilityVertex vertex : visibilityRepresentation.vertices.values()) {
            // Vertical space and margin rectangle.  This also covers us as far as vertices are concerned.
            int space = minVertexVerticalSpace.get(vertex.vertex);
            if (isLeft) {
                rectangles.add(
                    new Rectangle(vertex.minX - horizontalVertexMargin, vertex.y, vertex.maxX, vertex.y + space));
            } else {
                rectangles.add(
                    new Rectangle(vertex.minX, vertex.y, vertex.maxX + horizontalVertexMargin, vertex.y + space));
            }

            for (VisibilityEdge edge : vertex.edges) {
                VisibilityVertex adjVertex = edge.adjVertex(vertex);
                boolean isUp = adjVertex.y < vertex.y;
                if (!isUp) {
                    // Edge rectangle, minus the vertical space and margin rectangle
                    rectangles.add(
                        new Rectangle(edge.x, vertex.y + space, edge.x, Math.max(vertex.y + space, adjVertex.y)));
                }
            }
        }
        return rectangles;
    }

    /**
     * Asserts that the specified VisibilityRepresentation is a valid return value for the specified
     * VisibilityRepresentation.compute call.
     * @param visibilityRepresentation The VisibilityRepresentation to verify.
     * @param embedding The embedding for the graph.
     * @param minVertexWidths A map from each vertex to its minimum width in the visibility representation.  Each value
     *     should be positive.
     * @param minVertexVerticalSpace A map from each vertex to the minimum space below it.  See the comments for
     *     VisibilityRepresentation.compute.
     * @param edgeBorder The minimum distance between the edges adjacent to a vertex.  This should be positive.
     * @param horizontalVertexMargin The minimum margin around each vertex.  See the comments for
     *     VisibilityRepresentation.compute.
     * @param horizontalVertexPadding The minimum distance from an endpoint of a vertex to an adjacent edge.  This
     *     should be nonnegative.
     */
    private void checkVisibilityRepresentation(
            VisibilityRepresentation visibilityRepresentation, PlanarEmbedding embedding,
            Map<Vertex, Integer> minVertexWidths, Map<Vertex, Integer> minVertexVerticalSpace, int edgeBorder,
            int horizontalVertexMargin, int horizontalVertexPadding) {
        // Check the minimum x and y coordinates
        Map<Vertex, VisibilityVertex> visibilityVertices = visibilityRepresentation.vertices;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (VisibilityVertex visibilityVertex : visibilityVertices.values()) {
            minX = Math.min(minX, visibilityVertex.minX);
            minY = Math.min(minY, visibilityVertex.y);
        }
        assertEquals(0, minX);
        assertEquals(0, minY);

        // Special case for one vertex
        if (embedding.clockwiseOrder.size() == 1) {
            Vertex vertex = embedding.clockwiseOrder.keySet().iterator().next();
            VisibilityVertex visibilityVertex = visibilityVertices.get(vertex);
            assertTrue(visibilityVertex.maxX >= minVertexWidths.get(vertex));
            return;
        }

        // Check the vertex widths, border, padding, and clockwise ordering
        for (Entry<Vertex, List<Vertex>> entry : embedding.clockwiseOrder.entrySet()) {
            Vertex vertex = entry.getKey();
            List<Vertex> clockwiseOrder = entry.getValue();
            VisibilityVertex visibilityVertex = visibilityVertices.get(vertex);
            assertTrue(visibilityVertex.maxX - visibilityVertex.minX >= minVertexWidths.get(vertex));

            // Compute a map from each adjacent vertex to the VisibilityEdge to the vertex
            Map<Vertex, VisibilityEdge> visibilityEdges = new HashMap<Vertex, VisibilityEdge>();
            for (VisibilityEdge edge : visibilityVertex.edges) {
                visibilityEdges.put(edge.adjVertex(visibilityVertex).vertex, edge);
            }
            assertEquals(visibilityEdges.keySet(), new HashSet<Vertex>(clockwiseOrder));

            Vertex prevVertex = clockwiseOrder.get(clockwiseOrder.size() - 1);
            VisibilityEdge prevEdge = visibilityEdges.get(prevVertex);
            boolean prevIsUp = visibilityVertices.get(prevVertex).y < visibilityVertex.y;
            boolean wrapped = false;
            for (Vertex adjVertex : clockwiseOrder) {
                VisibilityVertex adjVisibilityVertex = visibilityVertices.get(adjVertex);
                boolean isUp = adjVisibilityVertex.y < visibilityVertex.y;
                VisibilityEdge edge = visibilityEdges.get(adjVertex);
                if (isUp) {
                    if (prevIsUp) {
                        if (edge.x < prevEdge.x + edgeBorder) {
                            // Either all edges adjacent to "vertex" are upward, or the VisibilityRepresentation is
                            // incorrect
                            assertFalse(wrapped);
                            wrapped = true;
                            assertTrue(edge.x >= visibilityVertex.minX + horizontalVertexPadding);
                            assertTrue(prevEdge.x <= visibilityVertex.maxX - horizontalVertexPadding);
                        }
                    } else {
                        assertTrue(edge.x >= visibilityVertex.minX + horizontalVertexPadding);
                        assertTrue(prevEdge.x >= visibilityVertex.minX + horizontalVertexPadding);
                    }
                } else if (!prevIsUp) {
                    if (edge.x + edgeBorder > prevEdge.x) {
                        // Either all edges adjacent to "vertex" are downward, or the VisibilityRepresentation is
                        // incorrect
                        assertFalse(wrapped);
                        wrapped = true;
                        assertTrue(edge.x <= visibilityVertex.maxX - horizontalVertexPadding);
                        assertTrue(prevEdge.x >= visibilityVertex.minX + horizontalVertexPadding);
                    }
                } else {
                    assertTrue(edge.x + horizontalVertexPadding <= visibilityVertex.maxX);
                    assertTrue(prevEdge.x + horizontalVertexPadding <= visibilityVertex.maxX);
                }

                prevEdge = edge;
                prevIsUp = isUp;
            }
        }

        // Check the external face.  It is sufficient to verify that one of the edges on the external face of the
        // VisibilityRepresentation drawing is an edge in embedding.externalFace.  This is because given an edge on the
        // external face and a clockwise ordering around all of the vertices, we can infer the remaining edges of the
        // external face, and we have already verified the correctness of the clockwise ordering.  The edge we verify is
        // the leftmost edge of a topmost vertex.
        for (VisibilityVertex visibilityVertex : visibilityVertices.values()) {
            if (visibilityVertex.y == 0) {
                // Find the leftmost edge
                VisibilityEdge bottomLeftEdge = visibilityVertex.edges.iterator().next();
                for (VisibilityEdge edge : visibilityVertex.edges) {
                    if (edge.x < bottomLeftEdge.x) {
                        bottomLeftEdge = edge;
                    }
                }

                // Check whether embedding.externalFace contains the edge
                Vertex externalFace1 = bottomLeftEdge.adjVertex(visibilityVertex).vertex;
                Vertex externalFace2 = visibilityVertex.vertex;
                boolean found = false;
                Vertex prevVertex = embedding.externalFace.get(embedding.externalFace.size() - 1);
                for (Vertex vertex : embedding.externalFace) {
                    if (prevVertex == externalFace1 && vertex == externalFace2) {
                        found = true;
                        break;
                    }
                    prevVertex = vertex;
                }
                assertTrue(found);
                break;
            }
        }

        // Check vertical spacing and margin
        assertFalse(
            overlaps(
                rectangles(
                    visibilityRepresentation, embedding, minVertexVerticalSpace, horizontalVertexMargin, false)));
        assertFalse(
            overlaps(
                rectangles(visibilityRepresentation, embedding, minVertexVerticalSpace, horizontalVertexMargin, true)));
    }

    /**
     * Asserts that the return value of VisibilityRepresentation.compute is correct when passed the specified arguments.
     */
    private void checkVisibilityRepresentation(
            PlanarEmbedding embedding, Map<Vertex, Integer> minVertexWidths,
            Map<Vertex, Integer> minVertexVerticalSpace, int edgeBorder, int horizontalVertexMargin,
            int horizontalVertexPadding) {
        VisibilityRepresentation visibilityRepresentation = VisibilityRepresentation.compute(
            embedding, minVertexWidths, minVertexVerticalSpace, edgeBorder,
            horizontalVertexMargin, horizontalVertexPadding);
        checkVisibilityRepresentation(
            visibilityRepresentation, embedding, minVertexWidths, minVertexVerticalSpace, edgeBorder,
            horizontalVertexMargin, horizontalVertexPadding);
    }

    /**
     * Asserts that the return value of VisibilityRepresentation.compute is correct when passed the specified embedding.
     */
    private void checkVisibilityRepresentation(PlanarEmbedding embedding) {
        VisibilityRepresentation visibilityRepresentation = VisibilityRepresentation.compute(embedding);
        Map<Vertex, Integer> minVertexWidths = new HashMap<Vertex, Integer>();
        Map<Vertex, Integer> minVertexVerticalSpace = new HashMap<Vertex, Integer>();
        for (Vertex vertex : embedding.clockwiseOrder.keySet()) {
            minVertexWidths.put(vertex, 1);
            minVertexVerticalSpace.put(vertex, 1);
        }
        checkVisibilityRepresentation(
            visibilityRepresentation, embedding, minVertexWidths, minVertexVerticalSpace, 1, 1, 0);
    }

    /** Tests VisibilityRepresentation.compute on simplistic graphs. */
    @Test
    public void testComputeSimple() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Map<Vertex, List<Vertex>> clockwiseOrder = Collections.singletonMap(vertex1, Collections.<Vertex>emptyList());
        List<Vertex> externalFace = Collections.singletonList(vertex1);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        checkVisibilityRepresentation(embedding);

        graph = new Graph();
        vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        vertex1.addEdge(vertex2);
        clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Collections.singletonList(vertex2));
        clockwiseOrder.put(vertex2, Collections.singletonList(vertex1));
        externalFace = Arrays.asList(vertex1, vertex2);
        embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        checkVisibilityRepresentation(embedding);
    }

    /** Tests VisibilityRepresentation.compute on graphs consisting of a cycle. */
    @Test
    public void testComputeCycle() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex2.addEdge(vertex3);
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex2));
        List<Vertex> externalFace = Arrays.asList(vertex1, vertex2, vertex3);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        Map<Vertex, Integer> minVertexWidths = new HashMap<Vertex, Integer>();
        minVertexWidths.put(vertex1, 1);
        minVertexWidths.put(vertex2, 4);
        minVertexWidths.put(vertex3, 1);
        Map<Vertex, Integer> minVertexVerticalSpace = new HashMap<Vertex, Integer>();
        minVertexVerticalSpace.put(vertex1, 5);
        minVertexVerticalSpace.put(vertex2, 2);
        minVertexVerticalSpace.put(vertex3, 6);
        checkVisibilityRepresentation(embedding, minVertexWidths, minVertexVerticalSpace, 1, 1, 1);

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex5);
        vertex2.addEdge(vertex3);
        vertex3.addEdge(vertex4);
        vertex4.addEdge(vertex5);
        clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex5));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex2, vertex4));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex3, vertex5));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex1, vertex4));
        externalFace = Arrays.asList(vertex1, vertex2, vertex3, vertex4, vertex5);
        embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        minVertexWidths = new HashMap<Vertex, Integer>();
        minVertexWidths.put(vertex1, 10);
        minVertexWidths.put(vertex2, 3);
        minVertexWidths.put(vertex3, 6);
        minVertexWidths.put(vertex4, 5);
        minVertexWidths.put(vertex5, 4);
        minVertexVerticalSpace = new HashMap<Vertex, Integer>();
        minVertexVerticalSpace.put(vertex1, 30);
        minVertexVerticalSpace.put(vertex2, 6);
        minVertexVerticalSpace.put(vertex3, 8);
        minVertexVerticalSpace.put(vertex4, 1);
        minVertexVerticalSpace.put(vertex5, 2);
        checkVisibilityRepresentation(embedding, minVertexWidths, minVertexVerticalSpace, 1, 1, 4);
        minVertexWidths = new HashMap<Vertex, Integer>();
        minVertexWidths.put(vertex1, 1);
        minVertexWidths.put(vertex2, 1);
        minVertexWidths.put(vertex3, 1);
        minVertexWidths.put(vertex4, 1);
        minVertexWidths.put(vertex5, 1);
        minVertexVerticalSpace = new HashMap<Vertex, Integer>();
        minVertexVerticalSpace.put(vertex1, 1);
        minVertexVerticalSpace.put(vertex2, 1);
        minVertexVerticalSpace.put(vertex3, 1);
        minVertexVerticalSpace.put(vertex4, 1);
        minVertexVerticalSpace.put(vertex5, 1);
        checkVisibilityRepresentation(embedding, minVertexWidths, minVertexVerticalSpace, 10, 1, 1);
    }

    /** Tests VisibilityRepresentation.compute on a graph consisting of a path. */
    @Test
    public void testComputePath() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex2.addEdge(vertex3);
        vertex3.addEdge(vertex4);
        vertex4.addEdge(vertex5);
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Collections.singletonList(vertex2));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex2, vertex4));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex3, vertex5));
        clockwiseOrder.put(vertex5, Collections.singletonList(vertex4));
        List<Vertex> externalFace = Arrays.asList(
            vertex1, vertex2, vertex3, vertex4, vertex5, vertex4, vertex3, vertex2);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        Map<Vertex, Integer> minVertexWidths = new HashMap<Vertex, Integer>();
        minVertexWidths.put(vertex1, 2);
        minVertexWidths.put(vertex2, 1);
        minVertexWidths.put(vertex3, 1);
        minVertexWidths.put(vertex4, 6);
        minVertexWidths.put(vertex5, 2);
        Map<Vertex, Integer> minVertexVerticalSpace = new HashMap<Vertex, Integer>();
        minVertexVerticalSpace.put(vertex1, 2);
        minVertexVerticalSpace.put(vertex2, 2);
        minVertexVerticalSpace.put(vertex3, 2);
        minVertexVerticalSpace.put(vertex4, 2);
        minVertexVerticalSpace.put(vertex5, 2);
        checkVisibilityRepresentation(embedding, minVertexWidths, minVertexVerticalSpace, 3, 2, 1);
        checkVisibilityRepresentation(embedding, minVertexWidths, minVertexVerticalSpace, 3, 3, 1);
    }

    /** Tests VisibilityRepresentation.compute on graphs consisting of a tree. */
    @Test
    public void testComputeTree() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3, vertex4));
        clockwiseOrder.put(vertex2, Collections.singletonList(vertex1));
        clockwiseOrder.put(vertex3, Collections.singletonList(vertex1));
        clockwiseOrder.put(vertex4, Collections.singletonList(vertex1));
        List<Vertex> externalFace = Arrays.asList(vertex1, vertex2, vertex1, vertex3, vertex1, vertex4);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        Map<Vertex, Integer> minVertexWidths = new HashMap<Vertex, Integer>();
        minVertexWidths.put(vertex1, 1);
        minVertexWidths.put(vertex2, 1);
        minVertexWidths.put(vertex3, 4);
        minVertexWidths.put(vertex4, 5);
        Map<Vertex, Integer> minVertexVerticalSpace = new HashMap<Vertex, Integer>();
        minVertexVerticalSpace.put(vertex1, 3);
        minVertexVerticalSpace.put(vertex2, 5);
        minVertexVerticalSpace.put(vertex3, 1);
        minVertexVerticalSpace.put(vertex4, 1);
        checkVisibilityRepresentation(embedding, minVertexWidths, minVertexVerticalSpace, 1, 1, 1);

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        vertex3 = graph.createVertex();
        vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        Vertex vertex6 = graph.createVertex();
        Vertex vertex7 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex5);
        vertex3.addEdge(vertex6);
        vertex3.addEdge(vertex7);
        clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex5, vertex4));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex7, vertex6));
        clockwiseOrder.put(vertex4, Collections.singletonList(vertex2));
        clockwiseOrder.put(vertex5, Collections.singletonList(vertex2));
        clockwiseOrder.put(vertex6, Collections.singletonList(vertex3));
        clockwiseOrder.put(vertex7, Collections.singletonList(vertex3));
        externalFace = Arrays.asList(
            vertex1, vertex3, vertex7, vertex3, vertex6, vertex3, vertex1, vertex2, vertex5, vertex2, vertex4, vertex2);
        embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        checkVisibilityRepresentation(embedding);
    }

    /**
     * Tests VisibilityRepresentation.compute on the Goldner-Harary graph
     * ( https://en.wikipedia.org/wiki/Goldner%E2%80%93Harary_graph ).
     */
    @Test
    public void testComputeGoldnerHarary() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        Vertex vertex6 = graph.createVertex();
        Vertex vertex7 = graph.createVertex();
        Vertex vertex8 = graph.createVertex();
        Vertex vertex9 = graph.createVertex();
        Vertex vertex10 = graph.createVertex();
        Vertex vertex11 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex1.addEdge(vertex5);
        vertex1.addEdge(vertex6);
        vertex1.addEdge(vertex7);
        vertex1.addEdge(vertex8);
        vertex1.addEdge(vertex11);
        vertex2.addEdge(vertex5);
        vertex2.addEdge(vertex6);
        vertex3.addEdge(vertex6);
        vertex3.addEdge(vertex7);
        vertex4.addEdge(vertex5);
        vertex4.addEdge(vertex11);
        vertex5.addEdge(vertex6);
        vertex5.addEdge(vertex9);
        vertex5.addEdge(vertex11);
        vertex6.addEdge(vertex7);
        vertex6.addEdge(vertex9);
        vertex6.addEdge(vertex10);
        vertex6.addEdge(vertex11);
        vertex7.addEdge(vertex8);
        vertex7.addEdge(vertex10);
        vertex7.addEdge(vertex11);
        vertex8.addEdge(vertex11);
        vertex9.addEdge(vertex11);
        vertex10.addEdge(vertex11);
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(
            vertex1, Arrays.asList(vertex2, vertex5, vertex4, vertex11, vertex8, vertex7, vertex3, vertex6));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex6, vertex5));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex7, vertex6));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex1, vertex5, vertex11));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex1, vertex2, vertex6, vertex9, vertex11, vertex4));
        clockwiseOrder.put(
            vertex6, Arrays.asList(vertex1, vertex3, vertex7, vertex10, vertex11, vertex9, vertex5, vertex2));
        clockwiseOrder.put(vertex7, Arrays.asList(vertex1, vertex8, vertex11, vertex10, vertex6, vertex3));
        clockwiseOrder.put(vertex8, Arrays.asList(vertex1, vertex11, vertex7));
        clockwiseOrder.put(vertex9, Arrays.asList(vertex5, vertex6, vertex11));
        clockwiseOrder.put(vertex10, Arrays.asList(vertex6, vertex7, vertex11));
        clockwiseOrder.put(
            vertex11, Arrays.asList(vertex1, vertex4, vertex5, vertex9, vertex6, vertex10, vertex7, vertex8));
        List<Vertex> externalFace = Arrays.asList(vertex1, vertex11, vertex4);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        checkVisibilityRepresentation(embedding);

        Map<Vertex, Integer> minVertexWidths = new HashMap<Vertex, Integer>();
        minVertexWidths.put(vertex1, 2);
        minVertexWidths.put(vertex2, 8);
        minVertexWidths.put(vertex3, 3);
        minVertexWidths.put(vertex4, 2);
        minVertexWidths.put(vertex5, 4);
        minVertexWidths.put(vertex6, 4);
        minVertexWidths.put(vertex7, 1);
        minVertexWidths.put(vertex8, 5);
        minVertexWidths.put(vertex9, 1);
        minVertexWidths.put(vertex10, 2);
        minVertexWidths.put(vertex11, 5);
        Map<Vertex, Integer> minVertexVerticalSpace = new HashMap<Vertex, Integer>();
        minVertexVerticalSpace.put(vertex1, 3);
        minVertexVerticalSpace.put(vertex2, 3);
        minVertexVerticalSpace.put(vertex3, 6);
        minVertexVerticalSpace.put(vertex4, 7);
        minVertexVerticalSpace.put(vertex5, 3);
        minVertexVerticalSpace.put(vertex6, 1);
        minVertexVerticalSpace.put(vertex7, 1);
        minVertexVerticalSpace.put(vertex8, 4);
        minVertexVerticalSpace.put(vertex9, 4);
        minVertexVerticalSpace.put(vertex10, 7);
        minVertexVerticalSpace.put(vertex11, 2);
        checkVisibilityRepresentation(embedding, minVertexWidths, minVertexVerticalSpace, 2, 4, 3);
    }

    /**
     * Tests VisibilityRepresentation.compute on a simple graph shown in
     * http://ls11-www.cs.uni-dortmund.de/_media/techreports/tr09-09.pdf (Zey (2009): Algorithms for Planar Graph
     * Augmentation), where it shows four drawings of one planar graph.
     */
    @Test
    public void testComputeZey() {
        Graph graph = new Graph();
        Vertex vertex0 = graph.createVertex();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        vertex0.addEdge(vertex1);
        vertex0.addEdge(vertex2);
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex4);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex4);
        vertex3.addEdge(vertex4);
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex0, Arrays.asList(vertex1, vertex2));
        clockwiseOrder.put(vertex1, Arrays.asList(vertex0, vertex4, vertex2));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex0, vertex1, vertex4, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex2, vertex4));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex1, vertex3, vertex2));
        List<Vertex> externalFace = Arrays.asList(vertex0, vertex1, vertex4, vertex3, vertex2);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        Map<Vertex, Integer> minVertexWidths = new HashMap<Vertex, Integer>();
        minVertexWidths.put(vertex0, 5);
        minVertexWidths.put(vertex1, 8);
        minVertexWidths.put(vertex2, 8);
        minVertexWidths.put(vertex3, 2);
        minVertexWidths.put(vertex4, 8);
        Map<Vertex, Integer> minVertexVerticalSpace = new HashMap<Vertex, Integer>();
        minVertexVerticalSpace.put(vertex0, 1);
        minVertexVerticalSpace.put(vertex1, 1);
        minVertexVerticalSpace.put(vertex2, 2);
        minVertexVerticalSpace.put(vertex3, 3);
        minVertexVerticalSpace.put(vertex4, 4);
        checkVisibilityRepresentation(embedding, minVertexWidths, minVertexVerticalSpace, 1, 2, 1);
    }

    /**
     * Tests VisibilityRepresentation.compute on the example in
     * http://cs.brown.edu/courses/cs252/misc/resources/lectures/pdf/notes15.pdf - lecture notes scribed by Loughlin,
     * Hoang, and Karon.
     */
    @Test
    public void testComputeLoughlinHoangKaron() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        Vertex vertex6 = graph.createVertex();
        Vertex vertex7 = graph.createVertex();
        Vertex vertex8 = graph.createVertex();
        Vertex vertex9 = graph.createVertex();
        Vertex vertex10 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex5);
        vertex1.addEdge(vertex10);
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex7);
        vertex3.addEdge(vertex5);
        vertex3.addEdge(vertex6);
        vertex4.addEdge(vertex8);
        vertex5.addEdge(vertex6);
        vertex6.addEdge(vertex9);
        vertex7.addEdge(vertex8);
        vertex7.addEdge(vertex9);
        vertex8.addEdge(vertex10);
        vertex9.addEdge(vertex10);
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex5, vertex3, vertex10));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex4, vertex7));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex5, vertex6));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex2, vertex8));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex1, vertex6, vertex3));
        clockwiseOrder.put(vertex6, Arrays.asList(vertex3, vertex5, vertex9));
        clockwiseOrder.put(vertex7, Arrays.asList(vertex2, vertex8, vertex9));
        clockwiseOrder.put(vertex8, Arrays.asList(vertex4, vertex10, vertex7));
        clockwiseOrder.put(vertex9, Arrays.asList(vertex6, vertex7, vertex10));
        clockwiseOrder.put(vertex10, Arrays.asList(vertex1, vertex9, vertex8));
        List<Vertex> externalFace = Arrays.asList(vertex1, vertex2, vertex4, vertex8, vertex10);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        checkVisibilityRepresentation(embedding);
    }
}
