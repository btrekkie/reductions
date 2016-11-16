package com.github.btrekkie.graph;

import java.util.LinkedHashSet;
import java.util.Set;

/** An undirected graph that permits multiple edges between a pair of vertices.  Self loops are not permitted. */
public class MultiGraph {
    /** The vertices in the graph. */
    public Set<MultiVertex> vertices = new LinkedHashSet<MultiVertex>();

    /** Equivalent implementation is contractual. */
    public MultiVertex createVertex() {
        MultiVertex vertex = new MultiVertex();
        vertices.add(vertex);
        return vertex;
    }
}
