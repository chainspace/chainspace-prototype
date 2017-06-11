package uk.ac.ucl.cs.sec.chainspace;


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
