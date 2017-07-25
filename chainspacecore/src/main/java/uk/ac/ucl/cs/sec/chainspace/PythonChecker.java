package uk.ac.ucl.cs.sec.chainspace;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 *
 */
class PythonChecker {

    private String pythonScriptPath;
    private String contractID, methodID;
    private Process checkerProcess;

    private static final String CHECKER_URL = "http://127.0.0.1:5000/";
    private static final int CACHE_DEPTH = 100000;

    private static final ArrayList<PythonChecker> cache = new ArrayList<>(CACHE_DEPTH);

    PythonChecker(String pythonScriptPath, String contractID, String methodID) throws StartCheckerException {

        // save variables
        this.pythonScriptPath = pythonScriptPath;
        this.contractID = contractID;
        this.methodID = methodID;

        // start the checker
        this.startChecker();

    }

    private void startChecker() throws StartCheckerException {

        ProcessBuilder pb = new ProcessBuilder(Arrays.asList("python", this.pythonScriptPath));
        try {

            // start thread
            this.checkerProcess = pb.start();
            // sleep
            Thread.sleep(1000);

        } catch (IOException | InterruptedException e) {
            throw new StartCheckerException("Couldn't start checker.");
        }

    }

    private String getContractID() {
        return contractID;
    }
    private String getMethodID() {
        return methodID;
    }
    private String getURL() {

        return CHECKER_URL + this.contractID + "/" + this.methodID;
    }

    private void stopChecker() {

        this.checkerProcess.destroy();

    }

    String check(TransactionForChecker transactionForChecker) throws IOException {

        return Utils.makePostRequest(this.getURL(), transactionForChecker.toJson());
    }

    static PythonChecker getFromCache(String pythonScriptPath, String contractID, String methodID)
            throws StartCheckerException
    {

        // check if that checker is already in the cache
        for (PythonChecker aCache : cache) {
            if ( contractID.equals(aCache.getContractID())
                    && methodID.equals(aCache.getMethodID()) ) {
                System.out.println("in cache");
                return aCache;
            }
        }

        // otherwise, update cache
        PythonChecker newChecker = new PythonChecker(pythonScriptPath, contractID, methodID);
        cache.add(newChecker);
        if (cache.size() > CACHE_DEPTH) {
            cache.get(0).stopChecker();
            cache.remove(0);
        }
        return newChecker;

    }

}
