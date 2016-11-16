package com.github.btrekkie.graph.ec.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.ec.EcNode;
import com.github.btrekkie.graph.ec.EcPlanarEmbedding;
import com.github.btrekkie.graph.planar.PlanarEmbedding;
import com.github.btrekkie.graph.planar.test.PlanarEmbeddingTest;

public class EcPlanarEmbeddingTest {
    /**
     * Returns whether "embedding" matches the specified planar embedding.  Returns false if "embedding" is null.
     * @param embedding The planar embedding.
     * @param clockwiseOrder The clockwise ordering of vertices with which to compare the planar embedding.
     * @param externalFace The external face with which to compare the planar embedding.  We compare
     *     embedding.externalFace with both "externalFace" and its mirror image, because in a planar graph with two
     *     faces, we may choose either face to be the external face.  This may be null, in which case we do not check
     *     the external face.
     * @return Whether the embedding matches.
     */
    public static boolean areEquivalent(
            PlanarEmbedding embedding, Map<Vertex, List<Vertex>> clockwiseOrder, List<Vertex> externalFace) {
        if (embedding == null || !clockwiseOrder.keySet().equals(embedding.clockwiseOrder.keySet())) {
            return false;
        }
        if (externalFace != null && !PlanarEmbeddingTest.isCyclicShift(embedding.externalFace, externalFace)) {
            List<Vertex> reversedExternalFace = new ArrayList<Vertex>(externalFace);
            Collections.reverse(reversedExternalFace);
            if (!PlanarEmbeddingTest.isCyclicShift(embedding.externalFace, reversedExternalFace)) {
                return false;
            }
        }
        for (Entry<Vertex, List<Vertex>> entry : clockwiseOrder.entrySet()) {
            List<Vertex> actual = embedding.clockwiseOrder.get(entry.getKey());
            if (!PlanarEmbeddingTest.isCyclicShift(actual, entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether "embedding" matches the specified planar embedding.  Returns false if "embedding" is null.
     * @param embedding The planar embedding.
     * @param clockwiseOrder The clockwise ordering of vertices with which to compare the planar embedding.
     * @return Whether the embedding matches.
     */
    public static boolean areEquivalent(PlanarEmbedding embedding, Map<Vertex, List<Vertex>> clockwiseOrder) {
        return areEquivalent(embedding, clockwiseOrder, null);
    }

    /** Tests EcPlanarEmbedding.embed. */
    @Test
    public void testEmbed() {
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
        assertNotNull(EcPlanarEmbedding.embed(vertex1, Collections.<Vertex, EcNode>emptyMap()));

        EcNode node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex2);
        EcNode.createVertex(node1, vertex3);
        EcNode.createVertex(node1, vertex4);
        EcNode node2 = EcNode.createVertex(null, vertex4);
        Map<Vertex, EcNode> constraints = new HashMap<Vertex, EcNode>();
        constraints.put(vertex1, node1);
        constraints.put(vertex5, node2);
        Map<Vertex, List<Vertex>> clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3, vertex4));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex2));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex1, vertex5));
        clockwiseOrder.put(vertex5, Collections.singletonList(vertex4));
        PlanarEmbedding embedding = EcPlanarEmbedding.embed(vertex1, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder));
    }

    /** Tests EcPlanarEmbedding.embed on graphs consisting of a cycle. */
    @Test
    public void testEmbedCycle() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        vertex1.addEdge(vertex2);
        Map<Vertex, List<Vertex>> clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Collections.singletonList(vertex2));
        clockwiseOrder.put(vertex2, Collections.singletonList(vertex1));
        List<Vertex> externalFace = Arrays.asList(vertex1, vertex2);
        PlanarEmbedding embedding = EcPlanarEmbedding.embed(vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertTrue(areEquivalent(embedding, clockwiseOrder, externalFace));

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex2.addEdge(vertex3);
        clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex2));
        externalFace = Arrays.asList(vertex1, vertex2, vertex3);
        embedding = EcPlanarEmbedding.embed(vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertTrue(areEquivalent(embedding, clockwiseOrder, externalFace));

        EcNode node1 = EcNode.create(null, EcNode.Type.MIRROR);
        EcNode node2 = EcNode.create(node1, EcNode.Type.GROUP);
        EcNode.createVertex(node2, vertex2);
        EcNode.createVertex(node2, vertex3);
        Map<Vertex, EcNode> constraints = Collections.singletonMap(vertex1, node1);
        embedding = EcPlanarEmbedding.embed(vertex1, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder, externalFace));

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
        embedding = EcPlanarEmbedding.embed(vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertTrue(areEquivalent(embedding, clockwiseOrder, externalFace));

        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex2);
        EcNode.createVertex(node1, vertex5);
        constraints = Collections.singletonMap(vertex1, node1);
        embedding = EcPlanarEmbedding.embed(vertex1, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder, externalFace));

        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex5);
        EcNode.createVertex(node1, vertex2);
        constraints = Collections.singletonMap(vertex1, node1);
        embedding = EcPlanarEmbedding.embed(vertex1, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder, externalFace));
    }

    /** Tests EcPlanarEmbedding.embed on graphs similar to complete graphs. */
    @Test
    public void testEmbedCompleteLike() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Map<Vertex, List<Vertex>> clockwiseOrder = Collections.singletonMap(vertex1, Collections.<Vertex>emptyList());
        List<Vertex> externalFace = Collections.singletonList(vertex1);
        PlanarEmbedding embedding = EcPlanarEmbedding.embed(vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertTrue(areEquivalent(embedding, clockwiseOrder, externalFace));

        graph = new Graph();
        vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex4);
        vertex3.addEdge(vertex4);
        EcNode node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex2);
        EcNode.createVertex(node1, vertex3);
        EcNode.createVertex(node1, vertex4);
        EcNode node2 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node2, vertex1);
        EcNode.createVertex(node2, vertex4);
        EcNode.createVertex(node2, vertex3);
        EcNode node3 = EcNode.create(null, EcNode.Type.MIRROR);
        EcNode.createVertex(node3, vertex4);
        EcNode.createVertex(node3, vertex2);
        EcNode.createVertex(node3, vertex1);
        EcNode node4 = EcNode.create(null, EcNode.Type.GROUP);
        EcNode.createVertex(node4, vertex1);
        EcNode.createVertex(node4, vertex2);
        EcNode.createVertex(node4, vertex3);
        Map<Vertex, EcNode> constraints = new HashMap<Vertex, EcNode>();
        constraints.put(vertex1, node1);
        constraints.put(vertex2, node2);
        constraints.put(vertex3, node3);
        constraints.put(vertex4, node4);
        clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3, vertex4));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex4, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex2, vertex4));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex1, vertex3, vertex2));
        embedding = EcPlanarEmbedding.embed(vertex1, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder));

        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex4);
        EcNode.createVertex(node1, vertex3);
        EcNode.createVertex(node1, vertex2);
        node2 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node2, vertex1);
        EcNode.createVertex(node2, vertex3);
        EcNode.createVertex(node2, vertex4);
        constraints = new HashMap<Vertex, EcNode>();
        constraints.put(vertex1, node1);
        constraints.put(vertex2, node2);
        clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex4, vertex3));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex3, vertex4));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex4, vertex2));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex1, vertex2, vertex3));
        embedding = EcPlanarEmbedding.embed(vertex1, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder));

        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex2);
        EcNode.createVertex(node1, vertex3);
        EcNode.createVertex(node1, vertex4);
        node2 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node2, vertex1);
        EcNode.createVertex(node2, vertex3);
        EcNode.createVertex(node2, vertex4);
        constraints = new HashMap<Vertex, EcNode>();
        constraints.put(vertex1, node1);
        constraints.put(vertex2, node2);
        assertNull(EcPlanarEmbedding.embed(vertex1, constraints));

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
        embedding = EcPlanarEmbedding.embed(vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertNull(embedding);

        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex2);
        EcNode.createVertex(node1, vertex3);
        node2 = EcNode.create(node1, EcNode.Type.GROUP);
        EcNode.createVertex(node2, vertex4);
        EcNode.createVertex(node2, vertex5);
        constraints = Collections.singletonMap(vertex1, node1);
        assertNull(EcPlanarEmbedding.embed(vertex1, constraints));

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
        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex1);
        EcNode.createVertex(node1, vertex5);
        node2 = EcNode.create(node1, EcNode.Type.GROUP);
        EcNode.createVertex(node2, vertex4);
        EcNode.createVertex(node2, vertex2);
        constraints = Collections.singletonMap(vertex3, node1);
        clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex3, vertex4, vertex5));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex3, vertex5, vertex4));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex5, vertex2, vertex4));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex1, vertex3, vertex2, vertex5));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex1, vertex4, vertex2, vertex3));
        embedding = EcPlanarEmbedding.embed(vertex1, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder));
    }

    /**
     * Tests EcPlanarEmbedding.embed on graphs similar to K33, the graph with vertices numbered 1 to 6 and edges from
     * each even-numbered vertex to each odd-numbered vertex.
     */
    @Test
    public void testEmbedK33Like() {
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
        PlanarEmbedding embedding = EcPlanarEmbedding.embed(vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertNull(embedding);

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
        EcNode node = EcNode.create(null, EcNode.Type.MIRROR);
        EcNode.createVertex(node, vertex5);
        EcNode.createVertex(node, vertex6);
        EcNode.createVertex(node, vertex7);
        Map<Vertex, EcNode> constraints = Collections.singletonMap(vertex4, node);
        assertNull(EcPlanarEmbedding.embed(vertex1, constraints));

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
        node = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node, vertex4);
        EcNode.createVertex(node, vertex5);
        EcNode.createVertex(node, vertex6);
        constraints = Collections.singletonMap(vertex2, node);
        Map<Vertex, List<Vertex>> clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex5, vertex6));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex4, vertex5, vertex6));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex4, vertex6, vertex5));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex2, vertex3));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex1, vertex2, vertex3));
        clockwiseOrder.put(vertex6, Arrays.asList(vertex1, vertex3, vertex2));
        embedding = EcPlanarEmbedding.embed(vertex1, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder));
    }

    /** Tests EcPlanarEmbedding.embed on tree graphs. */
    @Test
    public void testEmbedTree() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        EcNode node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex2);
        EcNode.createVertex(node1, vertex3);
        EcNode.createVertex(node1, vertex4);
        Map<Vertex, EcNode> constraints = Collections.singletonMap(vertex1, node1);
        Map<Vertex, List<Vertex>> clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3, vertex4));
        clockwiseOrder.put(vertex2, Collections.singletonList(vertex1));
        clockwiseOrder.put(vertex3, Collections.singletonList(vertex1));
        clockwiseOrder.put(vertex4, Collections.singletonList(vertex1));
        List<Vertex> externalFace = Arrays.asList(vertex1, vertex2, vertex1, vertex3, vertex1, vertex4);
        PlanarEmbedding embedding = EcPlanarEmbedding.embed(vertex1, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder, externalFace));

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
        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex5);
        EcNode.createVertex(node1, vertex4);
        EcNode.createVertex(node1, vertex1);
        EcNode node2 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node2, vertex7);
        EcNode.createVertex(node2, vertex6);
        EcNode.createVertex(node2, vertex1);
        constraints = new HashMap<Vertex, EcNode>();
        constraints.put(vertex2, node1);
        constraints.put(vertex3, node2);
        clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex5, vertex4));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex7, vertex6));
        clockwiseOrder.put(vertex4, Collections.singletonList(vertex2));
        clockwiseOrder.put(vertex5, Collections.singletonList(vertex2));
        clockwiseOrder.put(vertex6, Collections.singletonList(vertex3));
        clockwiseOrder.put(vertex7, Collections.singletonList(vertex3));
        externalFace = Arrays.asList(
            vertex1, vertex3, vertex7, vertex3, vertex6, vertex3, vertex1, vertex2, vertex5, vertex2, vertex4, vertex2);
        embedding = EcPlanarEmbedding.embed(vertex1, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder, externalFace));
    }

    /** Tests EcPlanarEmbedding.embed on the Petersen graph ( https://en.wikipedia.org/wiki/Petersen_graph ). */
    @Test
    public void testEmbedPetersen() {
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
        PlanarEmbedding embedding = EcPlanarEmbedding.embed(vertex0, Collections.<Vertex, EcNode>emptyMap());
        assertNull(embedding);

        EcNode node = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node, vertex2);
        EcNode.createVertex(node, vertex4);
        EcNode.createVertex(node, vertex8);
        Map<Vertex, EcNode> constraints = Collections.singletonMap(vertex3, node);
        assertNull(EcPlanarEmbedding.embed(vertex0, constraints));
    }

    /**
     * Tests EcPlanarEmbedding.embed on graphs similar to the Goldner-Harary graph
     * ( https://en.wikipedia.org/wiki/Goldner%E2%80%93Harary_graph ).
     */
    @Test
    public void testEmbedGoldnerHararyLike() {
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
        EcNode node1 = EcNode.create(null, EcNode.Type.MIRROR);
        EcNode node2 = EcNode.create(node1, EcNode.Type.GROUP);
        EcNode.createVertex(node2, vertex2);
        EcNode.createVertex(node2, vertex6);
        EcNode.createVertex(node2, vertex3);
        EcNode node3 = EcNode.create(node1, EcNode.Type.ORIENTED);
        EcNode.createVertex(node3, vertex5);
        EcNode.createVertex(node3, vertex4);
        EcNode node4 = EcNode.create(node1, EcNode.Type.MIRROR);
        EcNode node5 = EcNode.create(node4, EcNode.Type.GROUP);
        EcNode node6 = EcNode.create(node5, EcNode.Type.ORIENTED);
        EcNode.createVertex(node6, vertex7);
        EcNode.createVertex(node4, vertex8);
        EcNode.createVertex(node4, vertex11);
        Map<Vertex, EcNode> constraints = Collections.singletonMap(vertex1, node1);
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
        PlanarEmbedding embedding = EcPlanarEmbedding.embed(vertex1, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder));

        node1 = EcNode.create(null, EcNode.Type.MIRROR);
        node2 = EcNode.create(node1, EcNode.Type.GROUP);
        EcNode.createVertex(node2, vertex2);
        EcNode.createVertex(node2, vertex6);
        EcNode.createVertex(node2, vertex3);
        node3 = EcNode.create(node1, EcNode.Type.ORIENTED);
        EcNode.createVertex(node3, vertex5);
        EcNode.createVertex(node3, vertex4);
        node4 = EcNode.create(node1, EcNode.Type.ORIENTED);
        EcNode.createVertex(node4, vertex7);
        EcNode.createVertex(node4, vertex8);
        EcNode.createVertex(node4, vertex11);
        constraints = Collections.singletonMap(vertex1, node1);
        assertNull(EcPlanarEmbedding.embed(vertex1, constraints));

        vertex1.addEdge(vertex9);
        embedding = EcPlanarEmbedding.embed(vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertNull(embedding);
    }

    /** Tests EcPlanarEmbedding.embed on the graph for a dodecahedron. */
    @Test
    public void testEmbedDodecahedron() {
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
        EcNode node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex2);
        EcNode.createVertex(node1, vertex6);
        EcNode.createVertex(node1, vertex5);
        Map<Vertex, EcNode> constraints = Collections.singletonMap(vertex1, node1);
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
        PlanarEmbedding embedding = EcPlanarEmbedding.embed(vertex1, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder));

        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex2);
        EcNode.createVertex(node1, vertex10);
        EcNode.createVertex(node1, vertex4);
        EcNode node2 = EcNode.create(null, EcNode.Type.MIRROR);
        EcNode.createVertex(node2, vertex3);
        EcNode.createVertex(node2, vertex11);
        EcNode.createVertex(node2, vertex9);
        EcNode node3 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node3, vertex19);
        EcNode.createVertex(node3, vertex14);
        EcNode.createVertex(node3, vertex12);
        EcNode node4 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node4, vertex7);
        EcNode.createVertex(node4, vertex20);
        EcNode.createVertex(node4, vertex17);
        EcNode node5 = EcNode.create(null, EcNode.Type.GROUP);
        EcNode.createVertex(node5, vertex15);
        EcNode.createVertex(node5, vertex16);
        EcNode.createVertex(node5, vertex19);
        constraints = new HashMap<Vertex, EcNode>();
        constraints.put(vertex3, node1);
        constraints.put(vertex10, node2);
        constraints.put(vertex13, node3);
        constraints.put(vertex16, node4);
        constraints.put(vertex20, node5);
        for (List<Vertex> vertexClockwiseOrder : clockwiseOrder.values()) {
            Collections.reverse(vertexClockwiseOrder);
        }
        embedding = EcPlanarEmbedding.embed(vertex1, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder));

        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex2);
        EcNode.createVertex(node1, vertex7);
        EcNode.createVertex(node1, vertex9);
        node2 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node2, vertex5);
        EcNode.createVertex(node2, vertex15);
        EcNode.createVertex(node2, vertex13);
        constraints = new HashMap<Vertex, EcNode>();
        constraints.put(vertex8, node1);
        constraints.put(vertex14, node2);
        assertNull(EcPlanarEmbedding.embed(vertex1, constraints));
    }

    /**
     * Tests EcPlanarEmbedding.embed on a simple graph shown in
     * http://ls11-www.cs.uni-dortmund.de/_media/techreports/tr09-09.pdf (Zey (2009): Algorithms for Planar Graph
     * Augmentation), where it shows four drawings of one planar graph.
     */
    @Test
    public void testEmbedZey() {
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
        PlanarEmbedding embedding = EcPlanarEmbedding.embed(vertex0, Collections.<Vertex, EcNode>emptyMap());
        assertNotNull(embedding);

        EcNode node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex0);
        EcNode.createVertex(node1, vertex4);
        EcNode.createVertex(node1, vertex2);
        EcNode node2 = EcNode.create(null, EcNode.Type.GROUP);
        EcNode.createVertex(node2, vertex0);
        EcNode node3 = EcNode.create(node2, EcNode.Type.ORIENTED);
        EcNode.createVertex(node3, vertex4);
        EcNode.createVertex(node3, vertex3);
        EcNode.createVertex(node2, vertex1);
        Map<Vertex, EcNode> constraints = new HashMap<Vertex, EcNode>();
        constraints.put(vertex1, node1);
        constraints.put(vertex2, node2);
        Map<Vertex, List<Vertex>> clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex0, Arrays.asList(vertex1, vertex2));
        clockwiseOrder.put(vertex1, Arrays.asList(vertex0, vertex4, vertex2));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex0, vertex1, vertex4, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex2, vertex4));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex1, vertex3, vertex2));
        embedding = EcPlanarEmbedding.embed(vertex0, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder));

        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex0);
        EcNode.createVertex(node1, vertex2);
        EcNode.createVertex(node1, vertex4);
        node2 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node2, vertex3);
        EcNode.createVertex(node2, vertex2);
        EcNode.createVertex(node2, vertex1);
        constraints = new HashMap<Vertex, EcNode>();
        constraints.put(vertex1, node1);
        constraints.put(vertex4, node2);
        clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex0, Arrays.asList(vertex1, vertex2));
        clockwiseOrder.put(vertex1, Arrays.asList(vertex0, vertex2, vertex4));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex0, vertex4, vertex3, vertex1));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex2, vertex4));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex1, vertex3, vertex2));
        embedding = EcPlanarEmbedding.embed(vertex0, constraints);
        assertTrue(areEquivalent(embedding, clockwiseOrder));

        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex0);
        EcNode.createVertex(node1, vertex4);
        EcNode.createVertex(node1, vertex2);
        node2 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node2, vertex1);
        EcNode.createVertex(node2, vertex0);
        EcNode.createVertex(node2, vertex4);
        EcNode.createVertex(node2, vertex3);
        constraints = new HashMap<Vertex, EcNode>();
        constraints.put(vertex1, node1);
        constraints.put(vertex2, node2);
        assertNull(EcPlanarEmbedding.embed(vertex0, constraints));
    }
}
