package com.github.btrekkie.graph.planar.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.planar.PlanarEmbedding;

public class PlanarEmbeddingTest {
    /**
     * Returns whether "expected" is the same as "actual", after reversing it if isReversed is true and then optionally
     * taking a sublist of elements from the beginning and moving them to the end.  At present, this method assumes that
     * "expected" is empty or contains at least one Vertex that does not appear in the list multiple times.
     */
    private static boolean isCyclicShift(List<Vertex> actual, List<Vertex> expected, boolean isReversed) {
        if (actual.size() != expected.size()) {
            return false;
        } else if (expected.isEmpty()) {
            return true;
        }

        // Identify a non-repeated vertex
        Set<Vertex> nonRepeatedVertices = new HashSet<Vertex>();
        Set<Vertex> repeatedVertices = new HashSet<Vertex>();
        for (Vertex vertex : expected) {
            if (!repeatedVertices.contains(vertex) && !nonRepeatedVertices.add(vertex)) {
                nonRepeatedVertices.remove(vertex);
                repeatedVertices.add(vertex);
            }
        }
        if (nonRepeatedVertices.isEmpty()) {
            throw new RuntimeException("isCyclicShift requires at least one vertex in \"expected\" to be unique");
        }
        Vertex nonRepeatedVertex = nonRepeatedVertices.iterator().next();

        // Optionally reverse "expected"
        List<Vertex> orderedVertices = new ArrayList<Vertex>(expected);
        if (isReversed) {
            Collections.reverse(orderedVertices);
        }

        // Rotate orderedVertices so that nonRepeatedVertex is in the same position as in "actual"
        int expectedIndex = orderedVertices.indexOf(nonRepeatedVertex);
        int actualIndex = actual.indexOf(nonRepeatedVertex);
        if (actualIndex < 0) {
            return false;
        }
        List<Vertex> shiftedVertices = new ArrayList<Vertex>(expected.size());
        if (expectedIndex < actualIndex) {
            shiftedVertices.addAll(
                orderedVertices.subList(expected.size() - actualIndex + expectedIndex, expected.size()));
            shiftedVertices.addAll(orderedVertices.subList(0, expected.size() - actualIndex + expectedIndex));
        } else {
            shiftedVertices.addAll(orderedVertices.subList(expectedIndex - actualIndex, expected.size()));
            shiftedVertices.addAll(orderedVertices.subList(0, expectedIndex - actualIndex));
        }

        return shiftedVertices.equals(actual);
    }

    /**
     * Returns whether "expected" is the same as "actual", after optionally taking a sublist of elements from the
     * beginning and moving them to the end.  At present, this method assumes that "expected" is empty or contains at
     * least one Vertex that does not appear in the list multiple times.
     */
    public static boolean isCyclicShift(List<Vertex> actual, List<Vertex> expected) {
        return isCyclicShift(actual, expected, false);
    }

