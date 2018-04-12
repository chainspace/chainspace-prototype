package uk.ac.ucl.cs.sec.chainspace;

import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Service;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static uk.ac.ucl.cs.sec.chainspace.bft.ResponseType.ACCEPTED_T_COMMIT;


/**
 *
 *
 */
class ClientService {

    private final int port;
    private final String externalHostName;
    private final String databaseName;

    static String HOST_IP_SYSTEM_PROPERTY_NAME = "client.api.hostip";

    /**
     * Constructor
     * Runs a node service and init a database.
     */
    ClientService(int port) throws SQLException, ClassNotFoundException, UnknownHostException {


        // start service on given port
        addRoutes(Service.ignite().port(port));

        externalHostName = discoverExternalIPAddrress();
        this.port = port;

        this.databaseName = dbNameFromSystemProperty(HOST_IP_SYSTEM_PROPERTY_NAME);

        printInitMessage();


    }

    static String discoverExternalIPAddrress()  {
        String systemPropertyValue = System.getProperty(HOST_IP_SYSTEM_PROPERTY_NAME);
        try {
            if (systemPropertyValue != null) {
                return systemPropertyValue;
            }

            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println("Could not discover externalHostName, defaulting to 'localhost' - Unable to resolve  host [" + e.getMessage() + "]");
            return "localhost";
        }
    }


    private static String dbNameFromSystemProperty(String systemProperty) {
        String databaseName = System.getProperty(systemProperty);
        if (databaseName == null) {
            throw new RuntimeException("Could not read system property -Dclient.api.database - you need to specify this when running the service");
        }
        File databaseFile = new File(databaseName +".sqlite");
        if (!databaseFile.exists()) {
            throw new RuntimeException("-Dclient.api.database [" + databaseFile.getAbsolutePath() + "] does not exist!");
        }
        return databaseName;
    }

    private Connection initDbConnection() throws ClassNotFoundException, SQLException {

        return SQLiteConnector.openConnection(databaseName);
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
        service.path("/api", () -> service.path("/" + Main.VERSION, () -> {

            // debug
            service.get("/", this::getApiIndex);

            // process a transaction
            service.post("/transaction/process", this::processTransactionRequest);
            service.post("/transaction/dump", this::dumpTransactionRequest);

            service.get("/transactions", this::getTransactions);
            service.get("/objects", this::getObjects);
            service.get("/hashchain", this::getHashChain);

        }));

    }

    private String getApiIndex(Request request, Response response) {

        JSONObject responseJson = new JSONObject();
        responseJson.put("service-name", "Chainspace Client API");
        responseJson.put("version", Main.VERSION);



        JSONObject endpoints = new JSONObject();
        endpoints.put("index", getExternalUrl("/"));
        endpoints.put("transactions", getExternalUrl("/transactions"));
        endpoints.put("objects", getExternalUrl("/objects"));
        endpoints.put("hashchain", getExternalUrl("/hashchain"));
        endpoints.put("process-transaction", getExternalUrl("/transaction/process"));


        responseJson.put("endpoints", endpoints);

        response.type("application/json");
        response.status(200);
        return responseJson.toString();
    }


    private String getTransactions(Request request, Response response) {
        response.type("application/json");

        try (Connection conn = initDbConnection()) {

            TransactionQuery query = new TransactionQuery(conn);

            List<TransactionQuery.TransactionLogEntry> txs = query.retrieveTransactionLogEntries();

            JSONArray responseJson = new JSONArray();

            for (TransactionQuery.TransactionLogEntry entry : txs) {
                responseJson.put(entry.asMap());
            }

            response.status(200);
            return responseJson.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return createHttpResponse(response, "Internal Error: " + e.getMessage(),
                    "false", 500, "error").toString();
        }
    }

