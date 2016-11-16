package com.github.btrekkie.graph.planar;

/**
 * A directed edge, considered as one half of a "bidirectional" (undirected) edge, to be used for the implementation of
 * PlanarEmbedding.  See the comments for the implementation of that class.
 */
class HalfEdge {
    /** The other half of the "bidirectional" (undirected) edge for this. */
    public HalfEdge twinEdge;

    /** The end of the edge.  This is null if the end is a RootVertex rather than a PlanarVertex. */
    public PlanarVertex endVertex;

    /** The end of the edge.  This is null if the end is a PlanarVertex rather than a RootVertex. */
    public RootVertex endRootVertex;

    /**
     * Whether this is a "synthetic" half edge not corresponding to an edge the original graph for which we are
     * computing a planar graph.  This includes short circuit edges and special edges created in
     * PlanarEmbedding.mergeBiconnectedComponent.
     */
    public final boolean isSynthetic;

    /**
     * The next edge with the same start as this in the clockwise direction.  The sequence nextClockwise,
     * nextClockwise.nextClockwise, nextClockwise.nextClockwise.nextClockwise, etc. consists of the edges around the
     * start of this in clockwise order.  This is null prior to computing it in PlanarEmbedding.embedding.
     */
    public HalfEdge nextClockwise;

    private HalfEdge(PlanarVertex endVertex, RootVertex endRootVertex, boolean isSynthetic) {
        this.endVertex = endVertex;
        this.endRootVertex = endRootVertex;
        this.isSynthetic = isSynthetic;
    }

    /** Returns a new non-synthetic HalfEdge with the specified end. */
    public static HalfEdge create(PlanarVertex endVertex) {
        return new HalfEdge(endVertex, null, false);
    }

    /** Returns a new non-synthetic HalfEdge with the specified end. */
    public static HalfEdge create(RootVertex endRootVertex) {
        return new HalfEdge(null, endRootVertex, false);
    }

    /** Returns a new synthetic HalfEdge with the specified end. */
    public static HalfEdge createSynthetic(PlanarVertex endVertex) {
        return new HalfEdge(endVertex, null, true);
    }

    /** Returns a new synthetic HalfEdge with the specified end. */
    public static HalfEdge createSynthetic(RootVertex endRootVertex) {
        return new HalfEdge(null, endRootVertex, true);
    }

    /**
     * Returns the link from the end of this with the specified color.
     * @param isBlack Whether to return the black link.
     * @return The link.
     */
    public HalfEdge endLink(boolean isBlack) {
        if (endVertex != null) {
            return endVertex.link(isBlack);
        } else {
            return endRootVertex.link(isBlack);
        }
    }
}
