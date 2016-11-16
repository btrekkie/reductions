package com.github.btrekkie.graph.planar.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.bc.BlockNode;
import com.github.btrekkie.graph.planar.PlanarAugmentation;
import com.github.btrekkie.graph.planar.PlanarEmbedding;
import com.github.btrekkie.util.UnorderedPair;

public class PlanarAugmentationTest {
    /** Asserts that we cannot find anything wrong with the results of PlanarAugmentation.makeBiconnected(embedding). */
    private static void checkMakeBiconnected(PlanarEmbedding embedding) {
        PlanarAugmentation augmentation = PlanarAugmentation.makeBiconnected(embedding);
        assertEquals(augmentation.graph.vertices.size(), embedding.clockwiseOrder.size());
        assertTrue(BlockNode.compute(augmentation.graph.vertices.iterator().next()).children.isEmpty());
        boolean wasBiconnected = BlockNode.compute(
            embedding.clockwiseOrder.keySet().iterator().next()).children.isEmpty();

        // Assert that the vertices of the augmented graph are a superset of those in the input graph
        Map<Vertex, Vertex> vertexToOriginalVertex = augmentation.vertexToOriginalVertex;
        Set<UnorderedPair<Vertex>> edges = new HashSet<UnorderedPair<Vertex>>();
        for (Vertex vertex : augmentation.graph.vertices) {
            Vertex origVertex = vertexToOriginalVertex.get(vertex);
            for (Vertex adjVertex : vertex.edges) {
                edges.add(new UnorderedPair<Vertex>(origVertex, vertexToOriginalVertex.get(adjVertex)));
            }
        }
        int doubleEdgeCount = 0;
        for (Vertex vertex : embedding.clockwiseOrder.keySet()) {
            for (Vertex adjVertex : vertex.edges) {
                assertTrue(edges.contains(new UnorderedPair<Vertex>(vertex, adjVertex)));
                doubleEdgeCount++;
            }
        }
        if (wasBiconnected) {
            assertEquals(doubleEdgeCount / 2, edges.size());
        }

        // Assert that the clockwise ordering of the augmented graph is compatible with that of the input graph
        assertEquals(augmentation.embedding.clockwiseOrder.size(), embedding.clockwiseOrder.size());
        for (Entry<Vertex, List<Vertex>> entry : augmentation.embedding.clockwiseOrder.entrySet()) {
            Vertex vertex = vertexToOriginalVertex.get(entry.getKey());
            List<Vertex> clockwiseOrder = entry.getValue();
            List<Vertex> origClockwiseOrder = new ArrayList<Vertex>(clockwiseOrder.size());
            for (Vertex orderVertex : clockwiseOrder) {
                origClockwiseOrder.add(vertexToOriginalVertex.get(orderVertex));
            }
            List<Vertex> expectedClockwiseOrder = embedding.clockwiseOrder.get(vertex);
            assertNotNull(expectedClockwiseOrder);
            if (!wasBiconnected) {
                assertEquals(new HashSet<Vertex>(origClockwiseOrder).size(), origClockwiseOrder.size());
                origClockwiseOrder.retainAll(new HashSet<Vertex>(expectedClockwiseOrder));
            }
            assertTrue(PlanarEmbeddingTest.isCyclicShift(origClockwiseOrder, expectedClockwiseOrder));
        }

        // Check the external face of the augmented graph
        List<Vertex> origExternalFace = new ArrayList<Vertex>(augmentation.embedding.externalFace.size());
        for (Vertex vertex : augmentation.embedding.externalFace) {
            origExternalFace.add(vertexToOriginalVertex.get(vertex));
        }
        if (wasBiconnected || embedding.externalFace.size() <= 2) {
            // The external faces should exactly match
            assertTrue(PlanarEmbeddingTest.isCyclicShift(origExternalFace, embedding.externalFace));
        } else {
            // The external face of the augmented graph must have at least three vertices in common with that of the
            // input graph
            Set<Vertex> overlap = new HashSet<Vertex>(origExternalFace);
            overlap.retainAll(new HashSet<Vertex>(embedding.externalFace));
            assertTrue(overlap.size() >= 3);

            // Determine the elements of embedding.externalFace that appear only once and are in "overlap"
            Set<Vertex> singleOverlap = new HashSet<Vertex>();
            Set<Vertex> multipleOverlap = new HashSet<Vertex>();
            for (Vertex vertex : embedding.externalFace) {
                if (overlap.contains(vertex) && !multipleOverlap.contains(vertex) && !singleOverlap.add(vertex)) {
                    singleOverlap.remove(vertex);
                    multipleOverlap.add(vertex);
                }
            }

            // Make sure the ordering of the vertices in singleOverlap is the same in
            // augmentation.embedding.externalFace and embedding.externalFace
            List<Vertex> origExternalFaceOverlap = new ArrayList<Vertex>(origExternalFace);
            origExternalFaceOverlap.retainAll(singleOverlap);
            List<Vertex> expectedExternalFaceOverlap = new ArrayList<Vertex>(embedding.externalFace);
            expectedExternalFaceOverlap.retainAll(singleOverlap);
            assertTrue(PlanarEmbeddingTest.isCyclicShift(origExternalFaceOverlap, expectedExternalFaceOverlap));
        }
    }

