package com.github.btrekkie.graph.planar;

/**
 * A PlanarVertex or RootVertex along with a link direction, used in PlanarEmbedding to identify successive points on an
 * external face.  See the comments for the implementation of PlanarEmbedding.
 */
class VertexAndDir {
    /** The PlanarVertex, or null if the point is a RootVertex. */
    public final PlanarVertex vertex;

    /** The RootVertex, or null if the point is a PlanarVertex. */
    public final RootVertex rootVertex;

    /** Whether vertex.link(true) / rootVertex.link(true) is directed in the inward (backward) direction. */
    public final boolean isInBlack;

    public VertexAndDir(PlanarVertex vertex, boolean isInBlack) {
        this.vertex = vertex;
        rootVertex = null;
        this.isInBlack = isInBlack;
    }

    public VertexAndDir(RootVertex rootVertex, boolean isInBlack) {
        vertex = null;
        this.rootVertex = rootVertex;
        this.isInBlack = isInBlack;
    }
}
