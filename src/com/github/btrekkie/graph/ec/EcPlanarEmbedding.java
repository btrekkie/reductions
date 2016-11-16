package com.github.btrekkie.graph.ec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.MultiVertex;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.bc.BlockNode;
import com.github.btrekkie.graph.bc.CutNode;
import com.github.btrekkie.graph.ec.EcNode.Type;
import com.github.btrekkie.graph.planar.PlanarEmbedding;
import com.github.btrekkie.graph.spqr.SpqrNode;
import com.github.btrekkie.util.UnorderedPair;

/** Computes ec-planar embeddings: PlanarEmbeddings satisfying constraints specified by EcNodes. */
/* This is implemented using the algorithm described in http://jgaa.info/accepted/2008/GutwengerKleinMutzel2008.12.1.pdf
 * (Gutwenger, Klien, and Mutzel (2008): Planarity Testing and Optimal Edge Insertion with Embedding Constraints).
 */
public class EcPlanarEmbedding {
    /**
     * Appends the "consolidated" representation of "node" to consolidatedChildren.  That is, this determines a
     * simplified sequence of nodes equivalent to "node" and adds them to consolidatedChildren.  For example, if "node"
     * only has one child, it is equivalent to its child node.  This method is guaranteed to add the same sequence of
     * nodes if called multiple times with the same arguments.
     * @param node The node.
     * @param isOriented Whether the node is "effectively" or "equivalent to" a child of a node of type
     *     EcNode.Type.ORIENTED.
     * @param consolidatedChildren The list to which to add the equivalent sequence of nodes.
     */
    private static void addConsolidatedChildren(EcNode node, boolean isOriented, List<EcNode> consolidatedChildren) {
        while (node.children.size() == 1) {
            node = node.children.get(0);
        }
        if (node.type != EcNode.Type.ORIENTED || !isOriented) {
            consolidatedChildren.add(node);
        } else {
            for (EcNode child : node.children) {
                addConsolidatedChildren(child, isOriented, consolidatedChildren);
            }
        }
    }

    /**
     * Returns a simplified sequence of nodes equivalent to node.children.  For example, if node's first child only has
     * one child, we may treat this grandchild as if it were the first child.  This method is guaranteed to add the same
     * sequence of nodes if called multiple times on the same node.
     * @param node The node.
     * @param isOriented Whether the node is "effectively" or "equivalent to" a child of a node of type
     *     EcNode.Type.ORIENTED.
     * @param consolidatedChildren The list to which to add the equivalent sequence of nodes.
     */
    private static List<EcNode> consolidatedChildren(EcNode node) {
        while (node.children.size() == 1) {
            node = node.children.get(0);
        }
        List<EcNode> consolidatedChildren = new ArrayList<EcNode>();
        for (EcNode child : node.children) {
            addConsolidatedChildren(child, node.type == Type.ORIENTED, consolidatedChildren);
        }
        return consolidatedChildren;
    }

    /**
     * Adds the portion of the ec-expansion of the graph corresponding to "node" to "expansion".  This does not add any
     * edges to the portions of the ec-expansion corresponding to the vertices adjacent to "start".
     * @param expansion The graph for the ec-expansion.
     * @param start The vertex constrained according to the highest ancestor of "node".
     * @param input The vertex in the ec-expansion corresponding to the end of the edge from node.parent to parent.
     *     This is null if node.parent is null.
     * @param node The node to expand.
     * @param endToExpansionEndpoint A map to which to add mappings from each leaf vertex V in the subtree rooted at
     *     "node" to the vertex in the ec-expansion corresponding to the start of the edge from "start" to V.
     * @param hubs A set to which to add the hub vertices of all wheel gadgets in the ec-expansion for the subtree
     *     rooted at "node".
     * @param oHubFirsts A map to which to add mappings from each O-hub vertex V of a wheel gadget in the ec-expansion
     *     for the subtree rooted at "node" to the vertex that must be immediately counterclockwise from
     *     oHubSeconds.get(V) relative to V.  In other words, the edge from V to oHubSeconds.get(V) must be the next
     *     edge clockwise after the edge from V to oHubFirsts.get(V).
     * @param oHubSeconds A map to which to add mappings from each O-hub vertex V of a wheel gadget in the ec-expansion
     *     for the subtree rooted at "node" to the vertex that must be immediately clockwise from oHubFirsts.get(V)
     *     relative to V.  In other words, the edge from V to oHubSeconds.get(V) must be the next edge clockwise after
     *     the edge from V to oHubFirsts.get(V).
     * @param constraintVertices A map to which to add mappings from each node in the subtree rooted at "node" to the
     *     vertex in the ec-expansion for which we can determine the ordering of the node's children based on the
     *     clockwise order of vertices around it.  However, we skip over nodes with one child and nodes consolidated in
     *     consolidatedChildren.  See the comments for constraintOrder.
     * @param constraintStarts A map to which to add mappings from each node N in the subtree rooted at "node" to the
     *     vertex adjacent to constraintVertices.get(N) for identifying the child of N that appears first.  However, we
     *     skip over nodes with one child and nodes consolidated in consolidatedChildren.  See the comments for
     *     constraintOrder.
     * @param constraintOrder A map to which to add mappings from each node N in the subtree rooted at "node" to the
     *     vertices adjacent to constraintVertices.get(N) corresponding to N.children, in order.  However, we skip over
     *     nodes with one child and nodes consolidated in consolidatedChildren.  If we read off the vertices adjacent to
     *     constraintVertices.get(N) in clockwise order starting at constraintStarts.get(N), the index of the first
     *     vertex in constraintOrder.get(N) matches the index in N.children of the first child EcNode in clockwise
     *     order.  Likewise, the index of the second vertex matches the index of the second child, and so on.  However,
     *     the elements of constraintOrder.get(N) are null for EcNode.Type.VERTEX children of EcNode.Type.GROUP nodes,
     *     as we might not have created them yet.  We must infer the corresponding entries of constraintOrder.
     *
     *     For example, suppose we have a non-root node of type EcNode.Type.MIRROR with four children.  This expands to
     *     a wheel gadget with ten spokes.  One of the spokes is for the link from the node's parent to the node, and
     *     four of the other spokes are for the children.  Now say we compute a ec-planar embedding of the ec-expansion.
     *     We want to determine how this ec-planar embedding orders the children of the node.  To do so, we find the
     *     edge from the hub to the spoke for the link from the node's parent to the node.  Then, we find the next edge
     *     in the clockwise direction ending at a spoke corresponding to one of the children.  The child for this spoke
     *     is the child the ec-planar embedding orders first.  Then, we find the next edge in the clockwise direction
     *     ending at such a spoke.  This gives the child the ec-embedding orders second.  Likewise for the third and
     *     fourth children.
     */
    private static void expand(
            Graph expansion, Vertex start, Vertex input, EcNode node,
            Map<Vertex, Vertex> endToExpansionEndpoint, Set<Vertex> hubs,
            Map<Vertex, Vertex> oHubFirsts, Map<Vertex, Vertex> oHubSeconds, Map<EcNode, Vertex> constraintVertices,
            Map<EcNode, Vertex> constraintStarts, Map<EcNode, List<Vertex>> constraintOrder) {
        while (node.children.size() == 1) {
            node = node.children.get(0);
        }
        List<EcNode> children = consolidatedChildren(node);

        switch (node.type) {
            case VERTEX:
            {
                // We only get here if "start" has only one adjacent vertex
                Vertex expansionVertex = expansion.createVertex();
                endToExpansionEndpoint.put(node.vertex, expansionVertex);
                constraintVertices.put(node, expansionVertex);
                constraintOrder.put(node, Collections.<Vertex>singletonList(null));
                break;
            }
            case GROUP:
            {
                Vertex expansionVertex;
                if (input == null) {
                    expansionVertex = expansion.createVertex();
                } else {
                    expansionVertex = input;
                    constraintStarts.put(node, input.edges.iterator().next());
                }
                constraintVertices.put(node, expansionVertex);

                List<Vertex> order = new ArrayList<Vertex>(children.size());
                for (EcNode child : children) {
                    if (child.type == EcNode.Type.VERTEX) {
                        order.add(null);
                        endToExpansionEndpoint.put(child.vertex, expansionVertex);
                    } else {
                        Vertex childVertex = expansion.createVertex();
                        expansionVertex.addEdge(childVertex);
                        order.add(childVertex);
                        expand(
                            expansion, start, childVertex, child, endToExpansionEndpoint, hubs, oHubFirsts, oHubSeconds,
                            constraintVertices, constraintStarts, constraintOrder);
                    }
                }
                constraintOrder.put(node, order);
                break;
            }
            default:
            {
                // Add a wheel gadget
                Vertex hub = expansion.createVertex();
                hubs.add(hub);
                int spokeCount;
                if (input != null) {
                    spokeCount = 2 * children.size() + 2;
                } else {
                    spokeCount = 2 * children.size();
                }
                Vertex firstSpoke = expansion.createVertex();
                Vertex prevSpoke = firstSpoke;
                constraintStarts.put(node, firstSpoke);
                List<Vertex> order = new ArrayList<Vertex>(children.size());
                for (int i = 1; i < spokeCount; i++) {
                    Vertex spoke;
                    if (i % 2 == 0 || i / 2 < children.size()) {
                        spoke = expansion.createVertex();
                    } else {
                        spoke = input;
                    }
                    spoke.addEdge(hub);
                    spoke.addEdge(prevSpoke);

                    if (i % 2 == 1 && i / 2 < children.size()) {
                        // The spoke corresponds to a child node
                        EcNode child = children.get(i / 2);
                        if (child.type == EcNode.Type.VERTEX) {
                            endToExpansionEndpoint.put(child.vertex, spoke);
                        } else {
                            Vertex childVertex = expansion.createVertex();
                            spoke.addEdge(childVertex);
                            expand(
                                expansion, start, childVertex, child, endToExpansionEndpoint, hubs,
                                oHubFirsts, oHubSeconds, constraintVertices, constraintStarts, constraintOrder);
                        }
                        order.add(spoke);
                    }
                    prevSpoke = spoke;
                }
                firstSpoke.addEdge(hub);
                firstSpoke.addEdge(prevSpoke);

                constraintVertices.put(node, hub);
                constraintOrder.put(node, order);
                if (node.type == EcNode.Type.ORIENTED) {
                    oHubFirsts.put(hub, prevSpoke);
                    oHubSeconds.put(hub, firstSpoke);
                }
                break;
            }
        }
    }

