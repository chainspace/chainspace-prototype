package uk.ac.ucl.cs.sec.chainspace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

import static spark.Spark.*;

import static spark.route.HttpMethod.post;

/**
 * Created by mus on 12/06/17.
 */
public class Service {
    private Gson gson;
    private Connection connection;
    private int nodeID;

    public Service(int nodeID) throws ClassNotFoundException, SQLException {
        this.nodeID = nodeID;

        this.gson = new GsonBuilder().create();

        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection("jdbc:sqlite:node" + nodeID + ".sqlite");
        initialiseDatabase();

        addRoutes();
    }

    private void initialiseDatabase() throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS data (" +
                "object_id CHAR(32) NOT NULL UNIQUE," +
                "object TEXT NOT NULL," +
                "status INTEGER NOT NULL" +
                ")";
        statement.executeUpdate(sql);
    }

    private void addRoutes() {
        get("/api/1.0/node_id", (request, response) -> {
            JSONObject json = new JSONObject();
            json.put("node_id", nodeID);
            return json.toString();
        });
        post("/api/1.0/process_transaction", (request, response) -> processTransactionRequest(request, response));
    }

    private String processTransactionRequest(Request request, Response response) throws NoSuchAlgorithmException, SQLException, AbortTransactionException, IOException {
        Transaction transaction = Transaction.fromJson(request.body());
        processTransaction(transaction);
        return null;
    }

    private void processTransaction(Transaction transaction) throws AbortTransactionException, SQLException, NoSuchAlgorithmException, IOException {
        // check transaction's integrity
        if (transaction.getInputIDs() == null
                || transaction.getReferenceInputIDs() == null
                || transaction.getOutputs() == null
                || transaction.getParameters() == null) {
            throw new AbortTransactionException("Malformed transaction.");
        }

        // get input objects
        String[] inputs = new String[transaction.getInputIDs().length];
        for (int i = 0; i < transaction.getInputIDs().length; i++) {
            inputs[i] = getObject(transaction.getInputIDs()[i]);
        }

        // get reference input objects
        String[] referenceInputs = new String[transaction.getReferenceInputIDs().length];
        for (int i = 0; i < transaction.getReferenceInputIDs().length; i++) {
            referenceInputs[i] = getObject(transaction.getReferenceInputIDs()[i]);
        }

        // call the checker
        if (!callChecker(transaction, inputs, referenceInputs)) {
            throw new AbortTransactionException("The checker declined the transaction.");
        }

        // check if objects are active
        //if (!(areObjectActive(transaction.getInputIDs()) && areObjectActive(transaction.getReferenceInputIDs())) ) {
        //    throw new AbortTransactionException("Input not active.");
        //}


        // make input object inactive
        for (int i = 0; i < transaction.getInputIDs().length; i++) {
            String sql = "UPDATE data SET status = 0 WHERE object_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, transaction.getInputIDs()[i]);
            statement.executeUpdate();
            connection.commit();
        }

        // register new objects
        for (int i = 0; i < transaction.getOutputs().length; i++) {
            registerObject(transaction.getOutputs()[i]);
        }
    }

    private String getObject(String objectID) throws SQLException, AbortTransactionException {
        String sql = "SELECT * FROM data WHERE object_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, objectID);
        ResultSet resultSet = statement.executeQuery();

        // check if the object is in the database.
        if (!resultSet.isBeforeFirst()) {
            // if it's not, ask other shards
            throw new AbortTransactionException("Object doesn't exist.");
        }
        else {
            return resultSet.getString("object");
        }
    }
    private boolean callChecker(Transaction transaction, String[] inputs, String[] referenceInputs) throws IOException {
        // create transaction in JSON for checker
        JSONObject transactionForChecker = new JSONObject();
        // contract method
        transactionForChecker.put("contractMethod", "http://127.0.0.1:5001/bank/transfer");
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


        // make post request
        String url                  = "http://127.0.0.1:5001/bank/transfer";
        HttpClient httpClient       = HttpClientBuilder.create().build();
        StringEntity postingString  = new StringEntity(transactionForChecker.toString());
        HttpPost post               = new HttpPost(url);
        post.setEntity(postingString);
        post.setHeader("Content-type", "application/json");

        // get response
        HttpResponse response   = httpClient.execute(post);
        String responseString   = new BasicResponseHandler().handleResponse(response);
        JSONObject responseJson = new JSONObject(responseString);

        // return
        return responseJson.getString("status").equals("OK");
    }

    private void registerObject(String object) throws SQLException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(object.getBytes());
        byte[] hash = digest.digest();
        String hexhash = String.format("%064x", new java.math.BigInteger(1, hash));

        String sql = "INSERT INTO data (object_id, object, status) VALUES (?, ?, 1)";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, hexhash);
        statement.setString(2, object);
        statement.executeUpdate();
        connection.commit();
    }
}
