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
    private String contractID;
    private Process checkerProcess;

    private static final String CHECKER_URL = "http://127.0.0.1:5000/";
    private static final int CACHE_DEPTH = 20;

    private static final ArrayList<PythonChecker> cache = new ArrayList<>(CACHE_DEPTH);

    PythonChecker(String pythonScriptPath, String contractID) throws StartCheckerException {

        // save variables
        this.pythonScriptPath = pythonScriptPath;
        this.contractID = contractID;

        // if not, start a new checker
        this.startChecker();

    }

    void startChecker() throws StartCheckerException {

        ProcessBuilder pb = new ProcessBuilder(Arrays.asList("python", this.pythonScriptPath));
        try {

            // start thread
            System.out.println(30);
            this.checkerProcess = pb.start();
            // sleep
            Thread.sleep(100);
            System.out.println(30);

        } catch (IOException | InterruptedException e) {
            throw new StartCheckerException("Couldn't start checker.");
        }

    }

    public String getPythonScriptPath() {
        return pythonScriptPath;
    }

    public String getContractID() {
        return contractID;
    }

    void stopChecker() {

        this.checkerProcess.destroy();

    }

    String check(TransactionForChecker transactionForChecker) {

        try {

            return Utils.makePostRequest(CHECKER_URL, transactionForChecker.toJson());

        } catch (IOException e) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("success", "False");
            jsonObject.put("message", e.getMessage());
            return jsonObject.toString();
        }

    }

    static PythonChecker getFromCache(String pythonScriptPath, String contractID) throws StartCheckerException {

        // check if that checker is already in the cache
        for (PythonChecker aCache : cache) {
            if (Objects.equals(contractID, aCache.getContractID())) {
                return aCache;
            }
        }

        // otherwise, update cache
        PythonChecker newChecker = new PythonChecker(pythonScriptPath, contractID);
        cache.add(newChecker);
        if (cache.size() > CACHE_DEPTH) {cache.remove(0);}
        return newChecker;

    }

}
