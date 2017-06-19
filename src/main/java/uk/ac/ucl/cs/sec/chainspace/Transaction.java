package uk.ac.ucl.cs.sec.chainspace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;


class Transaction {
    private int contractID;
    private String[] inputIDs;
    private String[] referenceInputIDs;
    private String   parameters;
    private String[] outputIDs;

    Transaction(int contractID, String[] inputIDs, String[] referenceInputIDs, String parameters, String[] outputIDs) {
        this.contractID = contractID;
        this.inputIDs = inputIDs;
        this.referenceInputIDs = referenceInputIDs;
        this.parameters = parameters;
        this.outputIDs = outputIDs;
    }

    static Transaction fromJson(JSONObject json) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(json.toString(), Transaction.class);
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

    String[] getOutputIDs() {
        return outputIDs;
    }

    String getID() throws NoSuchAlgorithmException {
        return Utils.hash(this.toJson());
    }
}
