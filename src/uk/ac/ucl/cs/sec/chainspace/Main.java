package uk.ac.ucl.cs.sec.chainspace;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import uk.ac.ucl.cs.sec.chainspace.examples.BankAccount;

import java.io.IOException;
import java.sql.SQLException;

public class Main {

    /**
     * Main method.
     *
     * @param args args.
     */
    public static void main(String[] args) {

        try {

            test1();
            //test2();

        } catch (SQLException | ClassNotFoundException | IOException | ChainspaceException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test1. all objects are in the same node.
     *
     * @throws SQLException SQLException.
     * @throws ClassNotFoundException ClassNotFoundException.
     * @throws IOException IOException.
     * @throws ChainspaceException ChainspaceException.
     */
    private static void test1() throws SQLException, ClassNotFoundException, IOException, ChainspaceException {
        // init
        Gson gson = new GsonBuilder().create();

        // checker url
        String checkerURL = "http://127.0.0.1:5001/bank/transfer";

        // create Alice's account
        BankAccount aliceAccount = new BankAccount("Alice", 10);
        BankAccount sallyAccount = new BankAccount("Sally", 0);
        String aliceAccountJson  = gson.toJson(aliceAccount);
        String sallyAccountJson  = gson.toJson(sallyAccount);

        // create transfer
        Transaction transfer = aliceAccount.sendMoney(sallyAccount, 8, checkerURL);

        // instanciate the node
        Node node = new Node(1);

        // register objects
        node.registerObject(aliceAccountJson);
        node.registerObject(sallyAccountJson);

        // apply transaction
        node.applyTransaction(gson.toJson(transfer));

        // shut down the node
        node.shutdown();
    }

    /**
     * Test2. the transaction requires an input from each node.
     *
     * @throws SQLException SQLException.
     * @throws ClassNotFoundException ClassNotFoundException.
     * @throws IOException IOException.
     * @throws ChainspaceException ChainspaceException.
     */
    /*
    private static void test2() throws SQLException, ClassNotFoundException, IOException, ChainspaceException {
        // init
        Gson gson = new GsonBuilder().create();

        // checker url
        String checkerURL = "http://127.0.0.1:5001/bank/transfer";

        // create Alice's account
        BankAccount aliceAccount = new BankAccount("Alice", 10);
        BankAccount sallyAccount = new BankAccount("Sally", 0);
        String aliceAccountJson  = gson.toJson(aliceAccount);
        String sallyAccountJson  = gson.toJson(sallyAccount);

        // create transfer
        Transaction transfer = aliceAccount.sendMoney(sallyAccount, 8, checkerURL);

        // instanciate the nodes
        Node node1 = new Node(10);
        Node node2 = new Node(10);
        Node node3 = new Node(10);
        Node node4 = new Node(20);
        Node node5 = new Node(20);
        Node node6 = new Node(20);


        // register objects
        node1.registerObject(aliceAccountJson);
        node4.registerObject(sallyAccountJson);

        // apply transaction
        node1.applyTransaction(gson.toJson(transfer));
        node2.applyTransaction(gson.toJson(transfer));
        node3.applyTransaction(gson.toJson(transfer));
        node4.applyTransaction(gson.toJson(transfer));
        node5.applyTransaction(gson.toJson(transfer));
        node6.applyTransaction(gson.toJson(transfer));

        // shut down the node
        node1.shutdown();
        node2.shutdown();
        node3.shutdown();
        node4.shutdown();
        node5.shutdown();
        node6.shutdown();
    }
    */
}
