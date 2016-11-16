package com.github.btrekkie.graph.bc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.Vertex;

/**
 * A block node in a block-cut tree.  A block-cut tree decomposes a connected graph into its blocks (or biconnected
 * components) - the maximal subgraphs that remain connected when removing any one vertex.  See the picture at the top
 * of https://en.wikipedia.org/wiki/Biconnected_component .  A block-cut tree has two types of nodes: block nodes and
 * cut nodes.  The block nodes correspond to the graph's blocks, while the cut nodes correspond to its cut vertices -
 * the vertices whose removal would disconnect the graph.  There is an edge from a block node to a cut node if the block
 * contains the cut vertex.
 */
public class BlockNode {
    /** The parent of this node, if any. */
    public final CutNode parent;

    /** The children of this node. */
    public Collection<CutNode> children = new ArrayList<CutNode>();

    /** The block graph for this node.  This uses different Vertex objects from the original graph. */
    public final Graph block;

    /** A map from the vertices in "block" to the corresponding Vertex objects in the original graph. */
    public Map<Vertex, Vertex> blockVertexToVertex;

    /** Constructs a new BlockNode and adds it to parent.children. */
    public BlockNode(CutNode parent, Graph block, Map<Vertex, Vertex> blockVertexToVertex) {
        this.parent = parent;
        this.block = block;
        this.blockVertexToVertex = blockVertexToVertex;
        if (parent != null) {
            parent.children.add(this);
        }
    }

    /**
     * Creates and returns the BlockNodes containing edges from "start" to its children but not from "start" to its
     * parent, relative to the specified depth-first tree.  This also creates the CutNode children of the BlockNodes,
     * but not the children of those CutNodes.  Assumes that "start" is a cut vertex or is the root of the depth-first
     * search tree.
     * @param parent The parent node of the BlockNodes.  If this is null, there is special behavior where the parent of
     *     one of the BlockNodes is null, while the grandparent of the remaining BlockNodes is that root node.
     * @param start The starting vertex.
     * @param children A map from each vertex to its children in the depth-first search tree.
     * @param backEdges A map from each vertex to its back edges - the ancestors in the depth-first search tree other
     *     than the parent to which there is an edge.
     * @param depths A map from each vertex to its depth in the depth-first search tree.
     * @param lowpoints A map from each vertex to the depth of its lowpoint in the depth-first search tree.  See
     *     https://en.wikipedia.org/wiki/Biconnected_component for the definition of the term "lowpoint".
     * @return The BlockNodes we created.
     */
    private static Collection<BlockNode> blockNodes(
            CutNode parent, Vertex start, Map<Vertex, Collection<Vertex>> children,
            Map<Vertex, Collection<Vertex>> backEdges, Map<Vertex, Integer> depths, Map<Vertex, Integer> lowpoints) {
        Collection<BlockNode> blockNodes = new ArrayList<BlockNode>();
        for (Vertex startChild : children.get(start)) {
            if (lowpoints.get(startChild) < depths.get(start)) {
                continue;
            }

            // Use breadth-first search to find the vertices in the block
            Graph block = new Graph();
            Map<Vertex, Vertex> vertexToBlockVertex = new HashMap<Vertex, Vertex>();
            vertexToBlockVertex.put(start, block.createVertex());
            vertexToBlockVertex.put(startChild, block.createVertex());
            vertexToBlockVertex.get(start).addEdge(vertexToBlockVertex.get(startChild));
            Collection<Vertex> level = Collections.singleton(startChild);
            Collection<Vertex> cutVertices = new ArrayList<Vertex>();
            while (!level.isEmpty()) {
                Collection<Vertex> nextLevel = new ArrayList<Vertex>();
                for (Vertex vertex : level) {
                    Vertex blockVertex = vertexToBlockVertex.get(vertex);
                    for (Vertex backEdge : backEdges.get(vertex)) {
                        blockVertex.addEdge(vertexToBlockVertex.get(backEdge));
                    }

                    boolean isCutVertex = false;
                    for (Vertex child : children.get(vertex)) {
                        if (lowpoints.get(child) >= depths.get(vertex)) {
                            isCutVertex = true;
                        } else {
                            Vertex blockChild = block.createVertex();
                            blockVertex.addEdge(blockChild);
                            vertexToBlockVertex.put(child, blockChild);
                            nextLevel.add(child);
                        }
                    }
                    if (isCutVertex) {
                        cutVertices.add(vertex);
                    }
                }
                level = nextLevel;
            }

            // Create the BlockNode and its children
            Map<Vertex, Vertex> blockVertexToVertex = new HashMap<Vertex, Vertex>();
            for (Entry<Vertex, Vertex> entry : vertexToBlockVertex.entrySet()) {
                blockVertexToVertex.put(entry.getValue(), entry.getKey());
            }
            BlockNode blockNode = new BlockNode(parent, block, blockVertexToVertex);
            for (Vertex vertex : cutVertices) {
                new CutNode(blockNode, vertex);
            }

            if (parent == null && children.get(start).size() > 1) {
                // Special handling for null parent
                parent = new CutNode(blockNode, start);
            }
            blockNodes.add(blockNode);
        }
        return blockNodes;
    }

