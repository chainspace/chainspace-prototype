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
    private List<String> inputs;
    private List<String> outputs;

    public Transaction() {
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();
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

    public String isValid(TreeMap<String, String> table) {
        System.out.println("Checking if transaction is valid");
        for(String str: inputs) {
            String readValue = table.get(str);
            if (readValue == null || readValue.equals("0")) {
                return "Fail";
            }
        }
        return "Success";
    }
}

