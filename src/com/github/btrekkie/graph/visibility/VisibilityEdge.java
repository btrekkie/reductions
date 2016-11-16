package com.github.btrekkie.graph.visibility;

/** An edge in a VisibilityRepresentation.  See VisibilityRepresentation. */
public class VisibilityEdge {
    /** The first endpoint of the edge. */
    public final VisibilityVertex vertex1;

    /** The second endpoint of the edge. */
    public final VisibilityVertex vertex2;

    /** The x coordinate of the edge. */
    public final int x;

    public VisibilityEdge(VisibilityVertex vertex1, VisibilityVertex vertex2, int x) {
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        this.x = x;
    }

    /** Returns the endpoint of this other than "vertex".  Assumes that "vertex" is an endpoint of this. */
    public VisibilityVertex adjVertex(VisibilityVertex vertex) {
        if (vertex == vertex1) {
            return vertex2;
        } else if (vertex == vertex2) {
            return vertex1;
        } else {
            throw new IllegalArgumentException("The specified vertex is not an endpoint of this");
        }
    }
}
