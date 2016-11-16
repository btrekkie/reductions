package com.github.btrekkie.reductions.planar;

/** A factory for producing wire gadgets, gadgets that permit free travel from any port to any other port. */
public interface IPlanarWireFactory {
    /** Returns the width of vertical wires. */
    public int width();

    /** Returns the height of horizontal wires. */
    public int height();

    /**
     * Returns a new horizontal wire gadget, a gadget that connects a port on the left edge to a port on the right edge,
     * with the specified width.  The width parameter is guaranteed to be at least width().  The wire must have exactly
     * two ports.  For a given IPlanarWireFactory, all ports of all horizontal wires must have the same y coordinate.
     */
    public IPlanarGadget createHorizontalWire(int width);

    /**
     * Returns a new vertical wire gadget, a gadget that connects a port on the top edge to a port on the bottom edge,
     * with the specified height.  The height parameter is guaranteed to be at least height().  The wire must have
     * exactly two ports.  For a given IPlanarWireFactory, all ports of all vertical wires must have the same x
     * coordinate.
     */
    public IPlanarGadget createVerticalWire(int height);

    /**
     * Returns a turn wire gadget, a gadget that connects ports on each of the four edges of the gadget.  The gadget
     * must be this.width() x this.height().  The wire must have exactly four ports.  The ports on the left and right
     * edges must have the same y coordinates as wires returned by createHorizontalWire, while the ports on the top and
     * bottom edges must have the same x coordinates as the wires returned by createVerticalWire.  (It is okay to have a
     * port on a corner, in which case we take it to be either on exactly one of the sides, in order to match the
     * coordinates of horizontal and vertical wires.  However, this only works if the four ports are at distinct
     * points.)
     */
    public IPlanarGadget createTurnWire();
}