    /** Returns the vertices in the connected component containing "start". */
    private static Collection<Vertex> component(Vertex start) {
        // Use breadth-first search
        Set<Vertex> component = new LinkedHashSet<Vertex>();
        component.add(start);
        Collection<Vertex> level = Collections.singleton(start);
        while (!level.isEmpty()) {
            Collection<Vertex> nextLevel = new ArrayList<Vertex>();
            for (Vertex vertex : level) {
                for (Vertex adjVertex : vertex.edges) {
                    if (component.add(adjVertex)) {
                        nextLevel.add(adjVertex);
                    }
                }
            }
            level = nextLevel;
        }
        return component;
    }

    /**
     * Returns a PlanarEmbedding of the skeleton of the specified node of type SpqrNode.Type.R that correctly orients
     * all of the O-hubs in the skeleton, or null if there is no such planar embedding.  This is an embedding of a block
     * subgraph of an ec-expansion graph.  The vertices in the embedding are drawn from the block subgraph rather than
     * the ec-expansion graph, i.e. from blockVertexToVertex.keySet() rather than blockVertexToVertex.values().
     * @param node The node.
     * @param oHubFirsts A map from each O-hub vertex V to the vertex that must be immediately counterclockwise from
     *     oHubSeconds.get(V) relative to V.  The keys and values are drawn from the expansion graph rather than the
     *     block subgraph, i.e. from blockVertexToVertex.values() rather than blockVertexToVertex.keySet().
     * @param oHubSeconds A map from each O-hub vertex V to the vertex that must be immediately clockwise from
     *     oHubFirsts.get(V) relative to V.  The keys and values are drawn from the expansion graph rather than the
     *     block subgraph, i.e. from blockVertexToVertex.values() rather than blockVertexToVertex.keySet().
     * @param blockVertexToVertex A map from each vertex in the block subgraph to the corresponding vertex in the
     *     ec-expansion graph.
     * @return The embedding.
     */
    private static PlanarEmbedding embedRNode(
            SpqrNode node, Map<Vertex, Vertex> oHubFirsts, Map<Vertex, Vertex> oHubSeconds,
            Map<Vertex, Vertex> blockVertexToVertex) {
        // Convert the multigraph node.skeleton to a graph "graph"
        Graph graph = new Graph();
        Map<MultiVertex, Vertex> multiVertexToVertex = new HashMap<MultiVertex, Vertex>();
        Map<Vertex, MultiVertex> vertexToMultiVertex = new HashMap<Vertex, MultiVertex>();
        for (MultiVertex multiVertex : node.skeleton.vertices) {
            Vertex vertex = multiVertexToVertex.get(multiVertex);
            if (vertex == null) {
                vertex = graph.createVertex();
                multiVertexToVertex.put(multiVertex, vertex);
                vertexToMultiVertex.put(vertex, multiVertex);
            }
            for (MultiVertex adjMultiVertex : multiVertex.edges) {
                Vertex adjVertex = multiVertexToVertex.get(adjMultiVertex);
                if (adjVertex == null) {
                    adjVertex = graph.createVertex();
                    multiVertexToVertex.put(adjMultiVertex, adjVertex);
                    vertexToMultiVertex.put(adjVertex, adjMultiVertex);
                }
                vertex.addEdge(adjVertex);
            }
        }

        PlanarEmbedding embedding = PlanarEmbedding.compute(graph.vertices.iterator().next());
        if (embedding == null) {
            return null;
        }

        // Translate from vertices in "graph" to vertices in blockVertexToVertex.keySet()
        Map<MultiVertex, Vertex> skeletonVertexToVertex = node.skeletonVertexToVertex;
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        for (Entry<Vertex, List<Vertex>> entry : embedding.clockwiseOrder.entrySet()) {
            Vertex graphVertex = entry.getKey();
            List<Vertex> graphClockwiseOrder = entry.getValue();
            List<Vertex> blockClockwiseOrder = new ArrayList<Vertex>(graphClockwiseOrder.size());
            for (Vertex vertex : graphClockwiseOrder) {
                blockClockwiseOrder.add(skeletonVertexToVertex.get(vertexToMultiVertex.get(vertex)));
            }
            clockwiseOrder.put(skeletonVertexToVertex.get(vertexToMultiVertex.get(graphVertex)), blockClockwiseOrder);
        }
        List<Vertex> externalFace = new ArrayList<Vertex>(embedding.externalFace.size());
        for (Vertex graphVertex : embedding.externalFace) {
            externalFace.add(skeletonVertexToVertex.get(vertexToMultiVertex.get(graphVertex)));
        }
        embedding = PlanarEmbedding.createPartial(clockwiseOrder, externalFace);

        // Flip the embedding as necessary to correctly orient the O-hubs
        boolean canBeNonFlipped = true;
        boolean canBeFlipped = true;
        for (Vertex blockVertex : skeletonVertexToVertex.values()) {
            Vertex vertex = blockVertexToVertex.get(blockVertex);
            Vertex oHubFirst = oHubFirsts.get(vertex);
            if (oHubFirst != null) {
                // Find oHubFirst in embedding.clockwiseOrder
                List<Vertex> order = embedding.clockwiseOrder.get(blockVertex);
                int oHubFirstIndex = -1;
                for (int i = 0; i < order.size(); i++) {
                    Vertex blockOrderVertex = order.get(i);
                    Vertex orderVertex = blockVertexToVertex.get(blockOrderVertex);
                    if (orderVertex == oHubFirst) {
                        oHubFirstIndex = i;
                        break;
                    }
                }

                int nextIndex;
                if (oHubFirstIndex + 1 < order.size()) {
                    nextIndex = oHubFirstIndex + 1;
                } else {
                    nextIndex = 0;
                }
                Vertex blockOrderVertex = order.get(nextIndex);
                Vertex orderVertex = blockVertexToVertex.get(blockOrderVertex);

                if (orderVertex == oHubSeconds.get(vertex)) {
                    if (!canBeNonFlipped) {
                        return null;
                    }
                    canBeFlipped = false;
                } else if (!canBeFlipped) {
                    return null;
                } else {
                    canBeNonFlipped = false;
                }
            }
        }

        if (canBeNonFlipped) {
            return embedding;
        } else {
            return embedding.flip();
        }
    }

