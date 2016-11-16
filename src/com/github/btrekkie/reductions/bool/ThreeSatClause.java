package com.github.btrekkie.reductions.bool;

import java.util.Arrays;
import java.util.Collection;

/**
 * A clause in a 3-SAT problem.  A literal may appear multiple times in a given clause.  See the comments for ThreeSat.
 */
public class ThreeSatClause {
    /** The first literal of which this is comprised. */
    public final Literal literal1;

    /** The second literal of which this is comprised. */
    public final Literal literal2;

    /** The third literal of which this is comprised. */
    public final Literal literal3;

    public ThreeSatClause(Literal literal1, Literal literal2, Literal literal3) {
        this.literal1 = literal1;
        this.literal2 = literal2;
        this.literal3 = literal3;
    }

    /**
     * Returns the literals in the clause.  If a literal appears multiple times in the clause, then the returned
     * collection repeats the literal.
     */
    public Collection<Literal> literals() {
        return Arrays.asList(literal1, literal2, literal3);
    }
}
