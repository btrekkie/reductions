package com.github.btrekkie.graph.dual.test;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.MultiGraph;
import com.github.btrekkie.graph.MultiVertex;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.dual.DualGraph;
import com.github.btrekkie.graph.planar.PlanarEmbedding;
import com.github.btrekkie.util.UnorderedPair;

public class DualGraphTest {
    /** Returns whether "expected" matches "actual".  Assumes that "expected" is a valid dual graph. */
    private boolean areEquivalent(DualGraph actual, DualGraph expected) {
        if (!expected.edgeToDualEdge.keySet().equals(actual.edgeToDualEdge.keySet()) ||
                expected.graph.vertices.size() != actual.graph.vertices.size()) {
            return false;
        }

        // Compare edgeToDualEdge, and compute a map from the vertices in "expected" to the corresponding vertices in
        // "actual"
        Map<MultiVertex, MultiVertex> expectedToActual = new HashMap<MultiVertex, MultiVertex>();
        for (UnorderedPair<Vertex> edge : expected.edgeToDualEdge.keySet()) {
            MultiVertex expectedLeftFace = expected.leftFace(edge.value1, edge.value2);
            MultiVertex actualLeftFace = actual.leftFace(edge.value1, edge.value2);
            MultiVertex leftFace = expectedToActual.get(expectedLeftFace);
            if (leftFace == null) {
                expectedToActual.put(expectedLeftFace, actualLeftFace);
            } else if (actualLeftFace != leftFace) {
                return false;
            }

            MultiVertex expectedRightFace = expected.rightFace(edge.value1, edge.value2);
            MultiVertex actualRightFace = actual.rightFace(edge.value1, edge.value2);
            MultiVertex rightFace = expectedToActual.get(expectedRightFace);
            if (rightFace == null) {
                expectedToActual.put(expectedRightFace, actualRightFace);
            } else if (actualRightFace != rightFace) {
                return false;
            }
        }

        // Compare the edges in the dual graphs
        for (Entry<MultiVertex, MultiVertex> entry : expectedToActual.entrySet()) {
            // Compute a map from vertex to the number of edges between entry.getKey() and the vertex, after translating
            // the vertices using expectedToActual
            Map<MultiVertex, Integer> expectedEdgeCount = new HashMap<MultiVertex, Integer>();
            for (MultiVertex adjVertex : entry.getKey().edges) {
                MultiVertex adjExpectedVertex = expectedToActual.get(adjVertex);
                Integer edgeCount = expectedEdgeCount.get(adjExpectedVertex);
                if (edgeCount != null) {
                    expectedEdgeCount.put(adjExpectedVertex, edgeCount + 1);
                } else {
                    expectedEdgeCount.put(adjExpectedVertex, 1);
                }
            }

            // Compute a map from vertex to the number of edges between entry.getValue() and the vertex
            Map<MultiVertex, Integer> actualEdgeCount = new HashMap<MultiVertex, Integer>();
            for (MultiVertex adjVertex : entry.getValue().edges) {
                Integer edgeCount = actualEdgeCount.get(adjVertex);
                if (edgeCount != null) {
                    actualEdgeCount.put(adjVertex, edgeCount + 1);
                } else {
                    actualEdgeCount.put(adjVertex, 1);
                }
            }

            if (!actualEdgeCount.equals(expectedEdgeCount)) {
                return false;
            }
        }

        // Compare expected.edgeToDualEdge and actual.edgeToDualEdge
        for (Entry<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>> entry : expected.edgeToDualEdge.entrySet()) {
            UnorderedPair<Vertex> edge = entry.getKey();
            UnorderedPair<MultiVertex> expectedDualEdge = entry.getValue();
            if (expectedDualEdge == null) {
                if (actual.edgeToDualEdge.get(edge) != null || !actual.edgeToDualEdge.containsKey(edge)) {
                    return false;
                }
            } else {
                UnorderedPair<MultiVertex> expectedEdge = new UnorderedPair<MultiVertex>(
                    expectedToActual.get(expectedDualEdge.value1), expectedToActual.get(expectedDualEdge.value2));
                if (!expectedEdge.equals(actual.edgeToDualEdge.get(edge))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Tests DualGraph.compute on simplistic graphs. */
    @Test
    public void testComputeSimple() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Map<Vertex, List<Vertex>> clockwiseOrder = Collections.singletonMap(vertex1, Collections.<Vertex>emptyList());
        List<Vertex> externalFace = Collections.singletonList(vertex1);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        MultiGraph dualGraph = new MultiGraph();
        dualGraph.createVertex();
        Map<Vertex, Map<Vertex, MultiVertex>> rightFaces = Collections.singletonMap(
            vertex1, Collections.<Vertex, MultiVertex>emptyMap());
        DualGraph dual = new DualGraph(
            dualGraph, Collections.<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>>emptyMap(), rightFaces);
        assertTrue(areEquivalent(DualGraph.compute(embedding), dual));

        graph = new Graph();
        vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        vertex1.addEdge(vertex2);
        clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Collections.singletonList(vertex2));
        clockwiseOrder.put(vertex2, Collections.singletonList(vertex1));
        externalFace = Arrays.asList(vertex1, vertex2);
        embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        dualGraph = new MultiGraph();
        MultiVertex multiVertex = dualGraph.createVertex();
        Map<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>> edgeToDualEdge =
            new HashMap<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>>();
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex2), new UnorderedPair<MultiVertex>(multiVertex, multiVertex));
        rightFaces = new HashMap<Vertex, Map<Vertex, MultiVertex>>();
        rightFaces.put(vertex1, Collections.singletonMap(vertex2, multiVertex));
        rightFaces.put(vertex2, Collections.singletonMap(vertex1, multiVertex));
        dual = new DualGraph(dualGraph, edgeToDualEdge, rightFaces);
        assertTrue(areEquivalent(DualGraph.compute(embedding), dual));
    }