    /**
     * Returns the HalfEdges for the skeleton of the specified node of type SpqrNode.Type.P.  This does not set the
     * virtualMatch fields.
     */
    private static Collection<HalfEdge> createPHalfEdges(SpqrNode node) {
        Iterator<MultiVertex> vertexIterator = node.skeleton.vertices.iterator();
        Vertex vertex1 = node.skeletonVertexToVertex.get(vertexIterator.next());
        Vertex vertex2 = node.skeletonVertexToVertex.get(vertexIterator.next());
        int edgeCount = node.skeleton.vertices.iterator().next().edges.size();
        Collection<HalfEdge> halfEdges = new ArrayList<HalfEdge>(2 * edgeCount);

        // Create the first two HalfEdges
        HalfEdge firstHalfEdge = new HalfEdge(vertex1, node.realEdges.isEmpty());
        HalfEdge firstTwinEdge = new HalfEdge(vertex2, node.realEdges.isEmpty());
        firstHalfEdge.twinEdge = firstTwinEdge;
        firstTwinEdge.twinEdge = firstHalfEdge;
        halfEdges.add(firstHalfEdge);
        halfEdges.add(firstTwinEdge);

        // Create the remaining HalfEdges
        HalfEdge prevHalfEdge = firstHalfEdge;
        HalfEdge prevTwinEdge = firstTwinEdge;
        List<HalfEdge> twinEdges = new ArrayList<HalfEdge>(edgeCount - 1);
        for (int i = 0; i < edgeCount - 1; i++) {
            HalfEdge halfEdge = new HalfEdge(vertex1, true);
            HalfEdge twinEdge = new HalfEdge(vertex2, true);
            twinEdges.add(twinEdge);
            prevHalfEdge.nextClockwise = halfEdge;
            prevTwinEdge.nextClockwise = twinEdge;
            halfEdges.add(halfEdge);
            halfEdges.add(twinEdge);
            prevHalfEdge = halfEdge;
            prevTwinEdge = twinEdge;
        }
        prevHalfEdge.nextClockwise = firstHalfEdge;
        prevTwinEdge.nextClockwise = firstTwinEdge;

        // Set the twinEdge links
        HalfEdge halfEdge = firstHalfEdge.nextClockwise;
        for (int i = 0; i < edgeCount - 1; i++) {
            HalfEdge twinEdge = twinEdges.get(twinEdges.size() - i - 1);
            halfEdge.twinEdge = twinEdge;
            twinEdge.twinEdge = halfEdge;
            halfEdge = halfEdge.nextClockwise;
        }

        // Set the nextOnExternalFace links
        halfEdge = firstHalfEdge;
        for (int i = 0; i < edgeCount; i++) {
            HalfEdge next = halfEdge.twinEdge.nextClockwise;
            halfEdge.nextOnExternalFace = next;
            next.nextOnExternalFace = halfEdge;
            halfEdge = halfEdge.nextClockwise;
        }
        return halfEdges;
    }

