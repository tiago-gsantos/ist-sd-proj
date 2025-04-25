package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {

    public static void main(String[] args) {

        System.out.println(ClientMain.class.getSimpleName());

        boolean debug = false;

        // check arguments
        // check if debug mode is enabled
        if (args.length == 3 && args[2].equals("-debug")) {
            debug = true;
            System.err.println("\n# Client in debug mode\n");

            // print arguments
            System.err.printf("# Received %d arguments%n", args.length);
            for (int i = 0; i < args.length; i++) {
                System.err.printf("# arg[%d] = %s%n", i, args[i]);
            }
            System.err.println();
        }
        else if (args.length != 2) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<host:port> <client_id> [-debug]");
            return;
        }

        // get the host and port number of the server or front-end
        final String host_port = args[0];
        
        //get the client id
        final int client_id;
        try {
            client_id = Integer.parseInt(args[1]);
        } catch(NumberFormatException e) {
            System.err.println("Invalid client id! Must be an integer.");
            System.err.println("Usage: mvn exec:java -Dexec.args=<host:port> <client_id> [-debug]");
            return;
        }

        CommandProcessor parser = new CommandProcessor(new ClientService(host_port, client_id, debug));
        parser.parseInput();
    }
}
