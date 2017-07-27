package foo.gettingstarted;

// imports
import foo.gettingstarted.client.ConsoleClient;

/**
 * Main
 * Program entry point. Starts the client webservice.
 */
public class Main
{
    /**
     * main
     * Program entry point.
     * @param args 	path to the config file
     * @return -
     */
    public static void main(String[] args) {

        // TODO: run webservice
        ConsoleClient consoleClient = new ConsoleClient(args[0]);
        consoleClient.sendTransaction(0, "THIS IS A TEST.");

    } 
}