package uk.ac.ucl.cs.sec.chainspace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by mus on 12/06/17.
 */
public class Transaction {
    private int contractID;
    private String[] inputIDs;
    private String[] referenceInputIDs;
    private String parameters;
    private String[] outputs;

    public Transaction(int contractID, String[] inputIDs, String[] referenceInputIDs, String parameters, String[] outputs) {
        this.contractID = contractID;
        this.referenceInputIDs = referenceInputIDs;
        this.parameters = parameters;
        this.outputs = outputs;
    }

    public static Transaction fromJson(String json) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(json, Transaction.class);
    }

    public String toJson() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }

    public int getContractID() {
        return contractID;
    }

    public String[] getInputIDs() {
        return inputIDs;
    }

    public String[] getReferenceInputIDs() {
        return referenceInputIDs;
    }

    public String getParameters() {
        return parameters;
    }

    public String[] getOutputs() {
        return outputs;
    }

    public String getID() throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(this.toJson().getBytes());
        byte[] hash = digest.digest();
        String hexhash = String.format("%064x", new java.math.BigInteger(1, hash));

        return hexhash;
    }
}
