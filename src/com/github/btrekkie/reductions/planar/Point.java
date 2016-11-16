package com.github.btrekkie.reductions.planar;

/** A lattice point in the plane. */
public class Point {
    /** The x coordinate. */
    public final int x;

    /** The y coordinate. */
    public final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Point)) {
            return false;
        }
        Point point = (Point)obj;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return x + 31 * y;
    }

    @Override
    public String toString() {
        return "[Point x=" + x + ", y=" + y + ']';
    }
}
