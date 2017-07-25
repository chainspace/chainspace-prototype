package uk.ac.ucl.cs.sec.chainspace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;

/**
 *
 *
 */
class Transaction {
    private String   contractID;
    private String[] inputIDs;
    private String[] referenceInputIDs;
    private String[] parameters;
    private String[] returns;
    private String[] outputs;
    private String[] dependencies; // private Transaction[] dependencies;
    private String   methodID;

    /**
     * constructor
     */
    Transaction(
            String   contractID,
            String[] inputIDs,
            String[] referenceInputIDs,
            String[] parameters,
            String[] returns,
            String[] outputs,
            String[] dependencies
    ) {
        this.contractID         = contractID;
        this.inputIDs           = inputIDs;
        this.referenceInputIDs  = referenceInputIDs;
        this.parameters         = parameters;
        this.returns            = returns;
        this.outputs            = outputs;
        this.dependencies       = dependencies;
        this.methodID           = methodID;
    }


    /**
     * fromJson
     * Returns a transaction object from a json string representing it
     */
    static Transaction fromJson(JSONObject json) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(json.toString(), Transaction.class);
    }

    /**
     * toJson
     * Returns a json string representing the transaction
     */
    String toJson() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }

    /**
     * getID
     * Get the transaction's ID.
     */
    String getID() throws NoSuchAlgorithmException {
        return Utils.hash(this.toJson());
    }


    /*
        getters
     */

    String getContractID() {
        return contractID;
    }

    String[] getInputIDs() {
        return inputIDs;
    }

    String[] getReferenceInputIDs() {
        return referenceInputIDs;
    }

    String[] getParameters() {
        return parameters;
    }

    String[] getReturns() {
        return returns;
    }

    String[] getOutputs() {
        return outputs;
    }

    String[] getDependencies() {
        return dependencies;
    }

    String getMethodID() {
        return methodID;
    }
}
