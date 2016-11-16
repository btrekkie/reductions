package com.github.btrekkie.graph.visibility;

import java.util.ArrayList;
import java.util.Collection;

import com.github.btrekkie.graph.Vertex;

/** A Vertex in a VisibilityRepresentation.  See VisibilityRepresentation. */
public class VisibilityVertex {
    /** The Vertex in the Graph from which we produced the VisibilityRepresentation. */
    public final Vertex vertex;

    /** The y coordinate of the vertex. */
    public final int y;

    /** The x coordinate of the left endpoint of the vertex. */
    public final int minX;

    /** The x coordinate of the right endpoint of the vertex. */
    public final int maxX;

    /** The edges adjacent to the vertex. */
    public Collection<VisibilityEdge> edges = new ArrayList<VisibilityEdge>();

    public VisibilityVertex(Vertex vertex, int y, int minX, int maxX) {
        this.vertex = vertex;
        this.y = y;
        this.minX = minX;
        this.maxX = maxX;
    }
}
