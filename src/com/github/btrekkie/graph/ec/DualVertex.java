package com.github.btrekkie.graph.ec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A vertex the dual of a certain planar Graph (the "primal graph"), with self loops removed, relative to a certain
 * planar embedding.  The dual H of a graph G is a graph with one vertex for each face in G, including the external
 * face, and an edge between each pair of vertices corresponding to two faces in G separated by an edge.  The dual may
 * have multiple edges between the same pair of vertices, and it may include self loops.
 */
class DualVertex {
    /** A map from the vertices that are adjacent to this to the number of edges between those vertices and this. */
    public Map<DualVertex, Integer> edgeCounts = new LinkedHashMap<DualVertex, Integer>();

    /** Adds an edge from this to the specified vertex. */
    public void addEdge(DualVertex vertex) {
        Integer count = edgeCounts.get(vertex);
        if (count == null) {
            edgeCounts.put(vertex, 1);
        } else {
            edgeCounts.put(vertex, count + 1);
        }

        count = vertex.edgeCounts.get(this);
        if (count == null) {
            vertex.edgeCounts.put(this, 1);
        } else {
            vertex.edgeCounts.put(this, count + 1);
        }
    }

    /** Removes an edge from this to the specified vertex.  Assumes there is such an edge. */
    public void removeEdge(DualVertex vertex) {
        int count = edgeCounts.get(vertex);
        if (count <= 1) {
            edgeCounts.remove(vertex);
        } else {
            edgeCounts.put(vertex, count - 1);
        }

        count = vertex.edgeCounts.get(this);
        if (count <= 1) {
            vertex.edgeCounts.remove(this);
        } else {
            vertex.edgeCounts.put(this, count - 1);
        }
    }
}
