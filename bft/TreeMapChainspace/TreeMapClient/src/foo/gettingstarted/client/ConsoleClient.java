package foo.gettingstarted.client;

import foo.gettingstarted.RequestType;
import foo.gettingstarted.Transaction;

import java.util.Scanner;

public class ConsoleClient {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: ConsoleClient <client id>");
            System.exit(0);
        }

        MapClient[] clients = new MapClient[2];
        clients[0] = new MapClient(0, "config0" );
       //clients[1] = new MapClient(1, "config1" );

        Scanner sc = new Scanner(System.in);
        Scanner console = new Scanner(System.in);

        while (true) {

            System.out.println("Select a shard (type 0 or 1):");
            int cmd = sc.nextInt();
            MapClient client = clients[cmd];

            System.out.println("Select an option:");
            System.out.println("1. ADD A KEY AND VALUE TO THE MAP");
            System.out.println("2. READ A VALUE FROM THE MAP");
            System.out.println("3. REMOVE AND ENTRY FROM THE MAP");
            System.out.println("4. GET THE SIZE OF THE MAP");
            System.out.println("5. VALIDATE A TRANSACTION");
            System.out.println("6. COMMIT A TRANSACTION");
            System.out.println("7. PREPARE_T");

            cmd = sc.nextInt();
            String key, input;

            switch (cmd) {
                case RequestType.PUT:
                    System.out.println("Putting value in the map");
                    System.out.println("Enter the key:");
                    key = console.nextLine();
                    System.out.println("Enter the value:");
                    String value = console.nextLine();
                    String result = client.put(key, value);
                    System.out.println("Previous value: " + result);
                    break;
                case RequestType.GET:
                    System.out.println("Reading value from the map");
                    System.out.println("Enter the key:");
                    key = console.nextLine();
                    result = client.get(key);
                    System.out.println("Value read: " + result);
                    break;
                case RequestType.REMOVE:
                    System.out.println("Removing value in the map");
                    System.out.println("Enter the key:");
                    key = console.nextLine();
                    result = client.remove(key);
                    System.out.println("Value removed: " + result);
                    break;
                case RequestType.SIZE:
                    System.out.println("Getting the map size");
                    int size = client.size();
                    System.out.println("Map size: " + size);
                    break;
                case RequestType.TRANSACTION_VALIDITY:
                    System.out.println("Checking if a transaction is valid");
                    Transaction tran = new Transaction();

                    System.out.println("Enter transaction ID:");
                    input = console.nextLine();
                    tran.addID(input);

                    System.out.println("Enter inputs, one per line (type 'q' to stop):");
                    input = console.nextLine();

                    while (!(input.equalsIgnoreCase("q"))) {
                        tran.addInput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Enter outputs, one per line (type 'q' to stop):");
                    input=console.nextLine();
                    while (!(input.equalsIgnoreCase("q"))) {
                        tran.addOutput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Transaction to be added is:");
                    tran.print();
                    result = client.checkTransactionValidity(tran);
                    System.out.println("Transaction status: " + result);
                    break;

                case RequestType.TRANSACTION_COMMIT:
                    System.out.println("Committing  a transaction");
                    Transaction t = new Transaction();

                    System.out.println("Enter transaction ID:");
                    input = console.nextLine();
                    t.addID(input);

                    System.out.println("Enter inputs, one per line (type 'q' to stop):");
                    input = console.nextLine();

                    while (!(input.equalsIgnoreCase("q"))) {
                        t.addInput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Enter outputs, one per line (type 'q' to stop):");
                    input=console.nextLine();
                    while (!(input.equalsIgnoreCase("q"))) {
                        t.addOutput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Transaction to be added is:");
                    t.print();
                    result = client.commitTransaction(t);
                    System.out.println("Transaction status: " + result);
                    break;

                case RequestType.PREPARE_T:
                    System.out.println("Doing PREPARE_T");
                    Transaction tt = new Transaction();

                    System.out.println("Enter transaction ID:");
                    input = console.nextLine();
                    tt.addID(input);

                    System.out.println("Enter inputs, one per line (type 'q' to stop):");
                    input = console.nextLine();

                    while (!(input.equalsIgnoreCase("q"))) {
                        tt.addInput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Enter outputs, one per line (type 'q' to stop):");
                    input=console.nextLine();
                    while (!(input.equalsIgnoreCase("q"))) {
                        tt.addOutput(input);
                        input=console.nextLine();
                    }

                    System.out.println("Transaction to be added is:");
                    tt.print();
                    result = client.prepare_t(tt);
                    System.out.println("Transaction status: " + result);
                    break;

            }
        }
    }
}