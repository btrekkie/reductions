package com.github.btrekkie.graph.spqr.test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.MultiGraph;
import com.github.btrekkie.graph.MultiVertex;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.spqr.SpqrNode;
import com.github.btrekkie.util.UnorderedPair;

public class SpqrNodeTest {
    /**
     * Returns a map from each edge in the specified node's skeleton to the number of times it appears.  We represent
     * each edge as a pair of its endpoints in the original Graph.
     */
    private Map<UnorderedPair<Vertex>, Integer> edgeCounts(SpqrNode node) {
        Map<UnorderedPair<Vertex>, Integer> edgeCounts = new HashMap<UnorderedPair<Vertex>, Integer>();
        for (MultiVertex multiVertex : node.skeleton.vertices) {
            for (MultiVertex adjMultiVertex : multiVertex.edges) {
                Vertex vertex = node.skeletonVertexToVertex.get(multiVertex);
                Vertex adjVertex = node.skeletonVertexToVertex.get(adjMultiVertex);
                UnorderedPair<Vertex> edge = new UnorderedPair<Vertex>(vertex, adjVertex);
                Integer count = edgeCounts.get(edge);
                if (count == null) {
                    edgeCounts.put(edge, 1);
                } else {
                    edgeCounts.put(edge, count + 1);
                }
            }
        }
        for (Entry<UnorderedPair<Vertex>, Integer> entry : edgeCounts.entrySet()) {
            entry.setValue(entry.getValue() / 2);
        }
        return edgeCounts;
    }

    /**
     * Returns a representation of node.realEdges that refers to vertices in the original Graph rather than vertices in
     * node.skeleton.
     */
    private Set<UnorderedPair<Vertex>> realEdges(SpqrNode node) {
        Set<UnorderedPair<Vertex>> edges = new HashSet<UnorderedPair<Vertex>>();
        for (UnorderedPair<MultiVertex> edge : node.realEdges) {
            Vertex vertex1 = node.skeletonVertexToVertex.get(edge.value1);
            Vertex vertex2 = node.skeletonVertexToVertex.get(edge.value2);
            edges.add(new UnorderedPair<Vertex>(vertex1, vertex2));
        }
        return edges;
    }

    /**
     * Returns whether the skeleton's of the specified are the same, in terms of their vertices in the original Graph.
     */
    private boolean haveSameSkeleton(SpqrNode node1, SpqrNode node2) {
        return edgeCounts(node1).equals(edgeCounts(node2)) && realEdges(node1).equals(realEdges(node2));
    }

    /** Returns a map from the SpqrNodeProfiles of the specified node's children to the nodes with those profiles. */
    private Map<SpqrNodeProfile, Collection<SpqrNode>> childProfiles(SpqrNode node) {
        Map<SpqrNodeProfile, Collection<SpqrNode>> profiles = new HashMap<SpqrNodeProfile, Collection<SpqrNode>>();
        for (SpqrNode child : node.children) {
            SpqrNodeProfile profile = new SpqrNodeProfile(child);
            Collection<SpqrNode> nodes = profiles.get(profile);
            if (nodes == null) {
                nodes = new ArrayList<SpqrNode>();
                profiles.put(profile, nodes);
            }
            nodes.add(child);
        }
        return profiles;
    }