    /**
     * Creates the HalfEdges for the skeleton of the specified node of type SpqrNode.Type.S or SpqrNode.Type.R.  This is
     * for an embedding of a block subgraph of an ec-expansion graph.  This does not set the virtualMatch fields.
     * @param node The node.
     * @param nonPHalfEdges The map to which to add the HalfEdges.  It consists of mappings from each edge, represented
     *     as a pair of its endpoints, to a non-empty collection of the HalfEdges for the edge.
     * @param oHubFirsts A map from each O-hub vertex V to the vertex that must be immediately counterclockwise from
     *     oHubSeconds.get(V) relative to V.  The keys and values are drawn from the expansion graph rather than the
     *     block subgraph, i.e. from blockVertexToVertex.values() rather than blockVertexToVertex.keySet().
     * @param oHubSeconds A map from each O-hub vertex V to the vertex that must be immediately clockwise from
     *     oHubFirsts.get(V) relative to V.  The keys and values are drawn from the expansion graph rather than the
     *     block subgraph, i.e. from blockVertexToVertex.values() rather than blockVertexToVertex.keySet().
     * @param blockVertexToVertex A map from each vertex in the block subgraph to the corresponding vertex in the
     *     ec-expansion graph.
     * @return Whether there is an embedding for the skeleton that correctly orients all of its O-hubs.  If there is no
     *     such embedding, this method has no effect on nonPHalfEdges.
     */
    private static boolean createNonPHalfEdges(
            SpqrNode node, Map<UnorderedPair<Vertex>, Collection<HalfEdge>> nonPHalfEdges,
            Map<Vertex, Vertex> oHubFirsts, Map<Vertex, Vertex> oHubSeconds, Map<Vertex, Vertex> blockVertexToVertex) {
        // Translate node.realEdges according to skeletonVertexToVertex
        Map<MultiVertex, Vertex> skeletonVertexToVertex = node.skeletonVertexToVertex;
        Set<UnorderedPair<Vertex>> realEdges = new HashSet<UnorderedPair<Vertex>>();
        for (UnorderedPair<MultiVertex> realEdge : node.realEdges) {
            realEdges.add(
                new UnorderedPair<Vertex>(
                    skeletonVertexToVertex.get(realEdge.value1),
                    skeletonVertexToVertex.get(realEdge.value2)));
        }

        // Compute an embedding for the node's skeleton
        PlanarEmbedding embedding;
        if (node.type == SpqrNode.Type.R) {
            embedding = embedRNode(node, oHubFirsts, oHubSeconds, blockVertexToVertex);
            if (embedding == null) {
                return false;
            }
        } else {
            // The graph consists of a cycle, so we can construct the embedding easily enough

            // Compute clockwiseOrder
            Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
            for (MultiVertex vertex : node.skeleton.vertices) {
                Iterator<MultiVertex> iterator = vertex.edges.iterator();
                MultiVertex adjVertex1 = iterator.next();
                MultiVertex adjVertex2 = iterator.next();
                clockwiseOrder.put(
                    skeletonVertexToVertex.get(vertex),
                    Arrays.asList(
                        skeletonVertexToVertex.get(adjVertex1), skeletonVertexToVertex.get(adjVertex2)));
            }

            // Compute externalFace
            MultiVertex prevVertex = node.skeleton.vertices.iterator().next();
            MultiVertex firstVertex = prevVertex;
            MultiVertex vertex = prevVertex.edges.iterator().next();
            List<Vertex> externalFace = new ArrayList<Vertex>(node.skeleton.vertices.size());
            externalFace.add(skeletonVertexToVertex.get(vertex));
            while (vertex != firstVertex) {
                Iterator<MultiVertex> iterator = vertex.edges.iterator();
                MultiVertex nextVertex = iterator.next();
                if (nextVertex == prevVertex) {
                    nextVertex = iterator.next();
                }
                prevVertex = vertex;
                vertex = nextVertex;
                externalFace.add(skeletonVertexToVertex.get(vertex));
            }
            embedding = PlanarEmbedding.createPartial(clockwiseOrder, externalFace);
        }

        Map<UnorderedPair<Vertex>, HalfEdge> nodeHalfEdges = new HashMap<UnorderedPair<Vertex>, HalfEdge>();
        for (Entry<Vertex, List<Vertex>> entry : embedding.clockwiseOrder.entrySet()) {
            Vertex vertex = entry.getKey();
            HalfEdge firstHalfEdge = null;
            HalfEdge prevHalfEdge = null;
            for (Vertex adjVertex : entry.getValue()) {
                // Get or create the HalfEdge from "vertex" to adjVertex
                UnorderedPair<Vertex> edge = new UnorderedPair<Vertex>(vertex, adjVertex);
                HalfEdge halfEdge = nodeHalfEdges.get(edge);
                if (halfEdge == null) {
                    // Create the two HalfEdges connecting "vertex" and adjVertex
                    boolean isVirtual = !realEdges.contains(edge);
                    halfEdge = new HalfEdge(adjVertex, isVirtual);
                    HalfEdge twinEdge = new HalfEdge(vertex, isVirtual);
                    halfEdge.twinEdge = twinEdge;
                    twinEdge.twinEdge = halfEdge;

                    nodeHalfEdges.put(edge, halfEdge);
                    Collection<HalfEdge> halfEdges = nonPHalfEdges.get(edge);
                    if (halfEdges == null) {
                        halfEdges = new ArrayList<HalfEdge>();
                        nonPHalfEdges.put(edge, halfEdges);
                    }
                    halfEdges.add(halfEdge);
                    halfEdges.add(twinEdge);
                } else if (halfEdge.end != adjVertex) {
                    halfEdge = halfEdge.twinEdge;
                }

                if (prevHalfEdge != null) {
                    prevHalfEdge.nextClockwise = halfEdge;
                } else {
                    firstHalfEdge = halfEdge;
                }
                prevHalfEdge = halfEdge;
            }
            prevHalfEdge.nextClockwise = firstHalfEdge;
        }

        Collection<List<Vertex>> externalFaces;
        if (node.type == SpqrNode.Type.R) {
            externalFaces = Collections.singleton(embedding.externalFace);
        } else {
            // There are two possible external faces: embedding.externalFace and its reverse
            List<Vertex> reversedExternalFace = new ArrayList<Vertex>(embedding.externalFace);
            Collections.reverse(reversedExternalFace);
            externalFaces = Arrays.asList(embedding.externalFace, reversedExternalFace);
        }

        // Set the nextOnExternalFace links
        for (List<Vertex> externalFace : externalFaces) {
            Vertex prevVertex = externalFace.get(externalFace.size() - 1);
            Vertex prevPrevVertex = externalFace.get(externalFace.size() - 2);
            UnorderedPair<Vertex> edge = new UnorderedPair<Vertex>(prevPrevVertex, prevVertex);
            HalfEdge prevHalfEdge = nodeHalfEdges.get(edge);
            if (prevHalfEdge.end != prevVertex) {
                prevHalfEdge = prevHalfEdge.twinEdge;
            }
            for (Vertex vertex : externalFace) {
                edge = new UnorderedPair<Vertex>(prevVertex, vertex);
                HalfEdge halfEdge = nodeHalfEdges.get(edge);
                if (halfEdge.end != vertex) {
                    halfEdge = halfEdge.twinEdge;
                }
                prevHalfEdge.nextOnExternalFace = halfEdge;
                prevHalfEdge = halfEdge;
                prevVertex = vertex;
            }
        }
        return true;
    }

