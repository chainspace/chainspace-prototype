package uk.ac.ucl.cs.sec.chainspace;

import org.json.JSONObject;
import spark.Request;
import spark.Response;


import java.sql.SQLException;

import static spark.Spark.*;


/**
 *
 *
 */
class Service {

    // instance variables
    private int nodeID;
    private Core core;

    /**
     * Constructor
     * Runs a node service and init a database.
     */
    Service(int nodeID) throws SQLException, ClassNotFoundException {

        // save node ID
        this.nodeID = nodeID;

        // run one core
        this.core = new Core(nodeID);

        // add routes
        addRoutes();
    }


    /**
     * routes for the web service
     */
    private void addRoutes() {

        // returns a json containing the node ID
        path("/api", () -> path("/1.0", () -> {

            // return node ID
            get("/node_id", this::returnNodeID);

            // debug : add an object to the database
            post("/debug_load", this::debugLoadRequest);

            // process a transaction
            post("/process_transaction", this::processTransactionRequest);

        }));

    }


    /**
     * returnNodeID
     * Return the node ID in json format.
     */
    private String returnNodeID(Request request, Response response) {
        JSONObject json = new JSONObject();
        json.put("Node ID", nodeID);
        return json.toString();
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

        // process the transaction & create response
        JSONObject responseJson = new JSONObject();
        try {

            // get the transaction as java object
            Transaction transaction;
            try {
                transaction = Transaction.fromJson(request.body());
            }
            catch (Exception e) {
                throw new AbortTransactionException("Malformed Transaction.");
            }

            // process the transaction
            core.processTransaction(transaction);

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
        }

        // print request
        printRequestDetails(request, responseJson.toString());

        // send
        response.type("application/json");
        return responseJson.toString();
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
