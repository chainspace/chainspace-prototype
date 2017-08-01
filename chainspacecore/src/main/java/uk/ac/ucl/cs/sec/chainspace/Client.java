package uk.ac.ucl.cs.sec.chainspace;

import uk.ac.ucl.cs.sec.chainspace.bft.ClientConfig;
import uk.ac.ucl.cs.sec.chainspace.bft.MapClient;
import uk.ac.ucl.cs.sec.chainspace.bft.RequestType;
import uk.ac.ucl.cs.sec.chainspace.bft.Transaction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by alberto on 01/08/2017.
 */
public class Client {



    // CONFIG -- port number
    public static final int PORT = 5000;


    // contains info about shards and corresponding config files
    // this info should be passed on the client class
    static HashMap<String,String> configData;
    static String shardConfigFile;
    static int thisClient;
    static MapClient client;


    /**
     * loadConfiguration
     * Load the configuration file.
     */
    private static boolean loadConfiguration() {
        boolean done = true;

        if(configData.containsKey(ClientConfig.thisClient)) {
            thisClient = Integer.parseInt(configData.get(ClientConfig.thisClient));
        }
        else {
            System.out.println("Could not find configuration for thisClient.");
            done = false;
        }

        if(configData.containsKey(ClientConfig.shardConfigFile)) {
            shardConfigFile = configData.get(ClientConfig.shardConfigFile);
        }
        else {
            System.out.println("Could not find configuration for shardConfigFile.");
            done = false;
        }

        return done;
    }


    /**
     * readConfiguration
     * Read the configuration file.
     */
    private static boolean readConfiguration(String configFile) {

        try {
            BufferedReader lineReader = new BufferedReader(new FileReader(configFile));
            String line;
            int countLine = 0;
            int limit = 2; //Split a line into two tokens, the key and value

            while ((line = lineReader.readLine()) != null) {
                countLine++;
                String[] tokens = line.split("\\s+",limit);

                if(tokens.length == 2) {
                    String token = tokens[0];
                    String value = tokens[1];
                    configData.put(token, value);
                }
                else
                    System.out.println("Skipping Line # "+countLine+" in config file: Insufficient tokens");
            }
            lineReader.close();
            return true;
        } catch (Exception e) {
            System.out.println("There was an exception reading configuration file: "+ e.toString());
            return false;
        }

    }


    /**
     * main
     * Client entry point.
     * @param args the path to the config file
     */
    public static void main(String[] args) {

        // check arguments
        if (args.length < 1) {
            System.out.println("Usage: ConsoleClient <config file path>");
            System.exit(0);
        }

        // get filepath
        String configFile = args[0];

        // fill config data
        configData = new HashMap<>();
        readConfiguration(configFile);
        if (!loadConfiguration()) {
            System.out.println("Could not load configuration. Now exiting.");
            System.exit(0);
        }

        // create clients for talking with other shards
        client = new MapClient(shardConfigFile);
        client.defaultShardID = 0;

        // start webservice
        startClientService();


        /*
        System.out.println("\n>> PREPARING TO SEND TRANSACTION...\n");

        CSTransaction csTransaction11 = new CSTransaction(
                "addition",
                new String[]{"hash_input_0"},
                new String[]{},
                new String[]{},
                new String[]{},
                new String[]{"1"},
                new CSTransaction[]{},
                "increment"
        );
        Store store11 = new Store();
        store11.put("hash_input_0", "0");


        Transaction t11 = null;
        try {
            t11 = new Transaction(csTransaction11, store11);
        } catch (NoSuchAlgorithmException e) {
            System.err.print("\n>> [ERROR] "); e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("Transaction to be added is:");
        t11.print();
        String result = client.submitTransaction(t11);
        System.out.println("Transaction status: " + result);
        */


    }

    private static void startClientService() {

        // verbose print
        if (Main.VERBOSE) { Utils.printHeader("Starting Chainspace..."); }

        // run chainspace service
        try {new ClientService(PORT);}
        catch (Exception e) {
            if (Main.VERBOSE) { Utils.printStacktrace(e); }
            else { System.err.println("[ERROR] Node service failed to start on port " + PORT); }
        }

        // verbose print
        if (Main.VERBOSE) { Utils.printLine(); }

    }

    static String submitTransaction(String request) throws AbortTransactionException, NoSuchAlgorithmException {

        Transaction transaction = new Transaction(request);
        return client.submitTransaction(transaction);

    }

    static void submitTransactionNoWait(String request) throws AbortTransactionException, NoSuchAlgorithmException {

        Transaction transaction = new Transaction(request);
        client.submitTransaction(transaction, 0);

    }

}


