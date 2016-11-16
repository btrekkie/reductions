package com.github.btrekkie.graph.bc.test;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.bc.BlockNode;
import com.github.btrekkie.graph.bc.CutNode;
import com.github.btrekkie.util.UnorderedPair;

public class BlockNodeTest {
    /**
     * Returns the vertex "v" in blockVertices with the minimum value vertexIds.get(blockVertexToVertex.get(v)).
     * Assumes that the relevant map entries are present.
     */
    private Vertex minVertex(
            Collection<Vertex> blockVertices, Map<Vertex, Vertex> blockVertexToVertex, Map<Vertex, Integer> vertexIds) {
        Vertex blockVertex = null;
        int vertexId = Integer.MAX_VALUE;
        for (Vertex curBlockVertex : blockVertices) {
            Vertex curVertex = blockVertexToVertex.get(curBlockVertex);
            int curVertexId = vertexIds.get(curVertex);
            if (curVertexId <= vertexId) {
                blockVertex = curBlockVertex;
                vertexId = curVertexId;
            }
        }
        return blockVertex;
    }

    /**
     * Adds a mapping from an edge identifying each block in the subtree rooted at "node" to the node to edgeToNode.
     * The identifying edge is an edge from the vertex with the minimum ID to the adjacent vertex with the minimum ID.
     * We represent an edge as a pair of its endpoints.
     * @param node The node.
     * @param edgeToNode The map to which to add the mappings.
     * @param vertexIds A map from vertices in the original graph (i.e. the values of node.blockVertexToVertex) to
     *     unique IDs in the range [0, vertexIds.size()).  This method adds entries for any vertices in the block that
     *     we have not yet assigned an ID.
     */
    private void identifyBlockNodes(
            BlockNode node, Map<UnorderedPair<Vertex>, BlockNode> edgeToNode, Map<Vertex, Integer> vertexIds) {
        // Make sure each vertex has an ID
        Map<Vertex, Vertex> blockVertexToVertex = node.blockVertexToVertex;
        for (Vertex vertex : blockVertexToVertex.values()) {
            if (!vertexIds.containsKey(vertex)) {
                vertexIds.put(vertex, vertexIds.size());
            }
        }

        // Determine the identifying edge
        Vertex vertex = minVertex(node.block.vertices, blockVertexToVertex, vertexIds);
        Vertex adjVertex = minVertex(vertex.edges, blockVertexToVertex, vertexIds);
        UnorderedPair<Vertex> edge = new UnorderedPair<Vertex>(
            blockVertexToVertex.get(vertex), blockVertexToVertex.get(adjVertex));
        edgeToNode.put(edge, node);

        // Recurse on the grandchildren
        for (CutNode child : node.children) {
            for (BlockNode grandchild : child.children) {
                identifyBlockNodes(grandchild, edgeToNode, vertexIds);
            }
        }
    }

    /**
     * Adds a mapping from each BlockNode in the subtree rooted at "node" to the cut vertices in the CutNodes adjacent
     * to the specified BlockNodes to adjCutVertices.
     */
    private void adjCutVertices(BlockNode node, Map<BlockNode, Set<Vertex>> adjCutVertices) {
        // Compute the adjacent cut vertices
        Set<Vertex> cutVertices = new HashSet<Vertex>();
        if (node.parent != null) {
            cutVertices.add(node.blockVertexToVertex.get(node.parent.vertex));
        }
        for (CutNode child : node.children) {
            cutVertices.add(node.blockVertexToVertex.get(child.vertex));
        }
        adjCutVertices.put(node, cutVertices);

        // Recurse on the grandchildren
        for (CutNode child : node.children) {
            for (BlockNode grandchild : child.children) {
                adjCutVertices(grandchild, adjCutVertices);
            }
        }
    }

    /**
     * Returns the edges in the specified node's block.  We represent an edge as a pair of its vertices, using the
     * Vertex objects from the original graph (i.e. the values of node.blockVertexToVertex).
     */
    private Set<UnorderedPair<Vertex>> edges(BlockNode node) {
        Set<UnorderedPair<Vertex>> edges = new HashSet<UnorderedPair<Vertex>>();
        for (Vertex vertex : node.block.vertices) {
            for (Vertex adjVertex : vertex.edges) {
                edges.add(
                    new UnorderedPair<Vertex>(
                        node.blockVertexToVertex.get(vertex), node.blockVertexToVertex.get(adjVertex)));
            }
        }
        return edges;
    }

    /** Returns whether the block graphs in the specified nodes are the same. */
    private boolean areGraphsEquivalent(BlockNode node1, BlockNode node2) {
        return edges(node1).equals(edges(node2));
    }