    /**
     * Returns the root of a block-cut tree for the connected component containing the specified vertex.  The choice of
     * a root node is arbitrary.
     */
    public static BlockNode compute(Vertex root) {
        if (root.edges.isEmpty()) {
            Graph block = new Graph();
            Vertex blockVertex = block.createVertex();
            Map<Vertex, Vertex> blockVertexToVertex = Collections.singletonMap(root, blockVertex);
            return new BlockNode(null, block, blockVertexToVertex);
        }

        // Use an iterative implementation of depth-first search
        List<Vertex> cutVertices = new ArrayList<Vertex>();
        Map<Vertex, Integer> depths = new HashMap<Vertex, Integer>();
        Map<Vertex, Integer> lowpoints = new HashMap<Vertex, Integer>();
        List<Vertex> path = new ArrayList<Vertex>();
        List<Iterator<Vertex>> pathIters = new ArrayList<Iterator<Vertex>>();
        Map<Vertex, Collection<Vertex>> children = new HashMap<Vertex, Collection<Vertex>>();
        Map<Vertex, Collection<Vertex>> backEdges = new HashMap<Vertex, Collection<Vertex>>();
        path.add(root);
        pathIters.add(root.edges.iterator());
        int depth = 0;
        depths.put(root, 0);
        children.put(root, new ArrayList<Vertex>());
        while (!path.isEmpty()) {
            if (!pathIters.get(pathIters.size() - 1).hasNext()) {
                pathIters.remove(pathIters.size() - 1);
                Vertex vertex = path.remove(path.size() - 1);

                // Compute the lowpoint
                int lowpoint = depth;
                for (Vertex adjVertex : vertex.edges) {
                    int adjDepth = depths.get(adjVertex);
                    if (adjDepth != depth - 1 && adjDepth < lowpoint) {
                        lowpoint = adjDepth;
                    }
                }
                Collection<Vertex> curChildren = children.get(vertex);
                for (Vertex child : curChildren) {
                    int childLowpoint = lowpoints.get(child);
                    if (childLowpoint < lowpoint) {
                        lowpoint = childLowpoint;
                    }
                }
                lowpoints.put(vertex, lowpoint);

                if (!path.isEmpty()) {
                    // Check whether "vertex" is a cut vertex
                    boolean isCutVertex = false;
                    for (Vertex child : curChildren) {
                        if (lowpoints.get(child) >= depth) {
                            isCutVertex = true;
                            break;
                        }
                    }
                    if (isCutVertex) {
                        cutVertices.add(vertex);
                    }
                }

                depth--;
            } else {
                Vertex start = path.get(path.size() - 1);
                Vertex end = pathIters.get(pathIters.size() - 1).next();
                if (!children.containsKey(end)) {
                    // Tree edge
                    children.get(start).add(end);
                    path.add(end);
                    pathIters.add(end.edges.iterator());
                    depth++;
                    depths.put(end, depth);
                    children.put(end, new ArrayList<Vertex>());
                    backEdges.put(end, new ArrayList<Vertex>());
                } else if (depths.get(end) < depth - 1) {
                    backEdges.get(start).add(end);
                }
            }
        }

        // Create the BlockNodes containing "root"
        Collection<BlockNode> blockNodes = blockNodes(null, root, children, backEdges, depths, lowpoints);
        BlockNode rootNode = null;
        Map<Vertex, CutNode> cutNodes = new HashMap<Vertex, CutNode>();
        for (BlockNode node : blockNodes) {
            if (node.parent == null) {
                rootNode = node;
            }
            for (CutNode child : node.children) {
                cutNodes.put(child.vertex, child);
            }
        }

        // Create the remaining BlockNodes, by creating the children of the cut vertices in topological order (visiting
        // parent cut vertices before child cut vertices)
        Collections.reverse(cutVertices);
        for (Vertex cutVertex : cutVertices) {
            blockNodes = blockNodes(cutNodes.get(cutVertex), cutVertex, children, backEdges, depths, lowpoints);
            for (BlockNode node : blockNodes) {
                for (CutNode child : node.children) {
                    cutNodes.put(child.vertex, child);
                }
            }
        }
        return rootNode;
    }
}
