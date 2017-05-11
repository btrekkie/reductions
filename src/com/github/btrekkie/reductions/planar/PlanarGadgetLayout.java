package com.github.btrekkie.reductions.planar;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.planar.PlanarEmbedding;
import com.github.btrekkie.graph.visibility.VisibilityEdge;
import com.github.btrekkie.graph.visibility.VisibilityRepresentation;
import com.github.btrekkie.graph.visibility.VisibilityVertex;

/**
 * Positions gadgets and creates and positions wires and barriers in order to form a specified graph of IPlanarGadgets.
 * See "layout".  The coordinate system we use for PlanarGadgetLayout has y coordinates increasing in the downward
 * direction.
 */
/* PlanarGadgetLayout is implemented by treating each gadget as a rectangle and connecting these rectangles with
 * vertical wires.  SinglePlanarGadgetLayout is a black box that takes the rectangular region that PlanarGadgetLayout
 * assigns to a gadget and adds wires and barriers that route its ports to the appropriate points on the top and bottom
 * edges of the region.  To compute the positions of the rectangles and vertical wires, we compute a
 * VisibilityRepresentation.  Given the appropriate spacing parameters, especially the minVertexWidths and
 * minVertexVerticalSpace arguments to VisibilityRepresentation.compute, we can position a gadget's rectangle so that
 * the top edge is where the corresponding VisibilityVertex is located.
 */
