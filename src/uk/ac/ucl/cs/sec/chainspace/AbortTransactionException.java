package uk.ac.ucl.cs.sec.chainspace;


/**
 * Custom exception. Thrown when a chainspace transaction aborts.
 */
class AbortTransactionException extends Exception {

    /**
     * Message constructor.
     *
     * @param message the error message.
     */
    AbortTransactionException(String message) {

        super(message);

    }
}
