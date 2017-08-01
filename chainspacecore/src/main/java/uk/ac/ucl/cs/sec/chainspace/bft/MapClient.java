package uk.ac.ucl.cs.sec.chainspace.bft;


// This is the class which sends requests to replicas
import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.ServiceProxy;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;

import java.nio.charset.Charset;

// Classes that need to be declared to implement this
// replicated Map
import java.io.*;
import java.util.*;

public class MapClient implements Map<String, String> {

    //private AsynchServiceProxy clientProxyAsynch[] = new AsynchServiceProxy[2];
    private HashMap<Integer,AsynchServiceProxy> clientProxyAsynch = null; // Asynch client proxies indexed by shard IDs
    //private TOMMessage clientAsynchResponse[] = new TOMMessage[2]; // Responses to AsynchServiceProxies
    //private boolean sentShardsAsynch[] =  new boolean[2]; // Shards to which Asynch requests have been sent

    //private ServiceProxy clientProxy[] = new ServiceProxy[2];
    private HashMap<Integer,ServiceProxy> clientProxy = null; // Synch client proxies indexed by shard IDs

    public int defaultShardID; // Use this for testing for a single shard from a driver test program
    public int thisShard; // The shard with which this replica-client is associated
    public int thisReplica; // The replica with which this replica-client is associated
    String strLabel;

    // Shard configuration info
    private HashMap<Integer,String> shardToConfig = null; // Configurations indexed by shard ID
    private static int currClientID; // Each shard gets its own client with a unique ID
    private HashMap<Integer,Integer> shardToClient = null; // Client IDs indexed by shard ID
    private HashMap<Integer,Integer> shardToClientAsynch = null; // Asynch Client IDs indexed by shard ID

    //public HashMap<String,Transaction> transactions = null; // Transactions indexed by Transaction ID

    // Asynch replies need to be saved somewhere until sufficient replies have been received or
    // the request times out, after which we do not need the replies any more and space can be released.
    // asynchReplies is indexed by (clientID,requestID,requestType) concatenated into a string which
    // will be always unique (as per class TomSender)
    private HashMap<String,TOMMessage> asynchReplies  = null;

    // FIXME: Choose suitable timeout values
    private int submitTTimeout = 10000; // How long should the client wait for responses from all shards
                                        // after calling SUBMIT_T
    private int createObjectTimeout = 0; // How long should the replica-client wait for responses from all shards
                                         // after calling CREATE_OBJECT


    public MapClient(String shardConfigFile) {
        String strModule = "MapClient: ";
        // Shards
        if(!initializeShards(shardConfigFile)) {
            logMsg(strLabel,strModule,"Could not read shard configuration file. Now exiting.");
            System.exit(0);
        }

        // Clients
        Random rand = new Random(System.currentTimeMillis());
        currClientID = Math.abs(rand.nextInt()); // Initializing with a random number because the program hangs
        // if there are two or more clients with the same ID

        initializeShardClients();

        asynchReplies = new HashMap<String,TOMMessage>();

        defaultShardID = 0;
        strLabel = "["+thisShard+":"+ thisReplica+"] "; // This string is used in debug messages
    }

    public int mapObjectToShard(String object) {
        // TODO: objects are not integers
        int iObject = Integer.parseInt(object, 16);
        if(iObject%2 == 0)
            return 0;
        return 1;
    }

    // This function returns a unique client ID every time it is called
    private int getNextClientID() {
        if(++currClientID == Integer.MIN_VALUE)
            currClientID = 0;
        return currClientID;
    }


    private boolean initializeShards(String configFile) {
        // The format pf configFile is <shardID> \t <pathToShardConfigFile>

        // Shard-to-Configuration Mapping
        shardToConfig = new HashMap<Integer,String>();
        String strModule = "initializeShards: ";

        try {
            BufferedReader lineReader = new BufferedReader(new FileReader(configFile));
            String line;
            int countLine = 0;
            int limit = 2; //Split a line into two tokens, the key and value

            while ((line = lineReader.readLine()) != null) {
                countLine++;
                String[] tokens = line.split("\\s+",limit);

                if(tokens.length == 2) {
                    int shardID = Integer.parseInt(tokens[0]);
                    String shardConfig = tokens[1];
                    shardToConfig.put(shardID, shardConfig);
                }
                else
                    logMsg(strLabel,strModule,"Skipping Line # "+countLine+" in config file: Insufficient tokens");
            }
            lineReader.close();
            return true;
        } catch (Exception e) {
            logMsg(strLabel,strModule,"There was an exception reading shard configuration file "+ e.toString());
            return false;
        }

    }


