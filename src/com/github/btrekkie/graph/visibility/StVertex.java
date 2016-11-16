package com.github.btrekkie.graph.visibility;

import com.github.btrekkie.graph.Vertex;

/**
 * A representation of a Vertex for computing a topological ordering of an st-orienation.  See the comments for
 * VisibilityRepresentation.stOrdering and for its implementation.
 */
class StVertex {
    /** The corresponding Vertex in the graph for which we are computing a topological ordering of an st-orienation. */
    public final Vertex vertex;

    /** The parent in the depth-first search tree, if any. */
    public StVertex parent;

    /** The depth in the depth-first search tree. */
    public final int depth;

    /**
     * The lowpoint of the vertex: the highest ancestor that can be reached by following zero or more tree edges
     * followed by one or zero back edges.
     */
    public StVertex lowpoint;

    /** The previous vertex in the list of the vertices in the topological ordering of an st-orienation. */
    public StVertex prev;

    /** The next vertex in the list of the vertices in the topological ordering of an st-orienation. */
    public StVertex next;

    /** Whether the vertex is "inverted" - whether its sign is -1. */
    public boolean isInverted;

    public StVertex(Vertex vertex, StVertex parent, int depth) {
        this.parent = parent;
        this.vertex = vertex;
        this.depth = depth;
    }
}
