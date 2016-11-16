package com.github.btrekkie.reductions.bool;

/**
 * A literal: either a boolean variable or its inverse.  If inverted, a literal is true iff the variable is false.
 * Otherwise, the literal is true iff the variable is true.
 */
public class Literal {
    public final Variable variable;

    public final boolean isInverted;

    public Literal(Variable variable, boolean isInverted) {
        this.variable = variable;
        this.isInverted = isInverted;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Literal)) {
            return false;
        }
        Literal literal = (Literal)obj;
        return variable == literal.variable && isInverted == literal.isInverted;
    }

    @Override
    public int hashCode() {
        return variable.hashCode() + (isInverted ? 1 : 0);
    }
}