    /**
     * Returns a PlanarEmbedding of blockNode.block that correctly orients all of the O-hubs in the graph, or null if
     * there is no such planar embedding.  This is an embedding of a block subgraph of an ec-expansion graph.  The
     * vertices in the embedding are drawn from the ec-expansion graph rather than the block subgraph, i.e. from
     * blockNode.blockVertexToVertex.values() rather than blockNode.blockVertexToVertex.keySet().
     * @param blockNode The block node.
     * @param oHubFirsts A map from each O-hub vertex V to the vertex that must be immediately counterclockwise from
     *     oHubSeconds.get(V) relative to V.  The keys and values are drawn from the expansion graph rather than the
     *     block subgraph, i.e. from blockNode.blockVertexToVertex.values() rather than
     *     blockNode.blockVertexToVertex.keySet().
     * @param oHubSeconds A map from each O-hub vertex V to the vertex that must be immediately clockwise from
     *     oHubFirsts.get(V) relative to V.  The keys and values are drawn from the expansion graph rather than the
     *     block subgraph, i.e. from blockNode.blockVertexToVertex.values() rather than
     *     blockNode.blockVertexToVertex.keySet().
     * @return The planar embedding.
     */
    private static PlanarEmbedding embed(
            BlockNode blockNode, Map<Vertex, Vertex> oHubFirsts, Map<Vertex, Vertex> oHubSeconds) {
        // Compute the SPQR tree of the block
        Map<Vertex, Vertex> blockVertexToVertex = blockNode.blockVertexToVertex;
        Iterator<Vertex> iterator = blockNode.block.vertices.iterator();
        SpqrNode rootSpqrNode = SpqrNode.create(iterator.next(), iterator.next());

        // Create the HalfEdges for the SPQR nodes, iterating over the nodes using breadth-first search
        Map<UnorderedPair<Vertex>, Collection<HalfEdge>> nonPHalfEdges =
            new LinkedHashMap<UnorderedPair<Vertex>, Collection<HalfEdge>>();
        Map<UnorderedPair<Vertex>, Collection<HalfEdge>> pHalfEdges =
            new LinkedHashMap<UnorderedPair<Vertex>, Collection<HalfEdge>>();
        Collection<SpqrNode> level = Collections.singleton(rootSpqrNode);
        while (!level.isEmpty()) {
            Collection<SpqrNode> nextLevel = new ArrayList<SpqrNode>();
            for (SpqrNode spqrNode : level) {
                if (spqrNode.type == SpqrNode.Type.P) {
                    Iterator<MultiVertex> vertexIterator = spqrNode.skeleton.vertices.iterator();
                    Vertex vertex1 = spqrNode.skeletonVertexToVertex.get(vertexIterator.next());
                    Vertex vertex2 = spqrNode.skeletonVertexToVertex.get(vertexIterator.next());
                    pHalfEdges.put(new UnorderedPair<Vertex>(vertex1, vertex2), createPHalfEdges(spqrNode));
                } else if (!createNonPHalfEdges(
                        spqrNode, nonPHalfEdges, oHubFirsts, oHubSeconds, blockVertexToVertex)) {
                    return null;
                }
                nextLevel.addAll(spqrNode.children);
            }
            level = nextLevel;
        }

        // Set the virtualMatch links from HalfEdges in P nodes to HalfEdges in non-P nodes
        Collection<HalfEdge> halfEdges = new ArrayList<HalfEdge>();
        for (Entry<UnorderedPair<Vertex>, Collection<HalfEdge>> entry : pHalfEdges.entrySet()) {
            Collection<HalfEdge> curPHalfEdges = entry.getValue();
            Collection<HalfEdge> curNonPHalfEdges = nonPHalfEdges.remove(entry.getKey());
            Vertex vertex = curPHalfEdges.iterator().next().end;
            Iterator<HalfEdge> nonPIterator = curNonPHalfEdges.iterator();
            for (HalfEdge pHalfEdge : curPHalfEdges) {
                if (pHalfEdge.end == vertex && pHalfEdge.isVirtual) {
                    HalfEdge nonPHalfEdge;
                    do {
                        nonPHalfEdge = nonPIterator.next();
                    } while (nonPHalfEdge.end != vertex || !nonPHalfEdge.isVirtual);
                    pHalfEdge.virtualMatch = nonPHalfEdge;
                    nonPHalfEdge.virtualMatch = pHalfEdge;
                    pHalfEdge.twinEdge.virtualMatch = nonPHalfEdge.twinEdge;
                    nonPHalfEdge.twinEdge.virtualMatch = pHalfEdge.twinEdge;
                }
            }
            halfEdges.addAll(curPHalfEdges);
            halfEdges.addAll(curNonPHalfEdges);
        }

        // Set the virtualMatch links between pairs of HalfEdges in non-P nodes
        for (Entry<UnorderedPair<Vertex>, Collection<HalfEdge>> entry : nonPHalfEdges.entrySet()) {
            Iterator<HalfEdge> halfEdgeIterator = entry.getValue().iterator();
            HalfEdge halfEdge1;
            do {
                halfEdge1 = halfEdgeIterator.next();
            } while (!halfEdge1.isVirtual && halfEdgeIterator.hasNext());
            if (halfEdgeIterator.hasNext()) {
                HalfEdge halfEdge2;
                do {
                    halfEdge2 = halfEdgeIterator.next();
                } while (halfEdge2.end != halfEdge1.end);
                halfEdge1.virtualMatch = halfEdge2;
                halfEdge2.virtualMatch = halfEdge1;
                halfEdge1.twinEdge.virtualMatch = halfEdge2.twinEdge;
                halfEdge2.twinEdge.virtualMatch = halfEdge1.twinEdge;
            }
            halfEdges.addAll(entry.getValue());
        }

        // Compute clockwiseOrder
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        for (HalfEdge halfEdge : halfEdges) {
            Vertex vertex = halfEdge.twinEdge.end;
            if (!clockwiseOrder.containsKey(vertex)) {
                List<Vertex> vertexClockwiseOrder = new ArrayList<Vertex>();
                HalfEdge orderEdge = halfEdge;
                if (!halfEdge.isVirtual) {
                    vertexClockwiseOrder.add(blockVertexToVertex.get(halfEdge.end));
                } else {
                    orderEdge = orderEdge.virtualMatch;
                }
                orderEdge = orderEdge.nextClockwise;
                while (orderEdge != halfEdge) {
                    if (!orderEdge.isVirtual) {
                        vertexClockwiseOrder.add(blockVertexToVertex.get(orderEdge.end));
                    } else {
                        orderEdge = orderEdge.virtualMatch;
                    }
                    orderEdge = orderEdge.nextClockwise;
                }
                clockwiseOrder.put(blockVertexToVertex.get(vertex), vertexClockwiseOrder);
            }
        }

        // Compute externalFace
        List<Vertex> externalFace = new ArrayList<Vertex>();
        Set<HalfEdge> visited = new HashSet<HalfEdge>();
        for (HalfEdge halfEdge : halfEdges) {
            if (halfEdge.isVirtual || !visited.add(halfEdge)) {
                continue;
            }
            externalFace.add(blockVertexToVertex.get(halfEdge.end));
            HalfEdge externalFaceEdge = halfEdge.nextOnExternalFace;
            while (externalFaceEdge != null && visited.add(externalFaceEdge)) {
                if (!externalFaceEdge.isVirtual) {
                    externalFace.add(blockVertexToVertex.get(externalFaceEdge.end));
                } else {
                    externalFaceEdge = externalFaceEdge.virtualMatch.twinEdge;
                }
                externalFaceEdge = externalFaceEdge.nextOnExternalFace;
            }
            if (externalFaceEdge == halfEdge) {
                break;
            } else {
                // The search for an external face failed, because either nextOnExternalFace was null or we reached a
                // HalfEdge we had already visited
                externalFace.clear();
            }
        }

        return PlanarEmbedding.createPartial(clockwiseOrder, externalFace);
    }

