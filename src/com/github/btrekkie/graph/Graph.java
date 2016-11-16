package com.github.btrekkie.graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** An undirected graph.  Self loops are not permitted. */
public class Graph {
    /** The vertices in the graph. */
    public Set<Vertex> vertices = new LinkedHashSet<Vertex>();

    /** Equivalent implementation is contractual. */
    public Vertex createVertex() {
        Vertex vertex = new Vertex();
        vertices.add(vertex);
        return vertex;
    }

    /**
     * Creates a PDF file depicting the graph.  Assumes the presence of the UNIX command-line program
     * /usr/local/bin/dot.  This method is intended for debugging.
     * @param filename The filename of the PDF file.
     * @throws IOException If there was an I/O exception writing the file.
     */
    public void writePdf(String filename) throws IOException {
        File file = File.createTempFile("graph", ".dot");
        try {
            Writer writer = new FileWriter(file);
            try {
                writer.write("graph {\n");
                Map<Vertex, Integer> vertexIds = new HashMap<Vertex, Integer>();
                for (Vertex vertex : vertices) {
                    int vertexId = vertexIds.size();
                    vertexIds.put(vertex, vertexId);
                    writer.write("    vertex" + vertexId + " [label=\"" + vertex.debugId + "\"];\n");
                }
                for (Vertex vertex : vertices) {
                    int vertexId = vertexIds.get(vertex);
                    for (Vertex adjVertex : vertex.edges) {
                        int adjVertexId = vertexIds.get(adjVertex);
                        if (vertexId < adjVertexId) {
                            writer.write("    vertex" + vertexId + " -- vertex" + adjVertexId + ";\n");
                        }
                    }
                }
                writer.write("}\n");
            } finally {
                writer.close();
            }
            try {
                Runtime.getRuntime().exec(
                    new String[]{"/usr/local/bin/dot", "-Tpdf", file.getAbsolutePath(), "-o", filename}).waitFor();
            } catch (InterruptedException exception) {
                throw new RuntimeException(exception);
            }
        } finally {
            file.delete();
        }
    }
}