    /**
     * Returns whether node1.children is equivalent to node2.children, including checking all descendants of node1 and
     * node2.
     */
    private boolean haveSameChildren(SpqrNode node1, SpqrNode node2) {
        Map<SpqrNodeProfile, Collection<SpqrNode>> profiles1 = childProfiles(node1);
        Map<SpqrNodeProfile, Collection<SpqrNode>> profiles2 = childProfiles(node2);
        if (!profiles1.keySet().equals(profiles2.keySet())) {
            return false;
        }
        for (Entry<SpqrNodeProfile, Collection<SpqrNode>> entry : profiles1.entrySet()) {
            Collection<SpqrNode> children1 = entry.getValue();
            Collection<SpqrNode> children2 = profiles2.get(entry.getKey());
            for (SpqrNode child1 : children1) {
                boolean found = false;
                for (SpqrNode child2 : children2) {
                    if (child1.type == child2.type && haveSameSkeleton(child1, child2)) {
                        found = true;
                        if (!haveSameChildren(child1, child2)) {
                            return false;
                        }
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Returns whether the subtree rooted at node1 is equivalent to the subtree rooted at node2. */
    private boolean areSame(SpqrNode node1, SpqrNode node2) {
        return node1.type == node2.type && node1.children.size() == node2.children.size() &&
            haveSameSkeleton(node1, node2) && haveSameChildren(node1, node2);
    }

    /** Tests SpqrNode.create on simplistic graphs. */
    @Test
    public void testCreateSimple() {
        Graph graph = new Graph();
        Vertex vertex1 = graph.createVertex();
        Vertex vertex2 = graph.createVertex();
        vertex1.addEdge(vertex2);

        MultiGraph skeleton = new MultiGraph();
        MultiVertex multiVertex1 = skeleton.createVertex();
        MultiVertex multiVertex2 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        Map<MultiVertex, Vertex> skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex1);
        skeletonVertexToVertex.put(multiVertex2, vertex2);
        Set<UnorderedPair<MultiVertex>> realEdges = Collections.singleton(
            new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        SpqrNode node = new SpqrNode(null, SpqrNode.Type.R, skeleton, skeletonVertexToVertex, realEdges);
        assertTrue(areSame(SpqrNode.create(vertex1, vertex2), node));

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        Vertex vertex3 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex2.addEdge(vertex3);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        MultiVertex multiVertex3 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex3);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex1);
        skeletonVertexToVertex.put(multiVertex2, vertex2);
        skeletonVertexToVertex.put(multiVertex3, vertex3);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        node = new SpqrNode(null, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);
        assertTrue(areSame(SpqrNode.create(vertex1, vertex2), node));

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        vertex3 = graph.createVertex();
        Vertex vertex4 = graph.createVertex();
        Vertex vertex5 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex5);
        vertex2.addEdge(vertex3);
        vertex3.addEdge(vertex4);
        vertex4.addEdge(vertex5);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        MultiVertex multiVertex4 = skeleton.createVertex();
        MultiVertex multiVertex5 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex5);
        multiVertex2.addEdge(multiVertex3);
        multiVertex3.addEdge(multiVertex4);
        multiVertex4.addEdge(multiVertex5);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex1);
        skeletonVertexToVertex.put(multiVertex2, vertex2);
        skeletonVertexToVertex.put(multiVertex3, vertex3);
        skeletonVertexToVertex.put(multiVertex4, vertex4);
        skeletonVertexToVertex.put(multiVertex5, vertex5);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex5));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex4, multiVertex5));
        node = new SpqrNode(null, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);
        assertTrue(areSame(SpqrNode.create(vertex3, vertex4), node));

        graph = new Graph();
        vertex1 = graph.createVertex();
        vertex2 = graph.createVertex();
        vertex3 = graph.createVertex();
        vertex4 = graph.createVertex();
        vertex5 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex1.addEdge(vertex5);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex5);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex5);
        vertex4.addEdge(vertex5);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex4 = skeleton.createVertex();
        multiVertex5 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex1.addEdge(multiVertex4);
        multiVertex1.addEdge(multiVertex5);
        multiVertex2.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex5);
        multiVertex3.addEdge(multiVertex4);
        multiVertex3.addEdge(multiVertex5);
        multiVertex4.addEdge(multiVertex5);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex1);
        skeletonVertexToVertex.put(multiVertex2, vertex2);
        skeletonVertexToVertex.put(multiVertex3, vertex3);
        skeletonVertexToVertex.put(multiVertex4, vertex4);
        skeletonVertexToVertex.put(multiVertex5, vertex5);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex5));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex5));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex5));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex4, multiVertex5));
        node = new SpqrNode(null, SpqrNode.Type.R, skeleton, skeletonVertexToVertex, realEdges);
        assertTrue(areSame(SpqrNode.create(vertex2, vertex4), node));
    }

    /** Tests SpqrNode.create on the graph shown in the image at the top of https://en.wikipedia.org/wiki/SPQR_tree . */
    @Test
    public void testCreateWikipedia() {
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
        Vertex vertex15 = graph.createVertex();
        Vertex vertex16 = graph.createVertex();
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex7);
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex8);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex5);
        vertex4.addEdge(vertex6);
        vertex5.addEdge(vertex6);
        vertex5.addEdge(vertex7);
        vertex6.addEdge(vertex8);
        vertex7.addEdge(vertex12);
        vertex8.addEdge(vertex9);
        vertex8.addEdge(vertex10);
        vertex9.addEdge(vertex10);
        vertex9.addEdge(vertex14);
        vertex10.addEdge(vertex11);
        vertex11.addEdge(vertex13);
        vertex11.addEdge(vertex14);
        vertex12.addEdge(vertex13);
        vertex12.addEdge(vertex15);
        vertex12.addEdge(vertex16);
        vertex13.addEdge(vertex14);
        vertex13.addEdge(vertex15);
        vertex13.addEdge(vertex16);
        vertex15.addEdge(vertex16);

        MultiGraph skeleton = new MultiGraph();
        MultiVertex multiVertex1 = skeleton.createVertex();
        MultiVertex multiVertex2 = skeleton.createVertex();
        MultiVertex multiVertex3 = skeleton.createVertex();
        MultiVertex multiVertex4 = skeleton.createVertex();
        MultiVertex multiVertex5 = skeleton.createVertex();
        MultiVertex multiVertex6 = skeleton.createVertex();
        MultiVertex multiVertex7 = skeleton.createVertex();
        MultiVertex multiVertex8 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex1.addEdge(multiVertex7);
        multiVertex2.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex8);
        multiVertex3.addEdge(multiVertex4);
        multiVertex3.addEdge(multiVertex5);
        multiVertex4.addEdge(multiVertex6);
        multiVertex5.addEdge(multiVertex6);
        multiVertex5.addEdge(multiVertex7);
        multiVertex6.addEdge(multiVertex8);
        multiVertex7.addEdge(multiVertex8);
        Map<MultiVertex, Vertex> skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex1);
        skeletonVertexToVertex.put(multiVertex2, vertex2);
        skeletonVertexToVertex.put(multiVertex3, vertex3);
        skeletonVertexToVertex.put(multiVertex4, vertex4);
        skeletonVertexToVertex.put(multiVertex5, vertex5);
        skeletonVertexToVertex.put(multiVertex6, vertex6);
        skeletonVertexToVertex.put(multiVertex7, vertex7);
        skeletonVertexToVertex.put(multiVertex8, vertex8);
        Set<UnorderedPair<MultiVertex>> realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex7));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex8));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex5));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex4, multiVertex6));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex5, multiVertex6));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex5, multiVertex7));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex6, multiVertex8));
        SpqrNode node1 = new SpqrNode(null, SpqrNode.Type.R, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex4 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex4);
        multiVertex3.addEdge(multiVertex4);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex7);
        skeletonVertexToVertex.put(multiVertex2, vertex8);
        skeletonVertexToVertex.put(multiVertex3, vertex12);
        skeletonVertexToVertex.put(multiVertex4, vertex13);
        realEdges = Collections.singleton(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        SpqrNode node2 = new SpqrNode(node1, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex4 = skeleton.createVertex();
        multiVertex5 = skeleton.createVertex();
        multiVertex6 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex1.addEdge(multiVertex5);
        multiVertex2.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex6);
        multiVertex3.addEdge(multiVertex4);
        multiVertex4.addEdge(multiVertex5);
        multiVertex4.addEdge(multiVertex6);
        multiVertex5.addEdge(multiVertex6);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex8);
        skeletonVertexToVertex.put(multiVertex2, vertex9);
        skeletonVertexToVertex.put(multiVertex3, vertex10);
        skeletonVertexToVertex.put(multiVertex4, vertex11);
        skeletonVertexToVertex.put(multiVertex5, vertex13);
        skeletonVertexToVertex.put(multiVertex6, vertex14);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex6));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex4, multiVertex5));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex4, multiVertex6));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex5, multiVertex6));
        new SpqrNode(node2, SpqrNode.Type.R, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        for (int i = 0; i < 3; i++) {
            multiVertex1.addEdge(multiVertex2);
        }
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex12);
        skeletonVertexToVertex.put(multiVertex2, vertex13);
        realEdges = Collections.singleton(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        SpqrNode node3 = new SpqrNode(node2, SpqrNode.Type.P, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex4 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex1.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex4);
        multiVertex3.addEdge(multiVertex4);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex12);
        skeletonVertexToVertex.put(multiVertex2, vertex13);
        skeletonVertexToVertex.put(multiVertex3, vertex15);
        skeletonVertexToVertex.put(multiVertex4, vertex16);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        new SpqrNode(node3, SpqrNode.Type.R, skeleton, skeletonVertexToVertex, realEdges);

        assertTrue(areSame(SpqrNode.create(vertex3, vertex4), node1));
    }

    /**
     * Tests SpqrNode.create on the example in http://ls11-www.cs.uni-dortmund.de/_media/techreports/tr09-09.pdf (Zey
     * (2009): Algorithms for Planar Graph Augmentation).
     */
    @Test
    public void testCreateZey() {
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
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex5);
        vertex2.addEdge(vertex8);
        vertex2.addEdge(vertex9);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex6);
        vertex4.addEdge(vertex6);
        vertex4.addEdge(vertex7);
        vertex5.addEdge(vertex8);
        vertex6.addEdge(vertex7);
        vertex7.addEdge(vertex8);
        vertex8.addEdge(vertex9);

        MultiGraph skeleton = new MultiGraph();
        MultiVertex multiVertex1 = skeleton.createVertex();
        MultiVertex multiVertex2 = skeleton.createVertex();
        MultiVertex multiVertex3 = skeleton.createVertex();
        MultiVertex multiVertex4 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex1.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex4);
        multiVertex3.addEdge(multiVertex4);
        Map<MultiVertex, Vertex> skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex0);
        skeletonVertexToVertex.put(multiVertex2, vertex1);
        skeletonVertexToVertex.put(multiVertex3, vertex2);
        skeletonVertexToVertex.put(multiVertex4, vertex8);
        Set<UnorderedPair<MultiVertex>> realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        SpqrNode node1 = new SpqrNode(null, SpqrNode.Type.R, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex4 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex3);
        multiVertex3.addEdge(multiVertex4);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex0);
        skeletonVertexToVertex.put(multiVertex2, vertex3);
        skeletonVertexToVertex.put(multiVertex3, vertex7);
        skeletonVertexToVertex.put(multiVertex4, vertex8);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        SpqrNode node2 = new SpqrNode(node1, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex4 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex1.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex4);
        multiVertex3.addEdge(multiVertex4);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex3);
        skeletonVertexToVertex.put(multiVertex2, vertex4);
        skeletonVertexToVertex.put(multiVertex3, vertex6);
        skeletonVertexToVertex.put(multiVertex4, vertex7);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        new SpqrNode(node2, SpqrNode.Type.R, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex3);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex1);
        skeletonVertexToVertex.put(multiVertex2, vertex5);
        skeletonVertexToVertex.put(multiVertex3, vertex8);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        new SpqrNode(node1, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex2);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex2);
        skeletonVertexToVertex.put(multiVertex2, vertex8);
        realEdges = Collections.singleton(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        SpqrNode node3 = new SpqrNode(node1, SpqrNode.Type.P, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex3);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex2);
        skeletonVertexToVertex.put(multiVertex2, vertex8);
        skeletonVertexToVertex.put(multiVertex3, vertex9);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        new SpqrNode(node3, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);

        assertTrue(areSame(SpqrNode.create(vertex0, vertex1), node1));
    }

    /** Tests SpqrNode.create on the example in https://www.ads.tuwien.ac.at/people/Weiskircher/spqr.pdf.gz . */
    @Test
    public void testCreateWien() {
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
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex1.addEdge(vertex10);
        vertex2.addEdge(vertex5);
        vertex3.addEdge(vertex5);
        vertex4.addEdge(vertex5);
        vertex5.addEdge(vertex11);
        vertex6.addEdge(vertex7);
        vertex6.addEdge(vertex8);
        vertex6.addEdge(vertex10);
        vertex7.addEdge(vertex9);
        vertex7.addEdge(vertex12);
        vertex8.addEdge(vertex9);
        vertex8.addEdge(vertex12);
        vertex9.addEdge(vertex11);

        MultiGraph skeleton = new MultiGraph();
        MultiVertex multiVertex1 = skeleton.createVertex();
        MultiVertex multiVertex2 = skeleton.createVertex();
        MultiVertex multiVertex3 = skeleton.createVertex();
        MultiVertex multiVertex4 = skeleton.createVertex();
        MultiVertex multiVertex5 = skeleton.createVertex();
        MultiVertex multiVertex6 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex6);
        multiVertex2.addEdge(multiVertex3);
        multiVertex3.addEdge(multiVertex4);
        multiVertex4.addEdge(multiVertex5);
        multiVertex5.addEdge(multiVertex6);
        Map<MultiVertex, Vertex> skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex1);
        skeletonVertexToVertex.put(multiVertex2, vertex10);
        skeletonVertexToVertex.put(multiVertex3, vertex6);
        skeletonVertexToVertex.put(multiVertex4, vertex9);
        skeletonVertexToVertex.put(multiVertex5, vertex11);
        skeletonVertexToVertex.put(multiVertex6, vertex5);
        Set<UnorderedPair<MultiVertex>> realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex4, multiVertex5));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex5, multiVertex6));
        SpqrNode node1 = new SpqrNode(null, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex2);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex1);
        skeletonVertexToVertex.put(multiVertex2, vertex5);
        realEdges = Collections.emptySet();
        SpqrNode node2 = new SpqrNode(node1, SpqrNode.Type.P, skeleton, skeletonVertexToVertex, realEdges);

        for (Vertex vertex : new Vertex[]{vertex2, vertex3, vertex4}) {
            skeleton = new MultiGraph();
            multiVertex1 = skeleton.createVertex();
            multiVertex2 = skeleton.createVertex();
            multiVertex3 = skeleton.createVertex();
            multiVertex1.addEdge(multiVertex2);
            multiVertex1.addEdge(multiVertex3);
            multiVertex2.addEdge(multiVertex3);
            skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
            skeletonVertexToVertex.put(multiVertex1, vertex1);
            skeletonVertexToVertex.put(multiVertex2, vertex);
            skeletonVertexToVertex.put(multiVertex3, vertex5);
            realEdges = new HashSet<UnorderedPair<MultiVertex>>();
            realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
            realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
            new SpqrNode(node2, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);
        }

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex4 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex1.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex4);
        multiVertex3.addEdge(multiVertex4);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex6);
        skeletonVertexToVertex.put(multiVertex2, vertex7);
        skeletonVertexToVertex.put(multiVertex3, vertex8);
        skeletonVertexToVertex.put(multiVertex4, vertex9);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        SpqrNode node3 = new SpqrNode(node1, SpqrNode.Type.R, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex3);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex7);
        skeletonVertexToVertex.put(multiVertex2, vertex12);
        skeletonVertexToVertex.put(multiVertex3, vertex8);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        new SpqrNode(node3, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);

        assertTrue(areSame(SpqrNode.create(vertex10, vertex6), node1));
    }

    /**
     * Tests SpqrNode.create on the example in
     * http://page.mi.fu-berlin.de/rote/Lere/2002-SS/Seminar%20Graphenalgorithmen/SPQR.pdf (Battista and Tamassia
     * (1996): On-Line Maintenance of Triconnected Components with SPQR-Trees).
     */
    @Test
    public void testCreateBattistaTamassia() {
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
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex3);
        vertex1.addEdge(vertex4);
        vertex1.addEdge(vertex8);
        vertex1.addEdge(vertex14);
        vertex2.addEdge(vertex3);
        vertex2.addEdge(vertex4);
        vertex2.addEdge(vertex9);
        vertex2.addEdge(vertex12);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex8);
        vertex3.addEdge(vertex12);
        vertex4.addEdge(vertex5);
        vertex4.addEdge(vertex7);
        vertex5.addEdge(vertex6);
        vertex5.addEdge(vertex8);
        vertex6.addEdge(vertex7);
        vertex7.addEdge(vertex8);
        vertex8.addEdge(vertex12);
        vertex8.addEdge(vertex13);
        vertex8.addEdge(vertex14);
        vertex9.addEdge(vertex10);
        vertex10.addEdge(vertex11);
        vertex11.addEdge(vertex12);
        vertex12.addEdge(vertex14);
        vertex13.addEdge(vertex14);

        MultiGraph skeleton = new MultiGraph();
        MultiVertex multiVertex1 = skeleton.createVertex();
        MultiVertex multiVertex2 = skeleton.createVertex();
        MultiVertex multiVertex3 = skeleton.createVertex();
        MultiVertex multiVertex4 = skeleton.createVertex();
        MultiVertex multiVertex5 = skeleton.createVertex();
        MultiVertex multiVertex6 = skeleton.createVertex();
        MultiVertex multiVertex7 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex1.addEdge(multiVertex7);
        multiVertex2.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex5);
        multiVertex2.addEdge(multiVertex7);
        multiVertex3.addEdge(multiVertex4);
        multiVertex3.addEdge(multiVertex6);
        multiVertex4.addEdge(multiVertex5);
        multiVertex4.addEdge(multiVertex6);
        multiVertex4.addEdge(multiVertex7);
        multiVertex5.addEdge(multiVertex6);
        multiVertex5.addEdge(multiVertex7);
        multiVertex6.addEdge(multiVertex7);
        Map<MultiVertex, Vertex> skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex14);
        skeletonVertexToVertex.put(multiVertex2, vertex8);
        skeletonVertexToVertex.put(multiVertex3, vertex12);
        skeletonVertexToVertex.put(multiVertex4, vertex3);
        skeletonVertexToVertex.put(multiVertex5, vertex4);
        skeletonVertexToVertex.put(multiVertex6, vertex2);
        skeletonVertexToVertex.put(multiVertex7, vertex1);
        Set<UnorderedPair<MultiVertex>> realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex7));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex7));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex4, multiVertex5));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex4, multiVertex6));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex4, multiVertex7));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex5, multiVertex6));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex5, multiVertex7));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex6, multiVertex7));
        SpqrNode node1 = new SpqrNode(null, SpqrNode.Type.R, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        for (int i = 0; i < 3; i++) {
            multiVertex1.addEdge(multiVertex2);
        }
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex8);
        skeletonVertexToVertex.put(multiVertex2, vertex14);
        realEdges = Collections.singleton(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        SpqrNode node2 = new SpqrNode(node1, SpqrNode.Type.P, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex3);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex8);
        skeletonVertexToVertex.put(multiVertex2, vertex13);
        skeletonVertexToVertex.put(multiVertex3, vertex14);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        new SpqrNode(node2, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        for (int i = 0; i < 3; i++) {
            multiVertex1.addEdge(multiVertex2);
        }
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex2);
        skeletonVertexToVertex.put(multiVertex2, vertex12);
        realEdges = Collections.singleton(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        SpqrNode node3 = new SpqrNode(node1, SpqrNode.Type.P, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex4 = skeleton.createVertex();
        multiVertex5 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex5);
        multiVertex2.addEdge(multiVertex3);
        multiVertex3.addEdge(multiVertex4);
        multiVertex4.addEdge(multiVertex5);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex2);
        skeletonVertexToVertex.put(multiVertex2, vertex9);
        skeletonVertexToVertex.put(multiVertex3, vertex10);
        skeletonVertexToVertex.put(multiVertex4, vertex11);
        skeletonVertexToVertex.put(multiVertex5, vertex12);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex4, multiVertex5));
        new SpqrNode(node3, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex4 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex1.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex4);
        multiVertex3.addEdge(multiVertex4);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex8);
        skeletonVertexToVertex.put(multiVertex2, vertex7);
        skeletonVertexToVertex.put(multiVertex3, vertex5);
        skeletonVertexToVertex.put(multiVertex4, vertex4);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        SpqrNode node4 = new SpqrNode(node1, SpqrNode.Type.R, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex3);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex5);
        skeletonVertexToVertex.put(multiVertex2, vertex6);
        skeletonVertexToVertex.put(multiVertex3, vertex7);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        new SpqrNode(node4, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);

        assertTrue(areSame(SpqrNode.create(vertex1, vertex14), node1));
    }

    /**
     * Tests SpqrNode.create on the example in
     * https://cs.brown.edu/~rt/gdhandbook/chapters/planarity.pdf (Patrignani: Chapter 1. Planarity Testing and
     * Embedding, p5).
     */
    @Test
    public void testCreatePatrignani() {
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
        vertex1.addEdge(vertex2);
        vertex1.addEdge(vertex4);
        vertex2.addEdge(vertex3);
        vertex3.addEdge(vertex4);
        vertex3.addEdge(vertex5);
        vertex3.addEdge(vertex6);
        vertex3.addEdge(vertex8);
        vertex3.addEdge(vertex9);
        vertex4.addEdge(vertex5);
        vertex4.addEdge(vertex7);
        vertex6.addEdge(vertex7);
        vertex6.addEdge(vertex8);
        vertex7.addEdge(vertex8);
        vertex8.addEdge(vertex9);

        MultiGraph skeleton = new MultiGraph();
        MultiVertex multiVertex1 = skeleton.createVertex();
        MultiVertex multiVertex2 = skeleton.createVertex();
        MultiVertex multiVertex3 = skeleton.createVertex();
        MultiVertex multiVertex4 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex3);
        multiVertex3.addEdge(multiVertex4);
        Map<MultiVertex, Vertex> skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex1);
        skeletonVertexToVertex.put(multiVertex2, vertex2);
        skeletonVertexToVertex.put(multiVertex3, vertex3);
        skeletonVertexToVertex.put(multiVertex4, vertex4);
        Set<UnorderedPair<MultiVertex>> realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        SpqrNode node1 = new SpqrNode(null, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        for (int i = 0; i < 4; i++) {
            multiVertex1.addEdge(multiVertex2);
        }
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex3);
        skeletonVertexToVertex.put(multiVertex2, vertex4);
        realEdges = Collections.singleton(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        SpqrNode node2 = new SpqrNode(node1, SpqrNode.Type.P, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex3);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex3);
        skeletonVertexToVertex.put(multiVertex2, vertex4);
        skeletonVertexToVertex.put(multiVertex3, vertex5);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        new SpqrNode(node2, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex3);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex3);
        skeletonVertexToVertex.put(multiVertex2, vertex4);
        skeletonVertexToVertex.put(multiVertex3, vertex7);
        realEdges = Collections.singleton(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        SpqrNode node3 = new SpqrNode(node2, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex4 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex1.addEdge(multiVertex4);
        multiVertex2.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex4);
        multiVertex3.addEdge(multiVertex4);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex3);
        skeletonVertexToVertex.put(multiVertex2, vertex6);
        skeletonVertexToVertex.put(multiVertex3, vertex7);
        skeletonVertexToVertex.put(multiVertex4, vertex8);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex4));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex3, multiVertex4));
        SpqrNode node4 = new SpqrNode(node3, SpqrNode.Type.R, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        for (int i = 0; i < 3; i++) {
            multiVertex1.addEdge(multiVertex2);
        }
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex3);
        skeletonVertexToVertex.put(multiVertex2, vertex8);
        realEdges = Collections.singleton(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex2));
        SpqrNode node5 = new SpqrNode(node4, SpqrNode.Type.P, skeleton, skeletonVertexToVertex, realEdges);

        skeleton = new MultiGraph();
        multiVertex1 = skeleton.createVertex();
        multiVertex2 = skeleton.createVertex();
        multiVertex3 = skeleton.createVertex();
        multiVertex1.addEdge(multiVertex2);
        multiVertex1.addEdge(multiVertex3);
        multiVertex2.addEdge(multiVertex3);
        skeletonVertexToVertex = new HashMap<MultiVertex, Vertex>();
        skeletonVertexToVertex.put(multiVertex1, vertex3);
        skeletonVertexToVertex.put(multiVertex2, vertex8);
        skeletonVertexToVertex.put(multiVertex3, vertex9);
        realEdges = new HashSet<UnorderedPair<MultiVertex>>();
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex1, multiVertex3));
        realEdges.add(new UnorderedPair<MultiVertex>(multiVertex2, multiVertex3));
        new SpqrNode(node5, SpqrNode.Type.S, skeleton, skeletonVertexToVertex, realEdges);

        assertTrue(areSame(SpqrNode.create(vertex1, vertex2), node1));
    }
}
