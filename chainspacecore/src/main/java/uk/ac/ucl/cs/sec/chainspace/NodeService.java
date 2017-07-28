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
    private Core core;


    /**
     * Constructor
     * Runs a node service and init a database.
     */
    NodeService(int port) throws SQLException, ClassNotFoundException {

        // run one core
        this.core = new Core();

        // start service on given port
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
        service.path("/api", () -> service.path("/"+Main.VERSION, () -> {

            // debug
            service.get("/", (request, response) -> "Hello, world!");

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

        // process the transaction & create response
        JSONObject responseJson = new JSONObject();
        try {

            // pass transaction to the core
            //String[] out = this.core.processTransaction(request.body());
            String[] out = new String[]{};

            // create json response
            responseJson.put("success", "True");
            responseJson.put("new objects", out);
            response.status(200);

        }
        catch (Exception e) {

            // create json  error response
            responseJson.put("success", "False");
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
     * printInitMessage
     * Print on the console an init message.
     */
    private void printInitMessage(int port) {

        // print node info
        System.out.println("\nNode service is running on port " +port);

    }


    /**
     * printRequestDetails
     * Print on the console some details about the incoming request.
     */
    private void printRequestDetails(Request request, String response) {

        // print request summary
        System.out.println("\nNode service [POST] @" +request.url()+ " from " +request.ip());
        System.out.println("\trequest content: " + request.body());
        System.out.println("\tresponse content: " + response);
        if (Main.VERBOSE) { Utils.printLine(); }

    }

}
