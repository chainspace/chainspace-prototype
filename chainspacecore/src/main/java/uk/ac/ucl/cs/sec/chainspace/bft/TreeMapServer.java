package uk.ac.ucl.cs.sec.chainspace.bft;

// These are the classes which receive requests from clients
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;

// Classes that need to be declared to implement this
// replicated Map
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;

import uk.ac.ucl.cs.sec.chainspace.Core;


public class TreeMapServer extends DefaultRecoverable {

    Map<String, String> table;
    HashMap<String, TransactionSequence> sequences; // Indexed by Transaction ID
    int thisShard; // the shard this replica is part of
    int thisReplica; // ID of this replica within thisShard
    MapClient client;
    String conn;
    HashMap<String,String> configData;
    String shardConfigFile; // Contains info about shards and corresponding config files.
                            // This info should be passed on the client class.

    private Core core;

    public TreeMapServer(String configFile) {

        try { core = new Core(); }
        catch (ClassNotFoundException | SQLException e) {
            System.err.print(">> [ERROR] "); e.printStackTrace();
            System.exit(-1);
        }

        configData = new HashMap<String,String>(); // will be filled with config data by readConfiguration()
        readConfiguration(configFile);
        if(!loadConfiguration()) {
            System.out.println("Could not load configuration. Now exiting.");
            System.exit(0);
        }

        table = new TreeMap<>(); // contains objects and their state
        sequences = new  HashMap<>(); // contains operation sequences for transactions
        conn = "[?->"+thisShard+"] "; // This string is used in debug messages

        client = new MapClient(shardConfigFile); // Create clients for talking with other shards
        client.defaultShardID = thisShard;
        new ServiceReplica(thisReplica, this, this); // Create the server

    }

    private boolean loadConfiguration() {
        boolean done = true;

        if(configData.containsKey(ServerConfig.thisShard))
            thisShard = Integer.parseInt(configData.get(ServerConfig.thisShard));
        else {
            System.out.println("Could not find configuration for thisShardID.");
            done = false;
        }

        if(configData.containsKey(ServerConfig.shardConfigFile))
            shardConfigFile = configData.get(ServerConfig.shardConfigFile);
        else {
            System.out.println("Could not find configuration for shardConfigFile.");
            done = false;
        }

        if(configData.containsKey(ServerConfig.thisReplica))
            thisReplica = Integer.parseInt(configData.get(ServerConfig.thisReplica));
        else {
            System.out.println("Could not find configuration for thisReplica.");
            done = false;
        }
        return done;
    }

    private boolean readConfiguration(String configFile) {
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
            System.out.println("Usage: HashMapServer <configuration file>");
            System.exit(0);
        }

