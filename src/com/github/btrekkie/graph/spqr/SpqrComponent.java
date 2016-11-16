package com.github.btrekkie.graph.spqr;

/**
 * A component of the SPQR tree.  See the comments for the implementation of SpqrNode.  Initially, the components have
 * small graphs, but then we assemble them to form the larger skeleton graphs.
 */
class SpqrComponent {
    /** The first edge in the linked list of edges in the component's graph, if any. */
    public SpqrEdge head;

    /** The last edge in the linked list of edges in the component's graph, if any. */
    public SpqrEdge tail;

    /**
     * The node type for the component's graph.  This is null prior to computing it.  It is unspecified if the component
     * is empty (i.e. head == null).
     */
    public SpqrNode.Type type;

    /** Adds the specified edge to this component's graph. */
    private void addEdge(SpqrEdge edge) {
        edge.prev = tail;
        if (head == null) {
            head = edge;
        } else {
            tail.next = edge;
        }
        tail = edge;
    }

    /** Adds the specified edge to this component's graph. */
    public void addEdge(PalmEdge palmEdge) {
        addEdge(SpqrEdge.create(palmEdge));
    }

    /** Adds a virtual edge with the specified endpoints to this component's graph. */
    public void addVirtualEdge(PalmVertex vertex1, PalmVertex vertex2) {
        addEdge(SpqrEdge.createVirtual(vertex1, vertex2));
    }
}
