package com.github.btrekkie.graph.spqr;

import com.github.btrekkie.graph.Vertex;

/** A vertex in a palm forest, for computing an SPQR tree.  See the comments for the implementation of SpqrNode. */
class PalmVertex {
    /** The corresponding vertex in the original Graph. */
    public final Vertex vertex;

    /**
     * One more than the number of vertices we visited before this in the initial depth-first search performed on the
     * graph.  Contrast with "number".
     */
    public final int dfsNumber;

    /** The parent vertex in the palm forest, if any. */
    public PalmVertex parent;

    /** The edge from "parent" to this, if "parent" is non-null. */
    public PalmEdge parentEdge;

    /**
     * The first edge in the linked list of outgoing edges, if any.  Initially, the list is in depth-first search order,
     * but later it is in ascending order of phi(e).
     */
    public PalmEdge edgesHead;

    /**
     * The last edge in the linked list of outgoing edges, if any.  Initially, the list is in depth-first search order,
     * but later it is in ascending order of phi(e).
     */
    public PalmEdge edgesTail;

    /** The first edge in the linked list of incoming fronds in visit order, if any. */
    public PalmEdge frondsHead;

    /** The last edge in the linked list of incoming fronds in visit order, if any. */
    public PalmEdge frondsTail;

    /** The number of incoming or outgoing edges. */
    public int degree;

    /** The number of vertices in the subtree rooted at this vertex in the original palm tree. */
    public int descendantCount = 1;

    /** The number of this vertex, according to a numbering scheme satisfying properties P1, P2, and P3 in the paper. */
    public int number;

    /** The lowpoint of this vertex, as defined in the paper.  We initialize lowpoint1 to "this". */
    public PalmVertex lowpoint1;

    /** The second lowest point of this vertex, as defined in the paper.  We initialize lowpoint2 to "this". */
    public PalmVertex lowpoint2;

    public PalmVertex(Vertex vertex, PalmVertex parent, int dfsNumber, int degree) {
        this.vertex = vertex;
        this.parent = parent;
        this.dfsNumber = dfsNumber;
        this.degree = degree;
        lowpoint1 = this;
        lowpoint2 = this;
    }
}
