package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

import java.util.Scanner;
import io.grpc.StatusRuntimeException;

public class CommandProcessor {

    private static final String SPACE = " ";
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";
    private static final String PUT = "put";
    private static final String READ = "read";
    private static final String TAKE = "take";
    private static final String SLEEP = "sleep";
    private static final String EXIT = "exit";
    private static final String GET_TUPLE_SPACES_STATE = "getTupleSpacesState";

    private final ClientService clientService;

    public CommandProcessor(ClientService clientService) {
        this.clientService = clientService;
    }

    void parseInput() {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String[] split = line.split(SPACE);
            switch (split[0]) {
                case PUT:
                    this.put(split);
                    break;

                case READ:
                    this.read(split);
                    break;

                case TAKE:
                    this.take(split);
                    break;

                case GET_TUPLE_SPACES_STATE:
                    this.getTupleSpacesState();
                    break;

                case SLEEP:
                    this.sleep(split);
                    break;

                case EXIT:
                    exit = true;
                    break;

                default:
                    this.printUsage();
                    break;
            }
        }
        scanner.close();
        clientService.close();
    }

    private void put(String[] split){
        // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }

        String tuple = split[1];
        
        // check if delay was specified
        String delay = null;
        if (split.length == 5) {
            delay = String.join(" ", split[2], split[3], split[4]);
        }

        try {
            clientService.put(tuple, delay);

            System.out.println("OK\n");
        } catch (StatusRuntimeException e) {
            // print error description
            System.err.println("\nERROR\n" + e.getStatus().getDescription() + "\n");
        }
    }
    
    private void read(String[] split){
        // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }

        String tuple = split[1];

        // check if delay was specified
        String delay = null;
        if (split.length == 5) {
            delay = String.join(" ", split[2], split[3], split[4]);
        }

        try {
            String response = clientService.read(tuple, delay);

            System.out.println("OK\n"+ response + "\n");
        } catch (StatusRuntimeException e) {
            // print error description
            System.err.println("\nERROR\n" + e.getStatus().getDescription() + "\n");
        }
    }
    
    
    private void take(String[] split){
        // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }
        
        String tuple = split[1];

        // check if delay was specified
        String delay = null;
        if (split.length == 5) {
            delay = String.join(" ", split[2], split[3], split[4]);
        }
        
        try {
            String response = clientService.take(tuple, delay);
            
            System.out.println("OK\n"+ response + "\n");
        }
        catch (StatusRuntimeException e) {
            // print error description
            System.err.println("\nERROR\n" + e.getStatus().getDescription() + "\n");
        }
    }
    
    private void getTupleSpacesState(){
        try {
            String response = clientService.getTupleSpacesState();
            
            System.out.println("OK\n"+ response + "\n");
        }
        catch (StatusRuntimeException e) {
            // print error description
            System.err.println("\nERROR\n" + e.getStatus().getDescription() + "\n");
        }
    }
    
    private void sleep(String[] split) {
        if (split.length != 2){
            this.printUsage();
            return;
        }

        // checks if input String can be parsed as an Integer
        Integer time;
        try {
            time = Integer.parseInt(split[1]);
        } catch (NumberFormatException e) {
            this.printUsage();
            return;
        }

        try {
            Thread.sleep(time*1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                "- put <element[,more_elements]> [delay1,delay2,delay3]\n" +
                "- read <element[,more_elements]> [delay1,delay2,delay3]\n" +
                "- take <element[,more_elements]> [delay1,delay2,delay3]\n" +
                "- getTupleSpacesState\n" +
                "- sleep <integer>\n" +
                "- exit\n");
    }

    private boolean inputIsValid(String[] input){
        if (input.length < 2 
            ||
            !input[1].substring(0,1).equals(BGN_TUPLE) 
            || 
            !input[1].endsWith(END_TUPLE)
            || 
            input.length == 3 || input.length == 4
            ||
            (input.length == 5 && (stringToDouble(input[2]) == null || 
                                   stringToDouble(input[3]) == null ||
                                   stringToDouble(input[4]) == null))
            ||
            input.length > 5
            ) {
            return false;
        }
        else {
            return true;
        }
    }

    private Double stringToDouble(String str){
        try {
            Double n = Double.parseDouble(str);
            return n;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
