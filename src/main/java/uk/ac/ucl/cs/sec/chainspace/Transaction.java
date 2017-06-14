package uk.ac.ucl.cs.sec.chainspace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.security.NoSuchAlgorithmException;


class Transaction {
    private int contractID;
    private String[] inputIDs;
    private String[] referenceInputIDs;
    private String   parameters;
    private String[] outputs;

    Transaction(int contractID, String[] inputIDs, String[] referenceInputIDs, String parameters, String[] outputs) {
        this.contractID = contractID;
        this.inputIDs = inputIDs;
        this.referenceInputIDs = referenceInputIDs;
        this.parameters = parameters;
        this.outputs = outputs;
    }

    static Transaction fromJson(String json) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(json, Transaction.class);
    }

    String toJson() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }

    int getContractID() {
        return contractID;
    }

    String[] getInputIDs() {
        return inputIDs;
    }

    String[] getReferenceInputIDs() {
        return referenceInputIDs;
    }

    String getParameters() {
        return parameters;
    }

    String[] getOutputs() {
        return outputs;
    }

    String getID() throws NoSuchAlgorithmException {
        return Utils.hash(this.toJson());
    }
}
