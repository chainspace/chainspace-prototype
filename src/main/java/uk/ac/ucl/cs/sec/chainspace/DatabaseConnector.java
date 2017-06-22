package uk.ac.ucl.cs.sec.chainspace;


/**
 *
 *
 */
public interface DatabaseConnector {

    void saveObject(String transactionID, String[] objects) throws AbortTransactionException;

    boolean isInactive(String[] objectIDs) throws AbortTransactionException;

    void setInactive(String[] objectIDs) throws AbortTransactionException;

    void logTransaction(String transactionID, String transactionJson) throws Exception;

    void close() throws Exception;
}
