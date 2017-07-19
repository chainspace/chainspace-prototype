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
    private int      contractID;
    private String[] inputIDs;
    private String[] referenceInputIDs;
    private String[] parameters;
    private String[] returns;
    private String[] outputIDs;
    private String[] dependencies;

    /**
     * constructor
     */
    Transaction(
            int      contractID,
            String[] inputIDs,
            String[] referenceInputIDs,
            String[] parameters,
            String[] returns,
            String[] outputIDs,
            String[] dependencies
    ) {
        this.contractID         = contractID;
        this.inputIDs           = inputIDs;
        this.referenceInputIDs  = referenceInputIDs;
        this.parameters         = parameters;
        this.returns            = returns;
        this.outputIDs          = outputIDs;
        this.dependencies       = dependencies;
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

    int getContractID() {
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

    String[] getOutputIDs() {
        return outputIDs;
    }

    String[] getDependencies() {
        return dependencies;
    }
}