    private void initializeShardClients() {
        String strModule = "initializeShardClients: ";
        // Clients IDs indexed by shard IDs
        shardToClient = new HashMap<Integer,Integer>();
        shardToClientAsynch = new HashMap<Integer,Integer>();

        // Client objects indexed by shard IDs
        clientProxyAsynch = new HashMap<Integer,AsynchServiceProxy>();
        clientProxy = new HashMap<Integer,ServiceProxy>();

        // Each shard has a synch and asynch client, with different client IDs
        for(int shardID: shardToConfig.keySet()) {
            String config = shardToConfig.get(shardID);

            // Synch client proxy
            int clientID = this.getNextClientID();
            shardToClient.put(shardID, clientID);
            clientProxy.put(shardID, new ServiceProxy(clientID,config));
            logMsg(strLabel,strModule,"Created new client proxy ID "+clientID+" for shard "+shardID+" with config "+config);
            logMsg(strLabel,strModule,"The view of client "+clientID+" is: "+clientProxy.get(shardID).getViewManager().getCurrentView().toString());

            // Asynch client proxy
            int clientIDAsynch = this.getNextClientID();
            shardToClientAsynch.put(shardID, clientIDAsynch);
            clientProxyAsynch.put(shardID, new AsynchServiceProxy(clientIDAsynch,config));
            logMsg(strLabel,strModule,"Created new ASYNCH client proxy ID "+clientIDAsynch+" for shard "+shardID+" with config "+config);
            logMsg(strLabel,strModule,"The view of client "+clientID+" is: "+clientProxy.get(shardID).getViewManager().getCurrentView().toString());
        }
    }

