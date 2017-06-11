package uk.ac.ucl.cs.sec.chainspace;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import uk.ac.ucl.cs.sec.chainspace.examples.BankAccount;

import java.io.IOException;
import java.sql.SQLException;


/**
 * Main class.
 */
public class Main {


    /**
     *
     */
    public static Node[] nodeList;

    /**
     * Main method.
     *
     * @param args args.
     */
    public static void main(String[] args) {

        try {

            test1();
            test2();

        } catch (SQLException | ClassNotFoundException | IOException | AbortTransactionException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test1. all objects are in the same node.
     *
     * @throws SQLException SQLException.
     * @throws ClassNotFoundException ClassNotFoundException.
     * @throws IOException IOException.
     * @throws AbortTransactionException ChainspaceException.
     */
    private static void test1() throws SQLException, ClassNotFoundException, IOException, AbortTransactionException {
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

        // instantiate the node
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
     * @throws AbortTransactionException ChainspaceException.
     */
    private static void test2() throws SQLException, ClassNotFoundException, IOException, AbortTransactionException {

        /*
            run nodes
         */
        nodeList = new Node[]{
                new Node(10),
                new Node(10),
                new Node(10),
                new Node(20),
                new Node(20),
                new Node(20),
        };



        /*
            test a transaction
         */
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

        // register objects
        nodeList[0].registerObject(aliceAccountJson);
        nodeList[3].registerObject(sallyAccountJson);

        // apply transaction

        for (Node node: nodeList) {
            node.applyTransaction(gson.toJson(transfer));
        }

        //nodeList[0].applyTransaction((gson.toJson(transfer)));


        /*
            shut down the node
         */
        for (Node node: nodeList) {
            node.shutdown();
        }
    }
}
