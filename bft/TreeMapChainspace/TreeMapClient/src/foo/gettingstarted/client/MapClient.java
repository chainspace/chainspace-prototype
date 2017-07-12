package foo.gettingstarted.client;


// This is the class which sends requests to replicas
import bftsmart.tom.ServiceProxy;
import foo.gettingstarted.RequestType;
import foo.gettingstarted.Transaction;
import java.nio.charset.Charset;

// Classes that need to be declared to implement this
// replicated Map
import java.io.*;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MapClient implements Map<String, String> {

    ServiceProxy clientProxy = null;

    public MapClient(int clientId, String pathConfig) {

        //clientProxy = new ServiceProxy(clientId);
        clientProxy = new ServiceProxy(clientId, pathConfig);
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<String> values() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String put(String key, String value) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);

            oos.writeInt(RequestType.PUT);
            oos.writeUTF(key);
            oos.writeUTF(value);
            oos.close();
            byte[] reply = clientProxy.invokeOrdered(out.toByteArray());
            if (reply != null) {
                String previousValue = new String(reply);
                return previousValue;
            }
            return null;
        } catch (IOException ioe) {
            System.out.println("Exception putting value into hashmap: " + ioe.getMessage());
            return null;
        }
    }

    @Override
    public String get(Object key) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeInt(RequestType.GET);
            oos.writeUTF(String.valueOf(key));
            oos.close();
            byte[] reply = clientProxy.invokeUnordered(out.toByteArray());
            String value = new String(reply);
            return value;
        } catch (IOException ioe) {
            System.out.println("Exception getting value from the hashmap: " + ioe.getMessage());
            return null;
        }
    }

    @Override
    public String remove(Object key) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeInt(RequestType.REMOVE);
            oos.writeUTF(String.valueOf(key));
            oos.close();
            byte[] reply = clientProxy.invokeOrdered(out.toByteArray());
            if (reply != null) {
                String removedValue = new String(reply);
                return removedValue;
            }
            return null;
        } catch (IOException ioe) {
            System.out.println("Exception removing value from the hashmap: " + ioe.getMessage());
            return null;
        }
    }

    @Override
    public int size() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeInt(RequestType.SIZE);
            oos.close();
            byte[] reply = clientProxy.invokeUnordered(out.toByteArray());
            ByteArrayInputStream in = new ByteArrayInputStream(reply);
            DataInputStream dis = new DataInputStream(in);
            int size = dis.readInt();
            return size;
        } catch (IOException ioe) {
            System.out.println("Exception getting the size the hashmap: " + ioe.getMessage());
            return -1;
        }
    }

    public String doTransaction(Transaction t) {

        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bs);
            oos.writeInt(RequestType.TRANSACTION);
            oos.writeObject(t);
            oos.close();
            byte[] reply = clientProxy.invokeUnordered(bs.toByteArray());
            if (reply != null) {
                return new String(reply, Charset.forName("UTF-8"));
            }
            return null;
        } catch (IOException ioe) {
            System.out.println("Exception: " + ioe.getMessage());
            return null;
        }
    }
}