    /**
     * Returns a face of a block of an ec-expansion graph that may be the external face of the overall ec-expansion
     * graph, per the procedure described in lemma 3 of the paper, or null if the block has no such face.  This is
     * relative to a certain ec-planar embedding of the block.  The external face may not be an edge, an inner wheel
     * face of a wheel gadget, or an outer wheel face of an outer wheel gadget.  (Technically, if the ec-expansion graph
     * consists of a single edge, then the external face may be an edge, but this still returns null in that case.)
     * @param embedding The ec-planar embedding of the block.
     * @param hubs The hub vertices of all wheel gadgets in the ec-expansion graph.
     * @return The external face, as in PlanarEmbedding.externalFace.
     */
    private static List<Vertex> validExpansionExternalFace(PlanarEmbedding embedding, Set<Vertex> hubs) {
        if (embedding.externalFace.size() == 2) {
            return null;
        }

        // Check whether embedding.externalFace is an inner wheel face of a wheel gadget
        int hubIndex = -1;
        if (embedding.externalFace.size() == 3) {
            for (int i = 0; i < embedding.externalFace.size(); i++) {
                if (hubs.contains(embedding.externalFace.get(i))) {
                    hubIndex = i;
                    break;
                }
            }
        }

        List<Vertex> externalFace;
        if (hubIndex < 0) {
            externalFace = embedding.externalFace;
        } else {
            // embedding.externalFace is an inner wheel face.  This is not allowed.  Switch to another face.
            Vertex prevVertex;
            if (hubIndex > 0) {
                prevVertex = embedding.externalFace.get(hubIndex - 1);
            } else {
                prevVertex = embedding.externalFace.get(2);
            }
            Vertex vertex;
            if (hubIndex < 2) {
                vertex = embedding.externalFace.get(hubIndex + 1);
            } else {
                vertex = embedding.externalFace.get(0);
            }

            // Use embedding.clockwiseOrder to keep moving from one edge of the face to the next
            externalFace = new ArrayList<Vertex>();
            Vertex firstVertex = prevVertex;
            Vertex secondVertex = vertex;
            do {
                externalFace.add(vertex);
                List<Vertex> vertexClockwiseOrder = embedding.clockwiseOrder.get(vertex);
                int index = vertexClockwiseOrder.indexOf(prevVertex);
                prevVertex = vertex;
                if (index + 1 < vertexClockwiseOrder.size()) {
                    vertex = vertexClockwiseOrder.get(index + 1);
                } else {
                    vertex = vertexClockwiseOrder.get(0);
                }
            } while (prevVertex != firstVertex || vertex != secondVertex);
        }

        // Check whether externalFace is an outer wheel face of a wheel gadget
        Vertex hub = null;
        for (Vertex vertex : externalFace) {
            for (Vertex adjVertex : vertex.edges) {
                if (hubs.contains(adjVertex)) {
                    hub = adjVertex;
                    break;
                }
            }
            if (hub != null) {
                break;
            }
        }
        if (hub == null || !hub.edges.equals(new HashSet<Vertex>(externalFace))) {
            return externalFace;
        } else {
            // externalFace is an outer wheel face
            return null;
        }
    }

    /**
     * Returns an ec-planar embedding of the specified ec-expansion graph, or null if there is no such planar embedding.
     * @param expansion The ec-expansion graph.
     * @param hubs The hub vertices of all wheel gadgets in the ec-expansion graph.
     * @param oHubFirsts A map from each O-hub vertex V to the vertex that must be immediately counterclockwise from
     *     oHubSeconds.get(V) relative to V.
     * @param oHubSeconds A map from each O-hub vertex V to the vertex that must be immediately clockwise from
     *     oHubFirsts.get(V) relative to V.
     * @return The embedding.
     */
    private static PlanarEmbedding embed(
            Graph expansion, Set<Vertex> hubs, Map<Vertex, Vertex> oHubFirsts, Map<Vertex, Vertex> oHubSeconds) {
        // Compute the overall ec-planar embedding from ec-planar embeddings of the blocks.  Iterate over the blocks
        // using breadth-first search on the BC-tree.
        BlockNode rootBlockNode = BlockNode.compute(expansion.vertices.iterator().next());
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        Vertex firstExternalFaceVertex = null;
        Vertex secondExternalFaceVertex = null;
        Collection<BlockNode> level = Collections.singleton(rootBlockNode);
        while (!level.isEmpty()) {
            Collection<BlockNode> nextLevel = new ArrayList<BlockNode>();
            for (BlockNode blockNode : level) {
                PlanarEmbedding embedding = embed(blockNode, oHubFirsts, oHubSeconds);
                if (embedding == null) {
                    return null;
                }

                for (Entry<Vertex, List<Vertex>> entry : embedding.clockwiseOrder.entrySet()) {
                    // Before appending embedding.clockwiseOrder.get(vertex) to clockwiseOrder.get(vertex), rotate the
                    // former so that neither the first vertex nor the last vertex is a hub vertex.  This ensures that
                    // the inner wheel gadget faces end up as faces of the resulting embedding.
                    Vertex vertex = entry.getKey();
                    List<Vertex> vertexClockwiseOrder = entry.getValue();
                    List<Vertex> rotatedClockwiseOrder;
                    if (hubs.contains(vertexClockwiseOrder.get(0))) {
                        rotatedClockwiseOrder = new ArrayList<Vertex>(vertexClockwiseOrder.size());
                        rotatedClockwiseOrder.add(vertexClockwiseOrder.get(vertexClockwiseOrder.size() - 1));
                        rotatedClockwiseOrder.addAll(vertexClockwiseOrder.subList(0, vertexClockwiseOrder.size() - 1));
                    } else if (hubs.contains(vertexClockwiseOrder.get(vertexClockwiseOrder.size() - 1))) {
                        rotatedClockwiseOrder = new ArrayList<Vertex>(vertexClockwiseOrder.size());
                        rotatedClockwiseOrder.addAll(vertexClockwiseOrder.subList(1, vertexClockwiseOrder.size()));
                        rotatedClockwiseOrder.add(vertexClockwiseOrder.get(0));
                    } else {
                        rotatedClockwiseOrder = vertexClockwiseOrder;
                    }

                    // Append to clockwiseOrder.get(vertex)
                    List<Vertex> overallClockwiseOrder = clockwiseOrder.get(vertex);
                    if (overallClockwiseOrder == null) {
                        overallClockwiseOrder = new ArrayList<Vertex>();
                        clockwiseOrder.put(vertex, overallClockwiseOrder);
                    }
                    overallClockwiseOrder.addAll(rotatedClockwiseOrder);
                }

                // Set the external face vertices.  See the comments for validExpansionExternalFace.
                if (firstExternalFaceVertex == null) {
                    List<Vertex> externalFace = validExpansionExternalFace(embedding, hubs);
                    if (externalFace != null) {
                        // Use an arbitrary edge of externalFace as an edge in the resulting external face.  The
                        // resulting external face will differ from externalFace if we end up embedding other blocks on
                        // the exterior.
                        firstExternalFaceVertex = externalFace.get(0);
                        secondExternalFaceVertex = externalFace.get(1);
                    }
                }

                for (CutNode child : blockNode.children) {
                    nextLevel.addAll(child.children);
                }
            }
            level = nextLevel;
        }

        if (firstExternalFaceVertex == null) {
            // All faces of the blocks are either edges, inner wheel faces of a wheel gadget, or outer wheel faces of a
            // wheel gadget.  The original graph must be a tree.  Select an arbitrary edge that is not adjacent to a
            // hub.
            for (Entry<Vertex, List<Vertex>> entry : clockwiseOrder.entrySet()) {
                Vertex vertex = entry.getKey();
                if (!hubs.contains(vertex)) {
                    firstExternalFaceVertex = vertex;
                    break;
                }
            }
            secondExternalFaceVertex = clockwiseOrder.get(firstExternalFaceVertex).get(0);
        }

        // Determine the external face by walking clockwise starting at (firstExternalFaceVertex,
        // secondExternalFaceVertex)
        Map<Vertex, Map<Vertex, Vertex>> nextClockwise = new HashMap<Vertex, Map<Vertex, Vertex>>();
        Vertex prevVertex = firstExternalFaceVertex;
        Vertex vertex = secondExternalFaceVertex;
        List<Vertex> externalFace = new ArrayList<Vertex>();
        do {
            externalFace.add(vertex);

            // Compute vertexNextClockwise, a map from each vertex adjacent to "vertex" to the next vertex in the
            // clockwise direction
            Map<Vertex, Vertex> vertexNextClockwise = nextClockwise.get(vertex);
            if (vertexNextClockwise == null) {
                vertexNextClockwise = new HashMap<Vertex, Vertex>();
                List<Vertex> vertexClockwiseOrder = clockwiseOrder.get(vertex);
                Vertex prevOrderVertex = vertexClockwiseOrder.get(vertexClockwiseOrder.size() - 1);
                for (Vertex orderVertex : vertexClockwiseOrder) {
                    vertexNextClockwise.put(prevOrderVertex, orderVertex);
                    prevOrderVertex = orderVertex;
                }
                nextClockwise.put(vertex, vertexNextClockwise);
            }

            Vertex nextVertex = vertexNextClockwise.get(prevVertex);
            prevVertex = vertex;
            vertex = nextVertex;
        } while (prevVertex != firstExternalFaceVertex || vertex != secondExternalFaceVertex);

        return new PlanarEmbedding(clockwiseOrder, externalFace);
    }