    private String getObjects(Request request, Response response) throws SQLException {
        response.type("application/json");
        try (Connection conn = initDbConnection()) {

            TransactionQuery query = new TransactionQuery(conn);

            List<TransactionQuery.ChainspaceObject> csObjects = query.retrieveObjects();

            JSONArray responseJson = new JSONArray();

            for (TransactionQuery.ChainspaceObject csObject : csObjects) {
                responseJson.put(csObject.asMap());
            }

            response.status(200);
            return responseJson.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return createHttpResponse(response, "Internal Error: " + e.getMessage(),
                    "false", 500, "error").toString();
        }
    }

    /**
     * Refer to https://arxiv.org/pdf/1708.03778.pdf section on Node Hash Chains
     */
    private String getHashChain(Request request, Response response) {
        response.type("application/json");
        try (Connection conn = initDbConnection())  {

            TransactionQuery query = new TransactionQuery(conn);

            List<TransactionQuery.TransactionDigest> transactionDigests = query.retrieveDigests();

            JSONArray responseJson = new JSONArray();

            for (TransactionQuery.TransactionDigest digest : transactionDigests) {
                responseJson.put(digest.asMap());
            }

            response.status(200);
            return responseJson.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return createHttpResponse(response, "Internal Error: " + e.getMessage(),
                    "false", 500, "error").toString();
        }
    }


    /**
     * processTransactionRequest
     * This method receives a json transaction, processes it, and responds with the transaction ID.
     */
    private String processTransactionRequest(Request request, Response response) {

        // verbose print
        if (Main.VERBOSE) {
            Utils.printHeader("Incoming transaction");
        }

        // process the transaction & create response
        JSONObject responseJson;
        try {

            // submit the transaction
            String result = Client.submitTransaction(request.body());
            System.out.println("Result from Client.submitTransaction is " + result);

            if (ACCEPTED_T_COMMIT.equals(result)) {
                responseJson = createHttpResponse(response, result, "True", 200, "outcome");
            } else {
                responseJson = createHttpResponse(response, result, "False", 502, "outcome"); // bad gateway
            }


        } catch (Exception e) {

            responseJson = createHttpResponse(response, e.getMessage(), "False", 400, "message");

            // verbose print
            if (Main.VERBOSE) {
                Utils.printStacktrace(e);
            }

        }

        // print request
        printRequestDetails(request, responseJson.toString());

        // send
        response.type("application/json");
        return responseJson.toString();

    }

    private static JSONObject createHttpResponse(Response response, String result, String successResponse, int statusCode, String outcome) {
        JSONObject responseJson = new JSONObject();

        responseJson.put("success", successResponse);
        responseJson.put(outcome, result);

        response.status(statusCode);
        return responseJson;
    }


    /**
     * processTransactionRequest
     * This method receives a json transaction, processes it, and responds with the transaction ID.
     */
    private String dumpTransactionRequest(Request request, Response response) {

        if (Main.VERBOSE) {
            Utils.printHeader("Incoming transaction");
        }

        // process the transaction & create response
        JSONObject responseJson = new JSONObject();
        try {

            Client.submitTransactionNoWait(request.body());

            createHttpResponse(response, "", "True", 200, "");


        } catch (Exception e) {

            createHttpResponse(response, e.getMessage(), "False", 400, "message");

            // verbose print
            if (Main.VERBOSE) {
                Utils.printStacktrace(e);
            }

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
    private void printInitMessage() {

        System.out.println("\nChainspace Client API service is running @ " + getExternalUrl("/"));
        System.out.printf("\nReading from local database @ " + databaseName);
    }

    private String getExternalUrl(String path) {
        return String.format("http://%s:%d/api/%s%s", externalHostName, port, Main.VERSION, path);
    }


    /**
     * printRequestDetails
     * Print on the console some details about the incoming request.
     */
    private void printRequestDetails(Request request, String response) {

        // print request summary
        System.out.println("\nNode service [POST] @" + request.url() + " from " + request.ip());
        System.out.println("\trequest content: " + request.body());
        System.out.println("\tresponse content: " + response);
        if (Main.VERBOSE) {
            Utils.printLine();
        }

    }

}