    /**
     * Returns whether the trees rooted at "node1" and "node2" are the same, apart from the choice of root node.
     * Assumes that "node1" and "node2" are the roots of their respective trees.
     */
    private boolean areEquivalent(BlockNode node1, BlockNode node2) {
        if (node1.children.isEmpty()) {
            return node2.children.isEmpty() && areGraphsEquivalent(node1, node2);
        }

        Map<UnorderedPair<Vertex>, BlockNode> edgeToNode1 = new HashMap<UnorderedPair<Vertex>, BlockNode>();
        Map<UnorderedPair<Vertex>, BlockNode> edgeToNode2 = new HashMap<UnorderedPair<Vertex>, BlockNode>();
        Map<Vertex, Integer> vertexIds = new HashMap<Vertex, Integer>();
        identifyBlockNodes(node1, edgeToNode1, vertexIds);
        identifyBlockNodes(node2, edgeToNode2, vertexIds);
        Map<BlockNode, Set<Vertex>> adjCutVertices1 = new HashMap<BlockNode, Set<Vertex>>();
        Map<BlockNode, Set<Vertex>> adjCutVertices2 = new HashMap<BlockNode, Set<Vertex>>();
        adjCutVertices(node1, adjCutVertices1);
        adjCutVertices(node2, adjCutVertices2);
        if (!edgeToNode1.keySet().equals(edgeToNode2.keySet())) {
            return false;
        }
        for (Entry<UnorderedPair<Vertex>, BlockNode> entry : edgeToNode1.entrySet()) {
            BlockNode child1 = entry.getValue();
            BlockNode child2 = edgeToNode2.get(entry.getKey());
            if (!areGraphsEquivalent(child1, child2) ||
                    !adjCutVertices1.get(child1).equals(adjCutVertices2.get(child2))) {
                return false;
            }
        }
        return true;
    }

    /** Tests BlockNode.compute on some simple graphs. */
    @Test
    public void testComputeSimple() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Graph block = new Graph();
        Vertex blockVertex1 = block.createVertex();
        Map<Vertex, Vertex> blockVertexToVertex = Collections.singletonMap(blockVertex1, vertex1);
        BlockNode node = new BlockNode(null, block, blockVertexToVertex);
        assertTrue(areEquivalent(BlockNode.compute(vertex1), node));

