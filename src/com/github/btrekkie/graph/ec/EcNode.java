package com.github.btrekkie.graph.ec;

import java.util.ArrayList;
import java.util.List;

import com.github.btrekkie.graph.Vertex;

/**
 * A node in a constraint tree for ec-planar embedding.  This expresses constraints on the clockwise ordering of some or
 * all of the vertices adjacent to some source vertex in a planar embedding.  The leaves of a constraint tree are vertex
 * nodes.  In order for a constraint to be satisfied, it must be possible to order the children of each node in a way
 * that satisfies the node's type so that when we read off the leaf nodes in order, this matches the clockwise ordering
 * of the vertices around the source vertex.  There are four types of EcNodes:
 *
 * GROUP: The children may be permuted in any order.
 * ORIENTED: The children must appear in the given order.
 * MIRROR: The children may appear in the given order or in the reverse of the given order.
 * VERTEX: A leaf node.
 *
 * For example, say we have a vertex V with four adjacent vertices A, B, C, D.  Suppose the constraint tree consists of
 * a GROUP node with two children: an ORIENTED node with A and B as children, in that order, and an GROUP node with C
 * and D as children.  There are two possible clockwise orderings of vertices around V: A, B, C, D and A, B, D, C.  It
 * is also possible to read off the vertices in the order C, D, A, B or D, C, A, B, but these are equivalent to the
 * other orderings.
 */
public class EcNode {
    /** A type of node. */
    public static enum Type {
        /** A constraint permitting the children to be permuted in any order. */
        GROUP,

        /** A constraint requiring the children to appear in the given order. */
        ORIENTED,

        /** A constraint permitting the children to appear in the given order or in the reverse of the given order. */
        MIRROR,

        /** A leaf node. */
        VERTEX};

    /** The parent of this node, if any. */
    public final EcNode parent;

    public final Type type;

    /** The children of this node.  This is empty iff this is a VERTEX node. */
    public List<EcNode> children = new ArrayList<EcNode>();

    /** The vertex of this node, if any.  This is null iff this is not a VERTEX node. */
    public final Vertex vertex;

    /** Constructs a new EcNode and adds it to parent.children. */
    private EcNode(EcNode parent, Type type, Vertex vertex) {
        this.parent = parent;
        this.type = type;
        this.vertex = vertex;
        if (parent != null) {
            parent.children.add(this);
        }
    }

    /** Returns a new non-leaf node. */
    public static EcNode create(EcNode parent, Type type) {
        return new EcNode(parent, type, null);
    }

    /** Returns a new leaf node. */
    public static EcNode createVertex(EcNode parent, Vertex vertex) {
        return new EcNode(parent, Type.VERTEX, vertex);
    }
}
