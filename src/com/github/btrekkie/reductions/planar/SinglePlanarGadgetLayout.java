package com.github.btrekkie.reductions.planar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.visibility.VisibilityEdge;
import com.github.btrekkie.graph.visibility.VisibilityVertex;

/**
 * Positions a gadget and creates and positions wires and barriers in order to route the gadget's ports to the
 * appropriate points on the top and bottom edges of a specified rectangular region.  The gadget must have enough room
 * so that we can fit a wire at each port, barriers between the wires, and barriers at the corners, e.g. a barrier to
 * the left of the leftmost wire on the top that does not extend beyond the left edge of the gadget.  See "layout".
 */
/* The basic idea of SinglePlanarGadgetLayout is simple enough: we wind wires around the gadget in a clockwise spiral to
 * connect each port to the appropriate point, as shown below.  However, I found this surprisingly difficult to
 * implement.  My approach is to maintain a WireEndpoint for each port indicating the current end of the sequence of
 * wires starting at the port.  See the comments for WireEndpoint.  At each iteration, we extend each endpoint with one
 * wire, allocating a certain span of the side it will wind around next, and change its current "direction".  We extend
 * the endpoints in counterclockwise order, starting with some endpoint that is furthest clockwise on its respective
 * side.
 *
 * The fields specify the current state of the winding process.  The following diagram depicts a layout in progress,
 * showing the values of various fields:
 *
 *     ***************************************************
 *     *        *                                *       *
 *     * v      * t          vertex.y            * t     * v
 *     * e      * o                              * o     * e
 *     * r      * p     topY1                    * p     * r
 *     * t      * X                              * X     * t
 *     * e      * 1   *                          * 2     * e
 *     * x      *     * topY2                    *       * x
 *     * .   l  * l   *                          *       * .
 *     * m   e  * e   * r                        *       * m
 *     * i   f  * f   * i  ***********************       * a
 *     * n   t  * t   * g  * rightBarrierMinY            * x
 *     * X   X  * X   * h  *                             * X
 *     *     1  * 2   * t  *   ************* r   r       *
 *     *        *     * X  *   *           * i   i       *
 *     *        *     * F  *   *           * g   g       *
 *     *        *     * o **********       * h   h       *
 *     *        *     * r *        *       * t   t       *
 *     *        *     * T * Gadget *****   * X   X       *
 *     *        *     * o *        *   *   * 1   2       *
 *     *        *     * p **********   *   *             *
 *     *        *     * X  *     *     *   *             *
 *     *        *     * 1  *     *     *   *             *
 *     *        *     *    *     *     *   *             *
 *     *        *     ******     *     *   *             *
 *     *        *                *     *   *             *
 *     *        *                *     *   *             *
 *     *        *                *     *   *             *
 *     *        ******************     *   *             *
 *     *                               *   *             *
 *     *        bottomYForTopX1        *   *             *
 *     *                               *   *             *
 *     *                               *   *             *
 *     *                                   *             *
 *     * b                                 *             *
 *     * o                                 *             *
 *     * t                                 *             *
 *     * t                                               *
 *     * o                        bottomY1               *
 *     * m                                               *
 *     * X                                               *
 *     * 1                        bottomY2               *
 *     *                                                 *
 *     ***************************************************
 *
 *                        boundsBottomY
 *
 * To wind an endpoint around the left side, we add a vertical wire followed by a turn wire, and add a barrier gadget
 * extending from the bottom of the vertical wire toward the turn wire, but leaving room for the following barrier
 * gadget, which will start at the right side of the vertical wire near the top.  The other cases are symmetrical.
 *
 * In order to determine whether we can immediately connect an endpoint to its target point, we check each of the other
 * endpoints to see whether the path of any other endpoint to its target point must cross the path directly to the
 * target point.  This results in quadratic performance.  It's probably possible to achieve linear performance (constant
 * time per check for a crossing, at least on average), but I'm having a tough time figuring out how.  For what it's
 * worth, performance is linear in graphs of bounded degree.  Also, when we're using SinglePlanarGadgetLayout, it's
 * often as part of a quadratic time process anyway - due to either computing crossings, as in
 * EcPlanarEmbeddingWithCrossings, or rendering the resulting layout.
 */
class SinglePlanarGadgetLayout {
    /** The gadget we are positioning. */
    private final IPlanarGadget gadget;

    /**
     * A map from each vertex adjacent to the gadget to the index in gadget.ports() of the port to connect to the
     * vertex.  Note that not every port needs to be connected to something.
     */
    private Map<Vertex, Integer> edgePorts;

    /** The wire factory to use to create the wires. */
    private final IPlanarWireFactory wireFactory;

    /** The barrier factory to use to create the barriers. */
    private final IPlanarBarrierFactory barrierFactory;

    /** The y coordinate of the ports of horizontal wires. */
    private final int horizontalWireY;

    /** The x coordinate of the ports of vertical wires. */
    private final int verticalWireX;

    /**
     * The distance in the rightward direction from the left of the wire gadgets for the VisibilityEdges to the
     * VisibilityEdge.x values of the edges.
     */
    private final int edgeOffsetX;

    /** The VisibilityVertex indicating the position of the gadget and its edges. */
    private VisibilityVertex vertex;

    /** The top-left corner of the gadget. */
    private Point point;

    /**
     * The y coordinate of the lowest point we may next allocate below a wire for winding around the top of the gadget.
     */
    private int topY1;

    /**
     * The y coordinate of the top of the span we most recently allocated for winding around the top of the gadget.
     * This is point.y if we have not yet allocated such a span.
     */
    private int topY2;

    /**
     * The x coordinate of the right of the span we most recently allocated for winding around the right of the gadget.
     * This is point.x + gadget.width() if we have not yet allocated such a span.
     */
    private int rightX1;

    /**
     * The x coordinate of the leftmost point we may next allocate to the left of a wire for winding around the right of
     * the gadget.
     */
    private int rightX2;

    /**
     * The y coordinate of the bottom of the span we most recently allocated for winding around the bottom of the
     * gadget.  This is point.y + gadget.height() if we have not yet allocated such a span.
     */
    private int bottomY1;

    /**
     * The y coordinate of the highest point we may next allocate above a wire for winding around the bottom of the
     * gadget.
     */
    private int bottomY2;

