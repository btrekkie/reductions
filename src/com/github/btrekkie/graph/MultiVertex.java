package com.github.btrekkie.graph;

import java.util.ArrayList;
import java.util.Collection;

/** A vertex in a MultiGraph. */
public class MultiVertex {
    /**
     * The vertices that are adjacent to this.  This will contain the same vertex multiple times in the case of a
     * repeated edge.
     */
    public Collection<MultiVertex> edges = new ArrayList<MultiVertex>();

    /** Equivalent implementation is contractual. */
    public void addEdge(MultiVertex other) {
        edges.add(other);
        other.edges.add(this);
    }
}
