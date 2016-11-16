package com.github.btrekkie.graph.spqr;

/** An edge in a palm forest, for computing an SPQR tree.  See the comments for the implementation of SpqrNode. */
class PalmEdge {
    /** The edge's source vertex. */
    public final PalmVertex start;

    /** The edge's destination vertex. */
    public final PalmVertex end;

    public final boolean isFrond;

    /** Whether the edge is a virtual edge for a skeleton in the SPQR tree. */
    public boolean isVirtual;

    /**
     * The edge after this in the linked list of outgoing edges from "start", if any.  Initially, the list is in
     * depth-first search order, but later it is in ascending order of phi(e).
     */
    public PalmEdge next;

    /**
     * The edge before this in the linked list of outgoing edges from "start", if any.  Initially, the list is in
     * depth-first search order, but later it is in ascending order of phi(e).
     */
    public PalmEdge prev;

    /** The edge after this in the linked list of incoming edges to "end" in visit order, if any. */
    public PalmEdge nextFrond;

    /** The edge before this in the linked list of incoming edges to "end" in visit order, if any. */
    public PalmEdge prevFrond;

    /**
     * Whether this is the first edge in a path formed by performing a depth-first search on the original palm tree,
     * with adjacency lists ordered according to phi(e), and terminating each path when we reach the first frond.
     */
    public boolean isStart;

    private PalmEdge(PalmVertex start, PalmVertex end, boolean isFrond, boolean isVirtual) {
        this.start = start;
        this.end = end;
        this.isFrond = isFrond;
        this.isVirtual = isVirtual;
    }

    /** Returns a new non-frond edge from "start" to "end" for which isVirtual is false. */
    public static PalmEdge createRealTreeEdge(PalmVertex start, PalmVertex end) {
        return new PalmEdge(start, end, false, false);
    }

    /** Returns a new frond from "start" to "end" for which isVirtual is false. */
    public static PalmEdge createRealFrond(PalmVertex start, PalmVertex end) {
        return new PalmEdge(start, end, true, false);
    }

    /** Returns a new non-frond edge from "start" to "end" for which isVirtual is true. */
    public static PalmEdge createVirtualTreeEdge(PalmVertex start, PalmVertex end) {
        return new PalmEdge(start, end, false, true);
    }

    /** Returns a new frond from "start" to "end" for which isVirtual is true. */
    public static PalmEdge createVirtualFrond(PalmVertex start, PalmVertex end) {
        return new PalmEdge(start, end, true, true);
    }
}