    /**
     * The x coordinate of the rightmost point we may next allocate to the right of a wire for winding around the left
     * of the gadget.
     */
    private int leftX1;

    /**
     * The x coordinate of the left of the span we most recently allocated for winding around the left of the gadget.
     * This is point.x if we have not yet allocated such a span.
     */
    private int leftX2;

    /**
     * The x coordinate of the right edge of the wire we first added to wind around the left to connect a port to the
     * top.  This is leftX2 if we have finished making all connections and Integer.MAX_VALUE if we have not done so yet
     * and have yet to so connect such a port.
     */
    private int topX1;

    /**
     * The x coordinate of the left edge of the leftmost wire we used to connect a port to the top immediately after
     * winding around the top.  This is vertex.maxX if we have not done so.
     */
    private int topX2;

    /**
     * The x coordinate of the right edge of the rightmost wire we used to connect a port to the bottom immediately
     * after winding around the bottom.  This is vertex.minX if we have not done so.
     */
    private int bottomX1;

    /**
     * The x coordinate of the left edge of the wire we first added to wind around the right to connect a port to the
     * bottom.  This is rightX1 if we have finished making all connections and Integer.MAX_VALUE if we have not done so
     * yet and have yet to so connect such a port.
     */
    private int bottomX2;

    /**
     * The y coordinate of the bottom of the wire winding around the bottom immediately above the leftmost wire we used
     * to connect a port to the bottom immedately after winding around the bottom.
     */
    private int leftBarrierMaxY;

    /**
     * The y coordinate of the top of the wire winding around the top immediately below the rightmost wire we used to
     * connect a port to the top immedately after winding around the top.
     */
    private int rightBarrierMinY;

    /**
     * The x coordinate of left edge of the vertical wire immediately to the right of the wire we first added to wind
     * around the left to connect a port to the top.  This is point.x if the left edge of the gadget is farther left
     * than the next vertical wire, and Integer.MIN_VALUE if we have yet to so connect a port.
     */
    private int rightXForTopX1;

    /**
     * The y coordinate of the bottom edge of the non-turn wire we first added to wind around the left to connect a port
     * to the top.  This is Integer.MIN_VALUE if we have yet to so connect a port.
     */
    private int bottomYForTopX1;

    /**
     * The x coordinate of right edge of the vertical wire immediately to the left of the wire we first added to wind
     * around the right to connect a port to the bottom.  This is point.x + gadget.width() if the right edge of the
     * gadget is farther right than the next vertical wire, and Integer.MAX_VALUE if we have yet to so connect a port.
     */
    private int leftXForBottomX2;

    /**
     * The y coordinate of the top edge of the non-turn wire we first added to wind around the right to connect a port
     * to the bottom.  This is Integer.MAX_VALUE if we have yet to so connect a port.
     */
    private int topYForBottomX2;

    /**
     * The y coordinate of the bottom of the rectangular region surrounding the gadget where we are creating and
     * positioning wires and barriers.
     */
    private int boundsBottomY;

    /** Whether we are in the process of adding the first wire connected to each of the ports. */
    private boolean isFirstWind;

    public SinglePlanarGadgetLayout(
            IPlanarGadget gadget, Map<Vertex, Integer> edgePorts, IPlanarWireFactory wireFactory,
            IPlanarBarrierFactory barrierFactory, int horizontalWireY, int verticalWireX, int edgeOffsetX) {
        this.gadget = gadget;
        this.edgePorts = edgePorts;
        this.wireFactory = wireFactory;
        this.barrierFactory = barrierFactory;
        this.horizontalWireY = horizontalWireY;
        this.verticalWireX = verticalWireX;
        this.edgeOffsetX = edgeOffsetX;
    }

    /**
     * Returns the minimum width of the the rectangular region surrounding the gadget we require to route all of the
     * ports to the appropriate points.
     */
    public int minWidth() {
        // For each port, we wrap around the left and right sides a maximum of three times.  The first wrap around the
        // left side requires Math.max(wireFactory.width(), barrierFactory.minWidth()) space to the right of the
        // vertical wire, because if it is for a port on the left, we need room for the turn wire and for a barrier
        // gadget to the right of the wire.  Subsequent wraps require
        // barrierFactory.minWidth() + wireFactory.width() - 1 space to the right of the wire: barrierFactory.minWidth()
        // for the barrier gadget and wireFactory.width() - 1 in case we are about to connect to the top and need room
        // for a turn wire.  We need barrierFactory.minWidth() space to the left of the wire that last wraps around the
        // left side.
        int edgeCount = edgePorts.size();
        return gadget.width() +
            6 * Math.max(wireFactory.width(), barrierFactory.minWidth()) + // First time wrapping
            (6 * edgeCount - 3) * (barrierFactory.minWidth() + wireFactory.width() - 1) + // Wrapping after first time
            3 * barrierFactory.minWidth() + // The far edge
            6 * edgeCount * wireFactory.width(); // Wires
    }

    /**
     * Returns the minimum height of the the rectangular region surrounding the gadget we require to route all of the
     * ports to the appropriate points.
     */
    public int minHeight() {
        // For each port, we wrap around the top and bottom sides a maximum of three times.  The first wrap around the
        // top side requires Math.max(wireFactory.height(), barrierFactory.minHeight()) space below the horizontal wire,
        // because if it is for a port on the top, we need room for the turn wire and for a barrier gadget below the
        // wire.  Subsequent wraps require barrierFactory.minWidth() space below the wire.  We need
        // Math.max(wireFactory.height(), barrierFactory.minHeight()) space above the wire that last wraps around the
        // top side, again for a turn wire and a barrier gadget.
        int edgeCount = edgePorts.size();
        return gadget.height() +
            3 * Math.max(wireFactory.height(), barrierFactory.minHeight()) + // First time wrapping
            (6 * edgeCount - 3) * barrierFactory.minHeight() + // Wrapping after first time
            3 * Math.max(wireFactory.height(), barrierFactory.minHeight()) + // The far edge
            6 * edgeCount * wireFactory.height(); // Wires
    }

    /** Returns the x coordinate of the left edge where we should position the gadget. */
    private int gadgetX() {
        // See the comments for the implementation of minWidth() for an explanation of the following expression
        int edgeCount = edgePorts.size();
        return vertex.minX +
            Math.max(wireFactory.width(), barrierFactory.minWidth()) + // First time wrapping
            (2 * edgeCount - 1) * (barrierFactory.minWidth() + wireFactory.width() - 1) + // Wrapping after first time
            2 * barrierFactory.minWidth() + // The far edge
            2 * edgeCount * wireFactory.width(); // Wires
    }

