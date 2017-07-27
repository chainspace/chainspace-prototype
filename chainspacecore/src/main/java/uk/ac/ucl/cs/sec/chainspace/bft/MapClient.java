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

    private int invokeAsynchTimeout = 5000;

    public MapClient(String shardConfigFile) {
        // Shards
        if(!initializeShards(shardConfigFile)) {
            System.out.println("Could not read shard configuration file. Now exiting.");
            System.exit(0);
        }

        // Clients
        Random rand = new Random(System.currentTimeMillis());
        currClientID = Math.abs(rand.nextInt()); // Initializing with a random number because the program hangs
        // if there are two or more clients with the same ID

        initializeShardClients();

        asynchReplies = new HashMap<String,TOMMessage>();

        defaultShardID = 0;
    }

    public int mapObjectToShard(String object) {
        int iObject = Integer.parseInt(object);
        return iObject% shardToConfig.size();
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
                    System.out.println("Skipping Line # "+countLine+" in config file: Insufficient tokens");
            }
            lineReader.close();
            return true;
        } catch (Exception e) {
            System.out.println("There was an exception reading shard configuration file: "+ e.toString());
            return false;
        }

    }


    private void initializeShardClients() {
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
            System.out.println("initializeShardClients: Created new client proxy ID "+clientID+" for shard "+shardID+" with config "+config);
            System.out.println("The view of client "+clientID+" is: "+clientProxy.get(shardID).getViewManager().getCurrentView().toString());

            // Asynch client proxy
            int clientIDAsynch = this.getNextClientID();
            shardToClientAsynch.put(shardID, clientIDAsynch);
            clientProxyAsynch.put(shardID, new AsynchServiceProxy(clientIDAsynch,config));
            System.out.println("initializeShardClients: Created new ASYNCH client proxy ID "+clientIDAsynch+" for shard "+shardID+" with config "+config);
            System.out.println("The view of client "+clientID+" is: "+clientProxy.get(shardID).getViewManager().getCurrentView().toString());
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
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);

            oos.writeInt(RequestType.PUT);
            oos.writeUTF(key);
            oos.writeUTF(value);
            oos.close();
            System.out.println("Putting a key-value pair in shard ID "+defaultShardID);
            byte[] reply = clientProxy.get(defaultShardID).invokeOrdered(out.toByteArray());
            if (reply != null) {
                String previousValue = new String(reply);
                return previousValue;
            }
            return null;
        } catch (IOException ioe) {
            System.out.println("Exception putting value into hashmap: " + ioe.getMessage());
            return null;
        }
    }

    @Override
    public String get(Object key) {
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
            System.out.println("Exception getting value from the hashmap: " + ioe.getMessage());
            return null;
        }
    }

    @Override
    public String remove(Object key) {
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
            System.out.println("Exception removing value from the hashmap: " + ioe.getMessage());
            return null;
        }
    }

    @Override
    public int size() {
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
            System.out.println("Exception getting the size the hashmap: " + ioe.getMessage());
            return -1;
        }
    }

    public HashMap<String,Boolean> createObjects(List<String> outputObjects) {
        return createObjects(outputObjects,invokeAsynchTimeout);
    }

    public HashMap<String,Boolean> createObjects(List<String> outputObjects, int invokeTimeoutAsynch) {
        HashMap<Integer,Integer> shardToReq = new HashMap<Integer,Integer>();; // Request IDs indexed by shard IDs
        TOMMessageType reqType = TOMMessageType.ORDERED_REQUEST; // ACCEPT_T messages require BFT consensus, so type is ordered
        boolean earlyTerminate = false;
        String finalResponse = null;

        try {
            System.out.println("CREATE_OBJECT (DRIVER): Sending CREATE_OBJECT to relevant shards");

            // Send a request to each shard relevant to the ouputs
            for(String output: outputObjects) {
                int shardID = mapObjectToShard(output);
                if(shardID == -1) {
                    System.out.println("ACCEPT_T(DRIVER): Cannot map input "+output+" to a shard.");
                    earlyTerminate = true;
                    return null;
                }

                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bs);
                oos.writeInt(RequestType.CREATE_OBJECT);
                oos.writeUTF(output);
                oos.close();

                int req = clientProxyAsynch.get(shardID).invokeAsynchRequest(bs.toByteArray(), new ReplyListenerAsynch(shardID), reqType);
                System.out.println("CREATE_OBJECT (DRIVER): Sent to shard ID " + shardID + ", req ID " + req + " client ID " + shardToClientAsynch.get(shardID));
                shardToReq.put(shardID, req);
            }
            Thread.sleep(invokeTimeoutAsynch);//how long to wait for replies from all shards before doing cleanup and returning
        }
        catch(Exception e){
            System.out.println("CREATE_OBJECT (DRIVER): Experienced Exception: " + e.getMessage());
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

                        System.out.println("CREATE_OBJECT (DRIVER): Shard ID "+shard+" replied: "+strReply);

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
                System.out.println("CREATE_OBJECT (DRIVER): Could not be submitted!");
            }
            return replies;
        }
    }

    public String submitTransaction(Transaction t) {
        return submitTransaction(t, invokeAsynchTimeout);
    }


    public String submitTransaction(Transaction t, int invokeTimeoutAsynch) {
        Set<Integer> targetShards = new HashSet<Integer>();; // The shards relevant to this transaction
        HashMap<Integer,Integer> shardToReq = new HashMap<Integer,Integer>();; // Request IDs indexed by shard IDs
        TOMMessageType reqType = TOMMessageType.UNORDERED_REQUEST; // ACCEPT_T messages require BFT consensus, so type is ordered
        boolean earlyTerminate = false;
        String finalResponse = null;
        String transactionID = t.id;
        int msgType = RequestType.TRANSACTION_SUBMIT;

        try {
            System.out.println("SUBMIT_T (DRIVER): Sending SUBMIT_T to relevant shards");

            List<String> inputObjects = t.inputs;

            // Send a request to each shard relevant to this transaction
            for(String input: inputObjects) {
                int shardID = mapObjectToShard(input);

                if(shardID == -1) {
                    System.out.println("SUBMIT_T(DRIVER): Cannot map input "+input+" in transaction ID "+transactionID+" to a shard.");
                    finalResponse = "Local Error: Cannot map transaction to a shard" ;
                    earlyTerminate = true;
                    return finalResponse;
                }

                if(!targetShards.contains(shardID)) {
                    targetShards.add(shardID);
                    ByteArrayOutputStream bs = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bs);
                    oos.writeInt(msgType);
                    oos.writeObject(t);
                    oos.close();
                    int req = clientProxyAsynch.get(shardID).invokeAsynchRequest(bs.toByteArray(), new ReplyListenerAsynch(shardID), reqType);
                    System.out.println("SUBMIT_T(DRIVER): Sent SUBMIT_T  to shard ID " + shardID + ", req ID " + req + " client ID " + shardToClientAsynch.get(shardID));
                    shardToReq.put(shardID, req);
                }
            }

            Thread.sleep(invokeTimeoutAsynch);//how long to wait for replies from all shards before doing cleanup and returning
        } catch(Exception e){
            System.out.println("SUBMIT_T(DRIVER): Transaction ID "+transactionID+" experienced Exception: " + e.getMessage());
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

                        System.out.println("SUBMIT_T(DRIVER): Shard ID "+shard+" replied: "+strReply);

                        if (strReply.equals(ResponseType.ACCEPTED_T_ABORT)) {
                            System.out.println("SUBMIT_T(DRIVER): ACCEPTED_T_ABORT->Abort reply from shard ID "+shard);
                            return ResponseType.ACCEPTED_T_ABORT;
                        }
                    } else {
                        System.out.println("SUBMIT_T(DRIVER):ACCEPTED_T_ABORT->Null reply from shard ID "+shard);
                        return ResponseType.ACCEPTED_T_ABORT;
                    }
                    asynchReplies.remove(key);
                    // clientProxyAsynch.get(client).cleanAsynchRequest(req);
                }

                System.out.println("SUBMIT_T(DRIVER): COMMIT from all shards");
                return ResponseType.ACCEPTED_T_COMMIT;
            }
            else {
                System.out.println("SUBMIT_T(DRIVER): ACCEPT_T_SYSTEM_ERROR->Transaction ID " + transactionID + " could not be submitted!");
                return ResponseType.ACCEPT_T_SYSTEM_ERROR;
            }
        }
    }

    public byte[] prepare_t(Transaction t) {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bs);
            oos.writeInt(RequestType.PREPARE_T);
            oos.writeObject(t);
            oos.close();
            byte[] reply = clientProxy.get(defaultShardID).invokeOrdered(bs.toByteArray());
            System.out.println("PREPARE_T (DRIVER): Reply to shard ID "+defaultShardID+"is "+new String(reply,"UTF-8"));
            return reply;
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            return null;
        }
    }

    /*
    public String accept_t_commit(String transactionID) {
        return accept_t(transactionID, invokeAsynchTimeout, RequestType.ACCEPT_T_COMMIT);
    }
    */

    public String accept_t_commit(Transaction t) {
        //saveTransaction(t);
        return accept_t(t, invokeAsynchTimeout, RequestType.ACCEPT_T_COMMIT);
    }

    /*
    public String accept_t_abort(String transactionID) {
        return accept_t(transactionID, invokeAsynchTimeout, RequestType.ACCEPT_T_ABORT);
    }
    */

    public String accept_t_abort(Transaction t) {
        //saveTransaction(t);
        return accept_t(t, invokeAsynchTimeout, RequestType.ACCEPT_T_ABORT);
    }

    public String accept_t(Transaction t, int invokeTimeoutAsynch, int msgType) {
        Set<Integer> targetShards = new HashSet<Integer>();; // The shards relevant to this transaction
        HashMap<Integer,Integer> shardToReq = new HashMap<Integer,Integer>();; // Request IDs indexed by shard IDs
        TOMMessageType reqType = TOMMessageType.ORDERED_REQUEST; // ACCEPT_T messages require BFT consensus, so type is ordered
        boolean earlyTerminate = false;
        String finalResponse = null;
        String transactionID = t.id;

        try {
            System.out.println("ACCEPT_T (DRIVER): Sending ACCEPT_T to relevant shards");

            List<String> inputObjects = t.inputs;

            // Send a request to each shard relevant to this transaction
            for(String input: inputObjects) {
                int shardID = mapObjectToShard(input);

                if(shardID == -1) {
                    System.out.println("ACCEPT_T(DRIVER): Cannot map input "+input+" in transaction ID "+transactionID+" to a shard.");
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
                    int req = clientProxyAsynch.get(shardID).invokeAsynchRequest(bs.toByteArray(), new ReplyListenerAsynch(shardID), reqType);
                    System.out.println("ACCEPT_T(DRIVER): Sent ACCEPT_T type (" + msgType + ") to shard ID " + shardID + ", req ID " + req + " client ID " + shardToClientAsynch.get(shardID));
                    shardToReq.put(shardID, req);
                }
            }

            if(!earlyTerminate) {

                // all timeout values in milliseconds
                int minWait = 1; // First wait will be minWait long
                boolean firstAttempt = true;
                int maxWait = 10000;
                int timeoutIncrement = 2; // subsequent wait will proceed in timeoutIncrement until all shards reply
                // or waitedSoFar becomes greater than maxWait
                int waitedSoFar = 0;


                // Wait until we have waited for more than maxTimeoutVal
                while(waitedSoFar < maxWait) {

                    System.out.println("ACCEPT_T(DRIVER): Checking shard replies; been waiting for : " + waitedSoFar);

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

                            System.out.println("ACCEPT_T(DRIVER): Shard ID " + shard + " replied: " + strReply);

                            if (strReply.equals(ResponseType.ACCEPTED_T_ABORT))
                                abortShardReplies = true;
                        }
                    }

                    if(!missingShardReplies) {
                        if(!abortShardReplies) // Commit if all shards have replied and their reply is to commit
                            finalResponse = ResponseType.ACCEPTED_T_COMMIT;
                        else
                            finalResponse = ResponseType.ACCEPTED_T_ABORT;
                        System.out.println("ACCEPT_T(DRIVER): All shards replied; final response is: " + finalResponse);
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
                        System.out.println("ACCEPT_T(DRIVER): Timed out waiting for all shard replies; Doing : " + finalResponse);
                        finalResponse = ResponseType.ACCEPTED_T_ABORT;
                    }
                }
            }
        } catch(Exception e){
            System.out.println("ACCEPT_T(DRIVER): Transaction ID "+transactionID+" experienced Exception: " + e.getMessage());
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


    private class ReplyListenerAsynch implements ReplyListener {
        AsynchServiceProxy client;
        private int shardID;
        private int replyQuorum; // size of the reply quorum
        private TOMMessage replies[];
        private int receivedReplies; // Number of received replies


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

        private ReplyListenerAsynch(int shardID) {
            System.out.println("New reply listener class created: shard ID "+shardID);
            this.shardID = shardID;
            replyQuorum = getReplyQuorum(shardID);
            client = clientProxyAsynch.get(shardID);
            replies = new TOMMessage[client.getViewManager().getCurrentViewN()];
            receivedReplies = 0;
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
                            String key = shardToClientAsynch.get(shardID)+";"+context.getReqId()+";"+context.getRequestType();
                            TOMMessage shardResponse = extractor.extractResponse(replies, sameContent, pos);
                            asynchReplies.put(key,shardResponse);
                            try {
                                System.out.println("ACCEPT_T [replyReceived]: Final reply from shard ID" + shardID + ": " + new String(reply.getContent(), "UTF-8"));
                            }
                            catch(Exception e) {
                                System.out.println("ACCEPT_T [replyReceived]: Exception in printing final reply of shard ID "+shardID);
                            }

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
            return (int) Math.ceil((c.getViewManager().getCurrentViewN()) / 2) + 1;
        }
    }
}

