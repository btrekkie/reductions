package com.github.btrekkie.reductions.planar.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Test;

import com.github.btrekkie.graph.Graph;
import com.github.btrekkie.graph.Vertex;
import com.github.btrekkie.graph.planar.PlanarEmbedding;
import com.github.btrekkie.reductions.planar.IPlanarBarrierFactory;
import com.github.btrekkie.reductions.planar.IPlanarGadget;
import com.github.btrekkie.reductions.planar.IPlanarWireFactory;
import com.github.btrekkie.reductions.planar.PlanarGadgetLayout;
import com.github.btrekkie.reductions.planar.Point;

public class PlanarGadgetLayoutTest {
    /**
     * Returns whether any pair of the specified gadgets overlaps, excluding degenerate intersections of zero area.
     * @param gadgets A map from each gadget to its top-left corner.
     * @return Whether the gadgets overlap.
     */
    private static boolean overlaps(final Map<IPlanarGadget, Point> gadgets) {
        // This is implemented using a scan line algorithm.  We move from left to right, maintaining a record of the
        // intervals of y coordinates the scan line intersects.  If we ever add an interval that intersects an existing
        // interval, this returns true.

        // Sort the rectangles by minimum and maximum x coordinate
        List<IPlanarGadget> gadgetsByMinX = new ArrayList<IPlanarGadget>(gadgets.keySet());
        Collections.sort(gadgetsByMinX, new Comparator<IPlanarGadget>() {
            @Override
            public int compare(IPlanarGadget gadget1, IPlanarGadget gadget2) {
                return gadgets.get(gadget1).x - gadgets.get(gadget2).x;
            }
        });
        List<IPlanarGadget> gadgetsByMaxX = new ArrayList<IPlanarGadget>(gadgets.keySet());
        Collections.sort(gadgetsByMaxX, new Comparator<IPlanarGadget>() {
            @Override
            public int compare(IPlanarGadget gadget1, IPlanarGadget gadget2) {
                return gadgets.get(gadget1).x + gadget1.width() - gadgets.get(gadget2).x - gadget2.width();
            }
        });

        // Search for overlaps.  Maintain a map yIntervals from the minimum of each y interval to the corresponding
        // maximum.
        SortedMap<Integer, Integer> yIntervals = new TreeMap<Integer, Integer>();
        int gadgetsByMinXIndex = 0;
        int gadgetsByMaxXIndex = 0;
        while (gadgetsByMinXIndex < gadgetsByMinX.size()) {
            IPlanarGadget minXGadget = gadgetsByMinX.get(gadgetsByMinXIndex);
            IPlanarGadget maxXGadget = gadgetsByMaxX.get(gadgetsByMaxXIndex);
            Point minXPoint = gadgets.get(minXGadget);
            Point maxXPoint = gadgets.get(maxXGadget);
            if (minXPoint.x >= maxXPoint.x + maxXGadget.width()) {
                yIntervals.remove(maxXPoint.y);
                gadgetsByMaxXIndex++;
            } else {
                SortedMap<Integer, Integer> subMap = yIntervals.headMap(minXPoint.y + minXGadget.height());
                if (!subMap.isEmpty()) {
                    int maxY = subMap.get(subMap.lastKey());
                    if (maxY > minXPoint.y) {
                        return true;
                    }
                }
                yIntervals.put(minXPoint.y, minXPoint.y + minXGadget.height());
                gadgetsByMinXIndex++;
            }
        }
        return false;
    }