    /** Returns the y coordinate of the top edge where we should position the gadget. */
    private int gadgetY() {
        // See the comments for the implementation of minHeight() for an explanation of the following expression
        int edgeCount = edgePorts.size();
        return vertex.y +
            Math.max(wireFactory.height(), barrierFactory.minHeight()) + // First time wrapping
            (2 * edgeCount - 1) * barrierFactory.minHeight() + // Wrapping after the first time
            Math.max(wireFactory.height(), barrierFactory.minHeight()) + // The far edge
            2 * edgeCount * wireFactory.height(); // Wires
    }

    /**
     * Returns the distance from the top-left corner of the gadget to the specified point in the clockwise direction
     * along the edges of the gadget.  The point is specified relative to the top-left corner of the gadget and must lie
     * on one of its edges.  The return value is in the range [0, 2 * gadget.width() + 2 * gadget.height()).
     */
    private int position(Point point) {
        int width = gadget.width();
        int height = gadget.height();
        if (point.y == 0) {
            return point.x;
        } else if (point.x == width) {
            return width + point.y;
        } else if (point.y == height) {
            return 2 * width + height - point.x;
        } else {
            return 2 * width + 2 * height - point.y;
        }
    }

    /**
     * Throws a RuntimeException if the gadget's ports are invalid: if they do not lie on the edges of the gadget; they
     * are not specified in clockwise order; they include a repeated point; or the gadget does not have enough room so
     * that we can fit a wire at each port, barriers between the wires, and barriers at the corners, e.g. a barrier to
     * the left of the leftmost wire on the top that does not extend beyond the left edge of the gadget.
     */
    private void validatePorts() {
        int width = gadget.width();
        int height = gadget.height();
        List<Point> ports = gadget.ports();
        for (Point port : ports) {
            if (port.x < 0 || port.y < 0 || port.x > width || port.y > height) {
                throw new RuntimeException(
                    "The port " + port + " lies outside the boundaries of the gadget " + gadget);
            } else if (port.x != 0 && port.y != 0 && port.x != width && port.y != height) {
                throw new RuntimeException("The port " + port + " does not lie on an edge of the gadget " + gadget);
            } else if (port.x == 0 || port.x == width) {
                if (port.y < barrierFactory.minHeight() + horizontalWireY) {
                    throw new RuntimeException(
                        "The port " + port + " of " + gadget + " does not leave room for a barrier gadget above it.  " +
                        port.y + " (port.y) < " + barrierFactory.minHeight() +
                        " (IPlanarBarrierFactory.minHeight()) + " + horizontalWireY +
                        " (y coordinate of IPlanarWireFactory.horizontalWire().ports())");
                } else if (port.y > height - barrierFactory.minHeight() - wireFactory.height() + horizontalWireY) {
                    throw new RuntimeException(
                        "The port " + port + " of " + gadget + " does not leave room for a barrier gadget below it.  " +
                        port.y + " (port.y) > " + height + " (gadget.height()) - " + barrierFactory.minHeight() +
                        " (IPlanarBarrierFactory.minHeight()) - " + wireFactory.height() +
                        " (IPlanarWireFactory.height()) + " + horizontalWireY +
                        " (y coordinate of IPlanarWireFactory.horizontalWire().ports())");
                }
            } else if (port.x < barrierFactory.minWidth() + verticalWireX) {
                throw new RuntimeException(
                    "The port " + port + " of " + gadget + " does not leave room for a barrier gadget to the left.  " +
                    port.x + " (port.x) < " + barrierFactory.minWidth() + " (IPlanarBarrierFactory.minWidth()) + " +
                    verticalWireX + " (x coordinate of IPlanarWireFactory.verticalWire().ports())");
            } else if (port.x > width - barrierFactory.minWidth() - wireFactory.width() + verticalWireX) {
                throw new RuntimeException(
                    "The port " + port + " of " + gadget + " does not leave room for a barrier gadget to the right.  " +
                    port.x + " (port.x) > " + width + " (gadget.width()) - " + barrierFactory.minWidth() +
                    " (IPlanarBarrierFactory.minWidth()) - " + wireFactory.width() +
                    " (IPlanarWireFactory.width()) + " + verticalWireX +
                    " (x coordinate of IPlanarWireFactory.verticalWire().ports())");
            }
        }

        if (ports.size() > 2) {
            // Check whether the ports are in clockwise order
            int startPosition = position(ports.get(1));
            int endPosition = position(ports.get(0));
            for (int i = 2; i < ports.size(); i++) {
                int position = position(ports.get(i));
                if (position == startPosition || position == endPosition) {
                    throw new RuntimeException("The gadget " + gadget + " has multiple ports at " + ports.get(i));
                } else if (startPosition > endPosition) {
                    // The clockwise arc from startPosition to endPosition consists of the ranges
                    // (startPosition, 2 * gadget.width() + 2 * gadget.height()) and (0, endPosition)
                    if (position > endPosition && position < startPosition) {
                        throw new RuntimeException("The ports of " + gadget + " are not in clockwise order");
                    }
                } else if (position < startPosition || position > endPosition) {
                    throw new RuntimeException("The ports of " + gadget + " are not in clockwise order");
                }
                startPosition = position;
            }
        }

        if (ports.size() > 1) {
            // Make sure there is enough space between the ports
            Point prevPort = ports.get(ports.size() - 1);
            for (Point port : ports) {
                if (port.x == 0) {
                    if (prevPort.x == 0 && prevPort.y < port.y + wireFactory.height() + barrierFactory.minHeight()) {
                        throw new RuntimeException(
                            "There is not enough space between ports " + port + " and " + prevPort + " of " + gadget +
                            " for a wire and a barrier gadget.  " + prevPort.y + " (port2.y) < " + port.y +
                            " (port1.y) + " + wireFactory.height() + " (IPlanarWireFactory.height()) + " +
                            barrierFactory.minHeight() + " (IPlanarBarrierFactory.minHeight())");
                    }
                } else if (port.y == 0) {
                    if (prevPort.y == 0 && port.x < prevPort.x + wireFactory.width() + barrierFactory.minWidth()) {
                        throw new RuntimeException(
                            "There is not enough space between ports " + prevPort + " and " + port + " of " + gadget +
                            " for a wire and a barrier gadget.  " + port.x + " (port2.x) < " + prevPort.x +
                            " (port1.x) + " + wireFactory.width() + " (IPlanarWireFactory.width()) + " +
                            barrierFactory.minWidth() + " (IPlanarBarrierFactory.minWidth())");
                    }
                } else if (port.x == width) {
                    if (prevPort.x == width &&
                            port.y < prevPort.y + wireFactory.height() + barrierFactory.minHeight()) {
                        throw new RuntimeException(
                            "There is not enough space between ports " + prevPort + " and " + port + " of " + gadget +
                            " for a wire and a barrier gadget.  " + port.y + " (port2.y) < " + prevPort.y +
                            " (port1.y) + " + wireFactory.height() + " (IPlanarWireFactory.height()) + " +
                            barrierFactory.minHeight() + " (IPlanarBarrierFactory.minHeight())");
                    }
                } else {
                    if (prevPort.y == height && prevPort.x < port.x + wireFactory.width() + barrierFactory.minWidth()) {
                        throw new RuntimeException(
                            "There is not enough space between ports " + port + " and " + port + " of " + gadget +
                            " for a wire and a barrier gadget.  " + prevPort.x + " (port2.x) < " + port.x +
                            " (port1.x) + " + wireFactory.width() + " (IPlanarWireFactory.width()) + " +
                            barrierFactory.minWidth() + " (IPlanarBarrierFactory.minWidth())");
                    }
                }
                prevPort = port;
            }
        }
    }

