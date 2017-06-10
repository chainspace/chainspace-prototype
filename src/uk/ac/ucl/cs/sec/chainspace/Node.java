package uk.ac.ucl.cs.sec.chainspace;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;

import org.json.simple.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

import java.sql.*;


/**
 *
 */
public class Node
{
    // instance variables
    private Connection  connection;
    private Statement   stmt;
    private Gson        gson;

    /**
     * Constructor.
     *
     * @throws SQLException SQL exception.
     * @throws ClassNotFoundException ClassNotFound exception.
     */
    public Node(int shardID) throws SQLException, ClassNotFoundException {
        // init json converter
        gson = new GsonBuilder().create();

        // load driver and connect to the database
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:shard" +shardID+ ".db");
        stmt = connection.createStatement();

        // create table for data storage
        String sql = "CREATE TABLE IF NOT EXISTS DATA (" +
            "OBJ_ID   CHAR(64)   NOT NULL UNIQUE,"       +
            "OBJ      TEXT       NOT NULL,"              +
            "STATUS   INTEGER    NOT NULL"               +
            ")";
        stmt.executeUpdate(sql);
    }


    /**
     * Gently shutdown the node.
     *
     * @throws SQLException SQL exception.
     */
    public void shutdown() throws SQLException {
        stmt.close(); connection.close();
    }


    /**
     * Add a new object to the database.
     *
     * @param obj the object to add to the database.
     * @throws SQLException SQL exception.
     */
    public void registerObject(String obj) throws SQLException {
        String sql = "INSERT INTO DATA (OBJ_ID,OBJ,STATUS) " +
                "VALUES (" +obj.hashCode()+" , '" + obj + "', 1 );";
        stmt.executeUpdate(sql);
    }


    /**
     * Apply a chanspace transaction.
     *
     * @param transactionJson A JSON representation of a chainspace transaction
     * @return the transaction's ID
     * @throws SQLException SQL exception.
     * @throws IOException IO exception
     * @throws ChainspaceException chainspace exception.
     */
    public int applyTransaction(String transactionJson) throws SQLException, IOException, ChainspaceException {

        // check transaction's integrity
        Transaction transaction = gson.fromJson(transactionJson, Transaction.class);

        /*
            get input objects
         */
        String inputs [] = new String[transaction.getInputsID().length];
        for (int i = 0; i < transaction.getInputsID().length; i++) {
            String sql = "SELECT * FROM DATA WHERE OBJ_ID=" +transaction.getInputsID()[i];
            ResultSet rs = stmt.executeQuery(sql);

            if (!rs.isBeforeFirst() ) {
                throw new ChainspaceException("Input " +transaction.getInputsID()[i]+ " not in the database.");
            }

            // check objects are active:
            /*
            if (rs.getInt("STATUS") != 1) {
                throw new ChainspaceException("Input: " +transaction.getInputsID()[i]+ "not active.");
            }
            */

            // verify that object is actif
            if (! isObjectActive(transaction.getInputsID()[i])) {
                throw new ChainspaceException("Input " +transaction.getInputsID()[i]+ " not active.");
            }

            // set inputs
            inputs[i] = rs.getString("OBJ");
        }

        /*
            get reference input objects
         */
        String referenceInputs [] = new String[transaction.getReferenceInputsID().length];
        for (int i = 0; i < transaction.getReferenceInputsID().length; i++) {
            String sql = "SELECT * FROM DATA WHERE OBJ_ID=" +transaction.getReferenceInputsID()[i];
            ResultSet rs = stmt.executeQuery(sql);

            if (!rs.isBeforeFirst() ) {
                throw new ChainspaceException("Input " +transaction.getReferenceInputsID()[i]+ " not in the database.");
            }

            // check objects are active:
            /*
            if (rs.getInt("STATUS") != 1) {
                throw new ChainspaceException("Input: " +transaction.getReferenceInputsID()[i]+ "not active.");
            }
            */

            // verify that object is actif
            if (! isObjectActive(transaction.getReferenceInputsID()[i])) {
                throw new ChainspaceException("Input " +transaction.getReferenceInputsID()[i]+ " not active.");
            }

            // set reference inputs
            referenceInputs[i] = rs.getString("OBJ");
        }

        // call the checker
        if (! callChecker(transaction, inputs, referenceInputs)) {
            throw new ChainspaceException("The checker declined the transaction.");
        }

        // make input object inactive
        for (int i = 0; i < transaction.getInputsID().length; i++) {
            String sql = "UPDATE DATA SET STATUS=0 WHERE OBJ_ID=" + transaction.getInputsID()[i];
            stmt.executeUpdate(sql);
        }

        // register new objects
        for (int i = 0; i < transaction.getOutputs().length; i++) {
            registerObject(transaction.getOutputs()[i]);
        }

        // return
        return transaction.hashCode();

    }


    /**
     * call the checker.
     *
     * @param transaction the transaction to check.
     * @param inputs the input objects.
     * @param referenceInputs the reference input objects.
     * @return whether the transaction has been approved.
     * @throws IOException IO exception.
     */
    private boolean callChecker(Transaction transaction, String[] inputs, String[] referenceInputs) throws IOException {

        /*
            create transaction in JSON for checker
         */
        JSONObject transactionForChecker = new JSONObject();
        // contract method
        transactionForChecker.put("contractMethod", transaction.getContractMethod());
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
        for (String output : transaction.getOutputs()) {
            outputsForChecker.add(new JSONObject(output));
        }
        transactionForChecker.put("outputs", outputsForChecker);


        /*
            make post request
         */
        String url                  = transaction.getContractMethod();
        HttpClient httpClient       = HttpClientBuilder.create().build();
        StringEntity postingString  = new StringEntity(transactionForChecker.toString());
        HttpPost post               = new HttpPost(url);
        post.setEntity(postingString);
        post.setHeader("Content-type", "application/json");


        /*
            get response
         */
        HttpResponse response   = httpClient.execute(post);
        String responseString   = new BasicResponseHandler().handleResponse(response);
        JSONObject responseJson = new JSONObject(responseString);

        // return
        if (responseJson.getString("status").equals("OK")) { return true;}
        else { return false; }
    }


    /**
     * TODO BFT
     * @param objectID  the object ID to lock.
     * @return whether the object has been locked and the transaction can proceed.
     */
    private boolean isObjectActive (int objectID) {

        return true;

    }
}