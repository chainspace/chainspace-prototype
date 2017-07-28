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
public class TransactionForChecker {
    private String   contractID;
    private String[] inputs;
    private String[] referenceInputs;
    private String[] parameters;
    private String[] returns;
    private String[] outputs;
    private String[] dependencies;
    private String   methodID;

    /**
     * constructor
     */
    TransactionForChecker(
            String   contractID,
            String[] inputs,
            String[] referenceInputs,
            String[] parameters,
            String[] returns,
            String[] outputs,
            String[] dependencies,
            String   methodID
    ) {
        this.contractID       = contractID;
        this.inputs           = inputs;
        this.referenceInputs  = referenceInputs;
        this.parameters       = parameters;
        this.returns          = returns;
        this.outputs          = outputs;
        this.dependencies     = dependencies;
        this.methodID         = methodID;
    }


    /**
     * fromJson
     * Returns a transaction object from a json string representing it
     */
    static TransactionForChecker fromJson(JSONObject json) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(json.toString(), TransactionForChecker.class);
    }

    /**
     * toJson
     * Returns a json string representing the transaction
     */
    String toJson() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }


    /*
        getters
     */
    String getContractID() {
        return contractID;
    }
    public String[] getInputs() { return inputs;}
    public String[] getOutputs() { return outputs;}
    String getMethodID() {
        return methodID;
    }

    @Override
    public String toString() {
        return "TransactionForChecker{" +
                "contractID='" + contractID + '\'' +
                ", inputs=" + Arrays.toString(inputs) +
                ", referenceInputs=" + Arrays.toString(referenceInputs) +
                ", parameters=" + Arrays.toString(parameters) +
                ", returns=" + Arrays.toString(returns) +
                ", outputs=" + Arrays.toString(outputs) +
                ", dependencies=" + Arrays.toString(dependencies) +
                ", methodID='" + methodID + '\'' +
                '}';
    }
}
