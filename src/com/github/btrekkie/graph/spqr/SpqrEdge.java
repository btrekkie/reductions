package com.github.btrekkie.graph.spqr;

/** An edge in an SpqrComponent's graph.  See SpqrComponent. */
class SpqrEdge {
    /** The edge's first endpoint. */
    public final PalmVertex vertex1;

    /** The edge's second endpoint. */
    public final PalmVertex vertex2;

    public final boolean isVirtual;

    /** The edge before this in the linked list of edges in the component's graph, if any. */
    public SpqrEdge prev;

    /** The edge after this in the linked list of edges in the component's graph, if any. */
    public SpqrEdge next;

    private SpqrEdge(PalmVertex vertex1, PalmVertex vertex2, boolean isVirtual) {
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        this.isVirtual = isVirtual;
    }

    /** Returns a new SpqrEdge representation of the specified edge. */
    public static SpqrEdge create(PalmEdge edge) {
        return new SpqrEdge(edge.start, edge.end, edge.isVirtual);
    }

    /** Returns a new virtual edge with the specified endpoints. */
    public static SpqrEdge createVirtual(PalmVertex vertex1, PalmVertex vertex2) {
        return new SpqrEdge(vertex1, vertex2, true);
    }
}