    private String getKeyAsynchReplies(int a, int b, String c){
        return a+";"+b+";"+c;
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<String> values() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String put(String key, String value) {
        String strModule = "PUT (DRIVER)";
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);

            oos.writeInt(RequestType.PUT);
            oos.writeUTF(key);
            oos.writeUTF(value);
            oos.close();
            logMsg(strLabel,strModule,"Putting a key-value pair in shard ID "+defaultShardID);
            byte[] reply = clientProxy.get(defaultShardID).invokeOrdered(out.toByteArray());
            if (reply != null) {
                String previousValue = new String(reply);
                return previousValue;
            }
            return null;
        } catch (IOException ioe) {
           logMsg(strLabel,strModule,"Exception putting value into table " + ioe.getMessage());
            return null;
        }
    }

    @Override
    public String get(Object key) {
        String strModule = "GET (DRIVER)";
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeInt(RequestType.GET);
            oos.writeUTF(String.valueOf(key));
            oos.close();
            byte[] reply = clientProxy.get(defaultShardID).invokeUnordered(out.toByteArray());
            String value = new String(reply);
            return value;
        } catch (IOException ioe) {
            logMsg(strLabel,strModule,"Exception getting value from table " + ioe.getMessage());
            return null;
        }
    }

    @Override
    public String remove(Object key) {
        String strModule = "REMOVE (DRIVER)";
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeInt(RequestType.REMOVE);
            oos.writeUTF(String.valueOf(key));
            oos.close();
            byte[] reply = clientProxy.get(defaultShardID).invokeOrdered(out.toByteArray());
            if (reply != null) {
                String removedValue = new String(reply);
                return removedValue;
            }
            return null;
        } catch (IOException ioe) {
            logMsg(strLabel,strModule,"Exception removing value from table " + ioe.getMessage());
            return null;
        }
    }

    @Override
    public int size() {
        String strModule = "SIZE (DRIVER)";
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeInt(RequestType.SIZE);
            oos.close();
            byte[] reply = clientProxy.get(defaultShardID).invokeUnordered(out.toByteArray());
            ByteArrayInputStream in = new ByteArrayInputStream(reply);
            DataInputStream dis = new DataInputStream(in);
            int size = dis.readInt();
            return size;
        } catch (IOException ioe) {
            logMsg(strLabel,strModule,"Exception getting the size of table " + ioe.getMessage());
            return -1;
        }
    }

    public HashMap<String,Boolean> createObjects(List<String> outputObjects) {
        return createObjects(outputObjects,createObjectTimeout);
    }

    public HashMap<String,Boolean> createObjects(List<String> outputObjects, int invokeTimeoutAsynch) {
        HashMap<Integer,Integer> shardToReq = new HashMap<Integer,Integer>();; // Request IDs indexed by shard IDs
        TOMMessageType reqType = TOMMessageType.ORDERED_REQUEST; // ACCEPT_T messages require BFT consensus, so type is ordered
        boolean earlyTerminate = false;
        String strModule = "CREATE_OBJECT (DRIVER): ";

        try {
            HashMap<Integer,ArrayList<String>> shardToObjects = new HashMap<>(); // Objects managed by a shard

            // Group objects by the managing shard
            for(String output: outputObjects) {
                int shardID = mapObjectToShard(output);
                if(shardID == -1) {
                    logMsg(strLabel,strModule,"Cannot map output "+output+" to a shard.");
                    earlyTerminate = true;
                    return null;
                }
                if(!shardToObjects.containsKey(shardID)) {
                    shardToObjects.put(shardID, new ArrayList<String>());
                }
                shardToObjects.get(shardID).add(output);
            }

            // Send a request to each shard relevant to the outputs
            for(int shardID: shardToObjects.keySet()) {

                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bs);
                oos.writeInt(RequestType.CREATE_OBJECT);
                oos.writeObject(shardToObjects.get(shardID));
                oos.close();


                int req = clientProxyAsynch.get(shardID).invokeAsynchRequest(bs.toByteArray(), new ReplyListenerAsynchQuorum(shardID), reqType);
                logMsg(strLabel,strModule,"Sent a request to shard ID " + shardID);
                shardToReq.put(shardID, req);

            }
            Thread.sleep(invokeTimeoutAsynch);//how long to wait for replies from all shards before doing cleanup and returning
        }
        catch(Exception e){
            logMsg(strLabel,strModule,"Experienced Exception " + e.getMessage());
        }

        finally {
            // Initialize replies with false for each object. This will be returned to the caller
            HashMap<String,Boolean> replies = new HashMap<String,Boolean>();
            for(String each: outputObjects) {
                replies.put(each, false);
            }
            if(!earlyTerminate) {
                // Now responses from all shards should be in asynchReplies
                for(String output: outputObjects) {
                    int shard = mapObjectToShard(output);
                    int client = shardToClientAsynch.containsKey(shard) ? shardToClientAsynch.get(shard) : -1;
                    int req = shardToReq.containsKey(shard) ? shardToReq.get(shard) : -1;
                    String key = getKeyAsynchReplies(client, req, reqType.toString());
                    TOMMessage m = asynchReplies.get(key);

                    // finalResponse is ABORT if at least one shard replies ABORT or does not reply at all
                    if (m != null) {
                        byte[] reply = m.getContent();
                        String strReply = new String(reply, Charset.forName("UTF-8"));

                        logMsg(strLabel,strModule,"Shard ID "+shard+" replied "+strReply);

                        if (strReply.equals(ResponseType.CREATED_OBJECT))
                            replies.put(output, true);
                        else
                            replies.put(output, false);
                    } else {
                        replies.put(output, false);
                    }
                    asynchReplies.remove(key);
                }
            }
            else {
                logMsg(strLabel,strModule,"Could not send request!");
            }
            return replies;
        }
    }


    // The BFT initiator uses this function to inform other replicas about the
    // decision of a BFT round.
    // TODO: The message should include proof (e.g., bundle of signatures) that
    // TODO: other replicas agree on this decision
    public void broadcastBFTDecision(int msgType, Transaction t) {
        TOMMessageType reqType = TOMMessageType.UNORDERED_REQUEST;
        String strModule = "broadcastBFTDecision (DRIVER): ";
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bs);
            oos.writeInt(msgType);
            oos.writeObject(t);
            oos.close();

            // BFT initiator will broadcast the final msg to replicas in its own shard only,
            // even if the BFT itself involved multiple shards (such as in ACCEPT_T)
            int shardID = defaultShardID;
            logMsg(strLabel,strModule,"Sending msg "+msgType+" to shard ID "+shardID);
            int req = clientProxyAsynch.get(shardID).invokeAsynchRequest(bs.toByteArray(), new ReplyListener() {
                @Override
                public void replyReceived(RequestContext context, TOMMessage reply) { }
            }, reqType);
        }
        catch(Exception e){
            logMsg(strLabel,strModule,"Experienced Exception " + e.getMessage());
        }
    }


    public String submitTransaction(Transaction t) {
        return submitTransaction(t, submitTTimeout);
    }


    public String submitTransaction(Transaction t, int invokeTimeoutAsynch) {
        Set<Integer> targetShards = new HashSet<Integer>();; // The shards relevant to this transaction
        HashMap<Integer,Integer> shardToReq = new HashMap<Integer,Integer>();; // Request IDs indexed by shard IDs
        TOMMessageType reqType = TOMMessageType.UNORDERED_REQUEST; // ACCEPT_T messages require BFT consensus, so type is ordered
        boolean earlyTerminate = false;
        String finalResponse = null;
        String transactionID = t.id;
        int msgType = RequestType.TRANSACTION_SUBMIT;
        String strModule = "SUBMIT_T (DRIVER): ";

        try {
            List<String> inputObjects = t.inputs;

            // Send a request to each shard relevant to this transaction
            for(String input: inputObjects) {
                int shardID = mapObjectToShard(input);

                logMsg(strLabel,strModule,"Adding input "+input+" to transaction ID: "+t.id);

                if(shardID == -1) {
                    logMsg(strLabel,strModule,"Cannot map input "+input+" in transaction ID "+transactionID+" to a shard.");
                    finalResponse = "Local Error: Cannot map transaction to a shard" ;
                    earlyTerminate = true;
                    return finalResponse;
                }

                if(!targetShards.contains(shardID)) {
                    logMsg(strLabel,strModule," Sending SUBMIT_T to shard ID "+shardID);
                    targetShards.add(shardID);
                    ByteArrayOutputStream bs = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bs);
                    oos.writeInt(msgType);
                    oos.writeObject(t);
                    oos.close();
                    // SUBMIT_T request sent asynchronously to all relevant shards
                    // Expect single response from BFTInitiator of each shard to which the request was sent
                    int req = clientProxyAsynch.get(shardID).invokeAsynchRequest(bs.toByteArray(), new ReplyListenerAsynchSingle(shardID), reqType);
                    shardToReq.put(shardID, req);
                }
            }



            /*
                /!\ DEBUG
                Transactions with no inputs are sent to shard 0 (init functions).
                TODO
             */
            if (inputObjects.size() == 0) {
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bs);
                oos.writeInt(msgType);
                oos.writeObject(t);
                oos.close();
                int shardID = 0;
                int req = clientProxyAsynch.get(shardID).invokeAsynchRequest(
                        bs.toByteArray(), new ReplyListenerAsynchSingle(shardID), reqType
                );
                shardToReq.put(shardID, req);
            }
            /*
                END
             */



            Thread.sleep(invokeTimeoutAsynch);//how long to wait for replies from all shards before doing cleanup and returning
        } catch(Exception e){
            logMsg(strLabel,strModule,"Transaction ID "+transactionID+" experienced Exception " + e.getMessage());
        }

        finally {
            if(!earlyTerminate) {

                // Now responses from all shards should be in asynchReplies

                for (int shard : targetShards) {
                    int client = shardToClientAsynch.containsKey(shard) ? shardToClientAsynch.get(shard) : -1;
                    int req = shardToReq.containsKey(shard) ? shardToReq.get(shard) : -1;
                    String key = getKeyAsynchReplies(client, req, reqType.toString());
                    TOMMessage m = asynchReplies.get(key);

                    // finalResponse is ABORT if at least one shard replies ABORT or does not reply at all
                    if (m != null) {
                        byte[] reply = m.getContent();
                        String strReply = new String(reply, Charset.forName("UTF-8"));

                        logMsg(strLabel,strModule,"Shard ID "+shard+" replied "+strReply);

                        if (strReply.equals(ResponseType.ACCEPTED_T_ABORT)) {
                            logMsg(strLabel,strModule,"ACCEPTED_T_ABORT->Abort reply from shard ID "+shard);
                            return ResponseType.ACCEPTED_T_ABORT;
                        }
                    } else {
                        logMsg(strLabel,strModule,"ACCEPTED_T_ABORT->Null reply from shard ID "+shard);
                        return ResponseType.ACCEPTED_T_ABORT;
                    }
                    asynchReplies.remove(key);
                }

                logMsg(strLabel,strModule,"COMMIT from all shards");
                return ResponseType.ACCEPTED_T_COMMIT;
            }
            else {
                logMsg(strLabel,strModule,"ACCEPT_T_SYSTEM_ERROR->Transaction ID " + transactionID + " could not be submitted!");
                return ResponseType.ACCEPT_T_SYSTEM_ERROR;
            }
        }
    }

    public byte[] prepare_t(Transaction t) {
        String strModule = "PREPARE_T (DRIVER): ";
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bs);
            oos.writeInt(RequestType.PREPARE_T);
            oos.writeObject(t);
            oos.close();
            // PREPARE_T BFT round done synchronously within the local shard
            byte[] reply = clientProxy.get(defaultShardID).invokeOrdered(bs.toByteArray());
            logMsg(strLabel,strModule,"Reply from shard ID "+defaultShardID+"is "+new String(reply,"UTF-8"));
            return reply;
        } catch (Exception e) {
            logMsg(strLabel,strModule,"Exception: " + e.getMessage());
            return null;
        }
    }


    public String accept_t_commit(Transaction t) {
        //saveTransaction(t);
        return accept_t(t, RequestType.ACCEPT_T_COMMIT);
    }


    public String accept_t_abort(Transaction t) {
        //saveTransaction(t);
        return accept_t(t, RequestType.ACCEPT_T_ABORT);
    }

    public String accept_t(Transaction t, int msgType) {
        Set<Integer> targetShards = new HashSet<Integer>();; // The shards relevant to this transaction
        HashMap<Integer,Integer> shardToReq = new HashMap<Integer,Integer>();; // Request IDs indexed by shard IDs
        TOMMessageType reqType = TOMMessageType.ORDERED_REQUEST; // ACCEPT_T messages require BFT consensus, so type is ordered
        boolean earlyTerminate = false;
        String finalResponse = null;
        String transactionID = t.id;
        String strModule = "ACCEPT_T (DRIVER)";

        try {
            List<String> inputObjects = t.inputs;

            // Send a request to each shard relevant to this transaction
            for(String input: inputObjects) {
                int shardID = mapObjectToShard(input);

                if(shardID == -1) {
                    logMsg(strLabel,strModule,"Cannot map input "+input+" in transaction ID "+transactionID+" to a shard.");
                    finalResponse = ResponseType.ACCEPT_T_SYSTEM_ERROR;
                    earlyTerminate = true;
                    break;
                }

                if(!targetShards.contains(shardID)) {
                    targetShards.add(shardID);
                    ByteArrayOutputStream bs = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bs);
                    oos.writeInt(msgType);
                    oos.writeObject(t);
                    oos.close();
                    // ACCEPT_T BFT rounds done asynchronously over all relevant shards
                    int req = clientProxyAsynch.get(shardID).invokeAsynchRequest(bs.toByteArray(), new ReplyListenerAsynchQuorum(shardID), reqType);
                    logMsg(strLabel,strModule,"Sent ACCEPT_T type (" + msgType + ") to shard ID " + shardID);
                    shardToReq.put(shardID, req);
                }
            }

            if(!earlyTerminate) {

                // all timeout values in milliseconds
                // FIXME: Choose suitable timeout values
                int minWait = 1; // First wait will be minWait long
                int timeoutIncrement = 2; // subsequent wait will proceed in timeoutIncrement until all shards reply
                int maxWait = 10000; // time out if waitedSoFar exceeds maxWait

                boolean firstAttempt = true;
                int waitedSoFar = 0;


                // Wait until we have waited for more than maxWait
                while(waitedSoFar < maxWait) {

                    //logMsg(strLabel,strModule,"Checking shard replies; been waiting for " + waitedSoFar);

                    boolean abortShardReplies = false; // at least 1 shard replied abort
                    boolean missingShardReplies = false; // at least 1 shard reply is missing (null)

                    // Check responses from all shards in asynchReplies
                    for (int shard : targetShards) {

                        // Get a shard reply
                        int client = shardToClientAsynch.containsKey(shard) ? shardToClientAsynch.get(shard) : -1;
                        int req = shardToReq.containsKey(shard) ? shardToReq.get(shard) : -1;
                        String key = getKeyAsynchReplies(client, req, reqType.toString());
                        TOMMessage m = asynchReplies.get(key);

                        if (m == null) {
                            missingShardReplies = true;
                            break; // A shard hasn't replied yet, we need to wait more
                        }

                        else {
                            byte[] reply = m.getContent();
                            String strReply = new String(reply, Charset.forName("UTF-8"));

                            logMsg(strLabel,strModule,"Shard ID " + shard + " replied: " + strReply);

                            if (strReply.equals(ResponseType.ACCEPTED_T_ABORT))
                                abortShardReplies = true;
                        }
                    }

                    if(!missingShardReplies) {
                        if(!abortShardReplies) // Commit if all shards have replied and their reply is to commit
                            finalResponse = ResponseType.ACCEPTED_T_COMMIT;
                        else
                            finalResponse = ResponseType.ACCEPTED_T_ABORT;
                        logMsg(strLabel,strModule,"All shards replied; final response is " + finalResponse);
                        break;
                    }

                    if(firstAttempt) {
                        Thread.sleep(minWait);
                        waitedSoFar += minWait;
                        firstAttempt = false;
                    }
                    else {
                        Thread.sleep(timeoutIncrement);
                        waitedSoFar += timeoutIncrement;
                    }

                    if(waitedSoFar > maxWait) // We are about to exit this loop and haven't yet heard from all shards
                    {
                        logMsg(strLabel,strModule,"Timed out waiting for all shard replies. ABORT.");
                        finalResponse = ResponseType.ACCEPTED_T_ABORT;
                    }
                }
            }
        } catch(Exception e){
            logMsg(strLabel,strModule,"Transaction ID "+transactionID+" experienced Exception " + e.getMessage());
            finalResponse = ResponseType.ACCEPT_T_SYSTEM_ERROR;
        }

        finally {
            // Clean up
            for (int shard : targetShards) {
                int client = shardToClientAsynch.containsKey(shard) ? shardToClientAsynch.get(shard) : -1;
                int req = shardToReq.containsKey(shard) ? shardToReq.get(shard) : -1;
                String key = getKeyAsynchReplies(client, req, reqType.toString());
                asynchReplies.remove(key);
            }

            return finalResponse;
        }
    }


    private class ReplyListenerAsynchSingle implements ReplyListener {
        AsynchServiceProxy client;
        private int shardID;
        private TOMMessage replies[];
        private String strModule;

        private Comparator<byte[]> comparator = new Comparator<byte[]>() {
            @Override
            public int compare(byte[] o1, byte[] o2) {
                return Arrays.equals(o1, o2) ? 0 : -1;
            }
        };

        private Extractor extractor = new Extractor() {
            @Override
            public TOMMessage extractResponse(TOMMessage[] replies, int sameContent, int lastReceived) {
                return replies[lastReceived];
            }
        };

        private ReplyListenerAsynchSingle(int shardID) {
            this.shardID = shardID;
            client = clientProxyAsynch.get(shardID);
            replies = new TOMMessage[client.getViewManager().getCurrentViewN()];
            strModule = "AsynchReplyListenerSingle: ";
        }

        @Override
        public void replyReceived(RequestContext context, TOMMessage reply) {

            // When to give reply to the application layer
            int pos = client.getViewManager().getCurrentViewPos(reply.getSender());

            if (pos >= 0) { //only consider messages from replicas

                replies[pos] = reply;
                String strReply = null;
                try {
                    strReply = new String(reply.getContent(), "UTF-8");
                    // Ignore dummy responses, only capture response from the BFTInitiator (which is non-dummy)
                    if(!strReply.equals(ResponseType.DUMMY)) {
                        String key = shardToClientAsynch.get(shardID)+";"+context.getReqId()+";"+context.getRequestType();
                        int dummyVal = -1;
                        TOMMessage shardResponse = extractor.extractResponse(replies, dummyVal, pos);
                        asynchReplies.put(key,shardResponse);
                        client.cleanAsynchRequest(context.getReqId());
                    }
                }
                catch(Exception e) {
                    logMsg(strLabel,strModule,"Exception in printing final reply of shard ID "+shardID);
                }

            }
        }

    }

    private class ReplyListenerAsynchQuorum implements ReplyListener {
        AsynchServiceProxy client;
        private int shardID;
        private int replyQuorum; // size of the reply quorum
        private TOMMessage replies[];
        private int receivedReplies; // Number of received replies
        private String strModule;


        private Comparator<byte[]> comparator = new Comparator<byte[]>() {
            @Override
            public int compare(byte[] o1, byte[] o2) {
                return Arrays.equals(o1, o2) ? 0 : -1;
            }
        };

        private Extractor extractor = new Extractor() {
            @Override
            public TOMMessage extractResponse(TOMMessage[] replies, int sameContent, int lastReceived) {
                return replies[lastReceived];
            }
        };

        private ReplyListenerAsynchQuorum(int shardID) {
            this.shardID = shardID;
            replyQuorum = getReplyQuorum(shardID);
            client = clientProxyAsynch.get(shardID);
            replies = new TOMMessage[client.getViewManager().getCurrentViewN()];
            receivedReplies = 0;
            strModule = "ReplyListenerAsynchQuorum: ";
        }


        @Override
        public void replyReceived(RequestContext context, TOMMessage reply) {
            //System.out.println("New reply received by client ID "+client.getProcessId()+" from  "+reply.getSender());
            StringBuilder builder = new StringBuilder();
            builder.append("[RequestContext] id: " + context.getReqId() + " type: " + context.getRequestType());
            builder.append("[TOMMessage reply] sender id: " + reply.getSender() + " Hash content: " + Arrays.toString(reply.getContent()));
            //System.out.println("ACCEPT_T: New reply received from shard ID"+shardID+": "+builder.toString());

            // When to give reply to the application layer

            int pos = client.getViewManager().getCurrentViewPos(reply.getSender());

            if (pos >= 0) { //only consider messages from replicas

                int sameContent = 1;

                if (replies[pos] == null) {
                    receivedReplies++;
                }
                replies[pos] = reply;

                // Compare the reply just received, to the others
                for (int i = 0; i < replies.length; i++) {

                    if ((i != pos || client.getViewManager().getCurrentViewN() == 1) && replies[i] != null
                            && (comparator.compare(replies[i].getContent(), reply.getContent()) == 0)) {
                        sameContent++;
                        if (sameContent >= replyQuorum) {
                            String key = shardToClientAsynch.get(shardID) + ";" + context.getReqId() + ";" + context.getRequestType();
                            TOMMessage shardResponse = extractor.extractResponse(replies, sameContent, pos);
                            asynchReplies.put(key, shardResponse);
                            /*
                            try {
                                System.out.println("ACCEPT_T [replyReceived]: Final reply from shard ID" + shardID + ": " + new String(reply.getContent(), "UTF-8"));
                            }
                            catch(Exception e) {
                                System.out.println("ACCEPT_T [replyReceived]: Exception in printing final reply of shard ID "+shardID);
                            }
                            */

                            //System.out.println("ACCEPT_T: [RequestContext] clean request context id: " + context.getReqId());
                            client.cleanAsynchRequest(context.getReqId());
                        }
                    }
                }
            }

        }
    }

    private int getReplyQuorum(int shardID) {
        AsynchServiceProxy c = clientProxyAsynch.get(shardID);

        if (c.getViewManager().getStaticConf().isBFT()) {
            return (int) Math.ceil((c.getViewManager().getCurrentViewN()
                    + c.getViewManager().getCurrentViewF()) / 2) + 1;
        } else {
            //return (int) Math.ceil((c.getViewManager().getCurrentViewN()) / 2) + 1;
            // For unordered asynch requests, a single reply is fine.
            // Where such messages are used, either the reply doesn't matter
            // (broadcastBFTDecision) or only a single replica is expected to
            // reply (e.g., BFTInitiator in submitTransaction).
            return 1;
        }
    }

    void logMsg(String id, String module, String msg ) {
        System.out.println(id+module+msg);
    }
}


