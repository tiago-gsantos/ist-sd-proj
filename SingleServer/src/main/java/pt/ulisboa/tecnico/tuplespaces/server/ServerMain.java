package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;

import java.io.IOException;

public class ServerMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        
        System.out.println(ServerMain.class.getSimpleName());

        boolean debug = false;

        // check arguments
        // check if debug mode is enabled
        if(args.length == 2 && args[1].equals("-debug")) {
            debug = true;
            System.out.println("\nServer in debug mode\n");

            // print arguments
            System.out.printf("Received %d arguments%n", args.length);
            for (int i = 0; i < args.length; i++) {
                System.out.printf("arg[%d] = %s%n", i, args[i]);
            }
        }
        else if (args.length != 1) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<port> [-debug]");
            return;
        }

        final int port = Integer.parseInt(args[0]);
        
        // create service impl
        final ServerServiceImpl serviceImpl = new ServerServiceImpl();
        
        if(debug) { serviceImpl.setDebug(); }

        final BindableService impl = serviceImpl;

        // create a new server to listen on port and add the service with the interceptor
        Server server = ServerBuilder.forPort(port)
                                     .addService(ServerInterceptors.intercept(impl, new HeaderServerInterceptor()))
                                     .build();
        
        // start the server
        server.start();

        if(debug) { System.out.println("\nServer started\n"); }

        // wait until server is terminated.
	    server.awaitTermination();
    }
}

