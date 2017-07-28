package uk.ac.ucl.cs.sec.chainspace;


/**
 * Main
 * Main class, program entry point.
 */
public class Main {

    // CONFIG: port
    private static final int PORT = 3001;

    // CONFIG: database's name
    static String DATABASE_NAME = "database";

    // CONFIG: verbose option prints out extensive comments on the console
    static final boolean VERBOSE = true;

    // CONFIG: size of the cache (set to zero to disable cache)
    static final int CACHE_DEPTH = 0;


    /*

     -- DEBUG --

     These are the debug options:
        (1) DEBUG_ALLOW_REPEAT: when enabled, the system accepts multiple time the same transaction. Object are not
     uniques and are never set to 'inactive'. Input objects do not need to be created before being processed.

        (2) DEBUG_SKIP_CHECKER: when enabled, the checker never called. This is equivalent of having a checker that
     returns always 'true'.

        (3) DEBUG_IGNORE_DEPENDENCIES: when enables, transaction's dependencies are ignored and only the top-level
     transaction is executed (no cross-contract calls).

     All debug options should be set to false when running in production environment.

     */
    static final boolean DEBUG_ALLOW_REPEAT         = true;
    static final boolean DEBUG_SKIP_CHECKER         = true;
    static final boolean DEBUG_IGNORE_DEPENDENCIES  = true;


    // version
    static final String VERSION = "1.0";


    /**
     * main
     * @param args not used
     */
    public static void main(String[] args) {

        // verbose print
        if (Main.VERBOSE) { Utils.printHeader("Starting Chainspace..."); }

        // run chainspace service
        try {new NodeService(PORT);}
        catch (Exception e) {
            if (Main.VERBOSE) { Utils.printStacktrace(e); }
            else { System.err.println("[ERROR] Node service failed to start on port " + PORT); }
        }

        // verbose print
        if (Main.VERBOSE) { Utils.printLine(); }

    }

}
