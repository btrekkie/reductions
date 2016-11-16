package com.github.btrekkie.graph.bc;

import java.util.ArrayList;
import java.util.Collection;

import com.github.btrekkie.graph.Vertex;

/** A cut node in a block-cut tree.  See the comments for BlockNode. */
public class CutNode {
    /** The parent of this node, if any. */
    public final BlockNode parent;

    /** The children of this node. */
    public Collection<BlockNode> children = new ArrayList<BlockNode>();

    /** The cut vertex for this node. */
    public final Vertex vertex;

    /** Constructs a new BlockNode and adds it to parent.children. */
    public CutNode(BlockNode parent, Vertex vertex) {
        this.parent = parent;
        this.vertex = vertex;
        if (parent != null) {
            parent.children.add(this);
        }
    }
}
