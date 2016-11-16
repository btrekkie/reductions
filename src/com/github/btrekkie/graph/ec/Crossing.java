package com.github.btrekkie.graph.ec;

import com.github.btrekkie.graph.Vertex;

/**
 * A crossing added between two edges in a planar graph.  This is for the crossing vertices added in
 * PlanarEmbeddingWithCrossings.graph.
 */
class Crossing {
    /**
     * The vertex adjacent to the crossing vertex that is distinct from "start2" and "end2" and "across" from "end1",
     * i.e. that resides on a path corresponding to an edge in the input graph that contains "end1".
     */
    public Vertex start1;

    /**
     * The vertex adjacent to the crossing vertex that is distinct from "start2" and "end2" and "across" from "start1",
     * i.e. that resides on a path corresponding to an edge in the input graph that contains "start1".
     */
    public Vertex end1;

    /**
     * The vertex adjacent to the crossing vertex that is distinct from "start1" and "end1" and "across" from "end2",
     * i.e. that resides on a path corresponding to an edge in the input graph that contains "end2".
     */
    public Vertex start2;

    /**
     * The vertex adjacent to the crossing vertex that is distinct from "start1" and "end1" and "across" from "start2",
     * i.e. that resides on a path corresponding to an edge in the input graph that contains "start2".
     */
    public Vertex end2;

    public Crossing(Vertex start1, Vertex end1, Vertex start2, Vertex end2) {
        this.start1 = start1;
        this.end1 = end1;
        this.start2 = start2;
        this.end2 = end2;
    }
}
