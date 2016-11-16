package com.github.btrekkie.reductions.planar;

import java.util.List;

/**
 * An axis-aligned rectangular element in the plane.  We may join IPlanarGadgets at specified "ports".  See the comments
 * for ports().
 */
public interface IPlanarGadget {
    /** Returns the width of the gadget.  This must be positive. */
    public int width();

    /** Returns the height of the gadget.  This must be positive. */
    public int height();

    /**
     * Returns the positions of the ports of the gadget in clockwise order.  The "ports" are locations where we may
     * connect the gadget to another gadget, so that one port of this gadget coincides with one port of the other
     * gadget.  Ports are specified relative to the top-left corner of the gadget.  Each port must lie on an edge of the
     * gadget.  ports() may not return repeated points.
     */
    public List<Point> ports();
}
