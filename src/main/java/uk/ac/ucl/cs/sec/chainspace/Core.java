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
    private Cache cache;


    // TODO: load cache depth from config
    private static final int CACHE_DEPTH = 10;


    /**
     * Constructor
     * Runs a node service and init a database.
     */
    Core(int nodeID) throws ClassNotFoundException, SQLException {

        // init cache
        this.cache = new Cache(CACHE_DEPTH);

        // init the database connection
        this.databaseConnector = new DatabaseConnector(nodeID);

    }


    /**
     * close
     * Gently shutdown the core
     */
    void close() throws SQLException {
        this.databaseConnector.close();
    }


    /**
     * debugLoad
     * Debug method to quickly add an object to the node database. It returns the corresponding object ID.
     */
    void debugLoad(String object) throws NoSuchAlgorithmException {

            // add object to the database
            this.databaseConnector.saveObject("", object);

    }


    /**
     * processTransaction
     * This method processes a transaction object, call the checker, and store the outputs in the database if everything
     * goes fine.
     */
    void processTransaction(Transaction transaction, Store store)
            throws AbortTransactionException, SQLException, NoSuchAlgorithmException, IOException
    {

        // check if the transaction is in the cache (has recently been processed)
        if (this.cache.isInCache(transaction.toJson())) { return; }

        // check transaction's integrity
        if (!checkTransactionIntegrity(transaction, store)) {
            throw new AbortTransactionException("Malformed transaction or key-value store.");
        }

        // check input objects are active
        // TODO: optimise database query (one query instead of looping)
        for (int i = 0; i < transaction.getInputIDs().length; i++) {
            if (this.databaseConnector.isObjectInactive(transaction.getInputIDs()[i])) {
                throw new AbortTransactionException("Object " +transaction.getInputIDs()[i]+ " is inactive.");
            }
        }

        // check reference input objects are active
        // TODO: optimise database query (one query instead of looping)
        for (int i = 0; i < transaction.getReferenceInputIDs().length; i++) {
            if (this.databaseConnector.isObjectInactive(transaction.getReferenceInputIDs()[i])) {
                throw new AbortTransactionException("Object " +transaction.getReferenceInputIDs()[i]+ " is inactive.");
            }
        }


        // assemble inputs objects for checker
        String[] inputs = new String[transaction.getInputIDs().length];
        for (int i = 0; i < transaction.getInputIDs().length; i++) {
            inputs[i] = store.getValueFromKey(transaction.getInputIDs()[i]);
        }

        // assemble reference inputs objects for checker
        String[] referenceInputs = new String[transaction.getReferenceInputIDs().length];
        for (int i = 0; i < transaction.getReferenceInputIDs().length; i++) {
            referenceInputs[i] = store.getValueFromKey(transaction.getReferenceInputIDs()[i]);
        }

        // assemble output objects for checker
        String[] outputs = new String[transaction.getOutputIDs().length];
        for (int i = 0; i < transaction.getOutputIDs().length; i++) {
            outputs[i] = store.getValueFromKey(transaction.getOutputIDs()[i]);
        }



        // call the checker
        if (!callChecker(transaction, inputs, referenceInputs, outputs)) {
            throw new AbortTransactionException("The checker declined the transaction.");
        }



        // check if objects are active
        // This is the part where we call BFTSmart
        // TODO: check that all inputs are active.



        // make input (consumed) objects inactive
        // TODO: optimise database query (one query instead of looping)
        for (int i = 0; i < transaction.getInputIDs().length; i++) {
            this.databaseConnector.setObjectInactive(transaction.getInputIDs()[i]);
        }

        // register new objects
        // TODO: optimise database query (one query instead of looping)
        for (String output : outputs) {
            this.databaseConnector.saveObject(Utils.hash(transaction.toJson()), output);
        }

        // update logs
        this.databaseConnector.logTransaction(transaction.toJson());

    }


    /**
     * callChecker
     * This method format a packet and call the checker in order to verify the transaction.
     */
    @SuppressWarnings("unchecked") // these warning are caused by a bug in org.json.simple.JSONArray
    private boolean callChecker(Transaction transaction, String[] inputs, String[] referenceInputs, String[] outputs)
            throws IOException
    {

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
        for (Object output : outputs) {
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


    /**
     * checkTransactionIntegrity
     * Check the transaction's integrity.
     */
    private boolean checkTransactionIntegrity(Transaction transaction, Store store) throws NoSuchAlgorithmException {

        // check transaction's and store's format
        // all fields must be present. For instance, if a transaction has no parameters, and empty field should be sent
        if (store.getArray() == null
            || transaction.getInputIDs() == null
            || transaction.getReferenceInputIDs() == null
            || transaction.getOutputIDs() == null
            || transaction.getParameters() == null )
        {
            return false;
        }


        // check hashed of input objects
        for (String inputID: transaction.getInputIDs()) {
            if (! Utils.verifyHash(store.getValueFromKey(inputID), inputID)) {
                return false;
            }
        }

        // check hashed of reference input objects
        for (String referenceInputID: transaction.getReferenceInputIDs()) {
            if (! Utils.verifyHash(store.getValueFromKey(referenceInputID), referenceInputID)) {
                return false;
            }
        }

        // check hashed of output objects
        for (String outputID: transaction.getOutputIDs()) {
            if (! Utils.verifyHash(store.getValueFromKey(outputID), outputID)) {
                return false;
            }
        }

        // otherwise, return true
        return true;

    }

}
