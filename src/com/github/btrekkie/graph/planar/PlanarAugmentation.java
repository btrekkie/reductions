package com.github.btrekkie.graph.planar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.bc.BlockNode;
import com.github.btrekkie.graph.bc.CutNode;

/**
 * A planar augmentation: an addition of edges to an input graph that preserves planarity and is consistent with a
 * specified PlanarEmbedding.
 */
public class PlanarAugmentation {
    /** The PlanarEmbedding for "graph". */
    public final PlanarEmbedding embedding;

    /** The output graph, which consists of the input graph along with optional added edges. */
    public final Graph graph;

    /** A map from each vertex in "graph" to the corresponding vertex in the input graph. */
    public Map<Vertex, Vertex> vertexToOriginalVertex;

    public PlanarAugmentation(PlanarEmbedding embedding, Graph graph, Map<Vertex, Vertex> vertexToOriginalVertex) {
        this.embedding = embedding;
        this.graph = graph;
        this.vertexToOriginalVertex = vertexToOriginalVertex;
    }

    /**
     * Returns a map from each vertex to a map from each adjacent vertex to the next adjacent vertex in the clockwise
     * direction.  This is an alternative representation of clockwiseOrder.
     * @param clockwiseOrder The clockwise ordering of the vertices, as in PlanarEmbedding.clockwiseOrder.
     * @return The "next clockwise" map.
     */
    private static Map<Vertex, Map<Vertex, Vertex>> nextClockwise(Map<Vertex, List<Vertex>> clockwiseOrder) {
        Map<Vertex, Map<Vertex, Vertex>> nextClockwise = new HashMap<Vertex, Map<Vertex, Vertex>>();
        for (Entry<Vertex, List<Vertex>> entry : clockwiseOrder.entrySet()) {
            List<Vertex> vertexClockwiseOrder = entry.getValue();
            Map<Vertex, Vertex> vertexNextClockwise = new HashMap<Vertex, Vertex>();
            for (int i = 0; i < vertexClockwiseOrder.size() - 1; i++) {
                vertexNextClockwise.put(vertexClockwiseOrder.get(i), vertexClockwiseOrder.get(i + 1));
            }
            vertexNextClockwise.put(
                vertexClockwiseOrder.get(vertexClockwiseOrder.size() - 1), vertexClockwiseOrder.get(0));
            nextClockwise.put(entry.getKey(), vertexNextClockwise);
        }
        return nextClockwise;
    }

    /**
     * Returns a set consisting of an arbitrary non-cut vertex from each block in "graph" with exactly one cut vertex.
     * Assumes that "graph" is connected.
     */
    private static Set<Vertex> leafVertices(Graph graph) {
        BlockNode rootNode = BlockNode.compute(graph.vertices.iterator().next());

        // Use breadth-first search to iterate over the block nodes
        Set<Vertex> leafVertices = new HashSet<Vertex>();
        Collection<BlockNode> level = Collections.singleton(rootNode);
        while (!level.isEmpty()) {
            Collection<BlockNode> nextLevel = new ArrayList<BlockNode>();
            for (BlockNode node : level) {
                // Identify the sole adjacent cut vertex
                Vertex cutVertex;
                if (node.parent != null) {
                    if (!node.children.isEmpty()) {
                        cutVertex = null;
                    } else {
                        cutVertex = node.parent.vertex;
                    }
                } else if (node.children.size() != 1) {
                    cutVertex = null;
                } else {
                    cutVertex = node.children.iterator().next().vertex;
                }

                if (cutVertex != null) {
                    // Select an arbitrary non-cut vertex
                    Iterator<Vertex> iterator = node.block.vertices.iterator();
                    Vertex leafVertex = node.blockVertexToVertex.get(iterator.next());
                    if (leafVertex == cutVertex) {
                        leafVertex = node.blockVertexToVertex.get(iterator.next());
                    }
                    leafVertices.add(leafVertex);
                }

                for (CutNode child : node.children) {
                    nextLevel.addAll(child.children);
                }
            }
            level = nextLevel;
        }
        return leafVertices;
    }

