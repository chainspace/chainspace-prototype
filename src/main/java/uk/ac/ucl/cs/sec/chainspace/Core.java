package uk.ac.ucl.cs.sec.chainspace;

import org.json.JSONObject;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;


/**
 *
 *
 */
// TODO: load node config from external file
class Core {

    // instance variables
    private DatabaseConnector databaseConnector;
    private Cache cache;


    // TODO: load cache depth from config
    private static final int CACHE_DEPTH = 1;


    /**
     * Constructor
     * Runs a node service and init a database.
     */
    Core(int nodeID) throws ClassNotFoundException, SQLException {

        // init cache
        // here we are implementing a simple linear cash of complexity O(n)
        this.cache = new SimpleCache(CACHE_DEPTH);

        // init the database connection
        // here we're using SQLite as an example
        this.databaseConnector = new SQLiteConnector(nodeID);

    }


    /**
     * close
     * Gently shutdown the core
     */
    void close() throws Exception {
        this.databaseConnector.close();
    }


    /**
     * processTransaction
     * This method processes a transaction object, call the checker, and store the outputs in the database if
     * everything goes fine.
     */
    String[] processTransaction(String request) throws Exception {

        // get the transactions
        Transaction transaction = TransactionPackager.makeTransaction(request);
        TransactionForChecker transactionForChecker = TransactionPackager.makeFullTransaction(request);

        // recursively loop over dependencies
        if (! Main.DEBUG_IGNORE_DEPENDENCIES) {
            for (int i = 0; i < transaction.getDependencies().length; i++) {

                if (Main.VERBOSE) { System.out.println("\n[PROCESSING DEPENDENCY #" +i+ "]");}
                String[] returns = processTransaction(transaction.getDependencies()[i]);
                transactionForChecker.addParameters(returns);
                if (Main.VERBOSE) { System.out.println("\n[END DEPENDENCY #" +i+ "]");}

            }
        }

        // process top level transaction
        return processTransactionHelper(transaction, transactionForChecker);

    }

    /**
     * processTransactionHelper
     * Helper for processTransaction: executed on each recursion.
     */
    private String[] processTransactionHelper(Transaction transaction, TransactionForChecker transactionForChecker)
            throws Exception
    {

        // check if the transaction is in the cache (has recently been processed)
        if (! Main.DEBUG_ALLOW_REPEAT) {
            if (this.cache.isInCache(transaction.toJson())) {
                throw new AbortTransactionException("This transaction as already been executed.");
            }
        }

        // check input objects and reference inputs are active
        if (this.databaseConnector.isInactive(transaction.getInputIDs())) {
            throw new AbortTransactionException("At least one input object is inactive.");
        }
        if (this.databaseConnector.isInactive(transaction.getReferenceInputIDs())) {
            throw new AbortTransactionException("At least one reference input is inactive.");
        }

        // call the checker
        if (! Main.DEBUG_SKIP_CHECKER) {
            callChecker(transactionForChecker);
        }



        /*
            This is the part where we call BFTSmart.
            I would suggest to create a separate class that handle all the BFT
         */
        // check if objects are active
        // TODO: check that all inputs are active.



        // make input (consumed) objects inactive
        if (! Main.DEBUG_ALLOW_REPEAT) {
            this.databaseConnector.setInactive(transaction.getInputIDs());
        }

        // register new objects
        this.databaseConnector.saveObject(transaction.getID(), transactionForChecker.getOutputs());

        // update logs
        this.databaseConnector.logTransaction(transaction.getID(), transaction.toJson());

        // pass local returns
        return transaction.getReturns();

    }


    /**
     * callChecker
     * This method format a packet and call the checker in order to verify the transaction.
     */
    private void callChecker(TransactionForChecker transactionForChecker)
            throws IOException, AbortTransactionException
    {

        // get checker URL
        // TODO: at the moment the checker URL is hardcoded, this should be loaded from a config file
        String checkerURL = "http://127.0.0.1:5001/bank/transfer";

        // call the checker
        String responseString = Utils.makePostRequest(checkerURL, transactionForChecker.toJson());
        JSONObject responseJson = new JSONObject(responseString);

        // throw error if the checker declines the transaction
        if (responseJson.getString("status").equalsIgnoreCase("ERROR")) {
            throw new AbortTransactionException(responseJson.getString("message"));
        }
        else if(! responseJson.getString("status").equalsIgnoreCase("OK")) {
            throw new AbortTransactionException("The checker declined the transaction.");
        }

        if (Main.VERBOSE) { System.out.println("\nThe checker accepted the transaction!"); }

    }

}
