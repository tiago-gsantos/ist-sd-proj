package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;

import java.io.IOException;

public class FrontEndMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        
        System.out.println(FrontEndMain.class.getSimpleName());

        boolean debug = false;

        // check arguments
        // check if debug mode is enabled
        if(args.length == 5 && args[4].equals("-debug")) {
            debug = true;
            System.out.println("\nFront-End in debug mode\n");

            // print arguments
            System.out.printf("Received %d arguments%n", args.length);
            for (int i = 0; i < args.length; i++) {
                System.out.printf("arg[%d] = %s%n", i, args[i]);
            }
        }
        else if (args.length != 4) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<port> <host:port> <host:port> <host:port> [-debug]");
            return;
        }

        final int port = Integer.parseInt(args[0]);
        
        final int numServers = 3;
        final String[] host_port = {args[1], args[2], args[3]};
        
        // create service impl
        final FrontEndServiceImpl serviceImpl = new FrontEndServiceImpl();

        // create channel and stubs
        serviceImpl.createChannel(host_port, numServers);
        
        if(debug) { serviceImpl.setDebug(); }

        final BindableService impl = serviceImpl;

        // create a new server to listen on port and add the service with the interceptor
        Server frontEnd = ServerBuilder.forPort(port).addService(ServerInterceptors.intercept(impl, new HeaderFrontEndInterceptor())).build();
        
        // start the front-End
        frontEnd.start();

        if(debug) { System.out.println("\nFront-End started\n"); }

        // wait until server is terminated.
	    frontEnd.awaitTermination();
        serviceImpl.close();
    }
}