    /**
     * Determines the gadgets adjacent to each gadget.  This excludes pairs of gadgets that meet at their corners.
     * Assumes the gadgets don't overlap, excluding degenerate intersections of zero area.
     * @param gadgets A map from each gadget to its top-left corner.
     * @param leftGadgets A map to which to add the left adjacencies.  The map is from each gadget with at least one
     *     gadget that intersects the left edge to the gadgets that intersect the left edge, ordered from top to bottom.
     * @param rightGadgets A map to which to add the right adjacencies.  The map is from each gadget with at least one
     *     gadget that intersects the right edge to the gadgets that intersect the right edge, ordered from top to
     *     bottom.
     * @param topGadgets A map to which to add the top adjacencies.  The map is from each gadget with at least one
     *     gadget that intersects the top edge to the gadgets that intersect the top edge, ordered from left to right.
     * @param bottomGadgets A map to which to add the bottom adjacencies.  The map is from each gadget with at least one
     *     gadget that intersects the bottom edge to the gadgets that intersect the bottom edge, ordered from left to
     *     right.
     */
    private static void adjGadgets(
            final Map<IPlanarGadget, Point> gadgets,
            Map<IPlanarGadget, List<IPlanarGadget>> leftGadgets, Map<IPlanarGadget, List<IPlanarGadget>> rightGadgets,
            Map<IPlanarGadget, List<IPlanarGadget>> topGadgets, Map<IPlanarGadget, List<IPlanarGadget>> bottomGadgets) {
        // Compute maps from minimum and maximum x coordinates to gadgets
        Map<Integer, List<IPlanarGadget>> minXToGadgets = new HashMap<Integer, List<IPlanarGadget>>();
        Map<Integer, List<IPlanarGadget>> maxXToGadgets = new HashMap<Integer, List<IPlanarGadget>>();
        for (Entry<IPlanarGadget, Point> entry : gadgets.entrySet()) {
            IPlanarGadget gadget = entry.getKey();
            Point point = entry.getValue();
            List<IPlanarGadget> minXGadgets = minXToGadgets.get(point.x);
            if (minXGadgets == null) {
                minXGadgets = new ArrayList<IPlanarGadget>();
                minXToGadgets.put(point.x, minXGadgets);
            }
            minXGadgets.add(gadget);
            List<IPlanarGadget> maxXGadgets = maxXToGadgets.get(point.x + gadget.width());
            if (maxXGadgets == null) {
                maxXGadgets = new ArrayList<IPlanarGadget>();
                maxXToGadgets.put(point.x + gadget.width(), maxXGadgets);
            }
            maxXGadgets.add(gadget);
        }

        // Add the left-right adjacencies to leftGadgets and rightGadgets by iterating over all x coordinates that are
        // the left edge of some gadget and the right edge of some gadget
        Comparator<IPlanarGadget> yComparator = new Comparator<IPlanarGadget>() {
            @Override
            public int compare(IPlanarGadget gadget1, IPlanarGadget gadget2) {
                return gadgets.get(gadget1).y - gadgets.get(gadget2).y;
            }
        };
        Set<Integer> xs = new HashSet<Integer>(minXToGadgets.keySet());
        xs.retainAll(maxXToGadgets.keySet());
        for (int x : xs) {
            // Iterate over the left and right gadgets from top to bottom to identify any adjacencies
            List<IPlanarGadget> left = maxXToGadgets.get(x);
            List<IPlanarGadget> right = minXToGadgets.get(x);
            Collections.sort(left, yComparator);
            Collections.sort(right, yComparator);
            int leftIndex = 0;
            int rightIndex = 0;
            while (leftIndex < left.size() && rightIndex < right.size()) {
                IPlanarGadget leftGadget = left.get(leftIndex);
                Point leftPoint = gadgets.get(leftGadget);
                IPlanarGadget rightGadget = right.get(rightIndex);
                Point rightPoint = gadgets.get(rightGadget);
                if (leftPoint.y + leftGadget.height() > rightPoint.y &&
                        rightPoint.y + rightGadget.height() > leftPoint.y) {
                    List<IPlanarGadget> curRightGadgets = rightGadgets.get(leftGadget);
                    if (curRightGadgets == null) {
                        curRightGadgets = new ArrayList<IPlanarGadget>();
                        rightGadgets.put(leftGadget, curRightGadgets);
                    }
                    curRightGadgets.add(rightGadget);
                    List<IPlanarGadget> curLeftGadgets = leftGadgets.get(rightGadget);
                    if (curLeftGadgets == null) {
                        curLeftGadgets = new ArrayList<IPlanarGadget>();
                        leftGadgets.put(rightGadget, curLeftGadgets);
                    }
                    curLeftGadgets.add(leftGadget);
                }
                if (leftPoint.y + leftGadget.height() < rightPoint.y + rightGadget.height()) {
                    leftIndex++;
                } else {
                    rightIndex++;
                }
            }
        }

        // Compute maps from minimum and maximum y coordinates to gadgets
        Map<Integer, List<IPlanarGadget>> minYToGadgets = new HashMap<Integer, List<IPlanarGadget>>();
        Map<Integer, List<IPlanarGadget>> maxYToGadgets = new HashMap<Integer, List<IPlanarGadget>>();
        for (Entry<IPlanarGadget, Point> entry : gadgets.entrySet()) {
            IPlanarGadget gadget = entry.getKey();
            Point point = entry.getValue();
            List<IPlanarGadget> minYGadgets = minYToGadgets.get(point.y);
            if (minYGadgets == null) {
                minYGadgets = new ArrayList<IPlanarGadget>();
                minYToGadgets.put(point.y, minYGadgets);
            }
            minYGadgets.add(gadget);
            List<IPlanarGadget> maxYGadgets = maxYToGadgets.get(point.y + gadget.height());
            if (maxYGadgets == null) {
                maxYGadgets = new ArrayList<IPlanarGadget>();
                maxYToGadgets.put(point.y + gadget.height(), maxYGadgets);
            }
            maxYGadgets.add(gadget);
        }

        // Add the top-bottom adjacencies to topGadgets and bottomGadgets by iterating over all y coordinates that are
        // the top edge of some gadget and the bottom edge of some gadget
        Comparator<IPlanarGadget> xComparator = new Comparator<IPlanarGadget>() {
            @Override
            public int compare(IPlanarGadget gadget1, IPlanarGadget gadget2) {
                return gadgets.get(gadget1).x - gadgets.get(gadget2).x;
            }
        };
        Set<Integer> ys = new HashSet<Integer>(minYToGadgets.keySet());
        ys.retainAll(maxYToGadgets.keySet());
        for (int y : ys) {
            // Iterate over the top and bottom gadgets from left to right to identify any adjacencies
            List<IPlanarGadget> top = maxYToGadgets.get(y);
            List<IPlanarGadget> bottom = minYToGadgets.get(y);
            Collections.sort(top, xComparator);
            Collections.sort(bottom, xComparator);
            int topIndex = 0;
            int bottomIndex = 0;
            while (topIndex < top.size() && bottomIndex < bottom.size()) {
                IPlanarGadget topGadget = top.get(topIndex);
                Point topPoint = gadgets.get(topGadget);
                IPlanarGadget bottomGadget = bottom.get(bottomIndex);
                Point bottomPoint = gadgets.get(bottomGadget);
                if (topPoint.x + topGadget.width() > bottomPoint.x &&
                        bottomPoint.x + bottomGadget.width() > topPoint.x) {
                    List<IPlanarGadget> curBottomGadgets = bottomGadgets.get(topGadget);
                    if (curBottomGadgets == null) {
                        curBottomGadgets = new ArrayList<IPlanarGadget>();
                        bottomGadgets.put(topGadget, curBottomGadgets);
                    }
                    curBottomGadgets.add(bottomGadget);
                    List<IPlanarGadget> curTopGadgets = topGadgets.get(bottomGadget);
                    if (curTopGadgets == null) {
                        curTopGadgets = new ArrayList<IPlanarGadget>();
                        topGadgets.put(bottomGadget, curTopGadgets);
                    }
                    curTopGadgets.add(topGadget);
                }
                if (topPoint.x + topGadget.width() < bottomPoint.x + bottomGadget.width()) {
                    topIndex++;
                } else {
                    bottomIndex++;
                }
            }
        }
    }

    /**
     * Returns whether the edges of the specified gadget are entirely covered by other gadgets.  Assumes the gadgets
     * don't overlap, excluding degenerate intersections of zero area.
     * @param gadget The gadget.
     * @param gadgets A map from each gadget to its top-left corner.
     * @param leftGadgets The gadgets that intersect the left edge of "gadget", ordered from top to bottom.
     * @param rightGadgets The gadgets that intersect the right edge of "gadget", ordered from top to bottom.
     * @param topGadgets The gadgets that intersect the top edge of "gadget", ordered from left to right.
     * @param bottomGadgets The gadgets that intersect the bottom edge of "gadget", ordered from left to right.
     * @return Whether the gadget is surrounded.
     */
    private static boolean isSurrounded(
            IPlanarGadget gadget, Map<IPlanarGadget, Point> gadgets, List<IPlanarGadget> leftGadgets,
            List<IPlanarGadget> rightGadgets, List<IPlanarGadget> topGadgets, List<IPlanarGadget> bottomGadgets) {
        // Check the left edge
        Point point = gadgets.get(gadget);
        int prevY = point.y;
        for (IPlanarGadget adjGadget : leftGadgets) {
            int y = gadgets.get(adjGadget).y;
            if (y > prevY) {
                return false;
            }
            prevY = y + adjGadget.height();
        }
        if (prevY < point.y + gadget.height()) {
            return false;
        }

        // Check the right edge
        prevY = point.y;
        for (IPlanarGadget adjGadget : rightGadgets) {
            int y = gadgets.get(adjGadget).y;
            if (y > prevY) {
                return false;
            }
            prevY = y + adjGadget.height();
        }
        if (prevY < point.y + gadget.height()) {
            return false;
        }

        // Check the top edge
        int prevX = point.x;
        for (IPlanarGadget adjGadget : topGadgets) {
            int x = gadgets.get(adjGadget).x;
            if (x > prevX) {
                return false;
            }
            prevX = x + adjGadget.width();
        }
        if (prevX < point.x + gadget.width()) {
            return false;
        }

        // Check the bottom edge
        prevX = point.x;
        for (IPlanarGadget adjGadget : bottomGadgets) {
            int x = gadgets.get(adjGadget).x;
            if (x > prevX) {
                return false;
            }
            prevX = x + adjGadget.width();
        }
        if (prevX < point.x + gadget.width()) {
            return false;
        }
        return true;
    }

    /** Returns whether the specified gadget is a test wire gadget. */
    private static boolean isWire(IPlanarGadget gadget) {
        return gadget instanceof TestWireGadget || gadget instanceof TestTurnWireGadget;
    }

