package com.github.btrekkie.reductions.bool;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A 3-SAT problem instance.  It consists of a 3-CNF formula.  The formula consists of clauses.  Each clause consists of
 * three literals.  Each literal is either a boolean variable or its inverse.  If inverted, a literal is true iff the
 * variable is false.  Otherwise, the literal is true iff the variable is true.  A clause is true iff at least one of
 * the three literals is true.  A 3-CNF formula is true iff all of its clauses are true.  A 3-SAT problem has a solution
 * iff there is at least one satisfying assignment: a way of assigning a boolean value to each variable so that the
 * 3-CNF formula is true.  3-SAT is NP-complete.
 */
public class ThreeSat {
    /** The clauses that comprise the 3-CNF formula. */
    public Collection<ThreeSatClause> clauses;

    public ThreeSat(Collection<ThreeSatClause> clauses) {
        this.clauses = clauses;
    }

    /** Returns all of the variables in the 3-CNF formula. */
    public Set<Variable> variables() {
        Set<Variable> variables = new LinkedHashSet<Variable>();
        for (ThreeSatClause clause : clauses) {
            for (Literal literal : clause.literals()) {
                variables.add(literal.variable);
            }
        }
        return variables;
    }

    /** Returns a map from each literal that appears in at least one clause to the clauses in which it appears. */
    public Map<Literal, Set<ThreeSatClause>> literalClauses() {
        Map<Literal, Set<ThreeSatClause>> literalClauses = new HashMap<Literal, Set<ThreeSatClause>>();
        for (ThreeSatClause clause : clauses) {
            for (Literal literal : new HashSet<Literal>(clause.literals())) {
                Set<ThreeSatClause> curLiteralClauses = literalClauses.get(literal);
                if (curLiteralClauses == null) {
                    curLiteralClauses = new LinkedHashSet<ThreeSatClause>();
                    literalClauses.put(literal, curLiteralClauses);
                }
                curLiteralClauses.add(clause);
            }
        }
        return literalClauses;
    }
}