    /** Tests DualGraph.compute on a graph consisting of a cycle. */
    @Test
    public void testComputeCycle() {
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
        MultiGraph dualGraph = new MultiGraph();
        MultiVertex multiVertex1 = dualGraph.createVertex();
        MultiVertex multiVertex2 = dualGraph.createVertex();
        for (int i = 0; i < 5; i++) {
            multiVertex1.addEdge(multiVertex2);
        }
        Map<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>> edgeToDualEdge =
            new HashMap<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>>();
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex2), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex5), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex2, vertex3), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex3, vertex4), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex4, vertex5), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        Map<Vertex, Map<Vertex, MultiVertex>> rightFaces = new HashMap<Vertex, Map<Vertex, MultiVertex>>();
        Map<Vertex, MultiVertex> vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex2, multiVertex2);
        vertexRightFaces.put(vertex5, multiVertex1);
        rightFaces.put(vertex1, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex1, multiVertex1);
        vertexRightFaces.put(vertex3, multiVertex2);
        rightFaces.put(vertex2, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex2, multiVertex1);
        vertexRightFaces.put(vertex4, multiVertex2);
        rightFaces.put(vertex3, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex3, multiVertex1);
        vertexRightFaces.put(vertex5, multiVertex2);
        rightFaces.put(vertex4, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex1, multiVertex2);
        vertexRightFaces.put(vertex4, multiVertex1);
        rightFaces.put(vertex5, vertexRightFaces);
        DualGraph dual = new DualGraph(dualGraph, edgeToDualEdge, rightFaces);
        assertTrue(areEquivalent(DualGraph.compute(embedding), dual));
    }

    /** Tests DualGraph.compute on a graph consisting of a path. */
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
        MultiGraph dualGraph = new MultiGraph();
        MultiVertex multiVertex = dualGraph.createVertex();
        Map<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>> edgeToDualEdge =
            new HashMap<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>>();
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex2), new UnorderedPair<MultiVertex>(multiVertex, multiVertex));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex2, vertex3), new UnorderedPair<MultiVertex>(multiVertex, multiVertex));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex3, vertex4), new UnorderedPair<MultiVertex>(multiVertex, multiVertex));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex4, vertex5), new UnorderedPair<MultiVertex>(multiVertex, multiVertex));
        Map<Vertex, Map<Vertex, MultiVertex>> rightFaces = new HashMap<Vertex, Map<Vertex, MultiVertex>>();
        rightFaces.put(vertex1, Collections.singletonMap(vertex2, multiVertex));
        Map<Vertex, MultiVertex> vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex1, multiVertex);
        vertexRightFaces.put(vertex3, multiVertex);
        rightFaces.put(vertex2, vertexRightFaces);
        vertexRightFaces.put(vertex2, multiVertex);
        vertexRightFaces.put(vertex4, multiVertex);
        rightFaces.put(vertex3, vertexRightFaces);
        vertexRightFaces.put(vertex3, multiVertex);
        vertexRightFaces.put(vertex5, multiVertex);
        rightFaces.put(vertex4, vertexRightFaces);
        rightFaces.put(vertex5, Collections.singletonMap(vertex4, multiVertex));
        DualGraph dual = new DualGraph(dualGraph, edgeToDualEdge, rightFaces);
        assertTrue(areEquivalent(DualGraph.compute(embedding), dual));
    }

    /** Tests DualGraph.compute on a graph consisting of a tree. */
    @Test
    public void testComputeTree() {
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
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex5);
        vertex3.addEdge(vertex6);
        vertex3.addEdge(vertex7);
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex5, vertex4));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex7, vertex6));
        clockwiseOrder.put(vertex4, Collections.singletonList(vertex2));
        clockwiseOrder.put(vertex5, Collections.singletonList(vertex2));
        clockwiseOrder.put(vertex6, Collections.singletonList(vertex3));
        clockwiseOrder.put(vertex7, Collections.singletonList(vertex3));
        List<Vertex> externalFace = Arrays.asList(
            vertex1, vertex3, vertex7, vertex3, vertex6, vertex3, vertex1, vertex2, vertex5, vertex2, vertex4, vertex2);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        MultiGraph dualGraph = new MultiGraph();
        MultiVertex multiVertex = dualGraph.createVertex();
        Map<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>> edgeToDualEdge =
            new HashMap<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>>();
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex2), new UnorderedPair<MultiVertex>(multiVertex, multiVertex));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex3), new UnorderedPair<MultiVertex>(multiVertex, multiVertex));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex2, vertex4), new UnorderedPair<MultiVertex>(multiVertex, multiVertex));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex2, vertex5), new UnorderedPair<MultiVertex>(multiVertex, multiVertex));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex3, vertex6), new UnorderedPair<MultiVertex>(multiVertex, multiVertex));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex3, vertex7), new UnorderedPair<MultiVertex>(multiVertex, multiVertex));
        Map<Vertex, Map<Vertex, MultiVertex>> rightFaces = new HashMap<Vertex, Map<Vertex, MultiVertex>>();
        Map<Vertex, MultiVertex> vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex2, multiVertex);
        vertexRightFaces.put(vertex3, multiVertex);
        rightFaces.put(vertex1, vertexRightFaces);
        vertexRightFaces.put(vertex1, multiVertex);
        vertexRightFaces.put(vertex4, multiVertex);
        vertexRightFaces.put(vertex5, multiVertex);
        rightFaces.put(vertex2, vertexRightFaces);
        vertexRightFaces.put(vertex1, multiVertex);
        vertexRightFaces.put(vertex6, multiVertex);
        vertexRightFaces.put(vertex7, multiVertex);
        rightFaces.put(vertex3, vertexRightFaces);
        rightFaces.put(vertex4, Collections.singletonMap(vertex2, multiVertex));
        rightFaces.put(vertex5, Collections.singletonMap(vertex2, multiVertex));
        rightFaces.put(vertex6, Collections.singletonMap(vertex3, multiVertex));
        rightFaces.put(vertex7, Collections.singletonMap(vertex3, multiVertex));
        DualGraph dual = new DualGraph(dualGraph, edgeToDualEdge, rightFaces);
        assertTrue(areEquivalent(DualGraph.compute(embedding), dual));
    }

    /** Tests DualGraph.compute on graphs depicted in https://en.wikipedia.org/wiki/Dual_graph . */
    @Test
    public void testWikipedia() {
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
        vertex2.addEdge(vertex4);
        vertex3.addEdge(vertex4);
        vertex4.addEdge(vertex5);
        vertex4.addEdge(vertex6);
        vertex4.addEdge(vertex7);
        vertex5.addEdge(vertex6);
        vertex5.addEdge(vertex7);
        vertex6.addEdge(vertex7);
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex3, vertex4));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex4, vertex3));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex2, vertex4));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex1, vertex7, vertex6, vertex5, vertex3, vertex2));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex4, vertex6, vertex7));
        clockwiseOrder.put(vertex6, Arrays.asList(vertex4, vertex7, vertex5));
        clockwiseOrder.put(vertex7, Arrays.asList(vertex4, vertex5, vertex6));
        List<Vertex> externalFace = Arrays.asList(vertex1, vertex4, vertex7, vertex5, vertex4, vertex3);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        MultiGraph dualGraph = new MultiGraph();
        MultiVertex multiVertex1 = dualGraph.createVertex();
        MultiVertex multiVertex2 = dualGraph.createVertex();
        MultiVertex multiVertex3 = dualGraph.createVertex();
        MultiVertex multiVertex4 = dualGraph.createVertex();
        MultiVertex multiVertex5 = dualGraph.createVertex();
        MultiVertex multiVertex6 = dualGraph.createVertex();
        MultiVertex multiVertex7 = dualGraph.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex1.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex4);
        multiVertex3.addEdge(multiVertex4);
        multiVertex4.addEdge(multiVertex5);
        multiVertex4.addEdge(multiVertex6);
        multiVertex4.addEdge(multiVertex7);
        multiVertex5.addEdge(multiVertex6);
        multiVertex5.addEdge(multiVertex7);
        multiVertex6.addEdge(multiVertex7);
        Map<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>> edgeToDualEdge =
            new HashMap<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>>();
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex2), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex3), new UnorderedPair<MultiVertex>(multiVertex2, multiVertex4));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex4), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex4));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex2, vertex3), new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex2, vertex4), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex3, vertex4), new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex4, vertex5), new UnorderedPair<MultiVertex>(multiVertex4, multiVertex5));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex4, vertex6), new UnorderedPair<MultiVertex>(multiVertex5, multiVertex7));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex4, vertex7), new UnorderedPair<MultiVertex>(multiVertex4, multiVertex7));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex5, vertex6), new UnorderedPair<MultiVertex>(multiVertex5, multiVertex6));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex5, vertex7), new UnorderedPair<MultiVertex>(multiVertex4, multiVertex6));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex6, vertex7), new UnorderedPair<MultiVertex>(multiVertex6, multiVertex7));
        Map<Vertex, Map<Vertex, MultiVertex>> rightFaces = new HashMap<Vertex, Map<Vertex, MultiVertex>>();
        Map<Vertex, MultiVertex> vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex2, multiVertex2);
        vertexRightFaces.put(vertex3, multiVertex4);
        vertexRightFaces.put(vertex4, multiVertex1);
        rightFaces.put(vertex1, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex1, multiVertex1);
        vertexRightFaces.put(vertex3, multiVertex2);
        vertexRightFaces.put(vertex4, multiVertex3);
        rightFaces.put(vertex2, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex1, multiVertex2);
        vertexRightFaces.put(vertex2, multiVertex3);
        vertexRightFaces.put(vertex4, multiVertex4);
        rightFaces.put(vertex3, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex1, multiVertex4);
        vertexRightFaces.put(vertex2, multiVertex1);
        vertexRightFaces.put(vertex3, multiVertex3);
        vertexRightFaces.put(vertex5, multiVertex4);
        vertexRightFaces.put(vertex6, multiVertex5);
        vertexRightFaces.put(vertex7, multiVertex7);
        rightFaces.put(vertex4, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex4, multiVertex5);
        vertexRightFaces.put(vertex6, multiVertex6);
        vertexRightFaces.put(vertex7, multiVertex4);
        rightFaces.put(vertex5, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex4, multiVertex7);
        vertexRightFaces.put(vertex5, multiVertex5);
        vertexRightFaces.put(vertex7, multiVertex6);
        rightFaces.put(vertex6, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex4, multiVertex4);
        vertexRightFaces.put(vertex5, multiVertex6);
        vertexRightFaces.put(vertex6, multiVertex7);
        rightFaces.put(vertex7, vertexRightFaces);
        DualGraph dual = new DualGraph(dualGraph, edgeToDualEdge, rightFaces);
        assertTrue(areEquivalent(DualGraph.compute(embedding), dual));

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        vertex3 = graph.createVertex();
        vertex4 = graph.createVertex();
        vertex5 = graph.createVertex();
        vertex6 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex5);
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex6);
        vertex3.addEdge(vertex5);
        vertex4.addEdge(vertex6);
        vertex5.addEdge(vertex6);
        clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(vertex1, Arrays.asList(vertex2, vertex5, vertex3));
        clockwiseOrder.put(vertex2, Arrays.asList(vertex1, vertex4, vertex6));
        clockwiseOrder.put(vertex3, Arrays.asList(vertex1, vertex5));
        clockwiseOrder.put(vertex4, Arrays.asList(vertex2, vertex6));
        clockwiseOrder.put(vertex5, Arrays.asList(vertex1, vertex6, vertex3));
        clockwiseOrder.put(vertex6, Arrays.asList(vertex2, vertex4, vertex5));
        externalFace = Arrays.asList(vertex1, vertex2, vertex4, vertex6, vertex5, vertex3);
        embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        dualGraph = new MultiGraph();
        multiVertex1 = dualGraph.createVertex();
        multiVertex2 = dualGraph.createVertex();
        multiVertex3 = dualGraph.createVertex();
        multiVertex4 = dualGraph.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex4);
        multiVertex1.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex4);
        multiVertex3.addEdge(multiVertex4);
        multiVertex3.addEdge(multiVertex4);
        edgeToDualEdge = new HashMap<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>>();
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex2), new UnorderedPair<MultiVertex>(multiVertex2, multiVertex4));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex3), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex4));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex5), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex2, vertex4), new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex2, vertex6), new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex3, vertex5), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex4));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex4, vertex6), new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex5, vertex6), new UnorderedPair<MultiVertex>(multiVertex2, multiVertex4));
        rightFaces = new HashMap<Vertex, Map<Vertex, MultiVertex>>();
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex2, multiVertex2);
        vertexRightFaces.put(vertex3, multiVertex4);
        vertexRightFaces.put(vertex5, multiVertex1);
        rightFaces.put(vertex1, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex1, multiVertex4);
        vertexRightFaces.put(vertex4, multiVertex3);
        vertexRightFaces.put(vertex6, multiVertex2);
        rightFaces.put(vertex2, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex1, multiVertex1);
        vertexRightFaces.put(vertex5, multiVertex4);
        rightFaces.put(vertex3, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex2, multiVertex4);
        vertexRightFaces.put(vertex6, multiVertex3);
        rightFaces.put(vertex4, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex1, multiVertex2);
        vertexRightFaces.put(vertex3, multiVertex1);
        vertexRightFaces.put(vertex6, multiVertex4);
        rightFaces.put(vertex5, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex2, multiVertex3);
        vertexRightFaces.put(vertex4, multiVertex4);
        vertexRightFaces.put(vertex5, multiVertex2);
        rightFaces.put(vertex6, vertexRightFaces);
        dual = new DualGraph(dualGraph, edgeToDualEdge, rightFaces);
        assertTrue(areEquivalent(DualGraph.compute(embedding), dual));
    }

    /**
     * Tests DualGraph.compute on a graph shown in
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
        MultiGraph dualGraph = new MultiGraph();
        MultiVertex multiVertex1 = dualGraph.createVertex();
        MultiVertex multiVertex2 = dualGraph.createVertex();
        MultiVertex multiVertex3 = dualGraph.createVertex();
        MultiVertex multiVertex4 = dualGraph.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex1.addEdge(multiVertex4);
        multiVertex1.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex3);
        multiVertex3.addEdge(multiVertex4);
        Map<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>> edgeToDualEdge =
            new HashMap<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>>();
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex0, vertex1), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex0, vertex2), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex2), new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex1, vertex4), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex2, vertex3), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex4));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex2, vertex4), new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        edgeToDualEdge.put(
            new UnorderedPair<Vertex>(vertex3, vertex4), new UnorderedPair<MultiVertex>(multiVertex1, multiVertex4));
        Map<Vertex, Map<Vertex, MultiVertex>> rightFaces = new HashMap<Vertex, Map<Vertex, MultiVertex>>();
        Map<Vertex, MultiVertex> vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex1, multiVertex2);
        vertexRightFaces.put(vertex2, multiVertex1);
        rightFaces.put(vertex0, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex0, multiVertex1);
        vertexRightFaces.put(vertex2, multiVertex2);
        vertexRightFaces.put(vertex4, multiVertex3);
        rightFaces.put(vertex1, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex0, multiVertex2);
        vertexRightFaces.put(vertex1, multiVertex3);
        vertexRightFaces.put(vertex3, multiVertex1);
        vertexRightFaces.put(vertex4, multiVertex4);
        rightFaces.put(vertex2, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex2, multiVertex4);
        vertexRightFaces.put(vertex4, multiVertex1);
        rightFaces.put(vertex3, vertexRightFaces);
        vertexRightFaces = new HashMap<Vertex, MultiVertex>();
        vertexRightFaces.put(vertex1, multiVertex1);
        vertexRightFaces.put(vertex2, multiVertex3);
        vertexRightFaces.put(vertex3, multiVertex4);
        rightFaces.put(vertex4, vertexRightFaces);
        DualGraph dual = new DualGraph(dualGraph, edgeToDualEdge, rightFaces);
        assertTrue(areEquivalent(DualGraph.compute(embedding), dual));
    }
}