    /**
     * Returns whether there is at least a minBarrierWidth x minBarrierHeight region of barriers at each corner of the
     * specified gadget, excluding the corners where there are connections between pairs of wire gadgets.  We take each
     * gadget with no ports to be a barrier gadget.  Assumes the gadgets don't overlap, excluding degenerate
     * intersections of zero area.  Assumes the edges of the specified gadget are entirely covered by other gadgets.
     * @param gadget The gadget.
     * @param gadgets A map from each gadget to its top-left corner.
     * @param minBarrierWidth The minimum width of a barrier gadget.
     * @param minBarrierHeight The minimum height of a barrier gadget.
     * @param leftGadgets A map from each gadget with at least one gadget that intersects the left edge to the gadgets
     *     that intersect the left edge, ordered from top to bottom.
     * @param rightGadgets A map from each gadget with at least one gadget that intersects the right edge to the gadgets
     *     that intersect the right edge, ordered from top to bottom.
     * @param topGadgets A map from each gadget with at least one gadget that intersects the top edge to the gadgets
     *     that intersect the top edge, ordered from left to right.
     * @param bottomGadgets A map from each gadget with at least one gadget that intersects the bottom edge to the
     *     gadgets that intersect the bottom edge, ordered from left to right.
     * @return Whether the corners are blocked.
     */
    private static boolean areCornersBlocked(
            IPlanarGadget gadget, Map<IPlanarGadget, Point> gadgets, int minBarrierWidth, int minBarrierHeight,
            Map<IPlanarGadget, List<IPlanarGadget>> leftGadgets, Map<IPlanarGadget, List<IPlanarGadget>> rightGadgets,
            Map<IPlanarGadget, List<IPlanarGadget>> topGadgets, Map<IPlanarGadget, List<IPlanarGadget>> bottomGadgets) {
        // For the top-left corner, we check, at most, the topmost left gadget and its rightmost top gadget and the
        // leftmost top gadget and its bottommost left gadget.  The other corners are symmetrical.
        Point point = gadgets.get(gadget);
        List<IPlanarGadget> curLeftGadgets = leftGadgets.get(gadget);
        List<IPlanarGadget> curRightGadgets = rightGadgets.get(gadget);
        List<IPlanarGadget> curTopGadgets = topGadgets.get(gadget);
        List<IPlanarGadget> curBottomGadgets = bottomGadgets.get(gadget);

        // Check the top-left corner
        if (!isWire(gadget) || (!isWire(curLeftGadgets.get(0)) && !isWire(curTopGadgets.get(0)))) {
            IPlanarGadget barrier = curLeftGadgets.get(0);
            Point barrierPoint = gadgets.get(barrier);
            if (barrierPoint.y > point.y - minBarrierHeight) {
                if (barrierPoint.y < point.y) {
                    List<IPlanarGadget> barrierTopGadgets = topGadgets.get(barrier);
                    if (barrierTopGadgets == null) {
                        return false;
                    }
                    IPlanarGadget topGadget = barrierTopGadgets.get(barrierTopGadgets.size() - 1);
                    if (!topGadget.ports().isEmpty() || gadgets.get(topGadget).x + topGadget.width() < point.x) {
                        return false;
                    }
                } else {
                    barrier = curTopGadgets.get(0);
                    barrierPoint = gadgets.get(barrier);
                    if (barrierPoint.x > point.x - minBarrierWidth) {
                        List<IPlanarGadget> barrierLeftGadgets = leftGadgets.get(barrier);
                        if (barrierLeftGadgets == null) {
                            return false;
                        }
                        IPlanarGadget leftGadget = barrierLeftGadgets.get(barrierLeftGadgets.size() - 1);
                        if (!leftGadget.ports().isEmpty() ||
                                gadgets.get(leftGadget).y + leftGadget.height() < point.y) {
                            return false;
                        }
                    }
                }
            }
        }

        // Check the bottom-left corner
        if (!isWire(gadget) || (!isWire(curLeftGadgets.get(0)) && !isWire(curBottomGadgets.get(0)))) {
            IPlanarGadget barrier = curLeftGadgets.get(curLeftGadgets.size() - 1);
            Point barrierPoint = gadgets.get(barrier);
            if (barrierPoint.y + barrier.height() < point.y + gadget.height() + minBarrierHeight) {
                if (barrierPoint.y > point.y + gadget.height()) {
                    List<IPlanarGadget> barrierBottomGadgets = bottomGadgets.get(barrier);
                    if (barrierBottomGadgets == null) {
                        return false;
                    }
                    IPlanarGadget bottomGadget = barrierBottomGadgets.get(barrierBottomGadgets.size() - 1);
                    if (!bottomGadget.ports().isEmpty() ||
                            gadgets.get(bottomGadget).x + bottomGadget.width() < point.x) {
                        return false;
                    }
                } else {
                    barrier = curBottomGadgets.get(0);
                    barrierPoint = gadgets.get(barrier);
                    if (barrierPoint.x > point.x - minBarrierWidth) {
                        List<IPlanarGadget> barrierLeftGadgets = leftGadgets.get(barrier);
                        if (barrierLeftGadgets == null) {
                            return false;
                        }
                        IPlanarGadget leftGadget = barrierLeftGadgets.get(0);
                        if (!leftGadget.ports().isEmpty() || gadgets.get(leftGadget).y > point.y + gadget.height()) {
                            return false;
                        }
                    }
                }
            }
        }

        // Check the top-right corner
        if (!isWire(gadget) || (!isWire(curRightGadgets.get(0)) && !isWire(curTopGadgets.get(0)))) {
            IPlanarGadget barrier = curRightGadgets.get(0);
            Point barrierPoint = gadgets.get(barrier);
            if (barrierPoint.y > point.y - minBarrierHeight) {
                if (barrierPoint.y < point.y) {
                    List<IPlanarGadget> barrierTopGadgets = topGadgets.get(barrier);
                    if (barrierTopGadgets == null) {
                        return false;
                    }
                    IPlanarGadget topGadget = barrierTopGadgets.get(0);
                    if (!topGadget.ports().isEmpty() || gadgets.get(topGadget).x > point.x + gadget.width()) {
                        return false;
                    }
                } else {
                    barrier = curTopGadgets.get(curTopGadgets.size() - 1);
                    barrierPoint = gadgets.get(barrier);
                    if (barrierPoint.x + barrier.width() < point.x + gadget.width() + minBarrierWidth) {
                        List<IPlanarGadget> barrierRightGadgets = rightGadgets.get(barrier);
                        if (barrierRightGadgets == null) {
                            return false;
                        }
                        IPlanarGadget rightGadget = barrierRightGadgets.get(barrierRightGadgets.size() - 1);
                        if (!rightGadget.ports().isEmpty() ||
                                gadgets.get(rightGadget).y + rightGadget.height() < point.y) {
                            return false;
                        }
                    }
                }
            }
        }

        // Check the bottom-right corner
        if (!isWire(gadget) || (!isWire(curRightGadgets.get(0)) && !isWire(curBottomGadgets.get(0)))) {
            IPlanarGadget barrier = curRightGadgets.get(curRightGadgets.size() - 1);
            Point barrierPoint = gadgets.get(barrier);
            if (barrierPoint.y + barrier.height() < point.y + gadget.height() + minBarrierHeight) {
                if (barrierPoint.y > point.y + gadget.height()) {
                    List<IPlanarGadget> barrierBottomGadgets = bottomGadgets.get(barrier);
                    if (barrierBottomGadgets == null) {
                        return false;
                    }
                    IPlanarGadget bottomGadget = barrierBottomGadgets.get(0);
                    if (!bottomGadget.ports().isEmpty() || gadgets.get(bottomGadget).x > point.x + gadget.width()) {
                        return false;
                    }
                } else {
                    barrier = curBottomGadgets.get(curBottomGadgets.size() - 1);
                    barrierPoint = gadgets.get(barrier);
                    if (barrierPoint.x + barrier.width() < point.x + gadget.width() + minBarrierWidth) {
                        List<IPlanarGadget> barrierRightGadgets = rightGadgets.get(barrier);
                        if (barrierRightGadgets == null) {
                            return false;
                        }
                        IPlanarGadget rightGadget = barrierRightGadgets.get(0);
                        if (!rightGadget.ports().isEmpty() || gadgets.get(rightGadget).y > point.y + gadget.height()) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Adds mappings from each non-barrier gadget adjacent to "gadget" on its left edge to the indices in gadget.ports()
     * of the port at which those gadgets connect to edgePorts.  Throws an IllegalArgumentException if a gadget adjacent
     * to "gadget" on its left edge meets at zero ports or covers more than one port.  We take each gadget with no ports
     * to be a barrier gadget.
     * @param gadget The gadget.
     * @param ports The ports, as returned by gadget.ports().
     * @param portToIndex A map from each port in absolute coordinates to its index in "ports".
     * @param gadgets A map from each gadget to its top-left corner.
     * @param leftGadgets The gadgets that intersect the left edge of "gadget", ordered from top to bottom.
     * @param edgePorts The map to which to add the edge ports.
     */
    private static void leftEdgePorts(
            IPlanarGadget gadget, List<Point> ports, Map<Point, Integer> portToIndex, Map<IPlanarGadget, Point> gadgets,
            List<IPlanarGadget> leftGadgets, Map<IPlanarGadget, Integer> edgePorts) {
        Point point = gadgets.get(gadget);
        for (IPlanarGadget adjGadget : leftGadgets) {
            // Determine the port for adjGadget
            List<Point> adjPorts = adjGadget.ports();
            if (adjPorts.isEmpty()) {
                continue;
            }
            Point adjPoint = gadgets.get(adjGadget);
            Integer adjIndex = null;
            for (Point port : adjPorts) {
                adjIndex = portToIndex.get(new Point(adjPoint.x + port.x, adjPoint.y + port.y));
                if (adjIndex != null) {
                    break;
                }
            }
            if (adjIndex == null) {
                throw new IllegalArgumentException(
                    "The gadgets " + gadget + " and " + adjGadget + " are adjacent, but they do not meet at a port");
            }

            // Make sure adjGadget does not cover multiple ports
            if (ports.size() > 1) {
                int prevIndex;
                if (adjIndex > 0) {
                    prevIndex = adjIndex - 1;
                } else {
                    prevIndex = ports.size() - 1;
                }
                Point prevPort = ports.get(prevIndex);
                if (prevPort.x == 0 && prevPort.y > 0 && prevPort.y < gadget.height() &&
                        adjPoint.y + adjGadget.height() >= point.y + prevPort.y) {
                    throw new IllegalArgumentException(
                        "The gadget " + adjGadget + " is adjacent to multiple ports of " + gadget);
                }

                int nextIndex;
                if (adjIndex + 1 < ports.size()) {
                    nextIndex = adjIndex + 1;
                } else {
                    nextIndex = 0;
                }
                Point nextPort = ports.get(nextIndex);
                if (nextPort.x == 0 && nextPort.y > 0 && nextPort.y < gadget.height() && adjPoint.y <= point.y) {
                    throw new IllegalArgumentException(
                        "The gadget " + adjGadget + " is adjacent to multiple ports of " + gadget);
                }
            }
            edgePorts.put(adjGadget, adjIndex);
        }
    }

    /**
     * Adds mappings from each non-barrier gadget adjacent to "gadget" on its right edge to the indices in
     * gadget.ports() of the port at which those gadgets connect to edgePorts.  Throws an IllegalArgumentException if a
     * gadget adjacent to "gadget" on its right edge meets at zero ports or covers more than one port.  We take each
     * gadget with no ports to be a barrier gadget.
     * @param gadget The gadget.
     * @param ports The ports, as returned by gadget.ports().
     * @param portToIndex A map from each port in absolute coordinates to its index in "ports".
     * @param gadgets A map from each gadget to its top-left corner.
     * @param rightGadgets The gadgets that intersect the right edge of "gadget", ordered from top to bottom.
     * @param edgePorts The map to which to add the edge ports.
     */
    private static void rightEdgePorts(
            IPlanarGadget gadget, List<Point> ports, Map<Point, Integer> portToIndex, Map<IPlanarGadget, Point> gadgets,
            List<IPlanarGadget> rightGadgets, Map<IPlanarGadget, Integer> edgePorts) {
        Point point = gadgets.get(gadget);
        for (IPlanarGadget adjGadget : rightGadgets) {
            // Determine the port for adjGadget
            List<Point> adjPorts = adjGadget.ports();
            if (adjPorts.isEmpty()) {
                continue;
            }
            Point adjPoint = gadgets.get(adjGadget);
            Integer adjIndex = null;
            for (Point port : adjPorts) {
                adjIndex = portToIndex.get(new Point(adjPoint.x + port.x, adjPoint.y + port.y));
                if (adjIndex != null) {
                    break;
                }
            }
            if (adjIndex == null) {
                throw new IllegalArgumentException(
                    "The gadgets " + gadget + " and " + adjGadget + " are adjacent, but they do not meet at a port");
            }

            // Make sure adjGadget does not cover multiple ports
            if (ports.size() > 1) {
                int prevIndex;
                if (adjIndex > 0) {
                    prevIndex = adjIndex - 1;
                } else {
                    prevIndex = ports.size() - 1;
                }
                Point prevPort = ports.get(prevIndex);
                if (prevPort.x == gadget.width() && prevPort.y > 0 && prevPort.y < gadget.height() &&
                        adjPoint.y <= point.y) {
                    throw new IllegalArgumentException(
                        "The gadget " + adjGadget + " is adjacent to multiple ports of " + gadget);
                }

                int nextIndex;
                if (adjIndex + 1 < ports.size()) {
                    nextIndex = adjIndex + 1;
                } else {
                    nextIndex = 0;
                }
                Point nextPort = ports.get(nextIndex);
                if (nextPort.x == gadget.width() && nextPort.y > 0 && nextPort.y < gadget.height() &&
                        adjPoint.y + adjGadget.height() >= point.y + nextPort.y) {
                    throw new IllegalArgumentException(
                        "The gadget " + adjGadget + " is adjacent to multiple ports of " + gadget);
                }
            }
            edgePorts.put(adjGadget, adjIndex);
        }
    }

    /**
     * Adds mappings from each non-barrier gadget adjacent to "gadget" on its top edge to the indices in gadget.ports()
     * of the port at which those gadgets connect to edgePorts.  Throws an IllegalArgumentException if a gadget adjacent
     * to "gadget" on its top edge meets at zero ports or covers more than one port.  We take each gadget with no ports
     * to be a barrier gadget.
     * @param gadget The gadget.
     * @param ports The ports, as returned by gadget.ports().
     * @param portToIndex A map from each port in absolute coordinates to its index in "ports".
     * @param gadgets A map from each gadget to its top-left corner.
     * @param topGadgets The gadgets that intersect the top edge of "gadget", ordered from left to right.
     * @param edgePorts The map to which to add the edge ports.
     */
    private static void topEdgePorts(
            IPlanarGadget gadget, List<Point> ports, Map<Point, Integer> portToIndex, Map<IPlanarGadget, Point> gadgets,
            List<IPlanarGadget> topGadgets, Map<IPlanarGadget, Integer> edgePorts) {
        Point point = gadgets.get(gadget);
        for (IPlanarGadget adjGadget : topGadgets) {
            // Determine the port for adjGadget
            List<Point> adjPorts = adjGadget.ports();
            if (adjPorts.isEmpty()) {
                continue;
            }
            Point adjPoint = gadgets.get(adjGadget);
            Integer adjIndex = null;
            for (Point port : adjPorts) {
                adjIndex = portToIndex.get(new Point(adjPoint.x + port.x, adjPoint.y + port.y));
                if (adjIndex != null) {
                    break;
                }
            }
            if (adjIndex == null) {
                throw new IllegalArgumentException(
                    "The gadgets " + gadget + " and " + adjGadget + " are adjacent, but they do not meet at a port");
            }

            // Make sure adjGadget does not cover multiple ports
            if (ports.size() > 1) {
                int prevIndex;
                if (adjIndex > 0) {
                    prevIndex = adjIndex - 1;
                } else {
                    prevIndex = ports.size() - 1;
                }
                Point prevPort = ports.get(prevIndex);
                if (prevPort.y == 0 && prevPort.x > 0 && prevPort.x < gadget.width() && adjPoint.x <= point.x) {
                    throw new IllegalArgumentException(
                        "The gadget " + adjGadget + " is adjacent to multiple ports of " + gadget);
                }

                int nextIndex;
                if (adjIndex + 1 < ports.size()) {
                    nextIndex = adjIndex + 1;
                } else {
                    nextIndex = 0;
                }
                Point nextPort = ports.get(nextIndex);
                if (nextPort.y == 0 && nextPort.x > 0 && nextPort.x < gadget.width() &&
                        adjPoint.x + adjGadget.width() >= point.x + nextPort.x) {
                    throw new IllegalArgumentException(
                        "The gadget " + adjGadget + " is adjacent to multiple ports of " + gadget);
                }
            }
            edgePorts.put(adjGadget, adjIndex);
        }
    }

    /**
     * Adds mappings from each non-barrier gadget adjacent to "gadget" on its bottom edge to the indices in
     * gadget.ports() of the port at which those gadgets connect to edgePorts.  Throws an IllegalArgumentException if a
     * gadget adjacent to "gadget" on its bottom edge meets at zero ports or covers more than one port.  We take each
     * gadget with no ports to be a barrier gadget.
     * @param gadget The gadget.
     * @param ports The ports, as returned by gadget.ports().
     * @param portToIndex A map from each port in absolute coordinates to its index in "ports".
     * @param gadgets A map from each gadget to its top-left corner.
     * @param bottomGadgets The gadgets that intersect the bottom edge of "gadget", ordered from left to right.
     * @param edgePorts The map to which to add the edge ports.
     */
    private static void bottomEdgePorts(
            IPlanarGadget gadget, List<Point> ports, Map<Point, Integer> portToIndex, Map<IPlanarGadget, Point> gadgets,
            List<IPlanarGadget> bottomGadgets, Map<IPlanarGadget, Integer> edgePorts) {
        Point point = gadgets.get(gadget);
        for (IPlanarGadget adjGadget : bottomGadgets) {
            // Determine the port for adjGadget
            List<Point> adjPorts = adjGadget.ports();
            if (adjPorts.isEmpty()) {
                continue;
            }
            Point adjPoint = gadgets.get(adjGadget);
            Integer adjIndex = null;
            for (Point port : adjPorts) {
                adjIndex = portToIndex.get(new Point(adjPoint.x + port.x, adjPoint.y + port.y));
                if (adjIndex != null) {
                    break;
                }
            }
            if (adjIndex == null) {
                throw new IllegalArgumentException(
                    "The gadgets " + gadget + " and " + adjGadget + " are adjacent, but they do not meet at a port");
            }

            // Make sure adjGadget does not cover multiple ports
            if (ports.size() > 1) {
                int prevIndex;
                if (adjIndex > 0) {
                    prevIndex = adjIndex - 1;
                } else {
                    prevIndex = ports.size() - 1;
                }
                Point prevPort = ports.get(prevIndex);
                if (prevPort.y == gadget.height() && prevPort.x > 0 && prevPort.x < gadget.width() &&
                        adjPoint.x + adjGadget.width() >= point.x + prevPort.x) {
                    throw new IllegalArgumentException(
                        "The gadget " + adjGadget + " is adjacent to multiple ports of " + gadget);
                }

                int nextIndex;
                if (adjIndex + 1 < ports.size()) {
                    nextIndex = adjIndex + 1;
                } else {
                    nextIndex = 0;
                }
                Point nextPort = ports.get(nextIndex);
                if (nextPort.y == gadget.height() && nextPort.x > 0 && nextPort.x < gadget.width() &&
                        adjPoint.x <= point.x) {
                    throw new IllegalArgumentException(
                        "The gadget " + adjGadget + " is adjacent to multiple ports of " + gadget);
                }
            }
            edgePorts.put(adjGadget, adjIndex);
        }
    }

    /**
     * Returns a map from each non-barrier gadget adjacent to "gadget" to the indices in gadget.ports() of the port at
     * which those gadgets connect.  Throws an IllegalArgumentException if a gadget adjacent to "gadget" meets at zero
     * ports or covers more than one port.  We take each gadget with no ports to be a barrier gadget.
     * @param gadget The gadget.
     * @param gadgets A map from each gadget to its top-left corner.
     * @param leftGadgets The gadgets that intersect the left edge of "gadget", ordered from top to bottom.
     * @param rightGadgets The gadgets that intersect the right edge of "gadget", ordered from top to bottom.
     * @param topGadgets The gadgets that intersect the top edge of "gadget", ordered from left to right.
     * @param bottomGadgets The gadgets that intersect the bottom edge of "gadget", ordered from left to right.
     * @return The edge ports.
     */
    private static Map<IPlanarGadget, Integer> edgePorts(
            IPlanarGadget gadget, Map<IPlanarGadget, Point> gadgets, List<IPlanarGadget> leftGadgets,
            List<IPlanarGadget> rightGadgets, List<IPlanarGadget> topGadgets, List<IPlanarGadget> bottomGadgets) {
        // Compute a map from each port in absolute coordinates to its index
        Point point = gadgets.get(gadget);
        List<Point> ports = gadget.ports();
        Map<Point, Integer> portToIndex = new HashMap<Point, Integer>();
        int index = 0;
        for (Point port : ports) {
            portToIndex.put(new Point(point.x + port.x, point.y + port.y), index);
            index++;
        }

        // Compute the edge ports
        Map<IPlanarGadget, Integer> edgePorts = new LinkedHashMap<IPlanarGadget, Integer>();
        leftEdgePorts(gadget, ports, portToIndex, gadgets, leftGadgets, edgePorts);
        rightEdgePorts(gadget, ports, portToIndex, gadgets, rightGadgets, edgePorts);
        topEdgePorts(gadget, ports, portToIndex, gadgets, topGadgets, edgePorts);
        bottomEdgePorts(gadget, ports, portToIndex, gadgets, bottomGadgets, edgePorts);
        return edgePorts;
    }

    /**
     * Returns a map from each gadget G in "gadgets" adjacent to at least one non-barrier gadget to a map from each
     * adjacent non-barrier gadget to the indices in G.ports() of the port at which those gadgets connect.  Throws an
     * IllegalArgumentException if the gadgets violate the constraints described in the comments for PlanarGadgetLayout.
     * We take gadgets with no ports to be barrier gadgets, and gadgets of type TestWire and TestTurnWire to be wires.
     * Assumes that all barrier gadgets are at least minBarrierWidth x minBarrierHeight.
     * @param gadgets A map from each gadget to its top-left corner.
     * @param minBarrierWidth The minimum width of a barrier gadget.
     * @param minBarrierHeight The minimum height of a barrier gadget.
     * @return The graph.
     */
    public static Map<IPlanarGadget, Map<IPlanarGadget, Integer>> gadgetGraph(
            Map<IPlanarGadget, Point> gadgets, int minBarrierWidth, int minBarrierHeight) {
        if (overlaps(gadgets)) {
            throw new IllegalArgumentException("Two of the gadgets overlap each other");
        }

        // Check the top-left corner
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (Point point : gadgets.values()) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
        }
        if (minX != 0 || minY != 0) {
            throw new IllegalArgumentException("The top-left corner of the bounding box is not (0, 0)");
        }

        // Compute adjacent gadgets
        Map<IPlanarGadget, List<IPlanarGadget>> leftGadgets = new HashMap<IPlanarGadget, List<IPlanarGadget>>();
        Map<IPlanarGadget, List<IPlanarGadget>> rightGadgets = new HashMap<IPlanarGadget, List<IPlanarGadget>>();
        Map<IPlanarGadget, List<IPlanarGadget>> topGadgets = new HashMap<IPlanarGadget, List<IPlanarGadget>>();
        Map<IPlanarGadget, List<IPlanarGadget>> bottomGadgets = new HashMap<IPlanarGadget, List<IPlanarGadget>>();
        adjGadgets(gadgets, leftGadgets, rightGadgets, topGadgets, bottomGadgets);

        // Compute the graph
        Map<IPlanarGadget, Map<IPlanarGadget, Integer>> graph =
            new HashMap<IPlanarGadget, Map<IPlanarGadget, Integer>>();
        for (Entry<IPlanarGadget, Point> entry : gadgets.entrySet()) {
            IPlanarGadget gadget = entry.getKey();
            if (gadget.ports().isEmpty()) {
                continue;
            }

            // Make sure the edges and corners are blocked
            List<IPlanarGadget> curLeftGadgets = leftGadgets.get(gadget);
            List<IPlanarGadget> curRightGadgets = rightGadgets.get(gadget);
            List<IPlanarGadget> curTopGadgets = topGadgets.get(gadget);
            List<IPlanarGadget> curBottomGadgets = bottomGadgets.get(gadget);
            if (curLeftGadgets == null || curRightGadgets == null || curTopGadgets == null ||
                    curBottomGadgets == null ||
                    !isSurrounded(gadget, gadgets, curLeftGadgets, curRightGadgets, curTopGadgets, curBottomGadgets)) {
                throw new IllegalArgumentException("The gadget " + gadget + " is not surrounded by other gadgets");
            }
            if (!areCornersBlocked(
                    gadget, gadgets, minBarrierWidth, minBarrierHeight,
                    leftGadgets, rightGadgets, topGadgets, bottomGadgets)) {
                throw new IllegalArgumentException(
                    "One of the corners of the gadget " + gadget + " is not sufficiently padded by barriers");
            }

            graph.put(
                gadget, edgePorts(gadget, gadgets, curLeftGadgets, curRightGadgets, curTopGadgets, curBottomGadgets));
        }
        return graph;
    }

    /**
     * Returns the distance from the top-left corner of the specified gadget to the specified point in the clockwise
     * direction along the edges of the gadget.  The point is specified relative to the top-left corner of the gadget
     * and must lie on one of its edges.  The return value is in the range
     * [0, 2 * gadget.width() + 2 * gadget.height()).
     */
    private int position(Point point, IPlanarGadget gadget) {
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
     * Returns whether the external face of the specified gadget layout matches embedding.externalFace.  Assumes the
     * clockwise ordering of the gadgets matches embedding.clockwiseOrder.  Assumes that each wire gadget has exactly
     * two adjacent gadgets.  We take gadgets of type TestWire and TestTurnWire to be wires.
     * @param embedding The planar embedding.
     * @param gadgets A map from each vertex in the graph to the gadget for the vertex.
     * @param layout A map from each gadget to its top-left corner.
     * @param graph A map from each gadget G adjacent to at least one non-barrier gadget to a map from each adjacent
     *     non-barrier gadget to the indices in G.ports() of the port at which those gadgets connect.  This must be
     *     compatible with embedding.clockwiseOrder.
     * @return Whether the external face matches.
     */
    private boolean hasExternalFace(
            PlanarEmbedding embedding, Map<Vertex, IPlanarGadget> gadgets, Map<IPlanarGadget, Point> layout,
            Map<IPlanarGadget, Map<IPlanarGadget, Integer>> graph) {
        if (embedding.clockwiseOrder.size() == 1) {
            return true;
        }

        // Find a non-barrier gadget that is farthest left.  This is guaranteed to be on the external face.
        int minX = Integer.MAX_VALUE;
        IPlanarGadget firstExternalGadget = null;
        for (Vertex vertex : embedding.clockwiseOrder.keySet()) {
            IPlanarGadget gadget = gadgets.get(vertex);
            if (!gadget.ports().isEmpty()) {
                Point point = layout.get(gadget);
                if (point.x < minX) {
                    minX = point.x;
                    firstExternalGadget = gadget;
                }
            }
        }

        // Find the first adjacent gadget in the clockwise direction from the top-left corner.  This is guaranteed to be
        // on the external face, clockwise from firstExternalGadget.
        int minPosition = Integer.MAX_VALUE;
        int portIndex = -1;
        int index = 0;
        for (Point port : firstExternalGadget.ports()) {
            int position = position(port, firstExternalGadget);
            if (position < minPosition) {
                position = minPosition;
                portIndex = index;
            }
            index++;
        }
        IPlanarGadget start = null;
        for (Entry<IPlanarGadget, Integer> entry : graph.get(firstExternalGadget).entrySet()) {
            if (entry.getValue() == portIndex) {
                start = entry.getKey();
                break;
            }
        }

        // Follow wires from "start" away from firstExternalGadget to the first non-wire gadget
        IPlanarGadget prevGadget = firstExternalGadget;
        IPlanarGadget secondExternalGadget = start;
        while (isWire(secondExternalGadget)) {
            Iterator<IPlanarGadget> iterator = graph.get(secondExternalGadget).keySet().iterator();
            IPlanarGadget adjGadget1 = iterator.next();
            IPlanarGadget adjGadget2 = iterator.next();
            IPlanarGadget nextGadget;
            if (adjGadget1 == prevGadget) {
                nextGadget = adjGadget2;
            } else {
                nextGadget = adjGadget1;
            }
            prevGadget = secondExternalGadget;
            secondExternalGadget = nextGadget;
        }

        // Follow wires from firstExternalGadget away from "start" to the first non-wire gadget
        prevGadget = start;
        while (isWire(firstExternalGadget)) {
            Iterator<IPlanarGadget> iterator = graph.get(firstExternalGadget).keySet().iterator();
            IPlanarGadget adjGadget1 = iterator.next();
            IPlanarGadget adjGadget2 = iterator.next();
            IPlanarGadget nextGadget;
            if (adjGadget1 == prevGadget) {
                nextGadget = adjGadget2;
            } else {
                nextGadget = adjGadget1;
            }
            prevGadget = firstExternalGadget;
            firstExternalGadget = nextGadget;
        }

        // Check whether embedding.externalFace contains the edge from firstExternalFace to secondExternalFace.  It is
        // sufficient to verify a single edge, because given an edge and a clockwise ordering, we can infer the entire
        // external face.
        Vertex prevVertex = embedding.externalFace.get(embedding.externalFace.size() - 1);
        for (Vertex vertex : embedding.externalFace) {
            if (gadgets.get(prevVertex) == firstExternalGadget && gadgets.get(vertex) == secondExternalGadget) {
                return true;
            }
            prevVertex = vertex;
        }
        return false;
    }

    /**
     * Asserts that the return value of PlanarGadgetLayout.layout is correct when passed the specified arguments.  We
     * take gadgets with no ports to be barrier gadgets, and gadgets of type TestWire and TestTurnWire to be wires.
     */
    private void checkLayout(
            PlanarEmbedding embedding, Map<Vertex, IPlanarGadget> gadgets, Map<Vertex, Map<Vertex, Integer>> edgePorts,
            IPlanarWireFactory wireFactory, IPlanarBarrierFactory barrierFactory) {
        Map<IPlanarGadget, Point> layout = PlanarGadgetLayout.layout(
            embedding, gadgets, edgePorts, wireFactory, barrierFactory);
        Map<IPlanarGadget, Map<IPlanarGadget, Integer>> graph = gadgetGraph(
            layout, barrierFactory.minWidth(), barrierFactory.minHeight());

        // Make sure that each wire is adjacent to exactly two gadgets
        for (Entry<IPlanarGadget, Map<IPlanarGadget, Integer>> entry : graph.entrySet()) {
            IPlanarGadget gadget = entry.getKey();
            if (isWire(gadget)) {
                assertEquals(2, entry.getValue().size());
            }
        }

        // Verify the graph
        for (Vertex vertex : embedding.clockwiseOrder.keySet()) {
            IPlanarGadget gadget = gadgets.get(vertex);
            Map<IPlanarGadget, Integer> expectedEdgePorts = new HashMap<IPlanarGadget, Integer>();
            for (Entry<Vertex, Integer> entry : edgePorts.get(vertex).entrySet()) {
                expectedEdgePorts.put(gadgets.get(entry.getKey()), entry.getValue());
            }
            Map<IPlanarGadget, Integer> vertexGadgetPorts = graph.get(gadget);
            Map<IPlanarGadget, Integer> actualEdgePorts = new HashMap<IPlanarGadget, Integer>();
            for (IPlanarGadget start : vertexGadgetPorts.keySet()) {
                // Determine the first non-wire gadget on the path starting with the edge from "gadget" to "start", by
                // following the wires
                IPlanarGadget prevGadget = gadget;
                IPlanarGadget curGadget = start;
                while (isWire(curGadget)) {
                    Iterator<IPlanarGadget> iterator = graph.get(curGadget).keySet().iterator();
                    IPlanarGadget adjGadget1 = iterator.next();
                    IPlanarGadget adjGadget2 = iterator.next();
                    IPlanarGadget nextGadget;
                    if (adjGadget1 == prevGadget) {
                        nextGadget = adjGadget2;
                    } else {
                        nextGadget = adjGadget1;
                    }
                    prevGadget = curGadget;
                    curGadget = nextGadget;
                }

                actualEdgePorts.put(curGadget, vertexGadgetPorts.get(start));
            }
            assertEquals(expectedEdgePorts, actualEdgePorts);
        }
        assertTrue(hasExternalFace(embedding, gadgets, layout, graph));
    }

    /** Tests PlanarGadgetLayout.layout. */
    @Test
    public void testLayout() {
        Graph graph = new Graph();
        Vertex startVertex = graph.createVertex();
        Map<Vertex, List<Vertex>> clockwiseOrder = Collections.singletonMap(
            startVertex, Collections.<Vertex>emptyList());
        List<Vertex> externalFace = Collections.singletonList(startVertex);
        PlanarEmbedding embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        Map<Vertex, IPlanarGadget> gadgets = Collections.<Vertex, IPlanarGadget>singletonMap(
            startVertex, new TestTerminalGadget());
        Map<Vertex, Map<Vertex, Integer>> edgePorts = Collections.singletonMap(
            startVertex, Collections.<Vertex, Integer>emptyMap());
        IPlanarWireFactory wireFactory = new TestWireFactory(2, 2);
        IPlanarBarrierFactory barrierFactory = new TestBarrierFactory(4, 4);
        checkLayout(embedding, gadgets, edgePorts, wireFactory, barrierFactory);

        graph = new Graph();
        startVertex = graph.createVertex();
        Vertex finishVertex = graph.createVertex();
        startVertex.addEdge(finishVertex);
        clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(startVertex, Collections.singletonList(finishVertex));
        clockwiseOrder.put(finishVertex, Collections.singletonList(startVertex));
        externalFace = Arrays.asList(startVertex, finishVertex);
        embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        gadgets = new HashMap<Vertex, IPlanarGadget>();
        gadgets.put(startVertex, new TestTerminalGadget());
        gadgets.put(finishVertex, new TestTerminalGadget());
        edgePorts = new HashMap<Vertex, Map<Vertex, Integer>>();
        edgePorts.put(startVertex, Collections.singletonMap(finishVertex, 0));
        edgePorts.put(finishVertex, Collections.singletonMap(startVertex, 0));
        wireFactory = new TestWireFactory(2, 2);
        barrierFactory = new TestBarrierFactory(4, 4);
        checkLayout(embedding, gadgets, edgePorts, wireFactory, barrierFactory);
        wireFactory = new TestWireFactory(3, 3);
        barrierFactory = new TestBarrierFactory(1, 1);
        checkLayout(embedding, gadgets, edgePorts, wireFactory, barrierFactory);
        wireFactory = new TestWireFactory(6, 1);
        barrierFactory = new TestBarrierFactory(1, 4);
        checkLayout(embedding, gadgets, edgePorts, wireFactory, barrierFactory);

        graph = new Graph();
        startVertex = graph.createVertex();
        finishVertex = graph.createVertex();
        Vertex variableVertex1 = graph.createVertex();
        Vertex variableVertex2 = graph.createVertex();
        Vertex clauseVertex = graph.createVertex();
        Vertex junctionVertex1 = graph.createVertex();
        Vertex junctionVertex2 = graph.createVertex();
        Vertex junctionVertex3 = graph.createVertex();
        startVertex.addEdge(variableVertex1);
        variableVertex1.addEdge(junctionVertex1);
        variableVertex1.addEdge(variableVertex2);
        junctionVertex1.addEdge(clauseVertex);
        junctionVertex1.addEdge(junctionVertex2);
        junctionVertex2.addEdge(clauseVertex);
        junctionVertex2.addEdge(junctionVertex3);
        junctionVertex3.addEdge(clauseVertex);
        junctionVertex3.addEdge(variableVertex2);
        variableVertex2.addEdge(clauseVertex);
        clauseVertex.addEdge(finishVertex);
        clockwiseOrder = new LinkedHashMap<Vertex, List<Vertex>>();
        clockwiseOrder.put(startVertex, Collections.singletonList(variableVertex1));
        clockwiseOrder.put(finishVertex, Collections.singletonList(clauseVertex));
        clockwiseOrder.put(variableVertex1, Arrays.asList(startVertex, variableVertex2, junctionVertex1));
        clockwiseOrder.put(variableVertex2, Arrays.asList(variableVertex1, clauseVertex, junctionVertex3));
        clockwiseOrder.put(junctionVertex1, Arrays.asList(variableVertex1, junctionVertex2, clauseVertex));
        clockwiseOrder.put(junctionVertex2, Arrays.asList(junctionVertex1, junctionVertex3, clauseVertex));
        clockwiseOrder.put(junctionVertex3, Arrays.asList(junctionVertex2, variableVertex2, clauseVertex));
        clockwiseOrder.put(
            clauseVertex,
            Arrays.asList(junctionVertex1, junctionVertex2, junctionVertex3, variableVertex2, finishVertex));
        externalFace = Arrays.asList(
            startVertex, variableVertex1, variableVertex2, clauseVertex, finishVertex, clauseVertex, junctionVertex1,
            variableVertex1);
        embedding = new PlanarEmbedding(clockwiseOrder, externalFace);
        gadgets = new HashMap<Vertex, IPlanarGadget>();
        gadgets.put(startVertex, new TestTerminalGadget());
        gadgets.put(finishVertex, new TestTerminalGadget());
        gadgets.put(variableVertex1, new TestVariableGadget());
        gadgets.put(variableVertex2, new TestVariableGadget());
        gadgets.put(clauseVertex, new TestClauseGadget());
        gadgets.put(junctionVertex1, new TestJunctionGadget());
        gadgets.put(junctionVertex2, new TestJunctionGadget());
        gadgets.put(junctionVertex3, new TestJunctionGadget());
        edgePorts = new HashMap<Vertex, Map<Vertex, Integer>>();
        edgePorts.put(startVertex, Collections.singletonMap(variableVertex1, 0));
        edgePorts.put(finishVertex, Collections.singletonMap(clauseVertex, 0));
        Map<Vertex, Integer> vertexEdgePorts = new HashMap<Vertex, Integer>();
        vertexEdgePorts.put(startVertex, TestVariableGadget.MIN_ENTRY_PORT);
        vertexEdgePorts.put(variableVertex2, TestVariableGadget.MIN_EXIT_PORT);
        vertexEdgePorts.put(junctionVertex1, TestVariableGadget.MIN_EXIT_PORT + 1);
        edgePorts.put(variableVertex1, vertexEdgePorts);
        vertexEdgePorts = new HashMap<Vertex, Integer>();
        vertexEdgePorts.put(junctionVertex3, TestVariableGadget.MIN_ENTRY_PORT);
        vertexEdgePorts.put(variableVertex1, TestVariableGadget.MIN_ENTRY_PORT + 1);
        vertexEdgePorts.put(clauseVertex, TestVariableGadget.MIN_EXIT_PORT);
        edgePorts.put(variableVertex2, vertexEdgePorts);
        vertexEdgePorts = new HashMap<Vertex, Integer>();
        vertexEdgePorts.put(variableVertex1, 0);
        vertexEdgePorts.put(junctionVertex2, 1);
        vertexEdgePorts.put(clauseVertex, 2);
        edgePorts.put(junctionVertex1, vertexEdgePorts);
        vertexEdgePorts = new HashMap<Vertex, Integer>();
        vertexEdgePorts.put(junctionVertex1, 0);
        vertexEdgePorts.put(junctionVertex3, 1);
        vertexEdgePorts.put(clauseVertex, 2);
        edgePorts.put(junctionVertex2, vertexEdgePorts);
        vertexEdgePorts = new HashMap<Vertex, Integer>();
        vertexEdgePorts.put(junctionVertex2, 0);
        vertexEdgePorts.put(variableVertex2, 1);
        vertexEdgePorts.put(clauseVertex, 2);
        edgePorts.put(junctionVertex3, vertexEdgePorts);
        vertexEdgePorts = new HashMap<Vertex, Integer>();
        vertexEdgePorts.put(junctionVertex1, TestClauseGadget.MIN_CLAUSE_PORT);
        vertexEdgePorts.put(junctionVertex2, TestClauseGadget.MIN_CLAUSE_PORT + 1);
        vertexEdgePorts.put(junctionVertex3, TestClauseGadget.MIN_CLAUSE_PORT + 2);
        vertexEdgePorts.put(variableVertex2, TestClauseGadget.ENTRY_PORT);
        vertexEdgePorts.put(finishVertex, TestClauseGadget.EXIT_PORT);
        edgePorts.put(clauseVertex, vertexEdgePorts);
        wireFactory = new TestWireFactory(2, 2);
        barrierFactory = new TestBarrierFactory(4, 4);
        checkLayout(embedding, gadgets, edgePorts, wireFactory, barrierFactory);
        wireFactory = new TestWireFactory(3, 3);
        barrierFactory = new TestBarrierFactory(1, 1);
        checkLayout(embedding, gadgets, edgePorts, wireFactory, barrierFactory);
        wireFactory = new TestWireFactory(6, 1);
        barrierFactory = new TestBarrierFactory(1, 4);
        checkLayout(embedding, gadgets, edgePorts, wireFactory, barrierFactory);
    }
}
