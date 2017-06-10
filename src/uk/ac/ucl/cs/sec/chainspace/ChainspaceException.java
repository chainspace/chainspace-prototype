package uk.ac.ucl.cs.sec.chainspace;


public class ChainspaceException extends Exception {

    /**
     * no-args constructor.
     *
     */
    public ChainspaceException() {
        super();
    }

    /**
     * Message constructor.
     *
     * @param message the error message.
     */
    public ChainspaceException(String message) {
        super(message);
    }
}
