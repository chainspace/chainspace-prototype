package foo.gettingstarted;

/**
 * Created by sheharbano on 11/07/2017.
 */

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;
import java.util.TreeMap;

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

    public String getStatus(TreeMap<String, String> table, int shard) {
        // TODO: Check if the transaction is malformed, return INVALID_BADTRANSACTION

        // Check if all input objects are active
        // and at least one of the input objects is managed by this shard
        int nManagedObj = 0;

        for(String key: inputs) {
            String readValue = table.get(key);
            boolean managedObj = (ObjectStatus.mapObjectToShard(key)==shard);
            if(managedObj)
                nManagedObj++;
            if(managedObj && readValue == null)
                return INVALID_NOOBJECT;
            else if(managedObj && readValue != null) {
                if (readValue.equals(ObjectStatus.LOCKED))
                    return REJECTED_LOCKEDOBJECT;
                else if (managedObj && readValue.equals(ObjectStatus.INACTIVE))
                    return INVALID_INACTIVEOBJECT;
            }
        }
        // The case when this shard doesn't manage any of the input objects
        if(nManagedObj == 0)
            return INVALID_NOMANAGEDOBJECT;

        return VALID;
    }

}


