package com.github.btrekkie.graph.planar;

/**
 * An element in the stack PlanarEmbedding uses to keep track of biconnected components we will merge together.  See the
 * comments for the implementation fo PlanarEmbedding.
 */
class MergeStackEntry {
    /** The root of the biconnected component we will merge. */
    public final RootVertex rootVertex;

    /**
     * Whether the black link from rootVertex.vertex is an edge on the internal face that we will create when we add the
     * edge that merges together the biconnected components.  (If the black and white links are the same, this may be
     * false.)
     */
    public final boolean vertexIsInBlack;

    /**
     * Whether the black link from rootVertex is an edge on the internal face that we will create when we add the edge
     * that merges together the biconnected components.
     */
    public final boolean rootVertexIsOutBlack;

    public MergeStackEntry(RootVertex rootVertex, boolean vertexIsInBlack, boolean rootVertexIsOutBlack) {
        this.rootVertex = rootVertex;
        this.vertexIsInBlack = vertexIsInBlack;
        this.rootVertexIsOutBlack = rootVertexIsOutBlack;
    }
}
