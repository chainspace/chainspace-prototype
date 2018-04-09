package uk.ac.ucl.cs.sec.chainspace;


/**
 * Custom exception. Thrown when a chainspace transaction aborts.
 */
public class AbortTransactionException extends Exception {

    /**
     * Message constructor.
     *
     * @param message the error message.
     */
    public AbortTransactionException(String message) {

        super("[ERROR_TRANSACTIONABORT] " + message);

    }

    public AbortTransactionException(String message, Throwable cause) {
        super("[ERROR_TRANSACTIONABORT] " + message + " (see cause)", cause);
    }
}
