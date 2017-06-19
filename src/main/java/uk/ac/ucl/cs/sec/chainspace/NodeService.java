package uk.ac.ucl.cs.sec.chainspace;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Service;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;


/**
 *
 *
 */
class NodeService {

    // instance variables
    private int nodeID;
    private Core core;


    /**
     * Constructor
     * Runs a node service and init a database.
     */
    NodeService(int nodeID) throws SQLException, ClassNotFoundException {

        // save node ID
        this.nodeID = nodeID;

        // run one core
        this.core = new Core(nodeID);

        // start service on given port
        int port = 3000 + nodeID;
        addRoutes(Service.ignite().port(port));

        // print init message
        printInitMessage(port);
    }

    /**
     * finalize
     * Gently shut down the node's core when the garbage collector is called.
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.core.close();
    }

    /**
     * routes for the web service
     */
    private void addRoutes(Service service) {

        // returns a json containing the node ID
        service.path("/api", () -> service.path("/1.0", () -> {

            // return node ID
            service.get("/node_id", (request, response) -> new JSONObject().put("Node ID", nodeID).toString());

            // debug : add an object to the database
            service.post("/debug_load", this::debugLoadRequest);

            // process a transaction
            service.post("/process_transaction", this::processTransactionRequest);

        }));

    }


    /**
     * debugLoad
     * Debug method to quickly add an object to the node database. It returns the corresponding object ID.
     */
    private String debugLoadRequest(Request request, Response response) {

        // register objects & create response
        JSONObject responseJson = new JSONObject();
        try {
            // add object to db
            core.debugLoad(request.body());

            // create json response
            responseJson.put("status", "OK");
            responseJson.put("objectID", Utils.hash(request.body()));
            response.status(200);
        }
        catch (Exception e) {
            // create json response
            responseJson.put("status", "ERROR");
            responseJson.put("message", e.getMessage());
            response.status(500);
        }

        // print request
        printRequestDetails(request, responseJson.toString());

        // send
        response.type("application/json");
        return responseJson.toString();

    }


    /**
     * processTransactionRequest
     * This method receives a json transaction, processes it, and responds with the transaction ID.
     */
    private String processTransactionRequest(Request request, Response response) {

        // get json request
        JSONObject requestJson = new JSONObject(request.body());

        // broadcast transaction to other nodes
        try {
            broadcastTransaction(request.body());
        } catch (IOException e) {
            e.printStackTrace();
        } // ignore failures

        // process the transaction & create response
        JSONObject responseJson = new JSONObject();
        try {

            // get the transaction and the key-value store as java objects
            Transaction transaction;
            Store store;
            try {
                transaction = Transaction.fromJson(requestJson.getJSONObject("transaction"));
                store = Store.fromJson(requestJson.getJSONArray("store"));
            }
            catch (Exception e) {
                throw new AbortTransactionException("Malformed transaction or key-value store.");
            }

            // process the transaction
            core.processTransaction(transaction, store);

            // create json response
            responseJson.put("status", "OK");
            responseJson.put("transactionID", transaction.getID());
            response.status(200);
        }
        catch (Exception e) {
            // create json  error response
            responseJson.put("status", "ERROR");
            responseJson.put("message", e.getMessage());
            response.status(400);

            e.printStackTrace();
        }

        // print request
        printRequestDetails(request, responseJson.toString());

        // send
        response.type("application/json");
        return responseJson.toString();
    }


    /**
     * broadcastTransaction
     * Broadcast the transaction to other nodes.
     */
    private void broadcastTransaction(String body) throws IOException {

        // debug: avoid infinite loop
        if (this.nodeID == 2) { return; }

        // make post request
        HttpClient httpClient = HttpClientBuilder.create().build();
        StringEntity postingString = new StringEntity(body);
        HttpPost post = new HttpPost("http://127.0.0.1:3002/api/1.0/process_transaction");
        post.setEntity(postingString);
        post.setHeader("Content-type", "application/json");
        httpClient.execute(post);
    }


    /**
     * printInitMessage
     * Print on the console an init message.
     */
    private void printInitMessage(int port) {
        System.out.println("\nNode service #" +nodeID+ " is running on port " +port+ " ...");
    }


    /**
     * printRequestDetails
     * Print on the console some details about the incoming request.
     */
    private void printRequestDetails(Request request, String response) {
        System.out.println("\nNode service #" +nodeID+ " [POST] @" +request.url()+ " from " +request.ip());
        System.out.println("\trequest content: " + request.body());
        System.out.println("\tresponse content: " + response);
    }

}
