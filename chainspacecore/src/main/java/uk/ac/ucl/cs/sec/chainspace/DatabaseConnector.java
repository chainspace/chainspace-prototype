package uk.ac.ucl.cs.sec.chainspace;

import java.security.NoSuchAlgorithmException;

/**
 *
 *
 */
abstract class DatabaseConnector {


    abstract void saveObject(String transactionID, String[] objects) throws AbortTransactionException;

    abstract boolean isInactive(String[] objectIDs) throws AbortTransactionException;

    abstract void setInactive(String[] objectIDs) throws AbortTransactionException;

    abstract void logTransaction(String transactionID, String transactionJson) throws Exception;

    abstract void close() throws Exception;



    /**
     * generateObjectID
     * Create an object ID from the object and the trasaction that created it.
     */
     String generateObjectID(String transactionID, String object) throws NoSuchAlgorithmException {
        return Utils.hash(transactionID + "|" + object);
    }


    /**
     * generateHead
     * Create a new head from a new transaction and the previous head.
     */
    String generateHead(String oldHead, String transactionJson) throws NoSuchAlgorithmException {
        return Utils.hash(oldHead + "|" + transactionJson);
    }

    /**
     * generateHead
     * Create a new head from a new transaction (should be used only for the first transaction).
     */
    String generateHead(String transactionJson) throws NoSuchAlgorithmException {
        return Utils.hash(transactionJson);
    }


}
