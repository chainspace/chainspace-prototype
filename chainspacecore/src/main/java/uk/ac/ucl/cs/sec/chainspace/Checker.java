package uk.ac.ucl.cs.sec.chainspace;


/**
 * An abstract class representing a checker.
 *
 */
abstract class Checker {
    abstract void startChecker() throws StartCheckerException;
    abstract void stopChecker();
    abstract String check(TransactionForChecker transactionForChecker);

    String checkSingle(TransactionForChecker transactionForChecker) throws StartCheckerException {

        this.startChecker();
        String result = this.check(transactionForChecker);
        this.stopChecker();

        return result;
    }
}
