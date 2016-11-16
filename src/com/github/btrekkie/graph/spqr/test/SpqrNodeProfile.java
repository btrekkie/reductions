package com.github.btrekkie.graph.spqr.test;

import com.github.btrekkie.graph.spqr.SpqrNode;

/**
 * A summary description of an SpqrNode that typically allows us to quickly distinguish differing pairs of SpqrNodes.
 * Two SpqrNodeProfiles are equal if the subtrees rooted at their SpqrNodes are equivalent.  They are typically unequal
 * if the subtrees are different, but not necessarily.
 */
class SpqrNodeProfile {
    /** The type of the node. */
    private final SpqrNode.Type type;

    /** The number of children of the node. */
    private final int childCount;

    /** The number of vertices in the node's skeleton. */
    private final int skeletonVertexCount;

    /** The number of real edges in the node's skeleton. */
    private final int realEdgeCount;

    /** Constructs a new SpqrNodeProfile for the specified node. */
    public SpqrNodeProfile(SpqrNode node) {
        this.type = node.type;
        this.childCount = node.children.size();
        this.skeletonVertexCount = node.skeleton.vertices.size();
        this.realEdgeCount = node.realEdges.size();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SpqrNodeProfile)) {
            return false;
        }
        SpqrNodeProfile profile = (SpqrNodeProfile)obj;
        return type == profile.type && childCount == profile.childCount &&
            skeletonVertexCount == profile.skeletonVertexCount && realEdgeCount == profile.realEdgeCount;
    }

    @Override
    public int hashCode() {
        return type.hashCode() + 31 * childCount + 41 * skeletonVertexCount + 61 * realEdgeCount;
    }
}
