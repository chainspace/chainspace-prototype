package uk.ac.ucl.cs.sec.chainspace;

import java.util.Arrays;


/**
 * Class representing a Chainspace transaction.
 * This class is here to facilitate testing, but node could deal directly with a JSON transaction representation and
 * therefore we could suppress this class.
 */
public class Transaction {

    // instance variables
    private String    contractMethod;
    private int[]     inputsID;
    private int[]     referenceInputsID;
    private String    parameters;
    private String[]  outputs;

    /**
     * Constructor.
     *
     * @param contractMethod the contract method
     * @param inputsID the input objects ID.
     * @param referenceInputs the reference inputs ID.
     * @param parameters the transaction's parameters.
     * @param outputs the output objects.
     */
    public Transaction(String contractMethod, int[] inputsID, int[] referenceInputs, String parameters, String[] outputs) {
        this.contractMethod     = contractMethod;
        this.inputsID           = inputsID;
        this.referenceInputsID  = referenceInputs;
        this.parameters         = parameters;
        this.outputs            = outputs;
    }

    public String getContractMethod() {
        return contractMethod;
    }

    public int[] getInputsID() {
        return inputsID;
    }

    public int[] getReferenceInputsID() {
        return referenceInputsID;
    }

    public String getParameters() {
        return parameters;
    }

    public String[] getOutputs() {
        return outputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;

        Transaction that = (Transaction) o;

        if (!getContractMethod().equals(that.getContractMethod())) return false;
        if (!Arrays.equals(getInputsID(), that.getInputsID())) return false;
        if (!Arrays.equals(getReferenceInputsID(), that.getReferenceInputsID())) return false;
        if (!getParameters().equals(that.getParameters())) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(getOutputs(), that.getOutputs());
    }

    @Override
    public int hashCode() {
        int result = getContractMethod().hashCode();
        result = 31 * result + Arrays.hashCode(getInputsID());
        result = 31 * result + Arrays.hashCode(getReferenceInputsID());
        result = 31 * result + getParameters().hashCode();
        result = 31 * result + Arrays.hashCode(getOutputs());
        return result;
    }
}
