package com.github.btrekkie.graph.planar;

import java.util.List;

/** A face in a planar drawing.  See the comments for the implementation of PlanarEmbedding. */
class PlanarFace {
    /**
     * The edges that comprise the face, consisting of the edge from the first vertex to the second vertex, the edge
     * from the second vertex to the third vertex, and so on, ending with the first vertex.  This traversal is in the
     * same direction (clockwiseness) as rootVertex.link(isFlippedRelativeToRoot) was prior to merging the biconnected
     * component containing rootVertex into its parent biconnected component, or at present if we have not merged the
     * biconnected component containing rootVertex.  (If rootVertex is null, we use an arbitrary root vertex for the
     * root of the depth-first search tree to determine the direction.)
     */
    public List<HalfEdge> edges;

    /**
     * The RootVertex of the biconnected component to which the face belonged when we added the edge that created the
     * face.  This is null if this is the external face of the entire graph.
     */
    private final RootVertex rootVertex;

    /** Whether the direction of the edges is "flipped" as described in the comments for "edges". */
    private final boolean isFlippedRelativeToRoot;

    private PlanarFace(List<HalfEdge> edges, RootVertex rootVertex, boolean isFlippedRelativeToRoot) {
        this.edges = edges;
        this.rootVertex = rootVertex;
        this.isFlippedRelativeToRoot = isFlippedRelativeToRoot;
    }

    /** Returns a new internal face.*/
    public static PlanarFace createInternal(
            List<HalfEdge> edges, RootVertex rootVertex, boolean isFlippedRelativeToRoot) {
        return new PlanarFace(edges, rootVertex, isFlippedRelativeToRoot);
    }

    /** Returns a new external face.*/
    public static PlanarFace createExternal(List<HalfEdge> edges) {
        return new PlanarFace(edges, null, false);
    }

    /**
     * Returns whether the order of the traversal given by "edges" is in the opposite direction (clockwiseness) of that
     * of trv.link(false), where "trv" is the RootVertex for the root of the depth-first search tree that is an ancestor
     * of rootVertex.vertex.  In the case of the external face of the entire graph, this returns false.
     */
    public boolean isFlippedOverall() {
        if (rootVertex != null) {
            return (rootVertex.isFlippedRelativeToVertex != rootVertex.vertex.isFlippedOverall) !=
                isFlippedRelativeToRoot;
        } else {
            return isFlippedRelativeToRoot;
        }
    }

    /**
     * Returns whether this is an internal face, i.e. a face other than the "face" that surrounds the entire planar
     * drawing.  (If the graph is a cycle, it has an internal face for the interior area of the polygon and an identical
     * external face for the exterior area.)
     */
    public boolean isInternal() {
        return rootVertex != null;
    }
}
