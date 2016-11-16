package com.github.btrekkie.reductions.planar.test;

import com.github.btrekkie.reductions.planar.IPlanarGadget;

/** A location in a gadget graph: one of the ports of some gadget. */
class GadgetLocation {
    /** The gadget for the location. */
    public final IPlanarGadget gadget;

    /** The index of the port for the location in gadget.ports(). */
    public final int port;

    public GadgetLocation(IPlanarGadget gadget, int port) {
        this.gadget = gadget;
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GadgetLocation)) {
            return false;
        }
        GadgetLocation location = (GadgetLocation)obj;
        return gadget == location.gadget && port == location.port;
    }

    @Override
    public int hashCode() {
        return gadget.hashCode() + 31 * port;
    }
}
