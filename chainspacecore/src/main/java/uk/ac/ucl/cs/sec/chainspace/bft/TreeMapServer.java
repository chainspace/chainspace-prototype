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
import uk.ac.ucl.cs.sec.chainspace.SimpleLogger;


public class TreeMapServer extends DefaultRecoverable {

    SimpleLogger slogger;
    Map<String, String> table;
    HashMap<String, TransactionSequence> sequences; // Indexed by Transaction ID
    int thisShard; // the shard this replica is part of
    int thisReplica; // ID of this replica within thisShard
    MapClient client;
    String strLabel;
    HashMap<String,String> configData;
    String shardConfigFile; // Contains info about shards and corresponding config files.
                            // This info should be passed on the client class.

    private Core core;

    public TreeMapServer(String configFile) {
        this.slogger = new SimpleLogger();

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
        strLabel = "[s"+thisShard+"n"+ thisReplica+"] "; // This string is used in debug messages


        ServiceReplica server = new ServiceReplica(thisReplica, this, this); // Create the server

        try {
            Thread.sleep(5000);
        }
        catch(Exception e) {
            System.out.println("Error initializing the server. Now exiting.");
            System.exit(-1);
        }

        client = new MapClient(shardConfigFile, thisShard, thisReplica); // Create clients for talking with other shards
    }

    private boolean loadConfiguration() {
        String strModule = "loadConfiguration: ";
        boolean done = true;

        logMsg(strLabel,strModule,"Loading configuration");

        if(configData.containsKey(ServerConfig.thisShard)) {
            thisShard = Integer.parseInt(configData.get(ServerConfig.thisShard));
            logMsg(strLabel,strModule,"thisShard is "+thisShard);
        }
        else {
            logMsg(strLabel,strModule,"Could not find configuration for thisShardID.");
            done = false;
        }

        if(configData.containsKey(ServerConfig.shardConfigFile)) {
            shardConfigFile = configData.get(ServerConfig.shardConfigFile);
            logMsg(strLabel,strModule,"shardConfigFile is "+shardConfigFile);
        }
        else {
            logMsg(strLabel,strModule,"Could not find configuration for shardConfigFile.");
            done = false;
        }

        if(configData.containsKey(ServerConfig.thisReplica)) {
            thisReplica = Integer.parseInt(configData.get(ServerConfig.thisReplica));
            logMsg(strLabel,strModule,"thisReplica is "+thisReplica);
        }
        else {
            logMsg(strLabel,strModule,"Could not find configuration for thisReplica.");
            done = false;
        }
        return done;
    }

