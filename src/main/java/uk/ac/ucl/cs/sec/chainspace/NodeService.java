package uk.ac.ucl.cs.sec.chainspace;

import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Service;


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
