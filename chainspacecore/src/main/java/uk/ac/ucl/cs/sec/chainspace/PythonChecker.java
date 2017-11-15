package uk.ac.ucl.cs.sec.chainspace;

import java.io.*;
import java.lang.reflect.Field;
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
    private static final int CACHE_DEPTH = 10000;

    private static final ArrayList<PythonChecker> cache = new ArrayList<>(CACHE_DEPTH);

    private static int latestPort = 13000;
    private int port;


    /**
     * Provides a process wrapper around a python script
     * This python script should launch a checker api
     * There is an implementation of this in the chainspacecontract python library
     */
    PythonChecker(String contractID) throws StartCheckerException {

        this.pythonScriptPath = "contracts/" + contractID + ".py";
        this.contractID = contractID;

        if (latestPort == 65535) {
            latestPort = 13000;
        }
        latestPort += 1;
        port = latestPort;

        this.startChecker();

    }


    void startChecker() throws StartCheckerException {
        System.out.println("Working dir: " + new File(".").getAbsolutePath());
        String pythonExecutable = "../.chainspace.env/bin/python";
        ProcessBuilder pb = new ProcessBuilder(Arrays.asList(
                pythonExecutable,
                this.pythonScriptPath,
                "checker",
                "--port",
                String.valueOf(this.port)
        ));


        redirectCheckerOutputToFile(pb, this.port);

        if (Main.VERBOSE) {
            System.out.println("\nstartChecker: " + pythonExecutable + " " + this.pythonScriptPath + " checker --port " + this.port);
        }

        try {
            Process checkerProcess = pb.start();

            logPid(checkerProcess, this.port);


            Thread.sleep(1000);

            System.out.println("\ncheckerProcess isAlive: " + checkerProcess.isAlive());

            if (!checkerProcess.isAlive()) {
                throw new StartCheckerException("Checker failed to start! (see ./checker.XXX.log for log output) " + "Exit value " + checkerProcess.exitValue());
            } else {
                this.checkerProcess = checkerProcess;
            }

        } catch (IOException | InterruptedException e) {
            throw new StartCheckerException("Couldn't start checker" , e);
        }

    }

    private static void logPid(Process checkerProcess, int port) {
        long pid = getPidOfProcess(checkerProcess);
        try {
            File pidFile = new File("./checker.pids");

            if (!pidFile.exists()) {
                pidFile.createNewFile();
            }

            FileWriter w = null;
            try {
                w = new FileWriter(pidFile, true);
                w.append("" + pid + "\n");
                System.out.println("Written pid [" + pid + "] to " + pidFile.getAbsolutePath());
            } finally {
                if (w != null) {
                    w.close();
                }
            }


        } catch (Throwable t) {
            System.out.println("Could not write pid file out");
            t.printStackTrace();
        }
    }


    /**
     * Currently only works on unix
     */
    public static long getPidOfProcess(Process p) {
        long pid = -1;

        try {
            if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getLong(p);
                f.setAccessible(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not get pid...");
            pid = -1;
        }
        return pid;
    }

    private static void redirectCheckerOutputToFile(ProcessBuilder pb, int port) {
        File checkerLog =  new File("./checker." + port + ".log.0");

        try {
            if (!checkerLog.exists()) {
                checkerLog.createNewFile();
            }
            pb.redirectOutput(checkerLog);
            pb.redirectError(checkerLog);
            System.out.println("Redirected process output to " + checkerLog.getAbsolutePath());
        } catch (Throwable t) {
            System.out.println("Could not create checker log @ " + checkerLog.getAbsolutePath());
            t.printStackTrace();
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

        if (Main.VERBOSE) {
            System.out.println("\nChecker URL:");
            System.out.println("\t" + getURL(transactionForChecker.getMethodID()));
        }
        return Utils.makePostRequest(this.getURL(transactionForChecker.getMethodID()), transactionForChecker.toJson());
    }


    /**
     *
     */
    static PythonChecker getFromCache(String contractID)
            throws StartCheckerException {

        if (Main.VERBOSE) {
            System.out.println("\nChecker instance:");
        }

        PythonChecker cachedChecker = findInCache(contractID);

        if (cachedChecker != null) return cachedChecker;

        // otherwise, update cache
        PythonChecker newChecker = new PythonChecker(contractID);
        if (Main.VERBOSE) {
            System.out.println("\tNew checker created");
        }
        cache.add(newChecker);
        if (cache.size() > CACHE_DEPTH) {
            cache.get(0).stopChecker();
            cache.remove(0);
        }
        return newChecker;

    }

    private static PythonChecker findInCache(String contractID) {
        for (PythonChecker checker : cache) {
            if (contractID.equals(checker.getContractID())) {

                if (Main.VERBOSE) {
                    System.out.println("\tChecker found in cache");
                }
                return checker;

            }
        }
        return null;
    }

}
