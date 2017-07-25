package uk.ac.ucl.cs.sec.chainspace;


/**
 *
 *
 */
public class Main {

    // CONFIG: number of cores
    static final int CORES = 1;

    // CONFIG: verbose option prints out extensive comments on the console
    static final boolean VERBOSE = true;

    // CONFIG: size of the cache (set to zero to disable cache)
    static final int CACHE_DEPTH = 0;


    /*

     -- DEBUG --

     These are the debug options:
        (1) DEBUG_ALLOW_REPEAT: when enabled, the system accepts multiple time the same transaction. Object are not
     uniques and are never set to 'inactive'.

        (2) DEBUG_SKIP_CHECKER: when enabled, the checker never called. This is equivalent of having a checker that
     returns always 'true'.

        (3) DEBUG_NO_BROADCAST: in normal operations, when receiving a nex transaction, the first thing that the node
     does is to broadcast that transaction to all other nodes. When this option is enabled, the node does not broadcast
     the transaction and processes it by itself. This option is useful to limit the number of console's prints.

        (4) DEBUG_IGNORE_DEPENDENCIES: when enables, transaction's dependencies are ignored and only the top-level
     transaction is executed (no cross-contract calls).


     NOTE: all debug options should be set to false when running in production environment.

     */
    static final boolean DEBUG_ALLOW_REPEAT         = true;
    static final boolean DEBUG_SKIP_CHECKER         = false;
    static final boolean DEBUG_NO_BROADCAST         = true;
    static final boolean DEBUG_IGNORE_DEPENDENCIES  = false;


    /**
     * main
     * @param args not used
     */
    public static void main(String[] args) {

        // verbose print
        if (Main.VERBOSE) { Utils.printHeader("Starting Chainsapce..."); }


        // run chainspace service
        for (int i = 1; i <= CORES; i++) {
            runNodeService(i);
        }


        // verbose print
        if (Main.VERBOSE) { Utils.printLine(); }

    }


    /**
     * runNodeService
     * Run a node service with a given node's ID.
     * @param nodeID the node's ID
     */
    private static void runNodeService(int nodeID) {

        try {

            // run a new node instance
            new NodeService(nodeID);

        }
        catch (Exception e) {
            if (Main.VERBOSE) { Utils.printStacktrace(e); }
            else { System.err.println("[ERROR] Node service #" +nodeID+ " failled to start."); }
        }

    }

}
