package com.github.btrekkie.graph.spqr;

/** An potential type 2 split pair.  See the comments for the implementation of SpqrNode. */
class TStackEntry {
    /** The vertex with the largest PalmVertex.number value that is split off by this potential split pair. */
    public final PalmVertex high;

    /** The vertex in the potential split pair that is an ancestor of the other vertex. */
    public final PalmVertex start;

    /** The vertex in the potential split pair that is a descendant of the other vertex. */
    public final PalmVertex end;

    public TStackEntry(PalmVertex high, PalmVertex start, PalmVertex end) {
        this.high = high;
        this.start = start;
        this.end = end;
    }
}
