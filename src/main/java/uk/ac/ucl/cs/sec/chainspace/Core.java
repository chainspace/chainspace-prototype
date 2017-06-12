package uk.ac.ucl.cs.sec.chainspace;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.json.simple.JSONArray;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;


/**
 *
 *
 */
class Core {

    // instance variables
    private DatabaseConnector databaseConnector;

    /**
     * Constructor
     * Runs a node service and init a database.
     */
    Core(int nodeID) throws ClassNotFoundException, SQLException {

        // init the database connection
        this.databaseConnector = new DatabaseConnector(nodeID);

    }


    /**
     * debugLoad
     * Debug method to quickly add an object to the node database. It returns the corresponding object ID.
     */
    void debugLoad(String object) throws NoSuchAlgorithmException {

            // add object to the datbase
            databaseConnector.saveObject(object);

    }


    /**
     * processTransaction
     * This method processes a transaction object, call the checker, and store the outputs in the database if everything
     * goes fine.
     */
    void processTransaction(Transaction transaction)
            throws AbortTransactionException, SQLException, NoSuchAlgorithmException, IOException
    {

        // check transaction's integrity
        // all fields must be present. For instance, if a transaction has no parameters, and empty field should be sent
        if (transaction.getInputIDs() == null
                || transaction.getReferenceInputIDs() == null
                || transaction.getOutputs() == null
                || transaction.getParameters() == null) {
            throw new AbortTransactionException("Malformed transaction.");
        }

        // get input objects
        // TODO: optimise database query (one query instead of looping)
        String[] inputs = new String[transaction.getInputIDs().length];
        for (int i = 0; i < transaction.getInputIDs().length; i++) {
            inputs[i] = databaseConnector.getObject(transaction.getInputIDs()[i]);
            if (inputs[i] == null) {
                // TODO: if the current node does not hold the object, ask other nodes for it.
                throw new AbortTransactionException("Object doesn't exist.");
            }
        }

        // get reference input objects
        // TODO: optimise database query (one query instead of looping)
        String[] referenceInputs = new String[transaction.getReferenceInputIDs().length];
        for (int i = 0; i < transaction.getReferenceInputIDs().length; i++) {
            referenceInputs[i] = databaseConnector.getObject(transaction.getReferenceInputIDs()[i]);
            if (referenceInputs[i] == null) {
                // TODO: if the current node does not hold the object, ask other nodes for it.
                throw new AbortTransactionException("Object doesn't exist.");
            }
        }

        // call the checker
        if (!callChecker(transaction, inputs, referenceInputs)) {
            throw new AbortTransactionException("The checker declined the transaction.");
        }


        // check if objects are active
        // TODO: check that all inputs are active.


        // make input (consumed) objects inactive
        // TODO: optimise database query (one query instead of looping)
        for (int i = 0; i < transaction.getInputIDs().length; i++) {
            databaseConnector.setObjectInactive(transaction.getInputIDs()[i]);
        }

        // register new objects
        for (int i = 0; i < transaction.getOutputs().length; i++) {
            databaseConnector.saveObject(transaction.getOutputs()[i]);
        }

    }


    /**
     * callChecker
     * This method format a packet and call the checker in order to verify the transaction.
     */
    private boolean callChecker(Transaction transaction, String[] inputs, String[] referenceInputs) throws IOException {

        // get checker URL
        // TODO: at the moment the checker URL is hardcoded, this should be loaded from a config file
        String checkerURL = "http://127.0.0.1:5001/bank/transfer";

        // create transaction in JSON for checker
        JSONObject transactionForChecker = new JSONObject();
        // contract method
        transactionForChecker.put("contractID", transaction.getContractID());
        // parameters
        transactionForChecker.put("parameters", new JSONObject(transaction.getParameters()));

        // inputs
        JSONArray inputsForChecker = new JSONArray();
        for (String input : inputs) {
            inputsForChecker.add(new JSONObject(input));
        }
        transactionForChecker.put("inputs", inputsForChecker);

        // reference inputs
        JSONArray referenceInputsForChecker = new JSONArray();
        for (String referenceInput : referenceInputs) {
            referenceInputsForChecker.add(new JSONObject(referenceInput));
        }
        transactionForChecker.put("referenceInputs", referenceInputsForChecker);

        // outputs
        JSONArray outputsForChecker = new JSONArray();
        for (Object output : transaction.getOutputs()) {
            outputsForChecker.add(new JSONObject(output.toString()));
        }
        transactionForChecker.put("outputs", outputsForChecker);

        // make post request
        HttpClient httpClient = HttpClientBuilder.create().build();
        StringEntity postingString = new StringEntity(transactionForChecker.toString());
        HttpPost post = new HttpPost(checkerURL);
        post.setEntity(postingString);
        post.setHeader("Content-type", "application/json");

        // get response
        HttpResponse response   = httpClient.execute(post);
        String responseString   = new BasicResponseHandler().handleResponse(response);
        JSONObject responseJson = new JSONObject(responseString);

        // return
        return responseJson.getString("status").equals("OK");

    }

}
