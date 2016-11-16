package com.github.btrekkie.reductions.planar;

/**
 * A factory for producing IPlanarGadgets for ThreeSatPlanarGadgetLayout.  See the comments for
 * ThreeSatPlanarGadgetLayout.layout.
 */
public interface I3SatPlanarGadgetFactory {
    /**
     * Returns a new variable gadget.  Each variable gadget has at least two entry ports and at least two exit ports.
     * Entering at any entry port allows us to exit at any exit port, but not at any other entry port.  Subsequently
     * reentering the exit port does not allow us to exit at another exit port.
     *
     * Variable gadgets are permitted to be one-time use, in that when we enter an entry port, exit an exit port, and
     * then enter somewhere other than the exit port, any ability to access the different ports is permissible
     * thereafter.  Also, if we enter an exit port before entering any entry port, any ability to access the different
     * ports is permissible thereafter.  If we enter an entry port, exit an exit port, and reenter the exit port, any
     * ability to access the same entry and exit ports is permitted.
     */
    public IPlanarGadget createVariable();

    /**
     * Returns a new clause gadget.  Each clause gadget has at least three clause ports, an entry port, and an exit
     * port.  The gadget is initally locked.  Entering at a clause port unlocks the gadget if it is locked, at which
     * point we can exit through the same port, but not through any other port.  If we enter at the entry port, we can
     * exit through the exit port iff the gadget is unlocked.
     *
     * If we enter a locked clause gadget at a clause port, we may or may not have the option of leaving it locked.  If
     * we enter a second clause port or enter a clause port a second time, any ability or requirement to lock or unlock
     * the clause upon entry is permissible thereafter.  If we enter at the exit port, any ability to access the
     * different ports is permissible thereafter.
     */
    public IPlanarGadget createClause();

    /**
     * Returns a new crossover gadget.  Each crossover gadget has a first entry port, a first exit port, a second entry
     * port, and a second exit port.  A clockwise ordering of the ports alternates between first ports and second ports.
     * If we enter at the second entry port, we may exit at the second exit port.  If we enter at the first entry port,
     * we may exit at the first exit port, provided we do so before entering at the second entry port.
     *
     * To be more precise, if we first enter at an entry port, we may only exit through the corresponding exit port.  If
     * we enter an entry port and then exit through the corresponding exit port, later we may or may not be able to
     * enter at the same exit port and exit through the same entry port.  If our travels are limited to entering at a
     * certain entry port and exiting the corresponding exit port and then reversing our last pattern some number of
     * times, then if we enter at the last port through which we exited, we may only exit through the last port at which
     * we entered.
     *
     * If we first enter at an exit port, any ability to access the different ports is permissible thereafter.  If we
     * enter at the first entry port, exit through the first exit port, then enter through the second entry port, we may
     * exit through the second exit port, any ability to access the other ports at this time is permissible, and
     * subsequently, any ability to access the different ports is permissible.  If we enter an entry port, exit the
     * corresponding exit port, then enter the other exit port, any ability to access the different ports is permissble.
     * If we first enter at the second entry port, exit at the second exit port, and subsequently enter at the first
     * entry port, any ability to access the different ports is permissible thereafter.
     *
     * @param isClockwise Whether the arc from the first entry port to the second entry port and then to the first exit
     *     port is clockwise.
     * @return The crossover gadget.
     */
    public IPlanarGadget createCrossover(boolean isClockwise);

    /**
     * Returns a new junction gadget.  Each junction gadget has at least three ports.  If we enter at any port, we may
     * exit at any port, except possibly the port where we entered.
     */
    public IPlanarGadget createJunction();

    /**
     * The minimum index in IPlanarGadget.ports() of the entry ports of each gadget returned by createVariable().  The
     * ports in the range [minVariableEntryPort(), maxVariableEntryPort()] must consist exclusively of entry ports.  See
     * the comments for createVariable().
     */
    public int minVariableEntryPort();

    /**
     * The maximum index in IPlanarGadget.ports() of the entry ports of each gadget returned by createVariable().  The
     * ports in the range [minVariableEntryPort(), maxVariableEntryPort()] must consist exclusively of entry ports.  See
     * the comments for createVariable().
     */
    public int maxVariableEntryPort();

    /**
     * The minimum index in IPlanarGadget.ports() of the exit ports of each gadget returned by createVariable().  The
     * ports in the range [minVariableExitPort(), maxVariableExitPort()] must consist exclusively of exit ports.  See
     * the comments for createVariable().
     */
    public int minVariableExitPort();

    /**
     * The maximum index in IPlanarGadget.ports() of the exit ports of each gadget returned by createVariable().  The
     * ports in the range [minVariableExitPort(), maxVariableExitPort()] must consist exclusively of exit ports.  See
     * the comments for createVariable().
     */
    public int maxVariableExitPort();

    /**
     * The minimum index in IPlanarGadget.ports() of the clause ports of each gadget returned by createClause().  The
     * ports in the range [minClausePort(), maxClausePort()] must consist exclusively of clause ports.  See the comments
     * for createClause().
     */
    public int minClausePort();

    /**
     * The maximum index in IPlanarGadget.ports() of the clause ports of each gadget returned by createClause().  The
     * ports in the range [minClausePort(), maxClausePort()] must consist exclusively of clause ports.  See the comments
     * for createClause().
     */
    public int maxClausePort();

    /**
     * The index in IPlanarGadget.ports() of the entry port of each gadget returned by createClause().  See the comments
     * for createClause().
     */
    public int clauseEntryPort();

    /**
     * The index in IPlanarGadget.ports() of the exit port of each gadget returned by createClause().  See the comments
     * for createClause().
     */
    public int clauseExitPort();

    /**
     * The index in gadget.ports() of the first entry port of the specified gadget returned by createCrossover.  See the
     * comments for createCrossover.
     * @param gadget The gadget.
     * @param isClockwise Whether the arc from the first entry port to the second entry port and then to the first exit
     *     port is clockwise.
     * @return The port index.
     */
    public int firstCrossoverEntryPort(IPlanarGadget gadget, boolean isClockwise);

    /**
     * The index in gadget.ports() of the first exit port of the specified gadget returned by createCrossover.  See the
     * comments for createCrossover.
     * @param gadget The gadget.
     * @param isClockwise Whether the arc from the first entry port to the second entry port and then to the first exit
     *     port is clockwise.
     * @return The port index.
     */
    public int firstCrossoverExitPort(IPlanarGadget gadget, boolean isClockwise);

    /**
     * The index in gadget.ports() of the second entry port of the specified gadget returned by createCrossover.  See
     * the comments for createCrossover.
     * @param gadget The gadget.
     * @param isClockwise Whether the arc from the first entry port to the second entry port and then to the first exit
     *     port is clockwise.
     * @return The port index.
     */
    public int secondCrossoverEntryPort(IPlanarGadget gadget, boolean isClockwise);

    /**
     * The index in gadget.ports() of the second exit port of the specified gadget returned by createCrossover.  See the
     * comments for createCrossover.
     * @param gadget The gadget.
     * @param isClockwise Whether the arc from the first entry port to the second entry port and then to the first exit
     *     port is clockwise.
     * @return The port index.
     */
    public int secondCrossoverExitPort(IPlanarGadget gadget, boolean isClockwise);
}
