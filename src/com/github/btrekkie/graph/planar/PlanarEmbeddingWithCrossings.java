package com.github.btrekkie.graph.planar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.util.UnorderedPair;

/**
 * A PlanarEmbedding of a given graph, after adding vertices for crossings.  Each crossing vertex has a degree of four,
 * for the two edges of the input graph that are crossing at that vertex.  In addition, we may introduce degree-two
 * vertices for the purpose of avoiding multiple edges between the same pair of vertices.  For example, if two edges
 * adjacent to a given vertex cross each other and cross no other edges, then if we were to take the naive approach,
 * we would have two edges between the shared endpoint and the crossing.  Instead, we could add a vertex that subdivides
 * one of the edges in two, allowing us to avoid this.  Each edge in the input graph corresponds to a simple path in the
 * resulting graph, starting and ending with the corresponding endpoints in the resulting graph, and passing through
 * zero or more added vertices.
 */
public class PlanarEmbeddingWithCrossings {
    /** The graph with added vertices for crossing and for avoiding multiple edges between the same pair of vertices. */
    public final Graph graph;

    /** The planar embedding of "graph". */
    public final PlanarEmbedding embedding;

    /** A map from each vertex in the input graph to the corresponding vertex in "graph". */
    public Map<Vertex, Vertex> originalVertexToVertex;

    /**
     * A map from each edge in the input graph to the sequence of vertices in the corresponding path in "graph".  Each
     * path may appear in two possible orders, which are the reverse of each other.  Each path includes the vertices
     * corresponding to the endpoints of the input edge.
     */
    private Map<UnorderedPair<Vertex>, List<Vertex>> addedVertices;

    public PlanarEmbeddingWithCrossings(
            Graph graph, PlanarEmbedding embedding, Map<Vertex, Vertex> originalVertexToVertex,
            Map<UnorderedPair<Vertex>, List<Vertex>> addedVertices) {
        this.graph = graph;
        this.embedding = embedding;
        this.originalVertexToVertex = originalVertexToVertex;
        this.addedVertices = addedVertices;
    }

    /**
     * Returns the sequence of added vertices in the path in "graph" corresponding to the edge from "start" to "end" in
     * the input graph.  This excludes the vertices corresponding to the endpoints of the input edge.
     */
    public List<Vertex> addedVertices(Vertex start, Vertex end) {
        List<Vertex> vertices = addedVertices.get(new UnorderedPair<Vertex>(start, end));
        if (vertices == null) {
            return Collections.emptyList();
        } else {
            vertices = new ArrayList<Vertex>(vertices);
            if (vertices.get(0) != originalVertexToVertex.get(start)) {
                Collections.reverse(vertices);
            }
            vertices.remove(0);
            vertices.remove(vertices.size() - 1);
            return vertices;
        }
    }
}