        new TreeMapServer(args[0]);
    }

    @Override
    public byte[][] appExecuteBatch(byte[][] command, MessageContext[] mcs) {

        byte[][] replies = new byte[command.length][];
        for (int i = 0; i < command.length; i++) {
            replies[i] = executeSingle(command[i], mcs[i]);
        }

        return replies;
    }

    private byte[] executeSingle(byte[] command, MessageContext msgCtx) {
        int reqType;
        conn = "["+msgCtx.getSender()+"->"+thisShard+"] ";
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(command);
            ObjectInputStream ois = new ObjectInputStream(in);
            reqType = ois.readInt();
            System.out.println(conn+"Received a request of type "+reqType);
            if (reqType == RequestType.PUT) {
                String key = ois.readUTF();
                String value = ois.readUTF();
                String oldValue = table.put(key, value);
                byte[] resultBytes = null;
                if (oldValue != null) {
                    resultBytes = oldValue.getBytes();
                }
                return resultBytes;
            }
            else if (reqType == RequestType.CREATE_OBJECT) {
                String object = ois.readUTF();
                String status = ObjectStatus.ACTIVE; // New objects are active
                table.put(object, status);
                return ResponseType.CREATED_OBJECT.getBytes("UTF-8");
            }
            else if (reqType == RequestType.REMOVE) {
                String key = ois.readUTF();
                String removedValue = table.remove(key);
                byte[] resultBytes = null;
                if (removedValue != null) {
                    resultBytes = removedValue.getBytes();
                }
                return resultBytes;
            } else if (reqType == RequestType.PREPARE_T) {
                try {
                    System.out.println(conn+"PREPARE_T (MAIN): Received request");
                    Transaction t = (Transaction) ois.readObject();
                    t.print();

                    String reply = "";
                    // Check in sequences if it has been already decided
                    if(sequences.containsKey(t.id) && sequences.get(t.id).PREPARED_T_COMMIT) {
                        reply = ResponseType.PREPARED_T_COMMIT;
                        System.out.println(conn+"PREPARE_T (MAIN): found in sequences! responding with: "+reply);
                    }
                    else if (sequences.containsKey(t.id) && sequences.get(t.id).PREPARED_T_ABORT) {
                            reply = ResponseType.PREPARED_T_ABORT;
                            System.out.println(conn+"PREPARE_T (MAIN): found in sequences! responding with: "+reply);
                    }
                    // Run checkPrepareT function only when there is no information present in local sequences
                    else {
                        reply = checkPrepareT(t);
                        System.out.println(conn+"PREPARE_T (MAIN): checkPrepare responding with: "+reply);
                    }

                    return reply.getBytes("UTF-8");
                }
                catch (Exception  e) {
                    System.out.println(conn+"PREPARE_T (MAIN): Exception: " + e.getMessage());
                    return ResponseType.PREPARE_T_SYSTEM_ERROR.getBytes("UTF-8");
                }

            }
            else if (reqType == RequestType.ACCEPT_T_COMMIT) {
                try {
                    System.out.println(conn+"ACCEPT_T_COMMIT (MAIN): Received request");
                    Transaction t = (Transaction) ois.readObject();
                    t.print();

                    String reply = checkAcceptT(t);
                    System.out.println(conn+"ACCEPT_T_COMMIT (MAIN): checkAcceptT responding with: "+reply);

                    return reply.getBytes("UTF-8");
                }

                catch (Exception  e) {
                    System.out.println(conn+"ACCEPT_T_COMMIT (MAIN): Exception: " + e.getMessage());
                    return null;
                }
            }
            else if (reqType == RequestType.ACCEPT_T_ABORT) {
                try {
                    System.out.println(conn+"ACCEPT_T_ABORT (MAIN): Received request");
                    Transaction t = (Transaction) ois.readObject();
                    t.print();

                    String reply = checkAcceptT(t);
                    System.out.println(conn+"ACCEPT_T_ABORT (MAIN): checkAcceptT responding with: "+reply);

                    return reply.getBytes("UTF-8");
                }

                catch (Exception  e) {
                    System.out.println(conn+"ACCEPT_T_ABORT (MAIN): Exception: " + e.getMessage());
                    return null;
                }
            }
            else {
                System.out.println(conn+"Unknown request type: " + reqType);
                return null;
            }
        } catch (IOException e) {
            System.out.println(conn+"Exception reading data in the replica: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        int reqType;
        conn = "["+msgCtx.getSender()+"->"+thisShard+"] ";
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(command);
            ObjectInputStream ois = new ObjectInputStream(in);
            reqType = ois.readInt();
            if (reqType == RequestType.GET) {
                String key = ois.readUTF();
                String readValue = table.get(key);
                byte[] resultBytes = null;
                if (readValue != null) {
                    resultBytes = readValue.getBytes();
                }
                return resultBytes;
            }
            else if (reqType == RequestType.SIZE) {
                int size = table.size();

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(out);
                dos.writeInt(size);
                byte[] sizeInBytes = out.toByteArray();

                return sizeInBytes;
            }
            else if (reqType == RequestType.TRANSACTION_SUBMIT) {
                try {
                    System.out.println(conn+"TRANSACTION_SUBMIT: Received request");
                    Transaction t = (Transaction) ois.readObject();
                    t.print();

                    // TODO: What if the same transaction is submitted again?
                    // TODO: We should check in sequences and reject if it already exists
                    sequences.put(t.id, new TransactionSequence());
                    sequences.get(t.id).PREPARE_T = true;

                    // Send PREPARE_T
                    System.out.println(conn+"TRANSACTION_SUBMIT: Sending PREPARE_T");
                    client.defaultShardID = thisShard;
                    byte[] replyPrepareT = client.prepare_t(t);

                    // Process response to PREPARE_T, and send ACCEPT_T
                    String strReplyAcceptT;

                    // Process reply to PREPARE_T
                    String strReplyPrepareT = "";
                    if(replyPrepareT != null) {
                        strReplyPrepareT = new String(replyPrepareT, "UTF-8");
                        System.out.println(conn+"TRANSACTION_SUBMIT: Reply to PREPARE_T is "+strReplyPrepareT);
                    }
                    else
                        System.out.println(conn+"TRANSACTION_SUBMIT: Reply to PREPARE_T is null");

                    // PREPARED_T_ABORT, ACCEPT_T_ABORT
                    if(replyPrepareT == null || strReplyPrepareT.equals(ResponseType.PREPARED_T_ABORT) || strReplyPrepareT.equals(ResponseType.PREPARE_T_SYSTEM_ERROR)) {
                        sequences.get(t.id).PREPARED_T_ABORT = true;

                        // Alberto: Do application-specific stuff here
                        executePreparedTAbort(t);

                        // TODO: Can we skip BFT here and return to client?
                        sequences.get(t.id).ACCEPT_T_ABORT = true;

                        System.out.println(conn+"TRANSACTION_SUBMIT: Sending ACCEPT_T_ABORT");
                        strReplyAcceptT = client.accept_t_abort(t);

                        // TODO: If this shard is the one initiating ACCEPT_T_ABORT then
                        // TODO: it will always abort this transaction.
                        if(true) {
                            sequences.get(t.id).ACCEPTED_T_ABORT = true;
                            // Alberto: Do application-specific work here
                            executeAcceptedTAbort(t);
                        }
                        System.out.println(conn+"TRANSACTION_SUBMIT: ABORTED and reply to ACCEPT_T_ABORT is "+strReplyAcceptT);
                    }
                    // PREPARED_T_COMMIT, ACCEPT_T_COMMIT
                    else if(strReplyPrepareT.equals(ResponseType.PREPARED_T_COMMIT)) {
                        sequences.get(t.id).PREPARED_T_COMMIT = true;

                        //Alberto: Do application-specific stuff here
                        executePreparedTCommit(t);

                        sequences.get(t.id).ACCEPT_T_COMMIT = true;

                        System.out.println(conn+"TRANSACTION_SUBMIT: Sending ACCEPT_T_COMMIT");
                        strReplyAcceptT = client.accept_t_commit(t);

                        System.out.println(conn+"TRANSACTION_SUBMIT: Reply to ACCEPT_T_COMMIT is "+strReplyAcceptT);

                        if(strReplyAcceptT.equals(ResponseType.ACCEPT_T_SYSTEM_ERROR) || strReplyAcceptT.equals(ResponseType.ACCEPTED_T_ABORT)) {
                            sequences.get(t.id).ACCEPTED_T_ABORT = true;
                            executeAcceptedTAbort(t);
                        }
                        else if(strReplyAcceptT.equals(ResponseType.ACCEPTED_T_COMMIT)) {
                            sequences.get(t.id).ACCEPTED_T_COMMIT = true;
                            executeAcceptedTCommit(t);
                        }
                        System.out.println(conn+"TRANSACTION_SUBMIT: Reply to ACCEPT_T_COMMIT is "+strReplyAcceptT);
                    }
                    else {
                        System.out.println("TRANSACTION_SUBMIT: Unknown error in processing PREPARE_T");
                        strReplyAcceptT = ResponseType.PREPARE_T_SYSTEM_ERROR;
                    }

                    System.out.println(conn+"TRANSACTION_SUBMIT: Replying to client: "+strReplyAcceptT);
                    return strReplyAcceptT.getBytes("UTF-8");
                }

                catch (Exception  e) {
                    System.out.println(conn+"Exception: " + e.getMessage());
                    return ResponseType.SUBMIT_T_SYSTEM_ERROR.getBytes("UTF-8");
                }
            }
            else {
                System.out.println(conn+"Unknown request type: " + reqType);
                return ResponseType.SUBMIT_T_UNKNOWN_REQUEST.getBytes("UTF-8");
            }
        } catch (IOException e) {
            System.out.println(conn+"Exception reading data in the replica: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Alberto: Implement the following functions
    // ============
    // BLOCK BEGINS
    // ============
    private void executePreparedTAbort(Transaction t){
        // TODO: Things to do when transaction is PREPARED_T_ABORT
    }

    private void executePreparedTCommit(Transaction t){
        // TODO: Things to do when transaction is PREPARED_T_COMMIT

        // Lock transaction input objects
        setTransactionInputStatus(t, ObjectStatus.LOCKED);
    }

    private void executeAcceptedTAbort(Transaction t){
        // TODO: Things to do when transaction is ACCEPTED_T_ABORT

        // Unlock transaction input objects
        setTransactionInputStatus(t, ObjectStatus.ACTIVE);
    }

    private void executeAcceptedTCommit(Transaction t){
        // TODO: Things to do when transaction is ACCEPTED_T_COMMIT
        // Inactivate transaction input objects
        setTransactionInputStatus(t, ObjectStatus.INACTIVE);

        // Create output objects (local as well as those on other shards)
        int timeout = 0; // How long to wait for replies to be accumulated in replies.
                         // Default is no wait; just let object creation happen asynchronously.
        HashMap<String,Boolean> replies = client.createObjects(t.outputs, timeout);
    }


    private String checkPrepareT(Transaction t) {
        // TODO: Check if the transaction is malformed, return INVALID_BADTRANSACTION

        System.out.println("\n>> PROCESSING TRANSACTION...");
        if (t != null) {
            System.out.println(t.getCsTransaction().getContractID());
        }
        
        // Check if all input objects are active
        // and at least one of the input objects is managed by this shard
        String reply = ResponseType.PREPARED_T_COMMIT;
        String strErr = "Unknown";

        for(String key: t.inputs) {
            // get object's status
            String readValue = table.get(key);

            //
            if(ObjectStatus.mapObjectToShard(key) != thisShard) {
                strErr = Transaction.INVALID_NOMANAGEDOBJECT;
                reply = ResponseType.PREPARED_T_ABORT;
            }
            /*
            else if(readValue == null) {
                //TODO: check whether it is an init function
                //strErr = Transaction.INVALID_NOOBJECT;
                //reply = ResponseType.PREPARED_T_ABORT;
            }
            */
            else if ((ObjectStatus.LOCKED).equals(readValue)) {
                strErr = Transaction.REJECTED_LOCKEDOBJECT;
                reply = ResponseType.PREPARED_T_ABORT;
            }
            else if ((ObjectStatus.INACTIVE).equals(readValue)) {
                strErr = Transaction.INVALID_INACTIVEOBJECT;
                reply = ResponseType.PREPARED_T_ABORT;
            }
            else if (t != null) { // debug compatible
                System.out.println("\n>> RUNNING CORE...");
                System.out.println(t.getCsTransaction());
                try {
                    String[] out = core.processTransaction(t.getCsTransaction(), t.getStore());
                    System.out.println("\n>> PRINTING TRANSACTION'S OUTPUT...");
                    System.out.println(Arrays.toString(out));
                } catch (Exception e) {
                    strErr = e.getMessage();
                    reply = ResponseType.PREPARED_T_ABORT;
                }
            }
            else {
                System.out.println("\n>> DEBUG MODE");
            }
        }

        // print out error
        if(reply.equals(ResponseType.PREPARED_T_ABORT))
            System.out.println("PREPARE_T (checkPrepareT): Returning PREPARED_T_ABORT->"+strErr);

        // return
        return reply;
    }
    // ============
    // BLOCK ENDS
    // ============

    private String checkAcceptT(Transaction t) {
        if(sequences.containsKey(t.id) && sequences.get(t.id).ACCEPT_T_COMMIT)
            return ResponseType.ACCEPTED_T_COMMIT;
        // TODO: Optimization: If we hear about an ACCEPT_T from which we don't have
        // TODO: PREPARE_T in sequences, we can initiate one and then respond
        return ResponseType.ACCEPTED_T_ABORT;
    }


    public boolean setTransactionInputStatus(Transaction t, String status) {
        List<String> inputObjects = t.inputs;

        // Set status of all input objects relevant to this transaction
        for (String input : inputObjects) {
            int shardID = client.mapObjectToShard(input);

            if (shardID == thisShard) {
                table.put(input, status);
            }
        }
        return true;
    }

    @Override
    public void installSnapshot(byte[] state) {
        ByteArrayInputStream bis = new ByteArrayInputStream(state);
        try {
            ObjectInput in = new ObjectInputStream(bis);
            table = (Map<String, String>) in.readObject();
            in.close();
            bis.close();
        } catch (ClassNotFoundException e) {
            System.out.print("Coudn't find Map: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.print("Exception installing the application state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public byte[] getSnapshot() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(table);
            out.flush();
            out.close();
            bos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            System.out.println("Exception when trying to take a + "
                    + "snapshot of the application state" + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
    }
}
