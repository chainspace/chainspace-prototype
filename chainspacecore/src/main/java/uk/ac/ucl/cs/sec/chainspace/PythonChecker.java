package uk.ac.ucl.cs.sec.chainspace;

import java.io.IOException;

/**
 * A Python checker.
 */
public class PythonChecker extends Checker {
    private String pythonScriptPath;
    private Process checkerProcess;

    PythonChecker(String pythonScriptPath) {
        this.pythonScriptPath = pythonScriptPath;
    }

    @Override
    void startChecker() throws StartCheckerException {
        try {
            this.checkerProcess = new ProcessBuilder("python", pythonScriptPath).start();
        } catch (IOException e) {
            throw new StartCheckerException("Could not start checker.");
        }
    }

    @Override
    void stopChecker() {
        this.checkerProcess.destroy();
    }

    @Override
    Boolean check(TransactionForChecker transactionForChecker) {
        return null;
    }
}
