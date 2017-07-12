package uk.ac.ucl.cs.sec.chainspace;

/**
 * An abstract class representing a checker.
 */
public abstract class Checker {
    abstract void startChecker() throws StartCheckerException;
    abstract void stopChecker();
    abstract Boolean check(TransactionForChecker transactionForChecker);

    Boolean checkSingle(TransactionForChecker transactionForChecker) throws StartCheckerException {
        this.startChecker();
        Boolean result = this.check(transactionForChecker);
        this.stopChecker();

        return result;
    }
}
