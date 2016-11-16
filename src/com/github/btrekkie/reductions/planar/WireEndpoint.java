package com.github.btrekkie.reductions.planar;

/**
 * Information about the current end of the sequence of wires starting at some port of some gadget, for
 * SinglePlanarGadgetLayout.  See the comments for the implementation of that class.
 */
class WireEndpoint {
    /** A "position" of an endpoint, indicating the next side of the gadget it will wind around. */
    public static enum Position {
        /** Indicates that an endpoint will wind around the top of the gadget next. */
        TOP_LEFT,

        /** Indicates that an endpoint will wind around the right of the gadget next. */
        TOP_RIGHT,

        /** Indicates that an endpoint will wind around the bottom of the gadget next. */
        BOTTOM_RIGHT,

        /** Indicates that an endpoint will wind around the left of the gadget next. */
        BOTTOM_LEFT};

    /** The x coordinate of the left edge of the final wire we must create to connect the port to the target point. */
    public final int targetX;

    /** Whether we are connecting the port to a point above the gadget. */
    public final boolean isTargetUp;

    /**
     * The minimum x coordinate of the intersection between the last wire we created for the endpoint and the next wire
     * we will create for the endpoint.
     */
    public int x;

    /**
     * The minimum y coordinate of the intersection between the last wire we created for the endpoint and the next wire
     * we will create for the endpoint.
     */
    public int y;

    /** The current position of the endpoint, indicating the next side of the gadget it will wind around. */
    public Position position;

    /** Whether we have attached a wire to the port for the endpoint. */
    public boolean hasWire;

    /**
     * The coordinate of the edge furthest from the gadget of the furthest wire on the next side of the gadget the
     * endpoint will wind around.  For example, if "position" is Position.TOP_LEFT, this is the y coordinate of the top
     * of the highest horizontal wire that winds around the top of the gadget.
     */
    public int barrierCoord;

    public WireEndpoint(int targetX, boolean isTargetUp, int x, int y, Position position) {
        this.targetX = targetX;
        this.isTargetUp = isTargetUp;
        this.x = x;
        this.y = y;
        this.position = position;
    }
}
