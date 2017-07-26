package foo.gettingstarted.client;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.HashMap;

import bftsmart.tom.ServiceReplica;
import foo.gettingstarted.Config;
import foo.gettingstarted.RequestType;
import foo.gettingstarted.Transaction;

import java.util.Scanner;
import java.util.TreeMap;

public class ConsoleClient {

    static HashMap<String,String> configData;
    static String shardConfigFile; // Contains info about shards and corresponding config files.
    // This info should be passed on the client class.
    static int thisClient;

    private static boolean loadConfiguration() {
        boolean done = true;

        if(configData.containsKey(Config.thisClient))
            thisClient = Integer.parseInt(configData.get(Config.thisClient));
        else {
            System.out.println("Could not find configuration for thisClient.");
            done = false;
        }

        if(configData.containsKey(Config.shardConfigFile))
            shardConfigFile = configData.get(Config.shardConfigFile);
        else {
            System.out.println("Could not find configuration for shardConfigFile.");
            done = false;
        }

        return done;
    }

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

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: ConsoleClient <config file path>");
            System.exit(0);
        }

        String configFile = args[0];

        configData = new HashMap<String,String>(); // will be filled with config data by readConfiguration()
        readConfiguration(configFile);
        if(!loadConfiguration()) {
            System.out.println("Could not load configuration. Now exiting.");
            System.exit(0);
        }

        MapClient client = new MapClient(shardConfigFile); // Create clients for talking with other shards
        client.defaultShardID = 0;

        Scanner sc = new Scanner(System.in);
        Scanner console = new Scanner(System.in);

        while (true) {

            System.out.println("Select a shard (type 0 or 1):");
            int cmd = sc.nextInt();
            client.defaultShardID = cmd;

            System.out.println("Select an option:");
            System.out.println("1. ADD A KEY AND VALUE TO THE MAP");
            System.out.println("2. READ A VALUE FROM THE MAP");
            System.out.println("3. REMOVE AND ENTRY FROM THE MAP");
            System.out.println("4. GET THE SIZE OF THE MAP");
            System.out.println("5. VALIDATE A TRANSACTION");
            System.out.println("6. SUBMIT A TRANSACTION");
            System.out.println("7. PREPARE_T");
            System.out.println("8. ACCEPT_T_ABORT");
            System.out.println("9. ACCEPT_T_COMMIT");
            System.out.println("10. CREATE_OBJECT");

            cmd = sc.nextInt();
            String key, input;

            switch (cmd) {
                // All the tests below operate on client.testShardID which is user specified at the console
                case RequestType.PUT:
                    System.out.println("Putting value in the map");
                    System.out.println("Enter the key:");
                    key = console.nextLine();
                    System.out.println("Enter the value:");
                    String value = console.nextLine();
                    String result = client.put(key, value);
                    System.out.println("Previous value: " + result);
                    break;
                case RequestType.GET:
                    System.out.println("Reading value from the map");
                    System.out.println("Enter the key:");
                    key = console.nextLine();
                    result = client.get(key);
                    System.out.println("Value read: " + result);
                    break;
                case RequestType.REMOVE:
                    System.out.println("Removing value in the map");
                    System.out.println("Enter the key:");
                    key = console.nextLine();
                    result = client.remove(key);
                    System.out.println("Value removed: " + result);
                    break;
                case RequestType.SIZE:
                    System.out.println("Getting the map size");
                    int size = client.size();
                    System.out.println("Map size: " + size);
                    break;

                // All the tests below operate on whichever shards are relevant to the transaction
                // The shard ID provided by user at the console is ignored
                case RequestType.TRANSACTION_SUBMIT:
                    System.out.println("Committing  a transaction");
                    Transaction t2 = new Transaction();

                    System.out.println("Enter transaction ID:");
                    input = console.nextLine();
                    t2.addID(input);

                    System.out.println("Enter inputs, one per line (type 'q' to stop):");
                    input = console.nextLine();

                    while (!(input.equalsIgnoreCase("q"))) {
                        t2.addInput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Enter outputs, one per line (type 'q' to stop):");
                    input=console.nextLine();
                    while (!(input.equalsIgnoreCase("q"))) {
                        t2.addOutput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Transaction to be added is:");
                    t2.print();
                    result = client.submitTransaction(t2);
                    System.out.println("Transaction status: " + result);
                    break;

                case RequestType.PREPARE_T:
                    System.out.println("Doing PREPARE_T");
                    Transaction t3 = new Transaction();

                    System.out.println("Enter transaction ID:");
                    input = console.nextLine();
                    t3.addID(input);

                    System.out.println("Enter inputs, one per line (type 'q' to stop):");
                    input = console.nextLine();

                    while (!(input.equalsIgnoreCase("q"))) {
                        t3.addInput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Enter outputs, one per line (type 'q' to stop):");
                    input=console.nextLine();
                    while (!(input.equalsIgnoreCase("q"))) {
                        t3.addOutput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Transaction to be added is:");
                    t3.print();

                    byte[] reply = client.prepare_t(t3);
                    if(reply != null) {
                        try {
                            System.out.println("Transaction status: " + new String(reply, "UTF-8"));
                        }
                        catch(Exception e) {
                            System.out.println("Transaction status: Exception "+ e.toString());
                        }
                    }
                    else
                        System.out.println("Transaction status: null");
                    break;

                case RequestType.ACCEPT_T_COMMIT:
                    System.out.println("Doing ACCEPT_T_COMMIT");
                    Transaction t4 = new Transaction();

                    System.out.println("Enter transaction ID:");
                    input = console.nextLine();
                    t4.addID(input);

                    System.out.println("Enter inputs, one per line (type 'q' to stop):");
                    input = console.nextLine();

                    while (!(input.equalsIgnoreCase("q"))) {
                        t4.addInput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Enter outputs, one per line (type 'q' to stop):");
                    input=console.nextLine();
                    while (!(input.equalsIgnoreCase("q"))) {
                        t4.addOutput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Transaction to be added is:");
                    t4.print();

                    String strReply = client.accept_t_commit(t4);
                    /*
                    if(strReply != null)
                            System.out.println("Transaction status: " + strReply);
                    else
                        System.out.println("Transaction status: null");
                        */
                    break;
                case RequestType.CREATE_OBJECT:
                    System.out.println("Doing CREATE_OBJECT");
                    Transaction t5 = new Transaction();

                    System.out.println("Enter transaction ID:");
                    input = console.nextLine();
                    t5.addID(input);

                    System.out.println("Enter inputs, one per line (type 'q' to stop):");
                    input = console.nextLine();

                    while (!(input.equalsIgnoreCase("q"))) {
                        t5.addInput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Enter outputs, one per line (type 'q' to stop):");
                    input=console.nextLine();
                    while (!(input.equalsIgnoreCase("q"))) {
                        t5.addOutput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Transaction whose outputs are to be created is:");
                    t5.print();

                    HashMap<String, Boolean> replies = client.createObjects(t5.outputs);
                    if(replies != null) {
                        for(String output: replies.keySet())
                            System.out.println("Output "+output+" has been added result: "+replies.get(output));
                    }
                    else
                        System.out.println("Replies to CREATE_OBJECT is null");
                    break;

            }
        }
    }

}