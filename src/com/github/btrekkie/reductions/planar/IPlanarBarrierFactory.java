package com.github.btrekkie.reductions.planar;

/** A factory for producing barrier gadgets, gadgets that prevent access to regions of the plane. */
public interface IPlanarBarrierFactory {
    /** Returns the minimum width of a barrier gadget.  This must be positive. */
    public int minWidth();

    /** Returns the minimum height of a barrier gadget.  This must be positive. */
    public int minHeight();

    /** Returns a new barrier gadget of the specified size.  This must be at least minWidth() x minHeight(). */
    public IPlanarGadget createBarrier(int width, int height);
}
