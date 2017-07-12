package foo.gettingstarted.client;

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
        clients[1] = new MapClient(1, "config1" );

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
            System.out.println("5. SUBMIT A TRANSACTION");

            cmd = sc.nextInt();
            String key;

            switch (cmd) {
                case 1:
                    System.out.println("Putting value in the map");
                    System.out.println("Enter the key:");
                    key = console.nextLine();
                    System.out.println("Enter the value:");
                    String value = console.nextLine();
                    String result = client.put(key, value);
                    System.out.println("Previous value: " + result);
                    break;
                case 2:
                    System.out.println("Reading value from the map");
                    System.out.println("Enter the key:");
                    key = console.nextLine();
                    result = client.get(key);
                    System.out.println("Value read: " + result);
                    break;
                case 3:
                    System.out.println("Removing value in the map");
                    System.out.println("Enter the key:");
                    key = console.nextLine();
                    result = client.remove(key);
                    System.out.println("Value removed: " + result);
                    break;
                case 4:
                    System.out.println("Getting the map size");
                    int size = client.size();
                    System.out.println("Map size: " + size);
                    break;
                case 5:
                    Transaction tran = new Transaction();

                    System.out.println("Enter inputs, one per line (type 'q' to stop):");
                    String input = console.nextLine();

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
                    result = client.doTransaction(tran);
                    System.out.println("Transaction status: " + result);
                    break;
            }
        }
    }
}