    /** Tests PlanarAugmentation.makeBiconnected on simplistic graphs. */
    @Test
    public void testMakeBiconnectedSimple() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Map<Vertex, List<Vertex>> clockwiseOrder = Collections.singletonMap(vertex1, Collections.<Vertex>emptyList());
        List<Vertex> externalFace = Collections.singletonList(vertex1);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        checkMakeBiconnected(embedding);

        graph = new Graph();
        vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        vertex1.addEdge(vertex2);
        clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Collections.singletonList(vertex2));
        clockwiseOrder.put(vertex2, Collections.singletonList(vertex1));
        externalFace = Arrays.asList(vertex1, vertex2);
        embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        checkMakeBiconnected(embedding);
    }

    /** Tests PlanarAugmentation.makeBiconnected on a graph consisting of a cycle. */
    @Test
    public void testMakeBiconnectedCycle() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex5);
        vertex2.addEdge(vertex3);
        vertex3.addEdge(vertex4);
        vertex4.addEdge(vertex5);
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex5));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex2, vertex4));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex3, vertex5));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex1, vertex4));
        List<Vertex> externalFace = Arrays.asList(vertex1, vertex2, vertex3, vertex4, vertex5);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        checkMakeBiconnected(embedding);
    }

    /** Tests PlanarAugmentation.makeBiconnected on a graph consisting of a path. */
    @Test
    public void testMakeBiconnectedPath() {
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
        checkMakeBiconnected(embedding);
    }

    /** Tests PlanarAugmentation.makeBiconnected on graphs consisting of a tree. */
    @Test
    public void testMakeBiconnectedTree() {
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
        checkMakeBiconnected(embedding);

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
        checkMakeBiconnected(embedding);
    }

    /**
     * Tests PlanarAugmentation.makeBiconnected on the Goldner-Harary graph
     * ( https://en.wikipedia.org/wiki/Goldner%E2%80%93Harary_graph ).
     */
    @Test
    public void testMakeBiconnectedGoldnerHarary() {
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
        checkMakeBiconnected(embedding);
    }

    /**
     * Tests PlanarAugmentation.makeBiconnected on a graph consisting of triangles meeting at cut vertices, sometimes
     * with a triangle inside another.
     */
    @Test
    public void testNestedTriangles() {
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
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex2.addEdge(vertex3);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex5);
        vertex3.addEdge(vertex6);
        vertex3.addEdge(vertex7);
        vertex3.addEdge(vertex8);
        vertex3.addEdge(vertex9);
        vertex4.addEdge(vertex5);
        vertex6.addEdge(vertex7);
        vertex8.addEdge(vertex9);
        vertex8.addEdge(vertex10);
        vertex8.addEdge(vertex11);
        vertex8.addEdge(vertex12);
        vertex8.addEdge(vertex13);
        vertex10.addEdge(vertex11);
        vertex12.addEdge(vertex13);
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex3));
        clockwiseOrder.put(
            vertex3, Arrays.asList(vertex1, vertex2, vertex9, vertex7, vertex5, vertex4, vertex6, vertex8));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex3, vertex5));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex3, vertex4));
        clockwiseOrder.put(vertex6, Arrays.asList(vertex3, vertex7));
        clockwiseOrder.put(vertex7, Arrays.asList(vertex3, vertex6));
        clockwiseOrder.put(vertex8, Arrays.asList(vertex3, vertex9, vertex13, vertex11, vertex10, vertex12));
        clockwiseOrder.put(vertex9, Arrays.asList(vertex3, vertex8));
        clockwiseOrder.put(vertex10, Arrays.asList(vertex8, vertex11));
        clockwiseOrder.put(vertex11, Arrays.asList(vertex8, vertex10));
        clockwiseOrder.put(vertex12, Arrays.asList(vertex8, vertex13));
        clockwiseOrder.put(vertex13, Arrays.asList(vertex8, vertex12));
        List<Vertex> externalFace = Arrays.asList(
            vertex1, vertex2, vertex3, vertex9, vertex8, vertex13, vertex12, vertex8, vertex3);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        checkMakeBiconnected(embedding);
    }
}