    /**
     * Appends the leaf vertices in the subtree rooted at "node" to clockwiseOrder, in the clockwise order implied by
     * the specified combinatorial embedding of an ec-expansion graph.
     * @param start The vertex constrained according to the highest ancestor of "node".
     * @param node The node.
     * @param expansionClockwiseOrder The clockwise ordering of edges in the combinatorial embedding of the ec-expansion
     *     graph, as in PlanarEmbedding.clockwiseOrder.
     * @param edgeToExpansionEndpoint A map from each vetex V in the original, non-expanded graph to a map from each
     *     adjacent vertex W to the vertex in the ec-expansion corresponding to the start of the edge from V to W.
     * @param constraintVertices A map from each constraint node to the vertex in the ec-expansion for which we can
     *     determine the ordering of the node's children based on the clockwise order of vertices around it.  However,
     *     we skip over nodes with one child and nodes consolidated in consolidatedChildren.  See the comments for the
     *     constraintOrder argument to "expand".
     * @param constraintStarts A map from each constraint node N to the vertex adjacent to constraintVertices.get(N) for
     *     identifying the child of N that appears first.  However, we skip over nodes with one child and nodes
     *     consolidated in consolidatedChildren.  See the comments for the
     *     constraintOrder argument to "expand".
     * @param constraintOrder A map from each constraint node N to the vertices adjacent to constraintVertices.get(N)
     *     corresponding to N.children, in order.  However, we skip over nodes with one child and nodes consolidated in
     *     consolidatedChildren.  See the comments for the constraintOrder argument to "expand".
     * @param clockwiseOrder The list to which to append the leaf vertices.
     */
    private static void addClockwiseOrder(
            Vertex start, EcNode node, Map<Vertex, List<Vertex>> expansionClockwiseOrder,
            Map<Vertex, Map<Vertex, Vertex>> edgeToExpansionEndpoint, Map<EcNode, Vertex> constraintVertices,
            Map<EcNode, Vertex> constraintStarts, Map<EcNode, List<Vertex>> constraintOrder,
            List<Vertex> clockwiseOrder) {
        while (node.children.size() == 1) {
            node = node.children.get(0);
        }

        if (node.type == EcNode.Type.VERTEX) {
            clockwiseOrder.add(node.vertex);
        } else {
            // Compute a map from ec-expansion graph vertex to EcNode
            List<EcNode> children = consolidatedChildren(node);
            List<Vertex> nodeOrder = constraintOrder.get(node);
            Map<Vertex, EcNode> vertexToChild = new HashMap<Vertex, EcNode>();
            for (int i = 0; i < children.size(); i++) {
                EcNode child = children.get(i);
                Vertex orderVertex = nodeOrder.get(i);
                if (orderVertex != null) {
                    vertexToChild.put(orderVertex, child);
                } else {
                    vertexToChild.put(edgeToExpansionEndpoint.get(child.vertex).get(start), child);
                }
            }

            // Rotate expansionClockwiseOrder.get(constraintVertices.get(node)) so that constraintStarts.get(node) is
            // first
            List<Vertex> vertexClockwiseOrder = expansionClockwiseOrder.get(constraintVertices.get(node));
            Vertex constraintStart = constraintStarts.get(node);
            List<Vertex> rotatedClockwiseOrder;
            if (constraintStart == null) {
                rotatedClockwiseOrder = vertexClockwiseOrder;
            } else {
                int index = vertexClockwiseOrder.indexOf(constraintStarts.get(node));
                rotatedClockwiseOrder = new ArrayList<Vertex>(vertexClockwiseOrder.size());
                rotatedClockwiseOrder.addAll(vertexClockwiseOrder.subList(index, vertexClockwiseOrder.size()));
                rotatedClockwiseOrder.addAll(vertexClockwiseOrder.subList(0, index));
            }

            // Recurse on node.children, in the appropriate order
            for (Vertex orderVertex : rotatedClockwiseOrder) {
                EcNode child = vertexToChild.get(orderVertex);
                if (child != null) {
                    addClockwiseOrder(
                        start, child, expansionClockwiseOrder, edgeToExpansionEndpoint,
                        constraintVertices, constraintStarts, constraintOrder, clockwiseOrder);
                }
            }
        }
    }

