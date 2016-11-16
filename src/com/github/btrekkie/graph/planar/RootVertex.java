package com.github.btrekkie.graph.planar;

/**
 * Represents the presence of a cut vertex in a biconnected component containing an edge from the vertex to one of its
 * children in the depth-first search tree.  (After merging the biconnected component into its parent, we retain
 * references to the RootVertex object as a historical record, even though the vertex is no longer a cut vertex.)  See
 * the comments for the implementation of PlanarEmbedding.
 */
class RootVertex {
    /** The PlanarVertex object for the cut vertex. */
    public final PlanarVertex vertex;

    /**
     * The child of "vertex" for which the biconnected component rooted at this contains an edge from "vertex" to the
     * child.
     */
    public final PlanarVertex child;

    /**
     * The root vertex after this in the list of the roots of the biconnected components that contain "vertex", but not
     * vertex.parent.  If vertex.parent is null, the list consists of all biconnected components that contain "vertex".
     * Initially, the list is unsorted, but early in the process of computing a planar embedding, we sort it in
     * ascending order of RootVertex.child.lowpoint.index.
     */
    public RootVertex prev;

    /**
     * The root vertex before this in the list of the roots of the biconnected components that contain "vertex", but not
     * vertex.parent.  If vertex.parent is null, the list consists of all biconnected components that contain "vertex".
     * Initially, the list is unsorted, but early in the process of computing a planar embedding, we sort it in
     * ascending order of RootVertex.child.lowpoint.index.
     */
    public RootVertex next;

    /**
     * The root vertex after this in the list of the pertinent root vertices with the same "vertex" field, as defined in
     * the paper, at the current point of computation.  The list is ordered with all of the roots of internally active
     * vertices appearing before all of the roots of externally active root components.
     */
    public RootVertex prevPertinent;

    /**
     * The root vertex before this in the list of the pertinent root vertices with the same "vertex" field, as defined
     * in the paper, at the current point of computation.  The list is ordered with all of the roots of internally
     * active vertices appearing before all of the roots of externally active root components.
     */
    public RootVertex nextPertinent;

    /**
     * One of the two edges that starts at this vertex and is on the external face of the biconnected component rooted
     * at this.  Of the two edges, we identify blackLink as being the one colored black.  In the case of a biconnected
     * component with one edge, the black and white links are the same.  The links are unspecified if there is no
     * biconnected component rooted at this.
     */
    private HalfEdge blackLink;

    /**
     * One of the two edges that starts at this vertex and is on the external face of the biconnected component rooted
     * at this.  Of the two edges, we identify whiteLink as being the one colored white.  In the case of a biconnected
     * component with one edge, the black and white links are the same.  The links are unspecified if there is no
     * biconnected component rooted at this.
     */
    private HalfEdge whiteLink;

    /**
     * Whether link(false) was in the same direction (clockwiseness) as r.link(false) was prior to merging the
     * biconnected component rooted at this into its parent biconnected component rooted at the RootVertex "r".  This is
     * false if we have not merged the biconnected component rooted at this.  This is unspecified if the biconnected
     * component rooted at this is not presently on the outside of the embedding.
     */
    public boolean isFlippedRelativeToVertex;

    public RootVertex(PlanarVertex vertex, PlanarVertex child) {
        this.vertex = vertex;
        this.child = child;
    }

    /**
     * Returns one of the two edges that starts at this vertex and is on the external face of the biconnected component
     * rooted at this - the one we identify with the specified color.  In the case of a biconnected component with one
     * edge, the black and white links are the same.  The links are unspecified if there is no biconnected component
     * rooted at this.
     * @param isBlack Whether to return the black link.
     */
    public HalfEdge link(boolean isBlack) {
        return isBlack ? blackLink : whiteLink;
    }

    /**
     * Sets one of the two edges that starts at this vertex and is on the external face of the biconnected component
     * rooted at this - the one we identify with the specified color.  In the case of a biconnected component with one
     * edge, the black and white links are the same.  The links are unspecified if there is no biconnected component
     * rooted at this.
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