    private boolean readConfiguration(String configFile) {
        String strModule = "readConfiguration: ";
        logMsg(strLabel,strModule,"Reading configuration");
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
                    logMsg(strLabel,strModule,"Read this line from configuration file "+line);
                    logMsg(strLabel,strModule,"Shard ID "+token+" Replica ID "+value);
                }
                else
                    logMsg(strLabel,strModule,"Skipping Line # "+countLine+" in config file: Insufficient tokens");
            }
            lineReader.close();
            return true;
        } catch (Exception e) {
            logMsg(strLabel,strModule,"There was an exception reading configuration file "+ e.toString());
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
        String strModule = "executeSingle: ";
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(command);
            ObjectInputStream ois = new ObjectInputStream(in);
            reqType = ois.readInt();
            //logMsg(strLabel,strModule,"Received a request of type "+ RequestType.getReqName(reqType));
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
            else if (reqType == RequestType.REMOVE) {
                String key = ois.readUTF();
                String removedValue = table.remove(key);
                byte[] resultBytes = null;
                if (removedValue != null) {
                    resultBytes = removedValue.getBytes();
                }
                return resultBytes;
            }
            else if (reqType == RequestType.PREPARE_T) {
                strModule = "PREPARE_T (MAIN): ";
                try {
                    Transaction t = (Transaction) ois.readObject();
                    logMsg(strLabel,strModule,"Received request for transaction "+t.id);

                    String reply = "";
                    /*
                    // Check in sequences if it has been already decided
                    if(sequences.containsKey(t.id) && sequences.get(t.id).PREPARED_T_COMMIT) {
                        reply = ResponseType.PREPARED_T_COMMIT;
                        logMsg(strLabel,strModule,"Found in sequences! responding with "+reply);
                    }
                    else if (sequences.containsKey(t.id) && sequences.get(t.id).PREPARED_T_ABORT) {
                        reply = ResponseType.PREPARED_T_ABORT;
                        logMsg(strLabel,strModule,"Found in sequences! responding with "+reply);
                    }
                    // Run checkPrepareT function only when there is no information present in local sequences
                    else {
                    */
                    reply = checkPrepareT(t);
                    logMsg(strLabel,strModule,"checkPrepare responding with "+reply);
                    //}
                    return reply.getBytes("UTF-8");
                }
                catch (Exception  e) {
                    logMsg(strLabel,strModule," Exception " + e.getMessage());
                    e.printStackTrace();
                    return ResponseType.PREPARE_T_SYSTEM_ERROR.getBytes("UTF-8");
                }

            }
            else if (reqType == RequestType.ACCEPT_T_COMMIT) {
                strModule = "ACCEPT_T_COMMIT (MAIN): ";
                try {
                    Transaction t = (Transaction) ois.readObject();
                    logMsg(strLabel,strModule,"Received request for transaction "+t.id);

                    String reply = checkAcceptT(t);
                    logMsg(strLabel,strModule,"checkAcceptT responding with "+reply);

                    return reply.getBytes("UTF-8");
                }

                catch (Exception  e) {
                    logMsg(strLabel,strModule,"Exception " + e.getMessage());
                    return null;
                }
            }
            else if (reqType == RequestType.ACCEPT_T_ABORT) {
                strModule = "ACCEPT_T_ABORT (MAIN): ";
                try {
                    Transaction t = (Transaction) ois.readObject();

                    logMsg(strLabel,strModule,"Received request for transaction "+t.id);

                    String reply = checkAcceptT(t);
                    logMsg(strLabel,strModule,"checkAcceptT responding with "+reply);

                    return reply.getBytes("UTF-8");
                }

                catch (Exception  e) {
                    logMsg(strLabel,strModule,"Exception " + e.getMessage());
                    return null;
                }
            }
            else if (reqType == RequestType.CREATE_OBJECT) {
                strModule = "CREATE_OBJECT (MAIN): ";
                try {
                    ArrayList<String> objects = (ArrayList<String>) ois.readObject();
                    String status = ObjectStatus.ACTIVE; // New objects are active
                    for(String object: objects) {
                        table.put(object, status);

                        logMsg(strLabel,strModule,"Created object "+object);

                    }
                }
                catch(Exception e) {
                    logMsg(strLabel,strModule,"Exception " + e.getMessage());
                }
                return null; // No reply expected by the caller
            }

            else {
                logMsg(strLabel,strModule,"Unknown request type " + reqType);
                return null;
            }
        } catch (IOException e) {
            logMsg(strLabel,strModule,"Exception reading data in the replica " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        int reqType;
        String strModule = " ";
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
            // The BFTInitiator sends these messages to inform the replicas about the decision of a BFT round
            else if (reqType == RequestType.PREPARED_T_COMMIT || reqType == RequestType.PREPARED_T_ABORT ||
                        reqType == RequestType.ACCEPTED_T_COMMIT || reqType == RequestType.ACCEPTED_T_ABORT) {
                strModule = "broadcastBFTDecision (MAIN): ";
                try {
                    Transaction t = (Transaction) ois.readObject();

                    logMsg(strLabel,strModule,"Received msg type "+RequestType.getReqName(reqType));

                    switch(reqType) {
                        case RequestType.PREPARED_T_COMMIT:
                            executePreparedTCommit(t);
                            break;
                        case RequestType.PREPARED_T_ABORT:
                            executePreparedTAbort(t);
                            break;
                        case RequestType.ACCEPTED_T_COMMIT:
                            executeAcceptedTCommit(t);
                            break;
                        case RequestType.ACCEPTED_T_ABORT:
                            executeAcceptedTAbort(t);
                            break;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    logMsg(strLabel,strModule,"Exception reading data in the replica " + e.getMessage());
                }
                finally {
                    return null;  // No reply expected by the caller
                }
            }
            else if (reqType == RequestType.TRANSACTION_SUBMIT) {
                strModule = "SUBMIT_T (MAIN): ";
                try {
                    logMsg(strLabel,strModule,"Received request");
                    Transaction t = (Transaction) ois.readObject();
                    t.print();

                    // Early Reject: A transaction will only be processed again if the system
                    // previously aborted it. We do not process transactions that are already being
                    // processed or were previously committed
                    //if (sequences.containsKey(t.id) && !(sequences.get(t.id).ACCEPTED_T_ABORT)) {
                    //    logMsg(strLabel,strModule,"Early reject");
                    //    return ResponseType.SUBMIT_T_REJECTED.getBytes("UTF-8");
                    //}

                    sequences.put(t.id, new TransactionSequence()); // Create fresh sequence for this transaction

                    // If it is a BFTInitiator Replica
                    if(isBFTInitiator()) {

                        sequences.get(t.id).PREPARE_T = true;

                        // Send PREPARE_T
                        logMsg(strLabel,strModule,"Sending PREPARE_T");

                        // Ask client to do PREPARE_T
                        byte[] replyPrepareT = client.prepare_t(t,this.thisShard);

                        // Process response to PREPARE_T, and send ACCEPT_T
                        String strReplyAcceptT;

                        // Process reply to PREPARE_T
                        String strReplyPrepareT = "";
                        if (replyPrepareT != null) {
                            strReplyPrepareT = new String(replyPrepareT, "UTF-8");
                            logMsg(strLabel,strModule,"Reply to PREPARE_T is " + strReplyPrepareT);
                        } else {
                            logMsg(strLabel, strModule, "Reply to PREPARE_T is null");
                        }

                        // PREPARED_T_ABORT, ACCEPT_T_ABORT
                        if (replyPrepareT == null
                                || strReplyPrepareT.equals(ResponseType.PREPARED_T_ABORT)
                                || strReplyPrepareT.equals(ResponseType.PREPARE_T_SYSTEM_ERROR)) {

                            client.broadcastBFTDecision(RequestType.PREPARED_T_ABORT, t, this.thisShard);

                            // TODO: Can we skip BFT here and return to client?
                            sequences.get(t.id).ACCEPT_T_ABORT = true;

                            logMsg(strLabel,strModule, "Sending ACCEPT_T_ABORT");
                            strReplyAcceptT = client.accept_t_abort(t);

                            // TODO: If this shard is the one initiating ACCEPT_T_ABORT then
                            // TODO: it will always abort this transaction.
                            if (true) {
                                client.broadcastBFTDecision(RequestType.ACCEPTED_T_ABORT, t, this.thisShard);
                            }
                            logMsg(strLabel,strModule,"ABORTED. Reply to ACCEPT_T_ABORT is " + strReplyAcceptT);
                            logMsg(strLabel,strModule,"ABORTED. Shards have accepted the abort, returning " + strReplyPrepareT);
                            strReplyAcceptT = strReplyPrepareT;
                        }
                        // PREPARED_T_COMMIT, ACCEPT_T_COMMIT
                        else if (strReplyPrepareT.equals(ResponseType.PREPARED_T_COMMIT)) {

                            client.broadcastBFTDecision(RequestType.PREPARED_T_COMMIT, t, this.thisShard);

                            sequences.get(t.id).ACCEPT_T_COMMIT = true;

                            logMsg(strLabel,strModule, "Sending ACCEPT_T_COMMIT");
                            strReplyAcceptT = client.accept_t_commit(t);

                            logMsg(strLabel,strModule,"Reply to ACCEPT_T_COMMIT is " + strReplyAcceptT);

                            if (strReplyAcceptT.equals(ResponseType.ACCEPT_T_SYSTEM_ERROR) || strReplyAcceptT.equals(ResponseType.ACCEPTED_T_ABORT)) {
                                client.broadcastBFTDecision(RequestType.ACCEPTED_T_ABORT, t, this.thisShard);
                            } else if (strReplyAcceptT.equals(ResponseType.ACCEPTED_T_COMMIT)) {
                                client.broadcastBFTDecision(RequestType.ACCEPTED_T_COMMIT, t, this.thisShard);
                                slogger.log(String.join(",", t.inputs) + "-" + String.join(",", t.outputs) + " " + countUniqueInputShards(t));
                            }
                            logMsg(strLabel,strModule,"Reply to ACCEPT_T_COMMIT is " + strReplyAcceptT);
                        } else {
                            logMsg(strLabel,strModule,"Unknown error in processing PREPARE_T");
                            strReplyAcceptT = ResponseType.PREPARE_T_SYSTEM_ERROR;
                        }
                        logMsg(strLabel,strModule,"Finally replying to client: " + strReplyAcceptT);
                        // TODO: The final response should contain proof (bundle of signatures)
                        // TODO: from replicas to convince the client that all replicas in the
                        // TODO: shard agree on the final decision

                        strReplyAcceptT = strReplyAcceptT + ";" + t.getCsTransaction().getInputIDs()[0];
                        return strReplyAcceptT.getBytes("UTF-8");

                    }
                    // If it is a non-BFTInitiator Replica
                    // TODO: Optimization: Non-BFTInitiator replicas should relay submitTransaction
                    // TODO: to the BFTInitiator so it hears the msg even if the msg from client
                    // TODO: failed to reach it
                    else {
                        return ResponseType.DUMMY.getBytes("UTF-8"); // Dummy responses that will be ignored
                    }
                }

                catch (Exception  e) {
                    logMsg(strLabel,strModule,"Exception " + e.getMessage());
                    return ResponseType.SUBMIT_T_SYSTEM_ERROR.getBytes("UTF-8");
                }
            }
            else {
                logMsg(strLabel,strModule,"Unknown request type " + reqType);
                return ResponseType.SUBMIT_T_UNKNOWN_REQUEST.getBytes("UTF-8");
            }
        } catch (IOException e) {
            logMsg(strLabel,strModule,"Exception reading data in the replica " + e.getMessage());
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

        sequences.get(t.id).PREPARED_T_ABORT = true;
    }

    private void executePreparedTCommit(Transaction t){
        // TODO: Things to do when transaction is PREPARED_T_COMMIT

        // Lock transaction input objects that were previously active
        setTransactionInputStatus(t, ObjectStatus.LOCKED, ObjectStatus.ACTIVE);

        sequences.get(t.id).PREPARED_T_COMMIT = true;
    }

    private void executeAcceptedTAbort(Transaction t){
        // TODO: Things to do when transaction is ACCEPTED_T_ABORT
        // Unlock transaction input objects that were previously locked

        setTransactionInputStatus(t, ObjectStatus.ACTIVE, ObjectStatus.LOCKED);

        sequences.get(t.id).ACCEPTED_T_ABORT = true;
    }

    private void executeAcceptedTCommit(Transaction t){
        // TODO: Things to do when transaction is ACCEPTED_T_COMMIT
        // Inactivate transaction input objects
        setTransactionInputStatus(t, ObjectStatus.INACTIVE);
        String strModule = "executeAcceptedTCommit";

        if(isBFTInitiator()) {
            // Create output objects (local as well as those on other shards)
            int timeout = 0; // How long to wait for replies to be accumulated in replies.
            // Default is no wait; just let object creation happen asynchronously.
            logMsg(strLabel,strModule,"Creating outputs for the following transaction:");
            t.print();


            client.createObjects(t.outputs);
        }

        sequences.get(t.id).ACCEPTED_T_COMMIT = true;
    }


    private String checkPrepareT(Transaction t) {
        // TODO: Check if the transaction is malformed, return INVALID_BADTRANSACTION

        System.out.println("\n>> PROCESSING TRANSACTION...");
        if (t.getCsTransaction() != null) {
            System.out.println(t.getCsTransaction().getContractID());
        }
        // Check if all input objects are active
        // and at least one of the input objects is managed by this shard
        int nManagedObj = 0;
        String reply = ResponseType.PREPARED_T_COMMIT;
        String strErr = "Unknown";

        for(String key: t.inputs) {
            String readValue = table.get(key);
            boolean managedObj = (client.mapObjectToShard(key)==thisShard);
            if(managedObj)
                nManagedObj++;
            if(managedObj && readValue == null) {
                strErr = Transaction.INVALID_NOOBJECT;
                reply = ResponseType.PREPARED_T_ABORT;
            }
            else if(managedObj && readValue != null) {
                if (readValue.equals(ObjectStatus.LOCKED)) {
                    strErr = Transaction.REJECTED_LOCKEDOBJECT;
                    reply = ResponseType.PREPARED_T_ABORT;
                }
                else if (managedObj && readValue.equals(ObjectStatus.INACTIVE)) {
                    strErr = Transaction.INVALID_INACTIVEOBJECT;
                    reply = ResponseType.PREPARED_T_ABORT;
                }
            }
            // debug option -- should be removed
            else {
                System.out.println("\n>> DEBUG MODE");
            }

        }

        if (t.getCsTransaction() != null) { // debug compatible
            System.out.println("\n>> RUNNING CORE...");
            try {
                String[] out = core.processTransaction(t.getCsTransaction(), t.getStore());
                System.out.println("\n>> PRINTING TRANSACTION'S OUTPUT...");
                System.out.println(Arrays.toString(out));
            } catch (Exception e) {
                strErr = e.getMessage();
                reply = ResponseType.PREPARED_T_ABORT;
                e.printStackTrace();
            }
        }

        // The case when this shard doesn't manage any of the input objects
        // AND the transaction isn't an init transaction
        if(nManagedObj == 0 && t.inputs.size() != 0) {
            strErr = Transaction.INVALID_NOMANAGEDOBJECT;
            reply = ResponseType.PREPARED_T_ABORT;
        }

        if(reply.equals(ResponseType.PREPARED_T_ABORT))
            System.out.println("PREPARE_T (checkPrepareT): Returning PREPARED_T_ABORT->"+strErr);

        return reply;
    }

    // ============
    // BLOCK ENDS
    // ============

    private String checkAcceptT(Transaction t) {
        String strModule = "checkAcceptT";

        // FIXME: Choose suitable timeout values
        int minWait = 1; // First wait will be minWait long
        int timeoutIncrement = 2; // subsequent wait will proceed in timeoutIncrement until all shards reply
        int maxWait = 250; // time out if waitedSoFar exceeds maxWait

        boolean firstAttempt = true;
        int waitedSoFar = 0;

        if (sequences.containsKey(t.id) && !sequences.get(t.id).PREPARED_T_COMMIT &&
                !sequences.get(t.id).PREPARED_T_ABORT)
            logMsg(strLabel,strModule,"Waiting for PREPARED_T_* to be sequenced " +
                    " upon receiving ACCEPT_T.");

        try {
            while(waitedSoFar < maxWait) {
                if (sequences.containsKey(t.id) && (sequences.get(t.id).PREPARED_T_COMMIT ||
                        sequences.get(t.id).PREPARED_T_ABORT)) {
                    logMsg(strLabel,strModule,"Sequenced PREPARED_T_* upon receiving ACCEPT_T" +
                            "after waiting for "+waitedSoFar);
                    break;
                }

                if (firstAttempt) {
                    Thread.sleep(minWait);
                    waitedSoFar += minWait;
                    firstAttempt = false;
                }
                else {
                    Thread.sleep(timeoutIncrement);
                    waitedSoFar += timeoutIncrement;
                }

                if (waitedSoFar > maxWait) // We are about to exit this loop
                    logMsg(strLabel, strModule, "Timed out waiting for PREPARED_T_* to be sequenced " +
                            " upon receiving ACCEPT_T.");
            }
        }
        catch(Exception e) {
                logMsg(strLabel,strModule,"Exception making thread sleep "+e.toString());
        }

        if(sequences.containsKey(t.id) && sequences.get(t.id).PREPARED_T_COMMIT)
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
                String prev = table.put(input, status);
                System.out.println("Input object "+input+": "+prev+" -> "+status);
            }
        }
        return true;
    }

    public int countUniqueInputShards(Transaction t) {
        List<String> inputObjects = t.inputs;
        ArrayList<Integer> shardIDs = new ArrayList<Integer>();
        int unique = 0;
        for (String input : inputObjects) {
            Integer shardID = new Integer(client.mapObjectToShard(input));
            if (!shardIDs.contains(shardID)) {
                shardIDs.add(shardID);
                unique++;
            }
        }

        return unique;
    }

    public boolean setTransactionInputStatus(Transaction t, String status, String prevStatus) {
        List<String> inputObjects = t.inputs;

        // Set status of all input objects relevant to this transaction
        for (String input : inputObjects) {
            int shardID = client.mapObjectToShard(input);

            if (shardID == thisShard && table.get(input) != null && table.get(input).equals(prevStatus)) {
                String prev = table.put(input, status);
                System.out.println("Input object "+input+": "+prev+" -> "+status);
            }
        }
        return true;
    }

    // We don't want every replica to start a separate BFT round for
    // the same request. So within each shard, there is a designated
    // BFT initiator which conducts BFT round and broadcasts the output
    // to other replicas.
    // TODO: This function considers replica with ID 0 to be the shard initiator.
    // TODO: This should preferably be the SmartBFT leader defined in:
    // TODO: tom.core.ExecutionManager.currentLeader, but I can't access it
    // TODO: from the ServiceReplica object
    boolean isBFTInitiator() {
        if(thisReplica == 0)
            return true;
        return false;
    }

    void logMsg(String id, String module, String msg ) {
        System.out.println(id+module+msg);
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