    /**
     * Returns a new PlanarAugmentation object with the specified inserted edges.
     * @param embedding The embedding for the input graph that we are augmenting.
     * @param nextClockwiseInsertions A map from each vertex V to a map from each adjacent vertex W to a list of the
     *     vertices X to which we are adding an edge from V, so that the sequence of vertices immediately clockwise from
     *     W relative to V will consist of the sequence of vertices in the list.  nextClockwiseInsertions and its values
     *     may omit mappings for vertices without any such insertions.
     * @param externalFaceVertex1 An arbitrary vertex in the resulting external face.
     * @param externalFaceVertex2 The vertex in the resulting external face immediately clockwise relative to
     *     externalFaceVertex1.
     * @return The augmentation.
     */
    private static PlanarAugmentation createFromInsertions(
            PlanarEmbedding embedding, Map<Vertex, Map<Vertex, List<Vertex>>> nextClockwiseInsertions,
            Vertex externalFaceVertex1, Vertex externalFaceVertex2) {
        Graph graph = new Graph();

        // Create the vertices in "graph"
        Map<Vertex, Vertex> vertexToGraphVertex = new HashMap<Vertex, Vertex>();
        Map<Vertex, Vertex> graphVertexToVertex = new HashMap<Vertex, Vertex>();
        for (Vertex vertex : embedding.clockwiseOrder.keySet()) {
            Vertex graphVertex = graph.createVertex();
            vertexToGraphVertex.put(vertex, graphVertex);
            graphVertexToVertex.put(graphVertex, vertex);
        }

        // Compute the clockwise ordering for "graph"
        Map<Vertex, List<Vertex>> graphClockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        for (Entry<Vertex, List<Vertex>> entry : embedding.clockwiseOrder.entrySet()) {
            Vertex vertex = entry.getKey();
            List<Vertex> clockwiseOrder = entry.getValue();
            Map<Vertex, List<Vertex>> insertions = nextClockwiseInsertions.get(vertex);
            if (insertions == null) {
                insertions = Collections.emptyMap();
            }

            Vertex graphVertex = vertexToGraphVertex.get(vertex);
            List<Vertex> graphVertexClockwiseOrder = new ArrayList<Vertex>();
            for (Vertex orderVertex : clockwiseOrder) {
                graphVertexClockwiseOrder.add(vertexToGraphVertex.get(orderVertex));
                List<Vertex> vertexInsertions = insertions.get(orderVertex);
                if (vertexInsertions != null) {
                    for (Vertex insertion : vertexInsertions) {
                        graphVertexClockwiseOrder.add(vertexToGraphVertex.get(insertion));
                    }
                }
            }
            graphClockwiseOrder.put(graphVertex, graphVertexClockwiseOrder);

            for (Vertex graphOrderVertex : graphVertexClockwiseOrder) {
                graphVertex.addEdge(graphOrderVertex);
            }
        }

        // Compute the external face
        Map<Vertex, Map<Vertex, Vertex>> graphNextClockwise = nextClockwise(graphClockwiseOrder);
        List<Vertex> graphExternalFace = new ArrayList<Vertex>();
        Vertex start = vertexToGraphVertex.get(externalFaceVertex1);
        Vertex vertex = vertexToGraphVertex.get(externalFaceVertex2);
        Vertex prevVertex = start;
        graphExternalFace.add(vertex);
        while (vertex != start) {
            Vertex nextVertex = graphNextClockwise.get(vertex).get(prevVertex);
            prevVertex = vertex;
            vertex = nextVertex;
            graphExternalFace.add(vertex);
        }
        return new PlanarAugmentation(
            new PlanarEmbedding(graphClockwiseOrder, graphExternalFace), graph, graphVertexToVertex);
    }

