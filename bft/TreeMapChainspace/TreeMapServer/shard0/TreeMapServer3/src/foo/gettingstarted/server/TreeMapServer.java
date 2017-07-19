package foo.gettingstarted.server;

// These are the classes which receive requests from clients
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import foo.gettingstarted.RequestType;
import foo.gettingstarted.Transaction;
import foo.gettingstarted.TransactionSequence;
import foo.gettingstarted.client.MapClient;

// Classes that need to be declared to implement this
// replicated Map
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeMap;
import java.util.Map;


public class TreeMapServer extends DefaultRecoverable {

    Map<String, String> table;
    Map<String, TransactionSequence> sequences;
    int shard;
    MapClient[] clients = new MapClient[2];

    public TreeMapServer(int id) {
        table = new TreeMap<>();
        sequences = new  TreeMap<>();
        // TODO: Hardcoded for now, make configurable
        shard = 0;

        clients[0] = new MapClient(4, "config0" );
        //clients[1] = new MapClient(3, "config1" );
        new ServiceReplica(id, this, this);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: HashMapServer <server id>");
            System.exit(0);
        }

        new TreeMapServer(Integer.parseInt(args[0]));
    }

    @Override
    public byte[][] appExecuteBatch(byte[][] command, MessageContext[] mcs) {

        byte[][] replies = new byte[command.length][];
        for (int i = 0; i < command.length; i++) {
            replies[i] = executeSingle(command[i], mcs[i]);
        }

        return replies;
    }

    private byte[] executeSingle(byte[] command, MessageContext msgCtx) {
        int reqType;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(command);
            ObjectInputStream ois = new ObjectInputStream(in);
            reqType = ois.readInt();
            if (reqType == RequestType.PUT) {
                String key = ois.readUTF();
                String value = ois.readUTF();
                String oldValue = table.put(key, value);
                byte[] resultBytes = null;
                if (oldValue != null) {
                    resultBytes = oldValue.getBytes();
                }
                return resultBytes;
            } else if (reqType == RequestType.REMOVE) {
                String key = ois.readUTF();
                String removedValue = table.remove(key);
                byte[] resultBytes = null;
                if (removedValue != null) {
                    resultBytes = removedValue.getBytes();
                }
                return resultBytes;
            } else if (reqType == RequestType.PREPARE_T) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream ds = new DataOutputStream(out);
                try {
                    System.out.println("Processing transaction");
                    Transaction t = (Transaction) ois.readObject();
                    t.print();

                    // Check whether or not a transaction is valid
                    String t_status = t.getStatus(new TreeMap<String, String>(table), shard);
                    System.out.println(t_status);
                    ds.writeUTF(t_status);

                    byte[] reply = out.toByteArray();
                    return reply;
                }
                catch (Exception  e) {
                    System.out.println("Exception: " + e.getMessage());
                    return null;
                }

            } else {
                System.out.println("Unknown request type: " + reqType);
                return null;
            }
        } catch (IOException e) {
            System.out.println("Exception reading data in the replica: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        int reqType;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(command);
            ObjectInputStream ois = new ObjectInputStream(in);
            reqType = ois.readInt();
            if (reqType == RequestType.GET) {
                String key = ois.readUTF();
                String readValue = table.get(key);
                byte[] resultBytes = null;
                if (readValue != null) {
                    resultBytes = readValue.getBytes();
                }
                return resultBytes;
            } else if (reqType == RequestType.SIZE) {
                int size = table.size();

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(out);
                dos.writeInt(size);
                byte[] sizeInBytes = out.toByteArray();

                return sizeInBytes;
            }
            else if (reqType == RequestType.TRANSACTION_VALIDITY) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream ds = new DataOutputStream(out);
                try {
                    System.out.println("Processing transaction");
                    Transaction t = (Transaction) ois.readObject();
                    t.print();

                    // This simply tells the client whether or not a transaction is valid
                    String t_status = t.getStatus(new TreeMap<String, String>(table), shard);
                    System.out.println(t_status);

                    ds.writeUTF(t_status);
                    byte[] reply = out.toByteArray();
                    return reply;
                }

                catch (Exception  e) {
                    System.out.println("Exception: " + e.getMessage());
                    return null;
                }
            }
            else if (reqType == RequestType.TRANSACTION_COMMIT) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream ds = new DataOutputStream(out);
                try {
                    System.out.println("Processing transaction");
                    Transaction t = (Transaction) ois.readObject();
                    t.print();

                    String reply = clients[0].prepare_t(t);
                    System.out.println("Reply of BFT is: "+reply);
                    return reply.getBytes();
                }

                catch (Exception  e) {
                    System.out.println("Exception: " + e.getMessage());
                    return null;
                }
            }
            else {
                System.out.println("Unknown request type: " + reqType);
                return null;
            }
        } catch (IOException e) {
            System.out.println("Exception reading data in the replica: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void installSnapshot(byte[] state) {
        ByteArrayInputStream bis = new ByteArrayInputStream(state);
        try {
            ObjectInput in = new ObjectInputStream(bis);
            table = (Map<String, String>) in.readObject();
            in.close();
            bis.close();
        } catch (ClassNotFoundException e) {
            System.out.print("Coudn't find Map: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.print("Exception installing the application state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public byte[] getSnapshot() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(table);
            out.flush();
            out.close();
            bos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            System.out.println("Exception when trying to take a + "
                    + "snapshot of the application state" + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
    }
}
