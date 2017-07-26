package uk.ac.ucl.cs.sec.chainspace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

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
    private Transaction[] dependencies; // private Transaction[] dependencies;
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
            Transaction[] dependencies,
            String   methodID
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

    Transaction[] getDependencies() {
        return dependencies;
    }

    String getMethodID() { return methodID; }


    /*
    @Override
    public String toString() {
        return "Transaction{" +
                "contractID='" + contractID + '\'' +
                ", inputIDs=" + Arrays.toString(inputIDs) +
                ", referenceInputIDs=" + Arrays.toString(referenceInputIDs) +
                ", parameters=" + Arrays.toString(parameters) +
                ", returns=" + Arrays.toString(returns) +
                ", outputs=" + Arrays.toString(outputs) +
                ", methodID='" + methodID + '\'' +
                '}';
    }
    */

    /**
     * toStringArray
     * Convert an array of transactions into an array of strings. Only first-level dependencies are included.
     * @param transactionArray Transaction[]
     * @return String[]
     */
    static String[] toStringArray(Transaction[] transactionArray) {

        // TODO: fix this
        return new String[]{};

        /*
        // check empty arrays
        if ( transactionArray == null || transactionArray.length == 0) { return new String[]{}; }

        // convert transactions into strings
        String[] stringArray = new String[transactionArray.length];
        for (int i = 0; i < transactionArray.length; i++) {
            stringArray[i] = transactionArray[i].toJson();
        }

        return stringArray;
        */

    }
}
