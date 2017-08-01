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
class ClientService {

    /**
     * Constructor
     * Runs a node service and init a database.
     */
    ClientService(int port) throws SQLException, ClassNotFoundException {

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
            service.post("/transaction/dump", this::dumpTransactionRequest);

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

            // submit the transaction
            String result = Client.submitTransaction(request.body());

            // create json response
            responseJson.put("success", "True");
            responseJson.put("outcome", result);
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
     * processTransactionRequest
     * This method receives a json transaction, processes it, and responds with the transaction ID.
     */
    private String dumpTransactionRequest(Request request, Response response) {

        // verbose print
        if (Main.VERBOSE) { Utils.printHeader("Incoming transaction"); }

        // process the transaction & create response
        JSONObject responseJson = new JSONObject();
        try {

            // submit the transaction
            Client.submitTransactionNoWait(request.body());

            // create json response
            responseJson.put("success", "True");
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
