package com.github.btrekkie.graph;

import java.util.LinkedHashSet;
import java.util.Set;

/** A vertex in a Graph. */
public class Vertex {
    /** The value of debugId for the next Vertex we construct. */
    public static int nextDebugId = 0;

    /** The vertices that are adjacent to this. */
    public Set<Vertex> edges = new LinkedHashSet<Vertex>();

    /**
     * An integer identifying the vertex for debugging purposes.  IDs are assigned sequentially, until the client alters
     * nextDebugId.
     */
    public int debugId;

    public Vertex() {
        debugId = nextDebugId;
        nextDebugId++;
    }

    /** Equivalent implementation is contractual. */
    public void addEdge(Vertex other) {
        edges.add(other);
        other.edges.add(this);
    }

    /** Equivalent implementation is contractual. */
    public void removeEdge(Vertex other) {
        edges.remove(other);
        other.edges.remove(this);
    }

    @Override
    public String toString() {
        return "[Vertex debugId=" + debugId + ']';
    }
}