        graph = new Graph();
        vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        vertex1.addEdge(vertex2);
        block = new Graph();
        blockVertex1 = block.createVertex();
        Vertex blockVertex2 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex1);
        blockVertexToVertex.put(blockVertex2, vertex2);
        node = new BlockNode(null, block, blockVertexToVertex);
        assertTrue(areEquivalent(BlockNode.compute(vertex1), node));
    }

    /** Tests BlockNode.compute on a graph that consists of a path. */
    @Test
    public void testComputePath() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex2.addEdge(vertex3);
        vertex3.addEdge(vertex4);

        Graph block = new Graph();
        Vertex blockVertex1 = block.createVertex();
        Vertex blockVertex2 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        Map<Vertex, Vertex> blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex1);
        blockVertexToVertex.put(blockVertex2, vertex2);
        BlockNode blockNode1 = new BlockNode(null, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex2);
        blockVertexToVertex.put(blockVertex2, vertex3);
        CutNode cutNode1 = new CutNode(blockNode1, vertex2);
        BlockNode blockNode2 = new BlockNode(cutNode1, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex3);
        blockVertexToVertex.put(blockVertex2, vertex4);
        CutNode cutNode2 = new CutNode(blockNode2, vertex3);
        new BlockNode(cutNode2, block, blockVertexToVertex);
        assertTrue(areEquivalent(BlockNode.compute(vertex1), blockNode1));
    }

    /** Tests BlockNode.compute on a complete graph. */
    @Test
    public void testComputeComplete() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex4);
        vertex3.addEdge(vertex4);

        Graph block = new Graph();
        Vertex blockVertex1 = block.createVertex();
        Vertex blockVertex2 = block.createVertex();
        Vertex blockVertex3 = block.createVertex();
        Vertex blockVertex4 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertex1.addEdge(blockVertex3);
        blockVertex1.addEdge(blockVertex4);
        blockVertex2.addEdge(blockVertex3);
        blockVertex2.addEdge(blockVertex4);
        blockVertex3.addEdge(blockVertex4);
        Map<Vertex, Vertex> blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex1);
        blockVertexToVertex.put(blockVertex2, vertex2);
        blockVertexToVertex.put(blockVertex3, vertex3);
        blockVertexToVertex.put(blockVertex4, vertex4);
        BlockNode node = new BlockNode(null, block, blockVertexToVertex);
        assertTrue(areEquivalent(BlockNode.compute(vertex1), node));
    }

    /**
     * Tests BlockNode.compute on the graph in the image at the top of
     * https://en.wikipedia.org/wiki/Biconnected_component .
     */
    @Test
    public void testComputeWikipedia() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        Vertex vertex6 = graph.createVertex();
        Vertex vertex7 = graph.createVertex();
        Vertex vertex8 = graph.createVertex();
        Vertex vertex9 = graph.createVertex();
        Vertex vertex10 = graph.createVertex();
        Vertex vertex11 = graph.createVertex();
        Vertex vertex12 = graph.createVertex();
        Vertex vertex13 = graph.createVertex();
        Vertex vertex14 = graph.createVertex();
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex2.addEdge(vertex5);
        vertex2.addEdge(vertex6);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex8);
        vertex4.addEdge(vertex7);
        vertex5.addEdge(vertex10);
        vertex6.addEdge(vertex10);
        vertex7.addEdge(vertex9);
        vertex7.addEdge(vertex11);
        vertex7.addEdge(vertex12);
        vertex8.addEdge(vertex12);
        vertex9.addEdge(vertex13);
        vertex10.addEdge(vertex13);
        vertex12.addEdge(vertex14);

        Graph block = new Graph();
        Vertex blockVertex1 = block.createVertex();
        Vertex blockVertex2 = block.createVertex();
        Vertex blockVertex3 = block.createVertex();
        Vertex blockVertex4 = block.createVertex();
        Vertex blockVertex5 = block.createVertex();
        Vertex blockVertex6 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertex1.addEdge(blockVertex3);
        blockVertex2.addEdge(blockVertex3);
        blockVertex2.addEdge(blockVertex5);
        blockVertex3.addEdge(blockVertex4);
        blockVertex4.addEdge(blockVertex6);
        blockVertex5.addEdge(blockVertex6);
        Map<Vertex, Vertex> blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex1);
        blockVertexToVertex.put(blockVertex2, vertex3);
        blockVertexToVertex.put(blockVertex3, vertex4);
        blockVertexToVertex.put(blockVertex4, vertex7);
        blockVertexToVertex.put(blockVertex5, vertex8);
        blockVertexToVertex.put(blockVertex6, vertex12);
        BlockNode blockNode1 = new BlockNode(null, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex7);
        blockVertexToVertex.put(blockVertex2, vertex9);
        CutNode cutNode1 = new CutNode(blockNode1, vertex7);
        BlockNode blockNode2 = new BlockNode(cutNode1, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex9);
        blockVertexToVertex.put(blockVertex2, vertex13);
        CutNode cutNode2 = new CutNode(blockNode2, vertex9);
        BlockNode blockNode3 = new BlockNode(cutNode2, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex10);
        blockVertexToVertex.put(blockVertex2, vertex13);
        CutNode cutNode3 = new CutNode(blockNode3, vertex13);
        BlockNode blockNode4 = new BlockNode(cutNode3, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex3 = block.createVertex();
        blockVertex4 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertex1.addEdge(blockVertex3);
        blockVertex2.addEdge(blockVertex4);
        blockVertex3.addEdge(blockVertex4);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex2);
        blockVertexToVertex.put(blockVertex2, vertex5);
        blockVertexToVertex.put(blockVertex3, vertex6);
        blockVertexToVertex.put(blockVertex4, vertex10);
        CutNode cutNode4 = new CutNode(blockNode4, vertex10);
        new BlockNode(cutNode4, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex7);
        blockVertexToVertex.put(blockVertex2, vertex11);
        new BlockNode(cutNode1, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex12);
        blockVertexToVertex.put(blockVertex2, vertex14);
        CutNode cutNode5 = new CutNode(blockNode1, vertex12);
        new BlockNode(cutNode5, block, blockVertexToVertex);
        assertTrue(areEquivalent(BlockNode.compute(vertex1), blockNode1));
        assertTrue(areEquivalent(BlockNode.compute(vertex9), blockNode1));
    }

    /**
     * Tests BlockNode.compute on the block-cut tree shown in
     * http://ls11-www.cs.uni-dortmund.de/_media/techreports/tr09-09.pdf (Zey (2009): Algorithms for Planar Graph
     * Augmentation).
     */
    @Test
    public void testComputeZey() {
        Graph graph = new Graph();
        Vertex vertex0 = graph.createVertex();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        Vertex vertex6 = graph.createVertex();
        Vertex vertex7 = graph.createVertex();
        Vertex vertex8 = graph.createVertex();
        Vertex vertex9 = graph.createVertex();
        vertex0.addEdge(vertex1);
        vertex0.addEdge(vertex2);
        vertex0.addEdge(vertex3);
        vertex1.addEdge(vertex3);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex5);
        vertex3.addEdge(vertex7);
        vertex4.addEdge(vertex5);
        vertex6.addEdge(vertex7);
        vertex6.addEdge(vertex8);
        vertex7.addEdge(vertex9);
        vertex8.addEdge(vertex9);

        Graph block = new Graph();
        Vertex blockVertex1 = block.createVertex();
        Vertex blockVertex2 = block.createVertex();
        Vertex blockVertex3 = block.createVertex();
        Vertex blockVertex4 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertex1.addEdge(blockVertex3);
        blockVertex1.addEdge(blockVertex4);
        blockVertex2.addEdge(blockVertex4);
        blockVertex3.addEdge(blockVertex4);
        Map<Vertex, Vertex> blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex0);
        blockVertexToVertex.put(blockVertex2, vertex1);
        blockVertexToVertex.put(blockVertex3, vertex2);
        blockVertexToVertex.put(blockVertex4, vertex3);
        BlockNode blockNode1 = new BlockNode(null, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex3 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertex1.addEdge(blockVertex3);
        blockVertex2.addEdge(blockVertex3);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex2);
        blockVertexToVertex.put(blockVertex2, vertex4);
        blockVertexToVertex.put(blockVertex3, vertex5);
        CutNode cutNode1 = new CutNode(blockNode1, vertex2);
        new BlockNode(cutNode1, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex3);
        blockVertexToVertex.put(blockVertex2, vertex7);
        CutNode cutNode2 = new CutNode(blockNode1, vertex3);
        BlockNode blockNode2 = new BlockNode(cutNode2, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex3 = block.createVertex();
        blockVertex4 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertex1.addEdge(blockVertex3);
        blockVertex2.addEdge(blockVertex4);
        blockVertex3.addEdge(blockVertex4);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex6);
        blockVertexToVertex.put(blockVertex2, vertex7);
        blockVertexToVertex.put(blockVertex3, vertex8);
        blockVertexToVertex.put(blockVertex4, vertex9);
        CutNode cutNode3 = new CutNode(blockNode2, vertex7);
        new BlockNode(cutNode3, block, blockVertexToVertex);
        assertTrue(areEquivalent(BlockNode.compute(vertex0), blockNode1));
    }

    /**
     * Tests BlockNode.compute on the block-cut tree shown in
     * http://ravi-bhide.blogspot.com/2011/05/experiments-in-graph-3-coloring-block.html.
     */
    @Test
    public void testComputeBhide() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        Vertex vertex6 = graph.createVertex();
        Vertex vertex7 = graph.createVertex();
        Vertex vertex8 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex2.addEdge(vertex3);
        vertex3.addEdge(vertex5);
        vertex4.addEdge(vertex5);
        vertex4.addEdge(vertex7);
        vertex5.addEdge(vertex6);
        vertex5.addEdge(vertex7);
        vertex5.addEdge(vertex8);
        vertex6.addEdge(vertex8);

        Graph block = new Graph();
        Vertex blockVertex1 = block.createVertex();
        Vertex blockVertex2 = block.createVertex();
        Vertex blockVertex3 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertex1.addEdge(blockVertex3);
        blockVertex2.addEdge(blockVertex3);
        Map<Vertex, Vertex> blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex1);
        blockVertexToVertex.put(blockVertex2, vertex2);
        blockVertexToVertex.put(blockVertex3, vertex3);
        BlockNode blockNode1 = new BlockNode(null, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex3);
        blockVertexToVertex.put(blockVertex2, vertex5);
        CutNode cutNode1 = new CutNode(blockNode1, vertex3);
        BlockNode blockNode2 = new BlockNode(cutNode1, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex3 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertex1.addEdge(blockVertex3);
        blockVertex2.addEdge(blockVertex3);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex4);
        blockVertexToVertex.put(blockVertex2, vertex5);
        blockVertexToVertex.put(blockVertex3, vertex7);
        CutNode cutNode2 = new CutNode(blockNode2, vertex5);
        new BlockNode(cutNode2, block, blockVertexToVertex);

        block = new Graph();
        blockVertex1 = block.createVertex();
        blockVertex2 = block.createVertex();
        blockVertex3 = block.createVertex();
        blockVertex1.addEdge(blockVertex2);
        blockVertex1.addEdge(blockVertex3);
        blockVertex2.addEdge(blockVertex3);
        blockVertexToVertex = new HashMap<Vertex, Vertex>();
        blockVertexToVertex.put(blockVertex1, vertex5);
        blockVertexToVertex.put(blockVertex2, vertex6);
        blockVertexToVertex.put(blockVertex3, vertex8);
        new BlockNode(cutNode2, block, blockVertexToVertex);
        assertTrue(areEquivalent(BlockNode.compute(vertex1), blockNode1));
    }
}
