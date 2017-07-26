package uk.ac.ucl.cs.sec.chainspace;


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


}
