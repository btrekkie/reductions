package com.github.btrekkie.graph.ec.test;

import static org.junit.Assert.assertEquals;
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
import com.github.btrekkie.graph.ec.EcNode;
import com.github.btrekkie.graph.ec.EcPlanarEmbeddingWithCrossings;
import com.github.btrekkie.graph.planar.PlanarEmbeddingWithCrossings;

public class EcPlanarEmbeddingWithCrossingsTest {
    /**
     * Returns whether embedding.graph is the same as "graph" (after applying the substitutions in
     * embedding.originalVertexToVertex to the latter).
     */
    private boolean hasGraph(PlanarEmbeddingWithCrossings embedding, Graph graph) {
        if (embedding.graph.vertices.size() != graph.vertices.size()) {
            return false;
        }
        for (Vertex vertex : graph.vertices) {
            Set<Vertex> edges = new HashSet<Vertex>();
            for (Vertex adjVertex : vertex.edges) {
                edges.add(embedding.originalVertexToVertex.get(adjVertex));
            }
            if (!edges.equals(embedding.originalVertexToVertex.get(vertex).edges)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether embedding.embedding matches the specified planar embedding (after applying the substitutions in
     * embedding.originalVertexToVertex to the latter).
     * @param embedding The planar embedding.
     * @param clockwiseOrder The clockwise ordering of vertices with which to compare the planar embedding.
     * @param externalFace The external face with which to compare the planar embedding.  We compare
     *     embedding.embedding.externalFace with both "externalFace" and its mirror image, because in a planar graph
     *     with two faces, we may choose either face to be the external face.  This may be null, in which case we do not
     *     check the external face.
     * @return Whether the embedding matches.
     */
    private boolean hasEmbedding(
            PlanarEmbeddingWithCrossings embedding, Map<Vertex, List<Vertex>> clockwiseOrder,
            List<Vertex> externalFace) {
        Map<Vertex, Vertex> originalVertexToVertex = embedding.originalVertexToVertex;
        Map<Vertex, List<Vertex>> embeddingClockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        for (Entry<Vertex, List<Vertex>> entry : clockwiseOrder.entrySet()) {
            List<Vertex> vertexClockwiseOrder = new ArrayList<Vertex>(entry.getValue().size());
            for (Vertex vertex : entry.getValue()) {
                vertexClockwiseOrder.add(originalVertexToVertex.get(vertex));
            }
            embeddingClockwiseOrder.put(originalVertexToVertex.get(entry.getKey()), vertexClockwiseOrder);
        }

        List<Vertex> embeddingExternalFace;
        if (externalFace == null) {
            embeddingExternalFace = null;
        } else {
            embeddingExternalFace = new ArrayList<Vertex>(externalFace.size());
            for (Vertex vertex : externalFace) {
                embeddingExternalFace.add(originalVertexToVertex.get(vertex));
            }
        }
        return EcPlanarEmbeddingTest.areEquivalent(embedding.embedding, embeddingClockwiseOrder, embeddingExternalFace);
    }

    /**
     * Returns whether embedding.embedding matches the specified planar embedding (after applying the substitutions in
     * embedding.originalVertexToVertex to the latter).
     * @param embedding The planar embedding.
     * @param clockwiseOrder The clockwise ordering of vertices with which to compare the planar embedding.
     * @return Whether the embedding matches.
     */
    private boolean hasEmbedding(PlanarEmbeddingWithCrossings embedding, Map<Vertex, List<Vertex>> clockwiseOrder) {
        return hasEmbedding(embedding, clockwiseOrder, null);
    }

    /** Returns the number of added vertices in embedding.graph relative to the input graph "graph". */
    private int addedVertexCount(PlanarEmbeddingWithCrossings embedding, Graph graph) {
        // We count each crossing vertex four times.  One factor of two comes from the fact that we visit each edge
        // twice: once for each endpoint.  The other factor of two comes from the fact that each crossing vertex is the
        // intersection of two edges.
        int quadrupleAddedVertexCount = 0;
        for (Vertex vertex : graph.vertices) {
            for (Vertex adjVertex : vertex.edges) {
                for (Vertex addedVertex : embedding.addedVertices(vertex, adjVertex)) {
                    if (addedVertex.edges.size() == 2) {
                        quadrupleAddedVertexCount += 2;
                    } else {
                        quadrupleAddedVertexCount++;
                    }
                }
            }
        }
        return quadrupleAddedVertexCount / 4;
    }

    /** Returns the number of added crossing vertices in embedding.graph relative to the input graph "graph". */
    private int crossingCount(PlanarEmbeddingWithCrossings embedding, Graph graph) {
        // We count each crossing vertex four times.  One factor of two comes from the fact that we visit each edge
        // twice: once for each endpoint.  The other factor of two comes from the fact that each crossing vertex is the
        // intersection of two edges.
        int quadrupleCrossingCount = 0;
        for (Vertex vertex : graph.vertices) {
            for (Vertex adjVertex : vertex.edges) {
                for (Vertex addedVertex : embedding.addedVertices(vertex, adjVertex)) {
                    if (addedVertex.edges.size() == 4) {
                        quadrupleCrossingCount++;
                    }
                }
            }
        }
        return quadrupleCrossingCount / 4;
    }

    /** Tests EcPlanarEmbeddingWithCrossings.embed. */
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
        PlanarEmbeddingWithCrossings embedding = EcPlanarEmbeddingWithCrossings.embed(
            vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertEquals(0, addedVertexCount(embedding, graph));

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
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(hasEmbedding(embedding, clockwiseOrder));
        assertEquals(0, addedVertexCount(embedding, graph));
    }

    /** Tests EcPlanarEmbeddingWithCrossings.embed on graphs consisting of a cycle. */
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
        PlanarEmbeddingWithCrossings embedding = EcPlanarEmbeddingWithCrossings.embed(
            vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertTrue(hasGraph(embedding, graph));
        assertTrue(hasEmbedding(embedding, clockwiseOrder, externalFace));
        assertEquals(0, addedVertexCount(embedding, graph));

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
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertTrue(hasGraph(embedding, graph));
        assertTrue(hasEmbedding(embedding, clockwiseOrder, externalFace));
        assertEquals(0, addedVertexCount(embedding, graph));

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
        EcNode node = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node, vertex2);
        EcNode.createVertex(node, vertex5);
        Map<Vertex, EcNode> constraints = Collections.singletonMap(vertex1, node);
        clockwiseOrder = new HashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex5));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex2, vertex4));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex3, vertex5));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex4, vertex1));
        externalFace = Arrays.asList(vertex1, vertex2, vertex3, vertex4, vertex5);
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(hasGraph(embedding, graph));
        assertTrue(hasEmbedding(embedding, clockwiseOrder, externalFace));
        assertEquals(0, addedVertexCount(embedding, graph));
    }

    /** Tests EcPlanarEmbeddingWithCrossings.embed on graphs similar to complete graphs. */
    @Test
    public void testEmbedCompleteLike() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Map<Vertex, List<Vertex>> clockwiseOrder = Collections.singletonMap(vertex1, Collections.<Vertex>emptyList());
        List<Vertex> externalFace = Collections.singletonList(vertex1);
        PlanarEmbeddingWithCrossings embedding = EcPlanarEmbeddingWithCrossings.embed(
            vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertTrue(hasGraph(embedding, graph));
        assertTrue(hasEmbedding(embedding, clockwiseOrder, externalFace));
        assertEquals(0, addedVertexCount(embedding, graph));

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
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(hasGraph(embedding, graph));
        assertTrue(hasEmbedding(embedding, clockwiseOrder));
        assertEquals(0, addedVertexCount(embedding, graph));

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
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(hasGraph(embedding, graph));
        assertTrue(hasEmbedding(embedding, clockwiseOrder));
        assertEquals(0, addedVertexCount(embedding, graph));

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
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(crossingCount(embedding, graph) > 0);

        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex2);
        EcNode.createVertex(node1, vertex4);
        EcNode.createVertex(node1, vertex3);
        node2 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node2, vertex1);
        EcNode.createVertex(node2, vertex3);
        EcNode.createVertex(node2, vertex4);
        node3 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node3, vertex1);
        EcNode.createVertex(node3, vertex2);
        EcNode.createVertex(node3, vertex4);
        node4 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node4, vertex1);
        EcNode.createVertex(node4, vertex2);
        EcNode.createVertex(node4, vertex3);
        constraints.put(vertex1, node1);
        constraints.put(vertex2, node2);
        constraints.put(vertex3, node3);
        constraints.put(vertex4, node4);
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(crossingCount(embedding, graph) > 0);

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
        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex2);
        EcNode.createVertex(node1, vertex3);
        node2 = EcNode.create(node1, EcNode.Type.GROUP);
        EcNode.createVertex(node2, vertex4);
        EcNode.createVertex(node2, vertex5);
        constraints = Collections.singletonMap(vertex1, node1);
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(crossingCount(embedding, graph) > 0);

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
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(hasGraph(embedding, graph));
        assertTrue(hasEmbedding(embedding, clockwiseOrder));
        assertEquals(0, addedVertexCount(embedding, graph));

        // Per https://en.wikipedia.org/wiki/Crossing_number_(graph_theory) , a planar drawing of a complete graph on
        // seven vertices has at least nine crossings
        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        vertex3 = graph.createVertex();
        vertex4 = graph.createVertex();
        vertex5 = graph.createVertex();
        Vertex vertex6 = graph.createVertex();
        Vertex vertex7 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex1.addEdge(vertex5);
        vertex1.addEdge(vertex6);
        vertex1.addEdge(vertex7);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex5);
        vertex2.addEdge(vertex6);
        vertex2.addEdge(vertex7);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex5);
        vertex3.addEdge(vertex6);
        vertex3.addEdge(vertex7);
        vertex4.addEdge(vertex5);
        vertex4.addEdge(vertex6);
        vertex4.addEdge(vertex7);
        vertex5.addEdge(vertex6);
        vertex5.addEdge(vertex7);
        vertex6.addEdge(vertex7);
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertTrue(crossingCount(embedding, graph) >= 9);

        node1 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node1, vertex3);
        EcNode.createVertex(node1, vertex7);
        node2 = EcNode.create(node1, EcNode.Type.GROUP);
        EcNode.createVertex(node2, vertex2);
        EcNode.createVertex(node2, vertex6);
        EcNode.createVertex(node2, vertex4);
        EcNode.createVertex(node1, vertex5);
        node3 = EcNode.create(null, EcNode.Type.MIRROR);
        EcNode.createVertex(node3, vertex1);
        EcNode.createVertex(node3, vertex4);
        EcNode.createVertex(node3, vertex3);
        EcNode.createVertex(node3, vertex5);
        EcNode.createVertex(node3, vertex7);
        EcNode.createVertex(node3, vertex6);
        node4 = EcNode.create(null, EcNode.Type.GROUP);
        EcNode node5 = EcNode.create(node4, EcNode.Type.GROUP);
        EcNode node6 = EcNode.create(node5, EcNode.Type.ORIENTED);
        EcNode.createVertex(node6, vertex1);
        EcNode.createVertex(node6, vertex2);
        EcNode node7 = EcNode.create(node5, EcNode.Type.ORIENTED);
        EcNode.createVertex(node7, vertex4);
        EcNode.createVertex(node7, vertex5);
        EcNode node8 = EcNode.create(node5, EcNode.Type.ORIENTED);
        EcNode.createVertex(node8, vertex6);
        EcNode.createVertex(node8, vertex7);
        constraints = new HashMap<Vertex, EcNode>();
        constraints.put(vertex1, node1);
        constraints.put(vertex2, node3);
        constraints.put(vertex3, node4);
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(crossingCount(embedding, graph) >= 9);
    }

    /**
     * Tests EcPlanarEmbeddingWithCrossings.embed on graphs similar to K33, the graph with vertices numbered 1 to 6 and
     * edges from each even-numbered vertex to each odd-numbered vertex.
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
        PlanarEmbeddingWithCrossings embedding = EcPlanarEmbeddingWithCrossings.embed(
            vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertTrue(crossingCount(embedding, graph) > 0);

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
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(crossingCount(embedding, graph) > 0);

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
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(hasGraph(embedding, graph));
        assertTrue(hasEmbedding(embedding, clockwiseOrder));
        assertEquals(0, addedVertexCount(embedding, graph));
    }

    /**
     * Tests EcPlanarEmbeddingWithCrossings.embed on K55, the graph with vertices numbered 1 to 10 and edges from each
     * even-numbered vertex to each odd-numbered vertex.
     */
    @Test
    public void testEmbedK55() {
        // Per https://en.wikipedia.org/wiki/Crossing_number_(graph_theory) , a planar drawing of K55 has at least 16
        // crossings
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
        vertex1.addEdge(vertex6);
        vertex1.addEdge(vertex7);
        vertex1.addEdge(vertex8);
        vertex1.addEdge(vertex9);
        vertex1.addEdge(vertex10);
        vertex2.addEdge(vertex6);
        vertex2.addEdge(vertex7);
        vertex2.addEdge(vertex8);
        vertex2.addEdge(vertex9);
        vertex2.addEdge(vertex10);
        vertex3.addEdge(vertex6);
        vertex3.addEdge(vertex7);
        vertex3.addEdge(vertex8);
        vertex3.addEdge(vertex9);
        vertex3.addEdge(vertex10);
        vertex4.addEdge(vertex6);
        vertex4.addEdge(vertex7);
        vertex4.addEdge(vertex8);
        vertex4.addEdge(vertex9);
        vertex4.addEdge(vertex10);
        vertex5.addEdge(vertex6);
        vertex5.addEdge(vertex7);
        vertex5.addEdge(vertex8);
        vertex5.addEdge(vertex9);
        vertex5.addEdge(vertex10);
        PlanarEmbeddingWithCrossings embedding = EcPlanarEmbeddingWithCrossings.embed(
            vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertTrue(crossingCount(embedding, graph) >= 16);

        EcNode node1 = EcNode.create(null, EcNode.Type.MIRROR);
        EcNode node2 = EcNode.create(node1, EcNode.Type.GROUP);
        EcNode.createVertex(node2, vertex6);
        EcNode.createVertex(node2, vertex7);
        EcNode.createVertex(node1, vertex8);
        EcNode.createVertex(node1, vertex10);
        EcNode.createVertex(node1, vertex9);
        EcNode node3 = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node3, vertex3);
        EcNode.createVertex(node3, vertex5);
        EcNode.createVertex(node3, vertex2);
        EcNode.createVertex(node3, vertex1);
        EcNode.createVertex(node3, vertex4);
        Map<Vertex, EcNode> constraints = new HashMap<Vertex, EcNode>();
        constraints.put(vertex1, node1);
        constraints.put(vertex6, node3);
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(crossingCount(embedding, graph) >= 16);
    }

    /** Tests EcPlanarEmbeddingWithCrossings.embed on tree graphs. */
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
        PlanarEmbeddingWithCrossings embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(hasGraph(embedding, graph));
        assertTrue(hasEmbedding(embedding, clockwiseOrder, externalFace));
        assertEquals(0, addedVertexCount(embedding, graph));

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
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(hasGraph(embedding, graph));
        assertTrue(hasEmbedding(embedding, clockwiseOrder, externalFace));
        assertEquals(0, addedVertexCount(embedding, graph));
    }

    /**
     * Tests EcPlanarEmbeddingWithCrossings.embed on the Petersen graph
     * ( https://en.wikipedia.org/wiki/Petersen_graph ).
     */
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
        PlanarEmbeddingWithCrossings embedding = EcPlanarEmbeddingWithCrossings.embed(
            vertex0, Collections.<Vertex, EcNode>emptyMap());
        assertTrue(crossingCount(embedding, graph) > 0);

        EcNode node = EcNode.create(null, EcNode.Type.ORIENTED);
        EcNode.createVertex(node, vertex2);
        EcNode.createVertex(node, vertex4);
        EcNode.createVertex(node, vertex8);
        Map<Vertex, EcNode> constraints = Collections.singletonMap(vertex3, node);
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex0, constraints);
        assertTrue(crossingCount(embedding, graph) > 0);
    }

    /**
     * Tests EcPlanarEmbeddingWithCrossings.embed on graphs similar to the Goldner-Harary graph
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
        PlanarEmbeddingWithCrossings embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(hasGraph(embedding, graph));
        assertTrue(hasEmbedding(embedding, clockwiseOrder));
        assertEquals(0, addedVertexCount(embedding, graph));

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
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(crossingCount(embedding, graph) > 0);

        vertex1.addEdge(vertex9);
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, Collections.<Vertex, EcNode>emptyMap());
        assertTrue(crossingCount(embedding, graph) > 0);
    }

    /** Tests EcPlanarEmbeddingWithCrossings.embed on the graph for a dodecahedron. */
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
        PlanarEmbeddingWithCrossings embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(hasGraph(embedding, graph));
        assertTrue(hasEmbedding(embedding, clockwiseOrder));
        assertEquals(0, addedVertexCount(embedding, graph));

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
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(hasGraph(embedding, graph));
        assertTrue(hasEmbedding(embedding, clockwiseOrder));
        assertEquals(0, addedVertexCount(embedding, graph));

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
        embedding = EcPlanarEmbeddingWithCrossings.embed(vertex1, constraints);
        assertTrue(crossingCount(embedding, graph) > 0);
    }
}