    /**
     * Returns whether "embedding" matches the specified planar embedding or its reflection.  Returns false if
     * "embedding" is null.
     * @param embedding The planar embedding.
     * @param clockwiseOrder The clockwise ordering of vertices with which to compare the planar embedding.
     * @param externalFace The external face with which to compare the planar embedding.  This may be null, in which
     *     case we do not check the external face.
     * @return Whether the embedding matches.
     */
    private static boolean areEquivalent(
            PlanarEmbedding embedding, Map<Vertex, List<Vertex>> clockwiseOrder, List<Vertex> externalFace) {
        if (embedding == null || !clockwiseOrder.keySet().equals(embedding.clockwiseOrder.keySet())) {
            return false;
        }

        // Determine whether whether "embedding" would have to be the reflection of the specified embedding
        boolean isReversed;
        if (externalFace != null) {
            if (isCyclicShift(embedding.externalFace, externalFace, false)) {
                isReversed = false;
            } else if (isCyclicShift(embedding.externalFace, externalFace, true)) {
                isReversed = true;
            } else {
                return false;
            }
        } else {
            isReversed = false;
            for (Entry<Vertex, List<Vertex>> entry : clockwiseOrder.entrySet()) {
                if (entry.getValue().size() > 2) {
                    List<Vertex> actual = embedding.clockwiseOrder.get(entry.getKey());
                    if (isCyclicShift(actual, entry.getValue(), false)) {
                        isReversed = false;
                        break;
                    } else if (isCyclicShift(actual, entry.getValue(), true)) {
                        isReversed = true;
                        break;
                    } else {
                        return false;
                    }
                }
            }
        }

        // Check clockwiseOrder
        for (Entry<Vertex, List<Vertex>> entry : clockwiseOrder.entrySet()) {
            List<Vertex> actual = embedding.clockwiseOrder.get(entry.getKey());
            if (!isCyclicShift(actual, entry.getValue(), isReversed)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether "embedding" matches the specified planar embedding or its reflection.  Returns false if
     * "embedding" is null.
     * @param embedding The planar embedding.
     * @param clockwiseOrder The clockwise ordering of vertices with which to compare the planar embedding.
     * @return Whether the embedding matches.
     */
    private static boolean areEquivalent(PlanarEmbedding embedding, Map<Vertex, List<Vertex>> clockwiseOrder) {
        return areEquivalent(embedding, clockwiseOrder, null);
    }

    /** Tests PlanarEmbedding.compute. */
    @Test
    public void testCompute() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        Vertex vertex6 = graph.createVertex();
        Vertex vertex7 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex2.addEdge(vertex3);
        vertex4.addEdge(vertex5);
        vertex6.addEdge(vertex7);
        Map<Vertex, List<Vertex>> clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3, vertex4));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex2));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex1, vertex5));
        clockwiseOrder.put(vertex5, Collections.singletonList(vertex4));
        assertTrue(areEquivalent(PlanarEmbedding.compute(vertex1), clockwiseOrder));

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        vertex3 = graph.createVertex();
        vertex4 = graph.createVertex();
        vertex5 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex4);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex5);

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        vertex3 = graph.createVertex();
        vertex4 = graph.createVertex();
        vertex5 = graph.createVertex();
        vertex6 = graph.createVertex();
        vertex7 = graph.createVertex();
        Vertex vertex8 = graph.createVertex();
        Vertex vertex9 = graph.createVertex();
        Vertex vertex10 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex6);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex10);
        vertex3.addEdge(vertex4);
        vertex4.addEdge(vertex5);
        vertex5.addEdge(vertex6);
        vertex5.addEdge(vertex7);
        vertex7.addEdge(vertex8);
        vertex7.addEdge(vertex9);
        vertex6.addEdge(vertex10);
        assertNotNull(PlanarEmbedding.compute(vertex1));
    }

    /** Tests PlanarEmbedding.compute on graphs consisting of a cycle. */
    @Test
    public void testComputeCycle() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex2.addEdge(vertex3);
        Map<Vertex, List<Vertex>> clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex2));
        List<Vertex> externalFace = Arrays.asList(vertex1, vertex2, vertex3);
        assertTrue(areEquivalent(PlanarEmbedding.compute(vertex1), clockwiseOrder, externalFace));

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
        clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex5));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex2, vertex4));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex3, vertex5));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex4, vertex1));
        externalFace = Arrays.asList(vertex1, vertex2, vertex3, vertex4, vertex5);
        assertTrue(areEquivalent(PlanarEmbedding.compute(vertex1), clockwiseOrder, externalFace));
    }

    /** Tests PlanarEmbedding.compute on graphs similar to complete graphs. */
    @Test
    public void testComputeCompleteLike() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Map<Vertex, List<Vertex>> clockwiseOrder = Collections.singletonMap(vertex1, Collections.<Vertex>emptyList());
        List<Vertex> externalFace = Collections.singletonList(vertex1);
        assertTrue(areEquivalent(PlanarEmbedding.compute(vertex1), clockwiseOrder, externalFace));

        graph = new Graph();
        vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        vertex1.addEdge(vertex2);
        clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Collections.singletonList(vertex2));
        clockwiseOrder.put(vertex2, Collections.singletonList(vertex1));
        externalFace = Arrays.asList(vertex1, vertex2);
        assertTrue(areEquivalent(PlanarEmbedding.compute(vertex1), clockwiseOrder, externalFace));

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex4);
        vertex3.addEdge(vertex4);
        clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3, vertex4));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex4, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex2, vertex4));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex1, vertex3, vertex2));
        assertTrue(areEquivalent(PlanarEmbedding.compute(vertex1), clockwiseOrder));

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        vertex3 = graph.createVertex();
        vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex1.addEdge(vertex5);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex5);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex5);
        vertex4.addEdge(vertex5);
        assertNull(PlanarEmbedding.compute(vertex1));

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        vertex3 = graph.createVertex();
        vertex4 = graph.createVertex();
        vertex5 = graph.createVertex();
        Vertex vertex6 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex2.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex1.addEdge(vertex5);
        vertex1.addEdge(vertex6);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex5);
        vertex3.addEdge(vertex6);
        vertex4.addEdge(vertex5);
        vertex4.addEdge(vertex6);
        vertex5.addEdge(vertex6);
        assertNull(PlanarEmbedding.compute(vertex1));

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        vertex3 = graph.createVertex();
        vertex4 = graph.createVertex();
        vertex5 = graph.createVertex();
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex1.addEdge(vertex5);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex5);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex5);
        vertex4.addEdge(vertex5);
        clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex3, vertex4, vertex5));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex3, vertex5, vertex4));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex5, vertex2, vertex4));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex1, vertex3, vertex2, vertex5));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex1, vertex4, vertex2, vertex3));
        assertTrue(areEquivalent(PlanarEmbedding.compute(vertex1), clockwiseOrder));
    }

    /**
     * Tests PlanarEmbedding.compute on graphs similar to K33, the graph with vertices numbered 1 to 6 and edges from
     * each even-numbered vertex to each odd-numbered vertex.
     */
    @Test
    public void testComputeK33Like() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        Vertex vertex6 = graph.createVertex();
        vertex1.addEdge(vertex4);
        vertex1.addEdge(vertex5);
        vertex1.addEdge(vertex6);
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex5);
        vertex2.addEdge(vertex6);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex5);
        vertex3.addEdge(vertex6);
        assertNull(PlanarEmbedding.compute(vertex1));

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        vertex3 = graph.createVertex();
        vertex4 = graph.createVertex();
        vertex5 = graph.createVertex();
        vertex6 = graph.createVertex();
        Vertex vertex7 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex2.addEdge(vertex3);
        vertex1.addEdge(vertex5);
        vertex1.addEdge(vertex6);
        vertex1.addEdge(vertex7);
        vertex3.addEdge(vertex5);
        vertex3.addEdge(vertex6);
        vertex3.addEdge(vertex7);
        vertex4.addEdge(vertex5);
        vertex4.addEdge(vertex6);
        vertex4.addEdge(vertex7);
        assertNull(PlanarEmbedding.compute(vertex1));

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        vertex3 = graph.createVertex();
        vertex4 = graph.createVertex();
        vertex5 = graph.createVertex();
        vertex6 = graph.createVertex();
        vertex1.addEdge(vertex5);
        vertex1.addEdge(vertex6);
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex5);
        vertex2.addEdge(vertex6);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex5);
        vertex3.addEdge(vertex6);
        Map<Vertex, List<Vertex>> clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex5, vertex6));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex4, vertex5, vertex6));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex4, vertex6, vertex5));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex2, vertex3));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex1, vertex2, vertex3));
        clockwiseOrder.put(vertex6, Arrays.asList(vertex1, vertex3, vertex2));
        assertTrue(areEquivalent(PlanarEmbedding.compute(vertex1), clockwiseOrder));
    }

    /** Tests PlanarEmbedding.compute on tree graphs. */
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
        Map<Vertex, List<Vertex>> clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3, vertex4));
        clockwiseOrder.put(vertex2, Collections.singletonList(vertex1));
        clockwiseOrder.put(vertex3, Collections.singletonList(vertex1));
        clockwiseOrder.put(vertex4, Collections.singletonList(vertex1));
        List<Vertex> externalFace = Arrays.asList(vertex1, vertex2, vertex1, vertex3, vertex1, vertex4);
        assertTrue(areEquivalent(PlanarEmbedding.compute(vertex1), clockwiseOrder, externalFace));

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
        assertNotNull(PlanarEmbedding.compute(vertex1));
    }

    /** Tests PlanarEmbedding.compute on the Petersen graph ( https://en.wikipedia.org/wiki/Petersen_graph ). */
    @Test
    public void testComputePetersen() {
        Graph graph = new Graph();
        Vertex vertex0 = graph.createVertex();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        Vertex vertex6 = graph.createVertex();
        Vertex vertex7 = graph.createVertex();
        Vertex vertex8 = graph.createVertex();
        Vertex vertex9 = graph.createVertex();
        vertex0.addEdge(vertex1);
        vertex0.addEdge(vertex4);
        vertex0.addEdge(vertex5);
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex6);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex7);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex8);
        vertex4.addEdge(vertex9);
        vertex5.addEdge(vertex7);
        vertex5.addEdge(vertex8);
        vertex6.addEdge(vertex8);
        vertex6.addEdge(vertex9);
        vertex7.addEdge(vertex9);
        assertNull(PlanarEmbedding.compute(vertex0));
    }

    /**
     * Tests PlanarEmbedding.compute on graphs similar to the Goldner-Harary graph
     * ( https://en.wikipedia.org/wiki/Goldner%E2%80%93Harary_graph ).
     */
    @Test
    public void testComputeGoldnerHararyLike() {
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
        Map<Vertex, List<Vertex>> clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
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
        assertTrue(areEquivalent(PlanarEmbedding.compute(vertex1), clockwiseOrder));

        vertex1.addEdge(vertex9);
        assertNull(PlanarEmbedding.compute(vertex1));
    }

    /** Tests PlanarEmbedding.compute on the graph for a dodecahedron. */
    @Test
    public void testComputeDodecahedron() {
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
        Vertex vertex12 = graph.createVertex();
        Vertex vertex13 = graph.createVertex();
        Vertex vertex14 = graph.createVertex();
        Vertex vertex15 = graph.createVertex();
        Vertex vertex16 = graph.createVertex();
        Vertex vertex17 = graph.createVertex();
        Vertex vertex18 = graph.createVertex();
        Vertex vertex19 = graph.createVertex();
        Vertex vertex20 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex5);
        vertex1.addEdge(vertex6);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex8);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex10);
        vertex4.addEdge(vertex5);
        vertex4.addEdge(vertex12);
        vertex5.addEdge(vertex14);
        vertex6.addEdge(vertex7);
        vertex6.addEdge(vertex15);
        vertex7.addEdge(vertex8);
        vertex7.addEdge(vertex16);
        vertex8.addEdge(vertex9);
        vertex9.addEdge(vertex10);
        vertex9.addEdge(vertex17);
        vertex10.addEdge(vertex11);
        vertex11.addEdge(vertex12);
        vertex11.addEdge(vertex18);
        vertex12.addEdge(vertex13);
        vertex13.addEdge(vertex14);
        vertex13.addEdge(vertex19);
        vertex14.addEdge(vertex15);
        vertex15.addEdge(vertex20);
        vertex16.addEdge(vertex17);
        vertex16.addEdge(vertex20);
        vertex17.addEdge(vertex18);
        vertex18.addEdge(vertex19);
        vertex19.addEdge(vertex20);
        Map<Vertex, List<Vertex>> clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex6, vertex5));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex3, vertex8));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex2, vertex4, vertex10));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex3, vertex5, vertex12));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex1, vertex14, vertex4));
        clockwiseOrder.put(vertex6, Arrays.asList(vertex1, vertex7, vertex15));
        clockwiseOrder.put(vertex7, Arrays.asList(vertex6, vertex8, vertex16));
        clockwiseOrder.put(vertex8, Arrays.asList(vertex2, vertex9, vertex7));
        clockwiseOrder.put(vertex9, Arrays.asList(vertex8, vertex10, vertex17));
        clockwiseOrder.put(vertex10, Arrays.asList(vertex3, vertex11, vertex9));
        clockwiseOrder.put(vertex11, Arrays.asList(vertex10, vertex12, vertex18));
        clockwiseOrder.put(vertex12, Arrays.asList(vertex4, vertex13, vertex11));
        clockwiseOrder.put(vertex13, Arrays.asList(vertex12, vertex14, vertex19));
        clockwiseOrder.put(vertex14, Arrays.asList(vertex5, vertex15, vertex13));
        clockwiseOrder.put(vertex15, Arrays.asList(vertex6, vertex20, vertex14));
        clockwiseOrder.put(vertex16, Arrays.asList(vertex7, vertex17, vertex20));
        clockwiseOrder.put(vertex17, Arrays.asList(vertex9, vertex18, vertex16));
        clockwiseOrder.put(vertex18, Arrays.asList(vertex11, vertex19, vertex17));
        clockwiseOrder.put(vertex19, Arrays.asList(vertex13, vertex20, vertex18));
        clockwiseOrder.put(vertex20, Arrays.asList(vertex15, vertex16, vertex19));
        assertTrue(areEquivalent(PlanarEmbedding.compute(vertex1), clockwiseOrder));
    }
}
