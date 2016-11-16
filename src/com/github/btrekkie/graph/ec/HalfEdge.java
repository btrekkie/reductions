package com.github.btrekkie.graph.ec;

import com.github.btrekkie.graph.Vertex;

/**
 * A directed edge in a planar embedding (as in PlanarEmbedding) of the skeleton of an SpqrNode, considered as one half
 * of a "bidirectional" (undirected) edge.
 */
class HalfEdge {
    /** The end of the edge: an element of SpqrNode.skeletonVertexToVertex.values() for the appropriate node. */
    public final Vertex end;

    /** Whether the edge is a virtual edge.  See the comments for SpqrNode. */
    public final boolean isVirtual;

    /** The other half of the "bidirectional" (undirected) edge for this. */
    public HalfEdge twinEdge;

    /**
     * The next edge with the same start as this in the clockwise direction.  The sequence nextClockwise,
     * nextClockwise.nextClockwise, nextClockwise.nextClockwise.nextClockwise, etc. consists of the edges around the
     * start of this in clockwise order.
     */
    public HalfEdge nextClockwise;

    /**
     * The next edge on a possible external face of the skeleton in clockwise order, if it is known that this edge may
     * be an external face and remain compatible with the clockwise ordering of vertices for the planar embedding.  The
     * HalfEdges of a possible external face are in the clockwise direction, with each HalfEdge starting at the end of
     * the previous HalfEdge.  We represent all possible external faces of nodes of type SpqrNode.Type.P and
     * SpqrNode.Type.S and one possible external face of nodes of type SpqrNode.Type.R.
     */
    public HalfEdge nextOnExternalFace;

    /**
     * The matching virtual edge, or null if isVirtual is false.  This is a HalfEdge from another SpqrNode in the same
     * tree having the same start and end vertices.
     */
    public HalfEdge virtualMatch;

    public HalfEdge(Vertex end, boolean isVirtual) {
        this.end = end;
        this.isVirtual = isVirtual;
    }
}