public class PlanarGadgetLayout {
    /**
     * Positions gadgets and creates and positions wires and barriers in order to form the specified graph of
     * IPlanarGadgets.  The resulting layout will have the following characteristics:
     *
     * - No two gadgets will overlap.
     * - For each pair of adjacent non-barrier gadgets, one port of one gadget will coincide with one port of the other
     *   gadget.
     * - The gadgets in "gadgets" will connect to each other at the specified ports, after following some sequence of
     *   connected wires.
     * - All non-barrier gadgets will be "surrounded" by barriers, except where connected to wires.  This surrounding
     *   applies to corners: there will be at least a barrierFactory.minWidth() x barrierFactory.minHeight() region of
     *   barriers at each corner of each gadget, excluding the corners where there are connections between pairs of wire
     *   gadgets.
     * - There may be regions that are not occupied by any gadget.
     * - The top-left corner of the bounding box of all of the gadgets will be (0, 0).
     * - The "layout" method will attempt to produce a compact layout, i.e. one with a small bounding box.
     *
     * Each gadget in "gadgets" must have enough room so that we can fit a wire at each port, barriers between the
     * wires, and barriers at the corners, e.g. a barrier whose bottom-left corner is the top-left corner of the gadget.
     *
     * @param embedding The PlanarEmbedding indicating how to embed the gadgets.  This must be consistent with the ports
     *     specified in edgePorts.  The only information that "embedding" adds that we cannot infer from edgePorts is
     *     the external face.
     * @param gadgets A map from each vertex in the graph to the gadget for the vertex.
     * @param edgePorts A map from each vertex V in the graph to a map from each adjacent vertex W to the index in
     *     gadgets.get(V).ports() of the port to connect to W.  Note that not every port needs to be connected to
     *     something.
     * @param wireFactory The wire factory to use to create the wires.
     * @param barrierFactory The barrier factory to use to create the barriers.
     * @return A map from each gadget to its top-left corner.  This includes all of the gadgets in "gadgets", as well as
     *     any wires and barriers we create.
     */
    public static Map<IPlanarGadget, Point> layout(
            PlanarEmbedding embedding, Map<Vertex, IPlanarGadget> gadgets, Map<Vertex, Map<Vertex, Integer>> edgePorts,
            IPlanarWireFactory wireFactory, IPlanarBarrierFactory barrierFactory) {
        if (gadgets.size() == 1) {
            // Special case for one gadget
            IPlanarGadget gadget = gadgets.values().iterator().next();
            Map<IPlanarGadget, Point> layout = new LinkedHashMap<IPlanarGadget, Point>();
            layout.put(gadget, new Point(barrierFactory.minWidth(), barrierFactory.minHeight()));
            IPlanarGadget barrier = barrierFactory.createBarrier(
                gadget.width() + barrierFactory.minWidth(), barrierFactory.minHeight());
            layout.put(barrier, new Point(barrierFactory.minWidth(), 0));
            barrier = barrierFactory.createBarrier(
                barrierFactory.minWidth(), gadget.height() + barrierFactory.minHeight());
            layout.put(barrier, new Point(gadget.width() + barrierFactory.minWidth(), barrierFactory.minHeight()));
            barrier = barrierFactory.createBarrier(
                gadget.width() + barrierFactory.minWidth(), barrierFactory.minHeight());
            layout.put(barrier, new Point(0, gadget.height() + barrierFactory.minHeight()));
            barrier = barrierFactory.createBarrier(
                barrierFactory.minWidth(), gadget.height() + barrierFactory.minHeight());
            layout.put(barrier, new Point(0, 0));
            return layout;
        }

        // Check the ports of horizontal and vertical wires
        List<Point> ports = wireFactory.createHorizontalWire(wireFactory.width()).ports();
        if (ports.size() != 2) {
            throw new RuntimeException("A horizontal wire must have exactly two ports");
        }
        if (ports.get(0).y != ports.get(1).y) {
            throw new RuntimeException("The ports of a horizontal wire have different y coordinates");
        }
        int horizontalWireY = ports.get(0).y;
        ports = wireFactory.createVerticalWire(wireFactory.height()).ports();
        if (ports.size() != 2) {
            throw new RuntimeException("A vertical wire must have exactly two ports");
        }
        if (ports.get(0).x != ports.get(1).x) {
            throw new RuntimeException("The ports of a vertical wire have different x coordinates");
        }
        int verticalWireX = ports.get(0).x;

        // Compute the VisibilityRepresentation.  We leave Math.max(wireFactory.height(), barrierFactory.minHeight())
        // below each gadget to leave room for the wires connecting the gadgets and the adjacent barriers.  We set the
        // edge borders and other space parameters just right so that there is at least 2 * barrierFactory.minWidth()
        // between each pair of adjacent wires, i.e. the difference in VisibilityEdge.x is at least
        // 2 * barrierFactory.minWidth() + wireFactory.width().
        Map<Vertex, SinglePlanarGadgetLayout> singleLayouts = new HashMap<Vertex, SinglePlanarGadgetLayout>();
        Map<Vertex, Integer> minWidths = new HashMap<Vertex, Integer>();
        Map<Vertex, Integer> minHeights = new HashMap<Vertex, Integer>();
        for (Entry<Vertex, IPlanarGadget> entry : gadgets.entrySet()) {
            Vertex vertex = entry.getKey();
            IPlanarGadget gadget = entry.getValue();
            SinglePlanarGadgetLayout singleLayout = new SinglePlanarGadgetLayout(
                gadget, edgePorts.get(vertex), wireFactory, barrierFactory, horizontalWireY, verticalWireX,
                wireFactory.width() / 2);
            minWidths.put(vertex, singleLayout.minWidth());
            minHeights.put(
                vertex, singleLayout.minHeight() + Math.max(wireFactory.height(), barrierFactory.minHeight()));
            singleLayouts.put(vertex, singleLayout);
        }
        int halfDistanceBetweenWires = (wireFactory.width() + 1) / 2 + barrierFactory.minWidth();
        VisibilityRepresentation visibilityRepresentation = VisibilityRepresentation.compute(
            embedding, minWidths, minHeights, wireFactory.width() + 2 * barrierFactory.minWidth(),
            halfDistanceBetweenWires, halfDistanceBetweenWires);

        // Add the black box for each gadget
        Map<IPlanarGadget, Point> layout = new LinkedHashMap<IPlanarGadget, Point>();
        for (Vertex vertex : embedding.clockwiseOrder.keySet()) {
            VisibilityVertex visibilityVertex = visibilityRepresentation.vertices.get(vertex);
            singleLayouts.get(vertex).layout(visibilityVertex, layout);
        }

        // Add the wires connecting the black boxes and the barriers surrounding the wires
        for (VisibilityVertex vertex : visibilityRepresentation.vertices.values()) {
            int minHeight = minHeights.get(vertex.vertex);
            for (VisibilityEdge edge : vertex.edges) {
                VisibilityVertex adjVertex = edge.adjVertex(vertex);
                if (adjVertex.y > vertex.y) {
                    int bottomY = vertex.y + minHeight - Math.max(wireFactory.height(), barrierFactory.minHeight());
                    IPlanarGadget wire = wireFactory.createVerticalWire(adjVertex.y - bottomY);
                    Point wirePoint = new Point(edge.x - wireFactory.width() / 2, bottomY);
                    layout.put(wire, wirePoint);
                    IPlanarGadget barrier = barrierFactory.createBarrier(barrierFactory.minWidth(), wire.height());
                    layout.put(barrier, new Point(wirePoint.x - barrierFactory.minWidth(), bottomY));
                    barrier = barrierFactory.createBarrier(barrierFactory.minWidth(), wire.height());
                    layout.put(barrier, new Point(wirePoint.x + wireFactory.width(), bottomY));
                }
            }
        }

        // Move the gadgets so that the top-left corner is (0, 0)
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (Point point : layout.values()) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
        }
        Map<IPlanarGadget, Point> zeroLayout = new LinkedHashMap<IPlanarGadget, Point>();
        for (Entry<IPlanarGadget, Point> entry : layout.entrySet()) {
            Point point = entry.getValue();
            zeroLayout.put(entry.getKey(), new Point(point.x - minX, point.y - minY));
        }
        return zeroLayout;
    }
}
