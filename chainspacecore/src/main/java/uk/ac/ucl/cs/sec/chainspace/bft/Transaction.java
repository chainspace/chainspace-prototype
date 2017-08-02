package uk.ac.ucl.cs.sec.chainspace.bft;

/**
 * Created by sheharbano on 11/07/2017.
 */

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

import org.json.JSONObject;
import uk.ac.ucl.cs.sec.chainspace.*;

public class Transaction implements Serializable {
    public String id;
    public List<String> inputs;
    public List<String> outputs;

    // Transaction states
    public static final String VALID = "valid";
    public static final String INVALID_NOOBJECT = "Invalid: Input object(s) doesn't exist.";
    public static final String INVALID_NOMANAGEDOBJECT = "Invalid: None of the input object(s) is managed by this shard.";
    public static final String REJECTED_LOCKEDOBJECT = "Rejected: Input object(s) is locked. ";
    public static final String INVALID_INACTIVEOBJECT = "Invalid: Input object(s) is inactive.";
    public static final String INVALID_BADTRANSACTION = "Invalid: Malformed transaction.";

    public Transaction() {
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();
    }



    public void addID(String id) {
        this.id = id;
    }

    public void addInput(String in) {
        inputs.add(in);
    }

    public void addOutput(String in) {
        outputs.add(in);
    }

    public void print() {
        System.out.println("Inputs:");
        for (String s : inputs) {
            System.out.println(s);
        }
        System.out.println("Outputs:");
        for (String s : outputs) {
            System.out.println(s);
        }
    }

    public static byte[] toByteArray(Transaction t) {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        try {
            ObjectOutputStream os = new ObjectOutputStream(bs);
            os.writeObject(t);
            os.close();
            byte[] data = bs.toByteArray();
            return data;
        }
        catch (IOException ioe) {
            System.out.println("Exception: " + ioe.getMessage());
            return null;
        }
    }

    public static Transaction fromByteArray(byte[] data) {
        ByteArrayInputStream bs = new ByteArrayInputStream(data);
        try {
            ObjectInputStream os = new ObjectInputStream(bs);
            return (Transaction) os.readObject();
        }
        catch (Exception  e) {
            System.out.println("Exception: " + e.getMessage());
            return null;
        }
    }


    /*
        BLOCK ADDED
     */

    private CSTransaction csTransaction;
    private Store store;

    public Transaction(String request) throws AbortTransactionException, NoSuchAlgorithmException {
        this();

        // parse request
        JSONObject requestJson = new JSONObject(request);

        // extract transaction
        try { this.csTransaction = CSTransaction.fromJson(requestJson.getJSONObject("transaction")); }
        catch (Exception e) { throw new AbortTransactionException("Malformed transaction."); }

        // extract id-value store
        try { this.store = Store.fromJson(requestJson.getJSONObject("store")); }
        catch (Exception e) { throw new AbortTransactionException("Malformed id-value store."); }

        // init
        init();

    }

    // DEBUG CONSTRUCTOR
    public Transaction(CSTransaction csTransaction, Store store) throws NoSuchAlgorithmException {
        this();

        this.addID(csTransaction.getContractID());

        this.csTransaction = csTransaction;
        this.store = store;
        this.id = csTransaction.getID();

        init();

    }

    private void init() throws NoSuchAlgorithmException {
        for (int i = 0; i < csTransaction.getInputIDs().length; i++) {
            this.addInput(csTransaction.getInputIDs()[i]);
        }
        for (int i = 0; i < csTransaction.getOutputs().length; i++) {
            String objectID = Utils.generateObjectID(csTransaction.getID(), csTransaction.getOutputs()[i], i);
            this.addOutput(objectID);
        }
    }

    public CSTransaction getCsTransaction() {
        return csTransaction;
    }
    Store getStore() {
        return store;
    }

    /*
        END BLOCK
     */
}


