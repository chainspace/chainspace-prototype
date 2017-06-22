package uk.ac.ucl.cs.sec.chainspace;

import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Service;

import java.io.IOException;
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
            service.get("/node/id", (request, response) -> new JSONObject().put("Node ID", nodeID).toString());

            // process a transaction
            service.post("/transaction/process", this::processTransactionRequest);

        }));

    }


    /**
     * processTransactionRequest
     * This method receives a json transaction, processes it, and responds with the transaction ID.
     */
    private String processTransactionRequest(Request request, Response response) {

        // verbose print
        if (Main.VERBOSE) { Utils.printHeader("Incoming transaction"); }

        // broadcast transaction to other nodes
        if (! Main.DEBUG_NO_BROADCAST) {
            try {

                broadcastTransaction(request.body());

            } catch (IOException ignored) { // ignore failures here
                if (Main.VERBOSE) { Utils.printStacktrace(ignored); }
            }
        }


        // process the transaction & create response
        JSONObject responseJson = new JSONObject();
        try {

            // pass transaction to the core
            String[] returns = this.core.processTransaction(request.body());

            // create json response
            responseJson.put("status", "OK");
            responseJson.put("returns", returns);
            response.status(200);

        }
        catch (Exception e) {

            // create json  error response
            responseJson.put("status", "ERROR");
            responseJson.put("message", e.getMessage());
            response.status(400);

            // verbose print
            if (Main.VERBOSE) { Utils.printStacktrace(e); }

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
    private void broadcastTransaction(String data) throws IOException {

        // debug: avoid infinite loop
        // TODO: get the nodes ID and addresses from a config file
        if (this.nodeID == 2) { return; }
        String url = "http://127.0.0.1:3002/api/1.0/transaction/process";

        // make post request
        Utils.makePostRequest(url, data);

    }


    /**
     * printInitMessage
     * Print on the console an init message.
     */
    private void printInitMessage(int port) {

        // print node info
        System.out.println("\nNode service #" +nodeID+ " is running on port " +port);

    }


    /**
     * printRequestDetails
     * Print on the console some details about the incoming request.
     */
    private void printRequestDetails(Request request, String response) {

        // print request summary
        System.out.println("\nNode service #" +nodeID+ " [POST] @" +request.url()+ " from " +request.ip());
        System.out.println("\trequest content: " + request.body());
        System.out.println("\tresponse content: " + response);
        if (Main.VERBOSE) { Utils.printLine(); }

    }

}