    /**
     * Whether we would need to extend the specified endpoint by wrapping around all four sides of the gadget to reach
     * its target point, even if we didn't have to worry about crossing any other endpoints' wires.
     */
    private boolean needsFullLoop(WireEndpoint endpoint) {
        // For the first wire, leave room for a barrier and a turn wire
        if (!endpoint.hasWire) {
            switch (endpoint.position) {
                case TOP_LEFT:
                    if (endpoint.targetX < endpoint.x + Math.max(wireFactory.width(), barrierFactory.minWidth())) {
                        return true;
                    }
                    break;
                case BOTTOM_RIGHT:
                    if (endpoint.targetX >
                            endpoint.x - wireFactory.width() -
                            Math.max(wireFactory.width(), barrierFactory.minWidth())) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }

        switch (endpoint.position) {
            case TOP_LEFT:
                return endpoint.isTargetUp && endpoint.targetX < endpoint.x + wireFactory.width() &&
                    endpoint.targetX != endpoint.x;
            case BOTTOM_LEFT:
                return endpoint.isTargetUp && endpoint.targetX < endpoint.x + 2 * wireFactory.width() &&
                    endpoint.targetX != endpoint.x + wireFactory.width();
            default:
                return !endpoint.isTargetUp && endpoint.targetX > endpoint.x - 2 * wireFactory.width() &&
                    endpoint.targetX != endpoint.x - wireFactory.width();
        }
    }

    /**
     * Returns WireEndpoints indicating the initial state of the endpoints for the ports.  The list contains the
     * endpoints in counterclockwise order, where the first endpoint is for the port that is furthest clockwise on its
     * respective side of the gadget.  Assumes the ports are valid, as in validatePorts().
     */
    private List<WireEndpoint> initializeEndpoints() {
        // Create the WireEndpoints so that portEndpoints.get(i) is the endpoint for gadget.ports().get(i)
        int width = gadget.width();
        int height = gadget.height();
        List<Point> ports = gadget.ports();
        List<WireEndpoint> portEndpoints = new ArrayList<WireEndpoint>(ports.size());
        for (int i = 0; i < ports.size(); i++) {
            portEndpoints.add(null);
        }
        for (VisibilityEdge edge : vertex.edges) {
            VisibilityVertex adjVertex = edge.adjVertex(vertex);
            int portIndex = edgePorts.get(adjVertex.vertex);
            Point port = ports.get(portIndex);
            int x;
            int y;
            WireEndpoint.Position position;
            if (port.x == width) {
                x = point.x + width;
                y = point.y + port.y - horizontalWireY;
                position = WireEndpoint.Position.TOP_LEFT;
            } else if (port.y == height) {
                x = point.x + port.x - verticalWireX;
                y = point.y + height;
                position = WireEndpoint.Position.TOP_RIGHT;
            } else if (port.x == 0) {
                x = point.x;
                y = point.y + port.y - horizontalWireY;
                position = WireEndpoint.Position.BOTTOM_RIGHT;
            } else {
                x = point.x + port.x - verticalWireX;
                y = point.y;
                position = WireEndpoint.Position.BOTTOM_LEFT;
            }

            int targetX = edge.x - edgeOffsetX;
            boolean isTargetUp = adjVertex.y < vertex.y;
            WireEndpoint endpoint = new WireEndpoint(targetX, isTargetUp, x, y, position);
            portEndpoints.set(portIndex, endpoint);
        }

        // Find the index of the first port in the clockwise direction from the top-left corner
        int index = 0;
        int startIndex = 0;
        int minPosition = Integer.MAX_VALUE;
        for (Point port : ports) {
            int position = position(port);
            if (position < minPosition) {
                startIndex = index;
                minPosition = position;
            }
            index++;
        }

        // Order the endpoints in counterclockwise order, starting with the first port in the counterclockwise direction
        // from the top-left corner
        List<WireEndpoint> endpoints = new ArrayList<WireEndpoint>(vertex.edges.size());
        for (int i = 0; i < ports.size(); i++) {
            index = (startIndex - i - 1 + ports.size()) % ports.size();
            WireEndpoint endpoint = portEndpoints.get(index);
            if (endpoint != null) {
                endpoints.add(endpoint);
            }
        }

        // Initialize the barrierCoord fields
        WireEndpoint prevEndpoint = endpoints.get(endpoints.size() - 1);
        for (WireEndpoint endpoint : endpoints) {
            boolean isSame = endpoint.position == prevEndpoint.position && endpoints.size() > 1;
            int barrierCoord;
            switch (endpoint.position) {
                case TOP_LEFT:
                    if (isSame) {
                        barrierCoord = prevEndpoint.y;
                    } else {
                        barrierCoord = point.y + height;
                    }
                    break;
                case TOP_RIGHT:
                    if (isSame) {
                        barrierCoord = prevEndpoint.x + wireFactory.width();
                    } else {
                        barrierCoord = point.x;
                    }
                    break;
                case BOTTOM_RIGHT:
                    if (isSame) {
                        barrierCoord = prevEndpoint.y + wireFactory.height();
                    } else {
                        barrierCoord = point.y;
                    }
                    break;
                default:
                    if (isSame) {
                        barrierCoord = prevEndpoint.x;
                    } else {
                        barrierCoord = point.x + width;
                    }
            }
            endpoint.barrierCoord = barrierCoord;
            prevEndpoint = endpoint;
        }
        return endpoints;
    }

    /**
     * Winds the specified endpoint having position == WireEndpoint.Position.TOP_LEFT around the top side.
     * @param endpoint The endpoint.
     * @param endpointsSet The endpoints that we have yet to connect to their target points.
     * @param layout The map to which to add mappings from any gadgets we create to their top-left corners.
     * @return Whether we finished connecting the endpoint to its corresponding target point.
     */
    private boolean windTopLeft(
            WireEndpoint endpoint, Set<WireEndpoint> endpointsSet, Map<IPlanarGadget, Point> layout) {
        boolean finish;
        if (endpoint.isTargetUp) {
            // Determine whether to immediately connect to the target point
            if (needsFullLoop(endpoint)) {
                finish = false;
            } else {
                finish = true;
                for (WireEndpoint otherEndpoint : endpointsSet) {
                    if (otherEndpoint != endpoint) {
                        if (otherEndpoint.isTargetUp) {
                            if (otherEndpoint.targetX > endpoint.targetX) {
                                finish = false;
                            } else if (otherEndpoint.position == WireEndpoint.Position.BOTTOM_LEFT ||
                                    otherEndpoint.position == WireEndpoint.Position.TOP_LEFT) {
                                finish = !needsFullLoop(otherEndpoint);
                            }
                        } else if (otherEndpoint.position == WireEndpoint.Position.BOTTOM_LEFT ||
                                otherEndpoint.position == WireEndpoint.Position.TOP_LEFT) {
                            finish = false;
                        } else {
                            finish = !needsFullLoop(otherEndpoint);
                        }
                    }
                    if (!finish) {
                        break;
                    }
                }
            }

            if (finish) {
                // Connect to the target point
                if (endpoint.targetX != endpoint.x) {
                    IPlanarGadget wire = wireFactory.createHorizontalWire(endpoint.targetX - endpoint.x);
                    layout.put(wire, new Point(endpoint.x, endpoint.y));
                }
                IPlanarGadget wire = wireFactory.createTurnWire();
                layout.put(wire, new Point(endpoint.targetX, endpoint.y));
                wire = wireFactory.createVerticalWire(endpoint.y - vertex.y);
                layout.put(wire, new Point(endpoint.targetX, vertex.y));
                IPlanarGadget barrier = barrierFactory.createBarrier(
                    topX2 - endpoint.x, endpoint.barrierCoord - endpoint.y - wireFactory.height());
                layout.put(barrier, new Point(endpoint.x, endpoint.y + wireFactory.height()));
                barrier = barrierFactory.createBarrier(
                    topX2 - endpoint.targetX - wireFactory.width(),
                    endpoint.y + wireFactory.height() - vertex.y);
                layout.put(barrier, new Point(endpoint.targetX + wireFactory.width(), vertex.y));
                if (topX2 >= vertex.maxX) {
                    rightBarrierMinY = endpoint.barrierCoord;
                }
                topX2 = endpoint.targetX;
            }
        } else {
            // Determine whether some other endpoint would have to cross the path if we were to immediately connect to
            // the target point
            boolean cross = false;
            for (WireEndpoint otherEndpoint : endpointsSet) {
                if (otherEndpoint != endpoint) {
                    if (otherEndpoint.isTargetUp) {
                        if (otherEndpoint.position == WireEndpoint.Position.BOTTOM_LEFT ||
                                otherEndpoint.position == WireEndpoint.Position.TOP_LEFT) {
                            cross = needsFullLoop(otherEndpoint);
                        }
                    } else if (otherEndpoint.targetX < endpoint.targetX) {
                        if (otherEndpoint.position == WireEndpoint.Position.BOTTOM_LEFT ||
                                otherEndpoint.position == WireEndpoint.Position.TOP_LEFT) {
                            cross = true;
                        } else {
                            cross = needsFullLoop(otherEndpoint);
                        }
                    }
                }
                if (cross) {
                    break;
                }
            }

            // Determine whether to immediately connect to the target point
            if (cross) {
                finish = false;
            } else if (endpoint.targetX >= rightX2) {
                finish = true;
            } else {
                finish = false;
                if (rightX2 <= endpoint.targetX + wireFactory.width()) {
                    rightX2 = endpoint.targetX + wireFactory.width();
                } else if (rightX2 < endpoint.targetX + 2 * wireFactory.width()) {
                    rightX2 = endpoint.targetX + 2 * wireFactory.width();
                }
            }

            if (finish) {
                // Connect to the target point
                IPlanarGadget wire = wireFactory.createHorizontalWire(endpoint.targetX - endpoint.x);
                layout.put(wire, new Point(endpoint.x, endpoint.y));
                wire = wireFactory.createTurnWire();
                layout.put(wire, new Point(endpoint.targetX, endpoint.y));
                wire = wireFactory.createVerticalWire(boundsBottomY - endpoint.y - wireFactory.height());
                layout.put(wire, new Point(endpoint.targetX, endpoint.y + wireFactory.height()));
                if (rightX1 > endpoint.x) {
                    IPlanarGadget barrier = barrierFactory.createBarrier(
                        rightX1 - endpoint.x, endpoint.barrierCoord - endpoint.y - wireFactory.height());
                    layout.put(barrier, new Point(endpoint.x, endpoint.y + wireFactory.height()));
                }
                if (bottomX2 != Integer.MIN_VALUE) {
                    IPlanarGadget barrier = barrierFactory.createBarrier(
                        endpoint.targetX - rightX1, boundsBottomY - endpoint.y - wireFactory.height());
                    layout.put(barrier, new Point(rightX1, endpoint.y + wireFactory.height()));
                } else {
                    bottomX2 = endpoint.targetX;
                    leftXForBottomX2 = rightX1;
                    topYForBottomX2 = endpoint.y + wireFactory.height();
                }
                rightX2 = endpoint.targetX + wireFactory.width() + barrierFactory.minWidth();
                rightX1 = rightX2 - barrierFactory.minWidth();
            }
        }

        if (!finish) {
            // Wind around the top
            IPlanarGadget wire = wireFactory.createHorizontalWire(rightX2 - endpoint.x);
            layout.put(wire, new Point(endpoint.x, endpoint.y));
            wire = wireFactory.createTurnWire();
            layout.put(wire, new Point(rightX2, endpoint.y));
            if (rightX1 > endpoint.x) {
                IPlanarGadget barrier = barrierFactory.createBarrier(
                    rightX1 - endpoint.x, endpoint.barrierCoord - endpoint.y - wireFactory.height());
                layout.put(barrier, new Point(endpoint.x, endpoint.y + wireFactory.height()));
            }

            endpoint.x = rightX2;
            endpoint.y += wireFactory.height();
            endpoint.position = WireEndpoint.Position.TOP_RIGHT;
            endpoint.barrierCoord = rightX1;
            rightX2 += wireFactory.width() + barrierFactory.minWidth();
            rightX1 = rightX2 - barrierFactory.minWidth();
        }
        return finish;
    }

    /**
     * Winds the specified endpoint having position == WireEndpoint.Position.TOP_RIGHT around the right side.
     * @param endpoint The endpoint.
     * @param layout The map to which to add mappings from any gadgets we create to their top-left corners.
     */
    private void windTopRight(WireEndpoint endpoint, Map<IPlanarGadget, Point> layout) {
        IPlanarGadget wire = wireFactory.createVerticalWire(bottomY2 - endpoint.y);
        layout.put(wire, new Point(endpoint.x, endpoint.y));
        wire = wireFactory.createTurnWire();
        layout.put(wire, new Point(endpoint.x, bottomY2));
        if (bottomY1 > endpoint.y) {
            IPlanarGadget barrier = barrierFactory.createBarrier(
                endpoint.x - endpoint.barrierCoord, bottomY1 - endpoint.y);
            layout.put(barrier, new Point(endpoint.barrierCoord, endpoint.y));
        }

        endpoint.y = bottomY2;
        endpoint.position = WireEndpoint.Position.BOTTOM_RIGHT;
        endpoint.barrierCoord = bottomY1;
        bottomY2 += wireFactory.height() + barrierFactory.minHeight();
        bottomY1 = bottomY2 - barrierFactory.minHeight();
    }

    /**
     * Winds the specified endpoint having position == WireEndpoint.Position.BOTTOM_RIGHT around the top side.
     * @param endpoint The endpoint.
     * @param endpointsSet The endpoints that we have yet to connect to their target points.
     * @param layout The map to which to add mappings from any gadgets we create to their top-left corners.
     * @return Whether we finished connecting the endpoint to its corresponding target point.
     */
    private boolean windBottomRight(
            WireEndpoint endpoint, Set<WireEndpoint> endpointsSet, Map<IPlanarGadget, Point> layout) {
        boolean finish;
        if (endpoint.isTargetUp) {
            // Determine whether some other endpoint would have to cross the path if we were to immediately connect to
            // the target point
            boolean cross = false;
            for (WireEndpoint otherEndpoint : endpointsSet) {
                if (otherEndpoint != endpoint) {
                    if (otherEndpoint.isTargetUp) {
                        if (otherEndpoint.targetX > endpoint.targetX) {
                            if (otherEndpoint.position == WireEndpoint.Position.TOP_RIGHT ||
                                    otherEndpoint.position == WireEndpoint.Position.BOTTOM_RIGHT) {
                                cross = true;
                            } else {
                                cross = needsFullLoop(otherEndpoint);
                            }
                        }
                    } else if (otherEndpoint.position == WireEndpoint.Position.TOP_RIGHT ||
                            otherEndpoint.position == WireEndpoint.Position.BOTTOM_RIGHT) {
                        cross = needsFullLoop(otherEndpoint);
                    }
                }
                if (cross) {
                    break;
                }
            }

            // Determine whether to immediately connect to the target point
            if (cross) {
                finish = false;
            } else if (endpoint.targetX + wireFactory.width() <= leftX1) {
                finish = true;
            } else {
                finish = false;
                if (leftX1 >= endpoint.targetX) {
                    leftX1 = endpoint.targetX;
                } else if (leftX1 > endpoint.targetX - wireFactory.width()) {
                    leftX1 = endpoint.targetX - wireFactory.width();
                }
            }

            if (finish) {
                // Connect to the target point
                IPlanarGadget wire = wireFactory.createHorizontalWire(
                    endpoint.x - endpoint.targetX - wireFactory.width());
                layout.put(wire, new Point(endpoint.targetX + wireFactory.width(), endpoint.y));
                wire = wireFactory.createTurnWire();
                layout.put(wire, new Point(endpoint.targetX, endpoint.y));
                wire = wireFactory.createVerticalWire(endpoint.y - vertex.y);
                layout.put(wire, new Point(endpoint.targetX, vertex.y));
                if (leftX2 < endpoint.x) {
                    IPlanarGadget barrier = barrierFactory.createBarrier(
                        endpoint.x - leftX2, endpoint.y - endpoint.barrierCoord);
                    layout.put(barrier, new Point(leftX2, endpoint.barrierCoord));
                }
                if (topX1 != Integer.MAX_VALUE) {
                    IPlanarGadget barrier = barrierFactory.createBarrier(
                        leftX2 - endpoint.targetX - wireFactory.width(), endpoint.y - vertex.y);
                    layout.put(barrier, new Point(endpoint.targetX + wireFactory.width(), vertex.y));
                } else {
                    topX1 = endpoint.targetX + wireFactory.width();
                    rightXForTopX1 = leftX2;
                    bottomYForTopX1 = endpoint.y;
                }
                leftX1 = endpoint.targetX - barrierFactory.minWidth();
                leftX2 = leftX1 + barrierFactory.minWidth();
            }
        } else {
            // Determine whether to immediately connect to the target point
            if (needsFullLoop(endpoint)) {
                finish = false;
            } else {
                finish = true;
                for (WireEndpoint otherEndpoint : endpointsSet) {
                    if (otherEndpoint != endpoint) {
                        if (otherEndpoint.isTargetUp) {
                            if (otherEndpoint.position == WireEndpoint.Position.TOP_RIGHT ||
                                    otherEndpoint.position == WireEndpoint.Position.BOTTOM_RIGHT) {
                                finish = false;
                            } else {
                                finish = !needsFullLoop(otherEndpoint);
                            }
                        } else if (otherEndpoint.targetX < endpoint.targetX) {
                            finish = false;
                        } else if (otherEndpoint.position == WireEndpoint.Position.TOP_RIGHT ||
                                otherEndpoint.position == WireEndpoint.Position.BOTTOM_RIGHT) {
                            finish = !needsFullLoop(otherEndpoint);
                        }
                    }
                    if (!finish) {
                        break;
                    }
                }
            }

            if (finish) {
                // Connect to the target point
                if (endpoint.targetX != endpoint.x - wireFactory.width()) {
                    IPlanarGadget wire = wireFactory.createHorizontalWire(
                        endpoint.x - endpoint.targetX - wireFactory.width());
                    layout.put(wire, new Point(endpoint.targetX + wireFactory.width(), endpoint.y));
                }
                IPlanarGadget wire = wireFactory.createTurnWire();
                layout.put(wire, new Point(endpoint.targetX, endpoint.y));
                wire = wireFactory.createVerticalWire(boundsBottomY - endpoint.y - wireFactory.height());
                layout.put(wire, new Point(endpoint.targetX, endpoint.y + wireFactory.height()));
                IPlanarGadget barrier = barrierFactory.createBarrier(
                    endpoint.x - bottomX1, endpoint.y - endpoint.barrierCoord);
                layout.put(barrier, new Point(bottomX1, endpoint.barrierCoord));
                barrier = barrierFactory.createBarrier(endpoint.targetX - bottomX1, boundsBottomY - endpoint.y);
                layout.put(barrier, new Point(bottomX1, endpoint.y));
                if (bottomX1 <= vertex.minX) {
                    leftBarrierMaxY = endpoint.barrierCoord;
                }
                bottomX1 = endpoint.targetX + wireFactory.width();
            }
        }

        if (!finish) {
            // Wind around the bottom
            IPlanarGadget wire = wireFactory.createHorizontalWire(endpoint.x - leftX1);
            layout.put(wire, new Point(leftX1, endpoint.y));
            wire = wireFactory.createTurnWire();
            layout.put(wire, new Point(leftX1 - wireFactory.width(), endpoint.y));
            if (leftX2 < endpoint.x) {
                IPlanarGadget barrier = barrierFactory.createBarrier(
                    endpoint.x - leftX2, endpoint.y - endpoint.barrierCoord);
                layout.put(barrier, new Point(leftX2, endpoint.barrierCoord));
            }

            endpoint.x = leftX1 - wireFactory.width();
            endpoint.position = WireEndpoint.Position.BOTTOM_LEFT;
            endpoint.barrierCoord = leftX2;
            leftX1 -= wireFactory.width() + barrierFactory.minWidth();
            leftX2 = leftX1 + barrierFactory.minWidth();
        }
        return finish;
    }

    /**
     * Winds the specified endpoint having position == WireEndpoint.Position.BOTTOM_LEFT around the left side.
     * @param endpoint The endpoint.
     * @param layout The map to which to add mappings from any gadgets we create to their top-left corners.
     */
    private void windBottomLeft(WireEndpoint endpoint, Map<IPlanarGadget, Point> layout) {
        IPlanarGadget wire = wireFactory.createVerticalWire(endpoint.y - topY1);
        layout.put(wire, new Point(endpoint.x, topY1));
        wire = wireFactory.createTurnWire();
        layout.put(wire, new Point(endpoint.x, topY1 - wireFactory.height()));
        if (topY2 < endpoint.y) {
            IPlanarGadget barrier = barrierFactory.createBarrier(
                endpoint.barrierCoord - endpoint.x - wireFactory.width(), endpoint.y - topY2);
            layout.put(barrier, new Point(endpoint.x + wireFactory.width(), topY2));
        }

        endpoint.x += wireFactory.width();
        endpoint.y = topY1 - wireFactory.height();
        endpoint.position = WireEndpoint.Position.TOP_LEFT;
        endpoint.barrierCoord = topY2;
        topY1 -= wireFactory.height() + barrierFactory.minHeight();
        topY2 = topY1 + barrierFactory.minHeight();
    }

    /**
     * Positions the gadget and creates and positions wires and barriers in order to route the gadget's ports to the
     * appropriate points on the top and bottom edges of the rectangular region with corners (vertex.minX, vertex.y) and
     * (vertex.maxX, vertex.y + minHeight()).  The gadget, wires, and barriers may occupy any portion of the rectangular
     * region.  See the comments for the edgeOffsetX field for an description of the points to which we route the ports.
     * This method adds barriers surrounding the gadget and the wires, as elaborated in the comments for
     * PlanarGadgetLayout.layout, except at the points to which we routed the ports.  There must be room between and to
     * the left and right of the points to which we are routing the ports for barrier gadgets.  The clockwise ordering
     * around the gadget of the points to which we are routing the ports must match the clockwise ordering of the ports.
     * @param vertex The VisibilityVertex identifying the location of the rectangular region for the gadget and whose
     *     VisibilityEdges indicate the points to which to route the ports.
     * @param layout The map to which to add mappings from the gadget and from any wires or barriers we create to their
     *     top-left corners.
     */
    public void layout(VisibilityVertex vertex, Map<IPlanarGadget, Point> layout) {
        this.vertex = vertex;
        validatePorts();
        point = new Point(gadgetX(), gadgetY());
        layout.put(gadget, point);
        int width = gadget.width();
        int height = gadget.height();
        List<WireEndpoint> endpoints = initializeEndpoints();
        Set<WireEndpoint> endpointsSet = new LinkedHashSet<WireEndpoint>(endpoints);

        // Compute the coordinate of the wire that will appear furthest counterclockwise on each side, adjacent to the
        // gadget
        int minTopX = Integer.MAX_VALUE;
        int minRightY = Integer.MAX_VALUE;
        int maxBottomX = Integer.MIN_VALUE;
        int maxLeftY = Integer.MIN_VALUE;
        for (WireEndpoint endpoint : endpoints) {
            switch (endpoint.position) {
                case TOP_LEFT:
                    minRightY = Math.min(minRightY, endpoint.y);
                    break;
                case TOP_RIGHT:
                    maxBottomX = Math.max(maxBottomX, endpoint.x + wireFactory.width());
                    break;
                case BOTTOM_RIGHT:
                    maxLeftY = Math.max(maxLeftY, endpoint.y + wireFactory.height());
                    break;
                default:
                    minTopX = Math.min(minTopX, endpoint.x);
                    break;
            }
        }

        // Initialize various fields
        topY1 = point.y - Math.max(wireFactory.height(), barrierFactory.minHeight());
        topY2 = point.y;
        rightX1 = point.x + width;
        rightX2 = point.x + width + Math.max(wireFactory.width(), barrierFactory.minWidth());
        bottomY1 = point.y + height;
        bottomY2 = point.y + height + Math.max(wireFactory.height(), barrierFactory.minHeight());
        leftX1 = point.x - Math.max(wireFactory.width(), barrierFactory.minWidth());
        leftX2 = point.x;
        topX1 = Integer.MAX_VALUE;
        topX2 = vertex.maxX;
        bottomX1 = vertex.minX;
        bottomX2 = Integer.MIN_VALUE;
        leftBarrierMaxY = Integer.MIN_VALUE;
        rightBarrierMinY = Integer.MAX_VALUE;
        rightXForTopX1 = Integer.MIN_VALUE;
        bottomYForTopX1 = Integer.MIN_VALUE;
        leftXForBottomX2 = Integer.MAX_VALUE;
        topYForBottomX2 = Integer.MAX_VALUE;
        boundsBottomY = vertex.y + minHeight();
        isFirstWind = true;

        // Keep winding the endpoints until they all reach their target points
        while (!endpoints.isEmpty()) {
            List<WireEndpoint> newEndpoints = new ArrayList<WireEndpoint>();
            for (WireEndpoint endpoint : endpoints) {
                switch (endpoint.position) {
                    case TOP_LEFT:
                        if (windTopLeft(endpoint, endpointsSet, layout)) {
                            endpointsSet.remove(endpoint);
                        } else {
                            newEndpoints.add(endpoint);
                        }
                        break;
                    case TOP_RIGHT:
                        windTopRight(endpoint, layout);
                        newEndpoints.add(endpoint);
                        break;
                    case BOTTOM_RIGHT:
                        if (windBottomRight(endpoint, endpointsSet, layout)) {
                            endpointsSet.remove(endpoint);
                        } else {
                            newEndpoints.add(endpoint);
                        }
                        break;
                    default:
                        windBottomLeft(endpoint, layout);
                        newEndpoints.add(endpoint);
                        break;
                }
                endpoint.hasWire = true;
            }

            if (isFirstWind) {
                // Add barriers at the counterclockwise end of each edge of the gadget
                if (minTopX < Integer.MAX_VALUE && topY2 < point.y) {
                    IPlanarGadget barrier = barrierFactory.createBarrier(minTopX - point.x, point.y - topY2);
                    layout.put(barrier, new Point(point.x, topY2));
                }
                if (minRightY < Integer.MAX_VALUE) {
                    int x;
                    if (topX2 >= vertex.maxX) {
                        x = rightX1;
                    } else {
                        x = topX2;
                    }
                    if (x > point.x + width) {
                        IPlanarGadget barrier = barrierFactory.createBarrier(x - point.x - width, minRightY - point.y);
                        layout.put(barrier, new Point(point.x + width, point.y));
                    }
                }
                if (maxBottomX > Integer.MIN_VALUE && bottomY1 > point.y + height) {
                    IPlanarGadget barrier = barrierFactory.createBarrier(
                        point.x + width - maxBottomX, bottomY1 - point.y - height);
                    layout.put(barrier, new Point(maxBottomX, point.y + height));
                }
                if (maxLeftY > Integer.MIN_VALUE) {
                    int x;
                    if (bottomX1 <= vertex.minX) {
                        x = leftX2;
                    } else {
                        x = bottomX1;
                    }
                    if (x < point.x) {
                        IPlanarGadget barrier = barrierFactory.createBarrier(point.x - x, point.y + height - maxLeftY);
                        layout.put(barrier, new Point(x, maxLeftY));
                    }
                }
                isFirstWind = false;
            }
            endpoints = newEndpoints;
        }

        // Add barriers for the outside
        if (leftBarrierMaxY == Integer.MIN_VALUE) {
            leftBarrierMaxY = bottomY1;
        }
        if (rightBarrierMinY == Integer.MAX_VALUE) {
            rightBarrierMinY = topY2;
        }
        if (topX1 == Integer.MAX_VALUE) {
            topX1 = leftX2;
        }
        if (bottomX2 == Integer.MIN_VALUE) {
            bottomX2 = rightX1;
        }
        IPlanarGadget barrier = barrierFactory.createBarrier(barrierFactory.minWidth(), leftBarrierMaxY - vertex.y);
        layout.put(barrier, new Point(leftX2 - barrierFactory.minWidth(), vertex.y));
        barrier = barrierFactory.createBarrier(barrierFactory.minWidth(), boundsBottomY - rightBarrierMinY);
        layout.put(barrier, new Point(rightX1, rightBarrierMinY));
        barrier = barrierFactory.createBarrier(topX2 - topX1, topY2 - vertex.y);
        layout.put(barrier, new Point(topX1, vertex.y));
        barrier = barrierFactory.createBarrier(bottomX2 - bottomX1, boundsBottomY - bottomY1);
        layout.put(barrier, new Point(bottomX1, bottomY1));

        // Now that we know the final value of topY2, we can go back and add the barrier to the right of the non-turn
        // wire we first added to wind around the left to connect a port to the top.  Likewise for the bottom.
        if (rightXForTopX1 != Integer.MIN_VALUE) {
            barrier = barrierFactory.createBarrier(rightXForTopX1 - topX1, bottomYForTopX1 - topY2);
            layout.put(barrier, new Point(topX1, topY2));
        }
        if (leftXForBottomX2 != Integer.MAX_VALUE) {
            barrier = barrierFactory.createBarrier(bottomX2 - leftXForBottomX2, bottomY1 - topYForBottomX2);
            layout.put(barrier, new Point(leftXForBottomX2, topYForBottomX2));
        }
    }
}