    /**
     * Returns a PlanarAugmentation that augments the specified graph to make it biconnected, i.e. to make it so that
     * the graph remains connected if we remove any vertex.  "Augmentation" refers to an addition of edges to the input
     * graph that preserves planarity and is consistent with the specified PlanarEmbedding.  This method adds O(V)
     * edges, where V is the number of vertices in the graph.  If the input graph is already biconnected, this does not
     * add any edges.
     * @param embedding The embedding for the input graph.
     * @return The augmentation.
     */
    public static PlanarAugmentation makeBiconnected(PlanarEmbedding embedding) {
        /* This method works by finding all of the faces and making each of them biconnected.  To make a face
         * biconnected, we first identify one non-cut vertex per block with exactly one cut vertex.  Then, we add edges
         * from each such vertex to the next one, in the order in which the vertices appear on the face.  As elaborated
         * in section 3.2 of http://ls11-www.cs.uni-dortmund.de/_media/techreports/tr09-09.pdf (Zey (2009): Algorithms
         * for Planar Graph Augmentation), this has the effect of merging the nodes of the BC-tree into a single node.
         */

        // Special case for graphs with one vertex
        if (embedding.clockwiseOrder.size() == 1) {
            Graph graph = new Graph();
            Vertex graphVertex = graph.createVertex();
            Map<Vertex, List<Vertex>> clockwiseOrder = Collections.singletonMap(
                graphVertex, Collections.<Vertex>emptyList());
            List<Vertex> externalFace = Collections.singletonList(graphVertex);
            Map<Vertex, Vertex> graphVertexToVertex = Collections.singletonMap(
                graphVertex, embedding.clockwiseOrder.keySet().iterator().next());
            return new PlanarAugmentation(
                new PlanarEmbedding(clockwiseOrder, externalFace), graph, graphVertexToVertex);
        }

        // Iterate over all of the edges in order to find all of the faces
        Map<Vertex, Map<Vertex, Vertex>> nextClockwise = nextClockwise(embedding.clockwiseOrder);
        Map<Vertex, Set<Vertex>> visited = new HashMap<Vertex, Set<Vertex>>();
        Map<Vertex, Map<Vertex, List<Vertex>>> nextClockwiseInsertions =
            new HashMap<Vertex, Map<Vertex, List<Vertex>>>();
        Vertex externalFaceVertex1 = embedding.externalFace.get(0);
        Vertex externalFaceVertex2 = embedding.externalFace.get(1);
        for (Vertex vertex : embedding.clockwiseOrder.keySet()) {
            Set<Vertex> vertexVisited = visited.get(vertex);
            if (vertexVisited == null) {
                vertexVisited = new HashSet<Vertex>();
                visited.put(vertex, vertexVisited);
            }
            for (Vertex adjVertex : vertex.edges) {
                if (!vertexVisited.add(adjVertex)) {
                    continue;
                }

                // Create a Graph which is to consist exclusively of the current face
                Graph faceGraph = new Graph();
                Map<Vertex, Vertex> vertexToGraphVertex = new HashMap<Vertex, Vertex>();
                Map<Vertex, Vertex> graphVertexToVertex = new HashMap<Vertex, Vertex>();
                Vertex graphVertex = faceGraph.createVertex();
                vertexToGraphVertex.put(vertex, graphVertex);
                graphVertexToVertex.put(graphVertex, vertex);

                // Iterate over the edges of the current face, and add them to faceGraph
                List<Vertex> face = new ArrayList<Vertex>();
                Vertex prevVertex = vertex;
                Vertex faceVertex = adjVertex;
                Vertex prevGraphVertex = graphVertex;
                boolean isExternalFace = false;
                do {
                    graphVertex = vertexToGraphVertex.get(faceVertex);
                    if (graphVertex == null) {
                        graphVertex = faceGraph.createVertex();
                        vertexToGraphVertex.put(faceVertex, graphVertex);
                        graphVertexToVertex.put(graphVertex, faceVertex);
                    }
                    face.add(graphVertex);

                    prevGraphVertex.addEdge(graphVertex);
                    if (prevVertex == externalFaceVertex1 && faceVertex == externalFaceVertex2) {
                        // This is the external face
                        isExternalFace = true;
                    }

                    Set<Vertex> prevVertexVisited = visited.get(prevVertex);
                    if (prevVertexVisited == null) {
                        prevVertexVisited = new HashSet<Vertex>();
                        visited.put(prevVertex, prevVertexVisited);
                    }
                    prevVertexVisited.add(faceVertex);

                    Vertex nextVertex = nextClockwise.get(faceVertex).get(prevVertex);
                    prevVertex = faceVertex;
                    faceVertex = nextVertex;
                    prevGraphVertex = graphVertex;
                } while (faceVertex != adjVertex || prevVertex != vertex);
                graphVertex = vertexToGraphVertex.get(faceVertex);
                if (graphVertex == null) {
                    graphVertex = faceGraph.createVertex();
                    vertexToGraphVertex.put(faceVertex, graphVertex);
                    graphVertexToVertex.put(graphVertex, faceVertex);
                }
                prevGraphVertex.addEdge(graphVertex);

                // Add edges between leaf vertices
                Set<Vertex> leafVertices = leafVertices(faceGraph);
                Vertex graphPredecessor = face.get(face.size() - 1);
                Vertex prevLeafVertex = null;
                Vertex prevPredecessor = null;
                for (Vertex curVertex : face) {
                    if (leafVertices.contains(curVertex)) {
                        Vertex leafVertex = graphVertexToVertex.get(curVertex);
                        Vertex predecessor = graphVertexToVertex.get(graphPredecessor);
                        if (prevLeafVertex != null) {
                            Map<Vertex, List<Vertex>> insertions = nextClockwiseInsertions.get(prevLeafVertex);
                            if (insertions == null) {
                                insertions = new HashMap<Vertex, List<Vertex>>();
                                nextClockwiseInsertions.put(prevLeafVertex, insertions);
                            }
                            List<Vertex> edgeInsertions = insertions.get(prevPredecessor);
                            if (edgeInsertions == null) {
                                edgeInsertions = new ArrayList<Vertex>();
                                insertions.put(prevPredecessor, edgeInsertions);
                            }
                            edgeInsertions.add(leafVertex);

                            insertions = nextClockwiseInsertions.get(leafVertex);
                            if (insertions == null) {
                                insertions = new HashMap<Vertex, List<Vertex>>();
                                nextClockwiseInsertions.put(leafVertex, insertions);
                            }
                            edgeInsertions = insertions.get(predecessor);
                            if (edgeInsertions == null) {
                                edgeInsertions = new ArrayList<Vertex>();
                                insertions.put(predecessor, edgeInsertions);
                            }
                            edgeInsertions.add(prevLeafVertex);

                            if (isExternalFace) {
                                // Set externalFaceVertex1 and externalFaceVertex2 to an edge that will be in the
                                // external face of the output graph
                                externalFaceVertex1 = prevLeafVertex;
                                externalFaceVertex2 = leafVertex;
                            }
                        }
                        prevLeafVertex = leafVertex;
                        prevPredecessor = predecessor;
                    }
                    graphPredecessor = curVertex;
                }
            }
        }

        return createFromInsertions(embedding, nextClockwiseInsertions, externalFaceVertex1, externalFaceVertex2);
    }
}