    /**
     * Returns the ec-planar embedding of a graph, given the ec-planar embedding of its ec-expansion graph.
     * @param expansionEmbedding The ec-planar embedding of the ec-expansion graph.
     * @param start An arbitrary vertex in the non-expanded graph.
     * @param constraints A map from each constrained vertex in the non-expanded graph to the root node of its
     *     constraint tree.  It is okay for a vertex not to have a constraint tree.
     * @param edgeToExpansionEndpoint A map from each vetex V in the non-expanded graph to a map from each adjacent
     *     vertex W to the vertex in the ec-expansion corresponding to the start of the edge from V to W.
     * @param expansionEdgeToEdge A map from each edge in the expansion graph to the corresponding edge in the
     *     non-expansion graph.  We represent each edge as a pair of its endpoints.
     * @param constraintVertices A map from each constraint node to the vertex in the ec-expansion for which we can
     *     determine the ordering of the node's children based on the clockwise order of vertices around it.  However,
     *     we skip over nodes with one child and nodes consolidated in consolidatedChildren.  See the comments for the
     *     constraintOrder argument to "expand".
     * @param constraintStarts A map from each constraint node N to the vertex adjacent to constraintVertices.get(N) for
     *     identifying the child of N that appears first.  However, we skip over nodes with one child and nodes
     *     consolidated in consolidatedChildren.  See the comments for the
     *     constraintOrder argument to "expand".
     * @param constraintOrder A map from each constraint node N to the vertices adjacent to constraintVertices.get(N)
     *     corresponding to N.children, in order.  However, we skip over nodes with one child and nodes consolidated in
     *     consolidatedChildren.  See the comments for the constraintOrder argument to "expand".
     * @return The ec-planar embedding for the non-expanded graph.
     */
    private static PlanarEmbedding contractEmbedding(
            PlanarEmbedding expansionEmbedding, Vertex start, Map<Vertex, EcNode> constraints,
            Map<Vertex, Map<Vertex, Vertex>> edgeToExpansionEndpoint,
            Map<UnorderedPair<Vertex>, UnorderedPair<Vertex>> expansionEdgeToEdge,
            Map<EcNode, Vertex> constraintVertices, Map<EcNode, Vertex> constraintStarts,
            Map<EcNode, List<Vertex>> constraintOrder) {
        // Compute clockwiseOrder
        Map<Vertex, List<Vertex>> clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        for (Vertex vertex : component(start)) {
            EcNode node = constraints.get(vertex);
            List<Vertex> vertexClockwiseOrder = new ArrayList<Vertex>(vertex.edges.size());
            if (node != null) {
                addClockwiseOrder(
                    vertex, node, expansionEmbedding.clockwiseOrder, edgeToExpansionEndpoint,
                    constraintVertices, constraintStarts, constraintOrder, vertexClockwiseOrder);
            } else {
                Map<Vertex, Vertex> expansionVertexToVertex = new HashMap<Vertex, Vertex>();
                for (Vertex adjVertex : vertex.edges) {
                    expansionVertexToVertex.put(edgeToExpansionEndpoint.get(adjVertex).get(vertex), adjVertex);
                }
                Vertex expansionVertex = edgeToExpansionEndpoint.get(vertex).get(vertex.edges.iterator().next());
                for (Vertex orderVertex : expansionEmbedding.clockwiseOrder.get(expansionVertex)) {
                    vertexClockwiseOrder.add(expansionVertexToVertex.get(orderVertex));
                }
            }
            clockwiseOrder.put(vertex, vertexClockwiseOrder);
        }

        // Compute externalFace, consisting of all edges in expansionEmbedding.externalFace with entries in
        // expansionEdgeToEdge
        List<Vertex> externalFace = new ArrayList<Vertex>();
        Vertex prevExpansionVertex = expansionEmbedding.externalFace.get(expansionEmbedding.externalFace.size() - 1);
        for (Vertex expansionVertex : expansionEmbedding.externalFace) {
            UnorderedPair<Vertex> expansionEdge = new UnorderedPair<Vertex>(prevExpansionVertex, expansionVertex);
            UnorderedPair<Vertex> edge = expansionEdgeToEdge.get(expansionEdge);
            if (edge != null) {
                if (edgeToExpansionEndpoint.get(edge.value1).get(edge.value2) == prevExpansionVertex) {
                    externalFace.add(edge.value2);
                } else {
                    externalFace.add(edge.value1);
                }
            }
            prevExpansionVertex = expansionVertex;
        }

        return new PlanarEmbedding(clockwiseOrder, externalFace);
    }

    /** Adds all leaf vertices in the subtree rooted at "node" to "vertices". */
    private static void addVertices(EcNode node, Collection<Vertex> vertices) {
        if (node.type == EcNode.Type.VERTEX) {
            vertices.add(node.vertex);
        } else {
            for (EcNode child : node.children) {
                addVertices(child, vertices);
            }
        }
    }

    /**
     * Throws an IllegalArgumentException if the leaf vertices of the tree rooted at the EcNode to which some vertex V
     * maps do not match the vertices adjacent to V.
     */
    static void assertValid(Map<Vertex, EcNode> constraints) {
        for (Entry<Vertex, EcNode> entry : constraints.entrySet()) {
            Vertex vertex = entry.getKey();
            EcNode node = entry.getValue();
            if (node != null) {
                Collection<Vertex> nodeVertices = new ArrayList<Vertex>();
                addVertices(node, nodeVertices);
                if (nodeVertices.size() != vertex.edges.size() ||
                        !vertex.edges.containsAll(nodeVertices)) {
                    throw new IllegalArgumentException(
                        "The leaf vertices of the constraint for " + vertex + " do not match that vertex's adjacent " +
                        "vertices");
                }
            }
        }
    }

    /**
     * Returns an ec-planar embedding of the connected component containing "start", or null if there is no ec-planar
     * embedding.
     * @param start The vertex.
     * @param constraints A map from each constrained vertex to the root node of its constraint tree.  It is okay for a
     *     vertex not to have a constraint tree.
     * @return The ec-planar embedding.
     */
    public static PlanarEmbedding embed(Vertex start, Map<Vertex, EcNode> constraints) {
        assertValid(constraints);
        if (start.edges.isEmpty()) {
            return new PlanarEmbedding(
                Collections.singletonMap(start, Collections.<Vertex>emptyList()), Collections.singletonList(start));
        }

        // Add the portion of the ec-expansion graph corresponding to each vertex to "expansion"
        Collection<Vertex> component = component(start);
        Graph expansion = new Graph();
        Map<Vertex, Map<Vertex, Vertex>> edgeToExpansionEndpoint = new HashMap<Vertex, Map<Vertex, Vertex>>();
        Set<Vertex> hubs = new HashSet<Vertex>();
        Map<Vertex, Vertex> oHubFirsts = new HashMap<Vertex, Vertex>();
        Map<Vertex, Vertex> oHubSeconds = new HashMap<Vertex, Vertex>();
        Map<EcNode, Vertex> constraintVertices = new HashMap<EcNode, Vertex>();
        Map<EcNode, Vertex> constraintStarts = new HashMap<EcNode, Vertex>();
        Map<EcNode, List<Vertex>> constraintOrder = new HashMap<EcNode, List<Vertex>>();
        for (Vertex vertex : component) {
            EcNode constraint = constraints.get(vertex);
            Map<Vertex, Vertex> endToExpansionEndpoint = new HashMap<Vertex, Vertex>();
            if (constraint != null) {
                expand(
                    expansion, vertex, null, constraint, endToExpansionEndpoint, hubs, oHubFirsts, oHubSeconds,
                    constraintVertices, constraintStarts, constraintOrder);
            } else {
                Vertex expansionVertex = expansion.createVertex();
                for (Vertex adjVertex : vertex.edges) {
                    endToExpansionEndpoint.put(adjVertex, expansionVertex);
                }
            }
            edgeToExpansionEndpoint.put(vertex, endToExpansionEndpoint);
        }

        // Add the edges corresponding to edges in the original graph to the ec-expansion graph
        Map<UnorderedPair<Vertex>, UnorderedPair<Vertex>> expansionEdgeToEdge =
            new HashMap<UnorderedPair<Vertex>, UnorderedPair<Vertex>>();
        for (Vertex vertex : component) {
            for (Vertex adjVertex : vertex.edges) {
                Vertex expansionVertex = edgeToExpansionEndpoint.get(vertex).get(adjVertex);
                Vertex adjExpansionVertex = edgeToExpansionEndpoint.get(adjVertex).get(vertex);
                UnorderedPair<Vertex> expansionEdge = new UnorderedPair<Vertex>(expansionVertex, adjExpansionVertex);
                if (!expansionEdgeToEdge.containsKey(expansionEdge)) {
                    expansionVertex.addEdge(adjExpansionVertex);
                    UnorderedPair<Vertex> edge = new UnorderedPair<Vertex>(vertex, adjVertex);
                    expansionEdgeToEdge.put(expansionEdge, edge);
                }
            }
        }

        // Compute the ec-planar embedding
        PlanarEmbedding expansionEmbedding = embed(expansion, hubs, oHubFirsts, oHubSeconds);
        if (expansionEmbedding == null) {
            return null;
        } else {
            return contractEmbedding(
                expansionEmbedding, start, constraints, edgeToExpansionEndpoint, expansionEdgeToEdge,
                constraintVertices, constraintStarts, constraintOrder);
        }
    }
}
