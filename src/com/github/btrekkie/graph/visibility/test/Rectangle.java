package com.github.btrekkie.graph.visibility.test;

/** An axis-aligned rectangle.  The rectangle may be degenerate, i.e. a line segment or a point. */
class Rectangle {
    /** The x coordinate of the left edge. */
    public final int minX;

    /** The y coordinate of the top edge. */
    public final int minY;

    /** The x coordinate of the right edge. */
    public final int maxX;

    /** The y coordinate of the bottom edge. */
    public final int maxY;

    public Rectangle(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }
}
