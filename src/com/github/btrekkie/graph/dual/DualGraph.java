package com.github.btrekkie.graph.dual;

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
import java.util.Set;

import com.github.btrekkie.graph.MultiGraph;
import com.github.btrekkie.graph.MultiVertex;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.planar.PlanarEmbedding;
import com.github.btrekkie.util.UnorderedPair;

/**
 * The dual of a certain planar Graph (the "primal graph"), with self loops removed, relative to a certain planar
 * embedding.  The dual H of a graph G is a graph with one vertex for each face in G, including the external face, and
 * an edge between each pair of vertices corresponding to two faces in G separated by an edge.  The dual is a
 * MultiGraph.
 */
public class DualGraph {
    /** The dual graph. */
    public final MultiGraph graph;

    /**
     * A map from each edge in the primal graph to the corresponding edge in "graph".  Each edge is represented as a
     * pair of its endpoints.  It includes self-loop edges in the dual, represented as UnorderedPairs with the same
     * value twice.
     */
    public Map<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>> edgeToDualEdge;

    /**
     * A map from each edge in "graph" to a collection of the corresponding edges in the primal graph.  Each edge is
     * represented as a pair of its endpoints.  It includes self-loop edges in the dual, represented as UnorderedPairs
     * with the same value twice.
     */
    public Map<UnorderedPair<MultiVertex>, Collection<UnorderedPair<Vertex>>> dualEdgeToEdges;

    /**
     * A map from each vertex V in the primal graph to a map from each adjacent vertex W to the MultiVertex
     * corresponding to the face immediately clockwise relative to the edge from V to W.
     */
    private Map<Vertex, Map<Vertex, MultiVertex>> rightFaces;

    public DualGraph(
            MultiGraph graph, Map<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>> edgeToDualEdge,
            Map<Vertex, Map<Vertex, MultiVertex>> rightFaces) {
        this.graph = graph;
        this.edgeToDualEdge = edgeToDualEdge;
        this.rightFaces = rightFaces;
        Map<UnorderedPair<MultiVertex>, Collection<UnorderedPair<Vertex>>> dualEdgeToEdges =
            new HashMap<UnorderedPair<MultiVertex>, Collection<UnorderedPair<Vertex>>>();
        for (Entry<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>> entry : edgeToDualEdge.entrySet()) {
            UnorderedPair<Vertex> edge = entry.getKey();
            UnorderedPair<MultiVertex> dualEdge = entry.getValue();
            Collection<UnorderedPair<Vertex>> edges = dualEdgeToEdges.get(dualEdge);
            if (edges == null) {
                edges = new ArrayList<UnorderedPair<Vertex>>();
                dualEdgeToEdges.put(dualEdge, edges);
            }
            edges.add(edge);
        }
        this.dualEdgeToEdges = dualEdgeToEdges;
    }

