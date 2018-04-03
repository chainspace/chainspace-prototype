package uk.ac.ucl.cs.sec.chainspace;

import bftsmart.tom.util.Logger;
import uk.ac.ucl.cs.sec.chainspace.bft.ClientConfig;
import uk.ac.ucl.cs.sec.chainspace.bft.MapClient;
import uk.ac.ucl.cs.sec.chainspace.bft.RequestType;
import uk.ac.ucl.cs.sec.chainspace.bft.Transaction;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Scanner;

import static java.lang.String.format;
import static java.lang.System.err;
import static java.lang.System.out;

/**
 * Created by alberto on 01/08/2017.
 */
public class Client {



    // CONFIG -- port number
    public static final int PORT = initialisePort();

    private static SimpleLogger slogger;


    // contains info about shards and corresponding config files
    // this info should be passed on the client class
    static HashMap<String,String> configData;
    static String shardConfigFile;
    static int thisClient;
    static MapClient client;



    private static int initialisePort() {
        return new Integer(System.getProperty("client.api.port", "5000"));
    }
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
            out.println("Could not find configuration for thisClient.");
            done = false;
        }

        if(configData.containsKey(ClientConfig.shardConfigFile)) {
            shardConfigFile = configData.get(ClientConfig.shardConfigFile);
        }
        else {
            out.println("Could not find configuration for shardConfigFile.");
            done = false;
        }

        return done;
    }


    /**
     * readConfiguration
     * Read the configuration file.
     */
    private static boolean readConfiguration(String configFilePath) {



        try {
            File configFile = new File(configFilePath);
            out.println(format("Reading config from [%s]", configFile.getAbsoluteFile()));
            if (!configFile.exists()) {
              throw new FileNotFoundException(configFile.getAbsolutePath());
            }
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
                    out.println("Skipping Line # "+countLine+" in config file: Insufficient tokens");
            }
            lineReader.close();
            return true;
        } catch (Exception e) {
            out.flush();
            e.printStackTrace();
            err.flush();
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
            out.println("Usage: ConsoleClient <config file path>");
            System.exit(0);
        }

        SystemProcess.writeProcessIdToFile("chainspace.client.api.process.id");

        // get filepath
        String configFile = args[0];

        // fill config data
        configData = new HashMap<>();
        readConfiguration(configFile);
        if (!loadConfiguration()) {
            out.println("Could not load configuration. Now exiting.");
            System.exit(0);
        }

        // create clients for talking with other shards - connects directly to replica 0
        client = new MapClient(shardConfigFile, 0, 0);
        client.defaultShardID = 0;
        System.out.println("Initialised MapClient to talk to shard 0, replica 0");

        // start webservice
        try {
            startClientService();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("FATAL ERROR!!! Could not start webservice, shutting down.");
            System.exit(-1);
        }


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

        // simple measurements log
        slogger = new SimpleLogger("simplelog_client");

        // verbose print
        if (Main.VERBOSE) { Utils.printHeader("Starting Chainspace..."); }

        // run chainspace service
        try {
            new ClientService(PORT);
        }
        catch (Exception e) {
            throw new RuntimeException("client-api service failed to start on port " + PORT +" - see cause", e);
        }

        // verbose print
        if (Main.VERBOSE) { Utils.printLine(); }

    }

    static String submitTransaction(String request) throws AbortTransactionException, NoSuchAlgorithmException {

        out.println("\n>> SUBMITTING TRANSACTION...");
        Transaction transaction = new Transaction(request);
        return client.submitTransaction(transaction);

    }

    /**
     * Has no timeout configured to wait for replies
     */
    static void submitTransactionNoWait(String request) throws AbortTransactionException, NoSuchAlgorithmException {

        out.println("\n>> SUBMITTING TRANSACTION...");
        Transaction transaction = new Transaction(request);
        client.submitTransaction(transaction, 0);

        slogger.log(transaction.inputs.get(0));

    }

}


