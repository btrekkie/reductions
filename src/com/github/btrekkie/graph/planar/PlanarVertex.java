package com.github.btrekkie.graph.planar;

import java.util.ArrayList;
import java.util.List;

import com.github.btrekkie.graph.Vertex;

/**
 * A vertex in the depth-first search tree constructed in the implementation of PlanarEmbedding.  See the comments for
 * the implementation of PlanarEmbedding.
 */
class PlanarVertex {
    /** The corresponding Vertex in the graph whose planar embedding we are computing. */
    public final Vertex vertex;

    /** The parent in the depth-first search tree, if any. */
    public final PlanarVertex parent;

    /**
     * The index of the vertex in the topological ordering of the depth-first search tree, so that the index of a vertex
     * is less that that of its children.
     */
    public final int index;

    /** The lowpoint of the vertex, as defined in the paper. */
    public PlanarVertex lowpoint;

    /** The ancestor nearest the root of the depth-first search tree to which this has a back edge, if any. */
    public PlanarVertex leastAncestor;

    /**
     * The first RootVertex in the list of the roots of the biconnected components that contain this, but not "parent".
     * If "parent" is null, the list consists of all biconnected components that contain this.  Initially, the list is
     * unsorted, but early in the process of computing a planar embedding, we sort it in ascending order of
     * RootVertex.child.lowpoint.index.
     */
    public RootVertex separatedChildrenHead;

    /**
     * The last RootVertex in the list of the roots of the biconnected components that contain this, but not "parent".
     * If "parent" is null, the list consists of all biconnected components that contain this.  Initially, the list is
     * unsorted, but early in the process of computing a planar embedding, we sort it in ascending order of
     * RootVertex.child.lowpoint.index.
     */
    public RootVertex separatedChildrenTail;

    /**
     * The first RootVertex in the list of the pertinent root vertices with RootVertex.vertex == this, as defined in the
     * paper, at the current point of computation.  The list is ordered with all of the roots of internally active
     * vertices appearing before all of the roots of externally active root components.
     */
    public RootVertex pertinentRootsHead;

    /**
     * The last RootVertex in the list of the pertinent root vertices with RootVertex.vertex == this, as defined in the
     * paper, at the current point of computation.  The list is ordered with all of the roots of internally active
     * vertices appearing before all of the roots of externally active root components.
     */
    public RootVertex pertinentRootsTail;

    /**
     * The last vertex at which we started the walk-up procedure (the endpoint of the relevant back edge that is an
     * ancestor of the other endpoint) and reached this vertex, if any.
     */
    public PlanarVertex visited;

    /**
     * The last ancestor with a back edge to this at which we started adding back edges to the planar embedding, if any.
     */
    public PlanarVertex backEdgeFlag;

    /** The descendants that are connected to this with a back edge. */
    public List<PlanarVertex> backEdges = new ArrayList<PlanarVertex>();

    /**
     * One of the two edges that starts at this vertex and is on the external face of some biconnected component.  Of
     * the two edges, we identify blackLink as being the one colored black.  In the case of a biconnected component with
     * one edge, the black and white links are the same.  The links are unspecified if this vertex is not on the
     * external face of some biconnected component.
     */
    private HalfEdge blackLink;

    /**
     * One of the two edges that starts at this vertex and is on the external face of some biconnected component.  Of
     * the two edges, we identify whiteLink as being the one colored white.  In the case of a biconnected component with
     * one edge, the black and white links are the same.  The links are unspecified if this vertex is not on the
     * external face of some biconnected component.
     */
    private HalfEdge whiteLink;

    /**
     * Equivalent to rootVertex.isFlippedRelativeToParent, where rootVertex is the RootVertex for which
     * RootVertex.child == this.
     */
    public boolean isFlippedRelativeToParent;

    /**
     * Whether there is an odd number of ancestors of this with isFlippedRelativeToParent == true, including this.  This
     * is unspecified prior to computing it is PlanarEmbedding.embedding.
     */
    public boolean isFlippedOverall;

    public PlanarVertex(Vertex vertex, PlanarVertex parent, int index) {
        this.vertex = vertex;
        this.parent = parent;
        this.index = index;
    }

    /**
     * Returns one of the two edges that starts at this vertex and is on the external face of some biconnected
     * component - the one we identify with the specified color.  In the case of a biconnected component with one edge,
     * the black and white links are the same.  The links are unspecified if this vertex is not on the external face of
     * some biconnected component.
     * @param isBlack Whether to return the black link.
     */
    public HalfEdge link(boolean isBlack) {
        return isBlack ? blackLink : whiteLink;
    }

    /**
     * Sets one of the two edges that starts at this vertex and is on the external face of some biconnected component -
     * the one we identify with the specified color.  In the case of a biconnected component with one edge, the black
     * and white links are the same.  The links are unspecified if this vertex is not on the external face of some
     * biconnected component.
     * @param isBlack Whether to set the black link.
     * @param link The edge to which to set the link.
     */
    public void setLink(boolean isBlack, HalfEdge link) {
        if (isBlack) {
            blackLink = link;
        } else {
            whiteLink = link;
        }
    }
}