    /** Returns the dual of the graph represented with the specified planar embedding. */
    public static DualGraph compute(PlanarEmbedding embedding) {
        if (embedding.externalFace.size() == 1) {
            MultiGraph dualGraph = new MultiGraph();
            dualGraph.createVertex();
            Map<Vertex, Map<Vertex, MultiVertex>> rightFaces = Collections.singletonMap(
                embedding.externalFace.iterator().next(), Collections.<Vertex, MultiVertex>emptyMap());
            return new DualGraph(
                dualGraph, Collections.<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>>emptyMap(), rightFaces);
        }

        // Compute a map from each vertex to a map from each adjacent vertex to the next adjacent vertex in the
        // clockwise direction
        Map<Vertex, Map<Vertex, Vertex>> nextClockwise = new HashMap<Vertex, Map<Vertex, Vertex>>();
        for (Entry<Vertex, List<Vertex>> entry : embedding.clockwiseOrder.entrySet()) {
            List<Vertex> clockwiseOrder = entry.getValue();
            Map<Vertex, Vertex> vertexNextClockwise = new HashMap<Vertex, Vertex>();
            for (int i = 0; i < clockwiseOrder.size() - 1; i++) {
                vertexNextClockwise.put(clockwiseOrder.get(i), clockwiseOrder.get(i + 1));
            }
            vertexNextClockwise.put(clockwiseOrder.get(clockwiseOrder.size() - 1), clockwiseOrder.get(0));
            nextClockwise.put(entry.getKey(), vertexNextClockwise);
        }

        // Compute a map from each edge in the primal graph to the corresponding vertices in the dual graph, by walking
        // over the edges of each face in the primal graph, and compute rightFaces
        MultiGraph dual = new MultiGraph();
        Map<UnorderedPair<Vertex>, Collection<MultiVertex>> edgeToDualVertices =
            new LinkedHashMap<UnorderedPair<Vertex>, Collection<MultiVertex>>();
        Map<Vertex, Map<Vertex, MultiVertex>> rightFaces = new HashMap<Vertex, Map<Vertex, MultiVertex>>();
        Map<Vertex, Set<Vertex>> visitedEdges = new HashMap<Vertex, Set<Vertex>>();
        for (Entry<Vertex, List<Vertex>> entry : embedding.clockwiseOrder.entrySet()) {
            Vertex vertex = entry.getKey();
            Set<Vertex> visited = visitedEdges.get(vertex);
            if (visited == null) {
                visited = new HashSet<Vertex>();
                visitedEdges.put(vertex, visited);
            }
            for (Vertex adjVertex : entry.getValue()) {
                if (!visited.add(adjVertex)) {
                    continue;
                }

                UnorderedPair<Vertex> edge = new UnorderedPair<Vertex>(vertex, adjVertex);
                Collection<MultiVertex> dualVertices = edgeToDualVertices.get(edge);
                if (dualVertices == null) {
                    dualVertices = new ArrayList<MultiVertex>(2);
                    edgeToDualVertices.put(edge, dualVertices);
                }
                MultiVertex dualVertex = dual.createVertex();
                dualVertices.add(dualVertex);

                Map<Vertex, MultiVertex> adjVertexRightFaces = rightFaces.get(adjVertex);
                if (adjVertexRightFaces == null) {
                    adjVertexRightFaces = new HashMap<Vertex, MultiVertex>();
                    rightFaces.put(adjVertex, adjVertexRightFaces);
                }
                adjVertexRightFaces.put(vertex, dualVertex);

                // Walk over the face including the edge from "vertex" to adjVertex
                Vertex prevVertex = adjVertex;
                Vertex faceVertex = nextClockwise.get(adjVertex).get(vertex);
                while (prevVertex != vertex || faceVertex != adjVertex) {
                    edge = new UnorderedPair<Vertex>(prevVertex, faceVertex);
                    dualVertices = edgeToDualVertices.get(edge);
                    if (dualVertices == null) {
                        dualVertices = new ArrayList<MultiVertex>(2);
                        edgeToDualVertices.put(edge, dualVertices);
                    }
                    dualVertices.add(dualVertex);

                    Map<Vertex, MultiVertex> faceVertexRightFaces = rightFaces.get(faceVertex);
                    if (faceVertexRightFaces == null) {
                        faceVertexRightFaces = new HashMap<Vertex, MultiVertex>();
                        rightFaces.put(faceVertex, faceVertexRightFaces);
                    }
                    faceVertexRightFaces.put(prevVertex, dualVertex);

                    Set<Vertex> prevVisited = visitedEdges.get(prevVertex);
                    if (prevVisited == null) {
                        prevVisited = new HashSet<Vertex>();
                        visitedEdges.put(prevVertex, prevVisited);
                    }
                    prevVisited.add(faceVertex);

                    Vertex nextVertex = nextClockwise.get(faceVertex).get(prevVertex);
                    prevVertex = faceVertex;
                    faceVertex = nextVertex;
                }
            }
        }

        // Compute edgeToDualEdge
        Map<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>> edgeToDualEdge =
            new LinkedHashMap<UnorderedPair<Vertex>, UnorderedPair<MultiVertex>>();
        for (Entry<UnorderedPair<Vertex>, Collection<MultiVertex>> entry : edgeToDualVertices.entrySet()) {
            Iterator<MultiVertex> iterator = entry.getValue().iterator();
            MultiVertex dualVertex1 = iterator.next();
            MultiVertex dualVertex2 = iterator.next();
            if (dualVertex1 != dualVertex2) {
                dualVertex1.addEdge(dualVertex2);
            }
            edgeToDualEdge.put(entry.getKey(), new UnorderedPair<MultiVertex>(dualVertex1, dualVertex2));
        }
        return new DualGraph(dual, edgeToDualEdge, rightFaces);
    }

    /**
     * Returns the MultiVertex corresponding to the face immediately counterclockwise relative to the edge from "start"
     * to "end".
     */
    public MultiVertex leftFace(Vertex start, Vertex end) {
        return rightFaces.get(end).get(start);
    }

    /**
     * Returns the MultiVertex corresponding to the face immediately clockwise relative to the edge from "start" to
     * "end".
     */
    public MultiVertex rightFace(Vertex start, Vertex end) {
        return rightFaces.get(start).get(end);
    }
}
