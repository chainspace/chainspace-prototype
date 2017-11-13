package uk.ac.ucl.cs.sec.chainspace;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 *
 */
class PythonChecker {

    private String pythonScriptPath;
    private String contractID, methodID;
    private Process checkerProcess;

    private static final String CHECKER_HOST = "127.0.0.1";
    private static final int CACHE_DEPTH    = 10000;

    private static final ArrayList<PythonChecker> cache = new ArrayList<>(CACHE_DEPTH);

    private static int latestPort = 13000;
    private int port;


    /**
     *
     */
    private PythonChecker(String contractID) throws StartCheckerException {

        // save variables
        this.pythonScriptPath = "contracts/" +contractID+ ".py";
        this.contractID = contractID;

        // set port
        if (latestPort == 65535) {
            latestPort = 13000;
        }
        latestPort += 1;
        port = latestPort;

        // start the checker
        this.startChecker();

    }


    /**
     *
     */
    private void startChecker() throws StartCheckerException {

        ProcessBuilder pb = new ProcessBuilder(Arrays.asList(
                "python",
                this.pythonScriptPath,
                "checker",
                "--port",
                String.valueOf(this.port)
        ));

        if(Main.VERBOSE) {
            System.out.println("\nstartChecker: python " + this.pythonScriptPath + " checker --port " + this.port);
        }
        try {

            // start thread
            this.checkerProcess = pb.start();
            // sleep
            if(Main.VERBOSE) {
                System.out.println("\ncheckerProcess isAlive: " + this.checkerProcess.isAlive());
                System.out.println("error stream: \n" + readFully(this.checkerProcess.getErrorStream(), "UTF-8"));
                System.out.println("input stream: " + readFully(this.checkerProcess.getInputStream(), "UTF-8"));
            }

            Thread.sleep(1000);

        } catch (IOException | InterruptedException e) {
            throw new StartCheckerException("Couldn't start checker.");
        }

    }

    public static String readFully(InputStream inputStream, String charsetName) {

        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charsetName));
            char[] charBuffer = new char[512];
            int bytesRead = -1;
            while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                stringBuilder.append(charBuffer, 0, bytesRead);
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(bufferedReader);
        }
        return stringBuilder.toString();
    }

    public static void closeQuietly(Reader reader) {
        if (reader == null) {
            return;
        }

        try {
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not close stream (See Cause)", e);
        }
    }



    /**
     *
     */
    private String getContractID() {
        return contractID;
    }

    /**
     *
     */
    private String getMethodID() {
        return methodID;
    }

    /**
     *
     */
    private String getURL(String methodID) {

        return "http://" + CHECKER_HOST + ":" + this.port + "/" + this.contractID + "/" + methodID;

    }


    /**
     *
     */
    private void stopChecker() {

        this.checkerProcess.destroy();

    }


    /**
     *
     */
    String check(TransactionForChecker transactionForChecker) throws IOException {

        if(Main.VERBOSE) {
            System.out.println("\nChecker URL:");
            System.out.println("\t" + getURL(transactionForChecker.getMethodID()));
        }
        return Utils.makePostRequest(this.getURL(transactionForChecker.getMethodID()), transactionForChecker.toJson());
    }


    /**
     *
     */
    static PythonChecker getFromCache(String contractID)
            throws StartCheckerException
    {

        // verbose print
        if( Main.VERBOSE ) { System.out.println("\nChecker instance:"); }

        // check if that checker is already in the cache
        for (PythonChecker aCache : cache) {
            if ( contractID.equals(aCache.getContractID()) ) {

                if( Main.VERBOSE ) { System.out.println("\tChecker found in cache"); }
                return aCache;

            }
        }

        // otherwise, update cache
        PythonChecker newChecker = new PythonChecker(contractID);
        if( Main.VERBOSE ) { System.out.println("\tNew checker created"); }
        cache.add(newChecker);
        if (cache.size() > CACHE_DEPTH) {
            cache.get(0).stopChecker();
            cache.remove(0);
        }
        return newChecker;

    }

}
