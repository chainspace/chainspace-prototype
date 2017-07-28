package uk.ac.ucl.cs.sec.chainspace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 *
 *
 */
public class CSTransaction implements Serializable {

    // instance variables
    private String contractID;
    private String[] inputIDs;
    private String[] referenceInputIDs;
    private String[] parameters;
    private String[] returns;
    private String[] outputs;
    private CSTransaction[] dependencies;
    private String methodID;

    /**
     * constructor
     */
    public CSTransaction(
        String contractID,
        String[] inputIDs,
        String[] referenceInputIDs,
        String[] parameters,
        String[] returns,
        String[] outputs,
        CSTransaction[] dependencies,
        String methodID
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
    public static CSTransaction fromJson(JSONObject json) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(json.toString(), CSTransaction.class);
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
    public String getID() throws NoSuchAlgorithmException {
        return Utils.hash(this.toJson());
    }


    /*
        getters
     */

    public String getContractID() {
        return contractID;
    }

    public String[] getInputIDs() {
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

    public String[] getOutputs() {
        return outputs;
    }

    CSTransaction[] getDependencies() {
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
    static String[] toStringArray(CSTransaction[] transactionArray) {

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
