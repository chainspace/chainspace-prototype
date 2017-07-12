package uk.ac.ucl.cs.sec.chainspace;

/**
 * An abstract class representing a checker.
 */
public abstract class Checker {
    abstract void startChecker();
    abstract void stopChecker();
    abstract Boolean check(TransactionForChecker transactionForChecker);

    Boolean checkSingle(TransactionForChecker transactionForChecker) {
        this.startChecker();
        Boolean result = this.check(transactionForChecker);
        this.stopChecker();

        return result;
    }
}