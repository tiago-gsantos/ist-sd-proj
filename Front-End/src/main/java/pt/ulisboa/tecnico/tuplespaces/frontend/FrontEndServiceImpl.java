package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesServerGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesServerOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.frontend.observers.*;
import pt.ulisboa.tecnico.tuplespaces.frontend.responsecollectors.*;
import io.grpc.Status;
import static io.grpc.Status.INVALID_ARGUMENT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;

public class FrontEndServiceImpl extends TupleSpacesGrpc.TupleSpacesImplBase {
    private TupleSpacesServerGrpc.TupleSpacesServerStub[] stubs;
    private ManagedChannel[] channels;

    private int numServers;

    private boolean debug = false;

    // defining header key for delay
    private static final Metadata.Key<String> DELAY_KEY = Metadata.Key.of("delay_key", Metadata.ASCII_STRING_MARSHALLER);

    public void setDebug() {
        this.debug = true;
    }

    public void createChannel(String[] host_port, int numServers) {
        this.numServers = numServers;
        this.channels = new ManagedChannel[this.numServers];
		this.stubs = new TupleSpacesServerGrpc.TupleSpacesServerStub[this.numServers];

		for (int i = 0; i < this.numServers; i++) {
            // create channels
			channels[i] = ManagedChannelBuilder.forTarget(host_port[i]).usePlaintext().build();
			// create async stubs
            stubs[i] = TupleSpacesServerGrpc.newStub(channels[i]);
		}
    }

    public void close() {
        if(this.debug) { System.err.println("Closing the channels"); }
        
        // shut down all channels
        for (ManagedChannel c : channels)
			c.shutdown();
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        if(this.debug) { System.err.println("Received a put request from the client. Sending it to all replicas: " + request.toString()); }
        
        // check if argument is valid
        if(!isValidTuple(request.getNewTuple())) {
            if(this.debug) { System.err.println("Invalid Argument\n"); }
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Argument").asRuntimeException());
            return;
        }

        // convert put request from client into put request to server
        PutRequestServer requestServer = PutRequestServer.newBuilder().setNewTuple(request.getNewTuple()).build();

        ResponseCollector putCollector = new ResponseCollector();

        // check if request has a header
        String headerValue = HeaderFrontEndInterceptor.DELAY_CONTEXT_KEY.get();
        if (headerValue != null) {
            if(this.debug) { System.err.println("The put request from the client had a header with value: " + headerValue); }
            
            // split 3 delay values
            String[] delay = headerValue.split(" ");

            //multicast put request to all replicas with the respective delay in the metadata
            for (int i = 0; i < this.numServers; i++){

                TupleSpacesServerGrpc.TupleSpacesServerStub stubWithHeader = createStubWithHeader(delay, i);              

                // send put request
                stubWithHeader.putServer(requestServer, new FrontEndObserverAck<>(this.debug, putCollector));
            }
        }
        else{
            //multicast put request to all replicas
            for (int i = 0; i < this.numServers; i++){
                stubs[i].putServer(requestServer, new FrontEndObserverAck<>(this.debug, putCollector));
            }
        }

        // respond to client
        PutResponse response = PutResponse.newBuilder().build();
        responseObserver.onNext(response);

        if(this.debug) { System.err.println("Put response sent to the client\n"); }
        
        // wait to receive response from all replicas
        waitForResponses(putCollector, this.numServers);

        // if at least one replica responded with an error, send an error back to the client
        for(String r : putCollector.getResponses()) {
            if(!r.equals("ACK")){
                responseObserver.onError(Status.fromCode(Status.Code.valueOf(r))
                                               .withDescription("An error occured with the server. Replicas may be incoherent\n")
                                               .asRuntimeException());
                return;
            }
        }
        
        // send completed put request to client
        responseObserver.onCompleted();
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        if(this.debug) { System.err.println("Received a read request from the client. Sending it to all replicas: " + request.toString()); }

        // check if argument is valid
        if(!isValidTuple(request.getSearchPattern())) {
            if(this.debug) { System.err.println("Invalid Argument\n"); }
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Argument").asRuntimeException());
            return;
        }

        // convert read request from client into read request to server
        ReadRequestServer requestServer = ReadRequestServer.newBuilder().setSearchPattern(request.getSearchPattern()).build();

        ResponseCollector r = new ResponseCollector();

        // check if request has a header
        String headerValue = HeaderFrontEndInterceptor.DELAY_CONTEXT_KEY.get();
        if (headerValue != null) {
            if(this.debug) { System.err.println("The put request from the client had a header with value: " + headerValue); }

            // split 3 delay values
            String[] delay = headerValue.split(" ");

            //multicast read request to all replicas with the respective delay in the metadata
            for (int i = 0; i < this.numServers; i++){
                TupleSpacesServerGrpc.TupleSpacesServerStub stubWithHeader = createStubWithHeader(delay, i);

                // send read request
                stubWithHeader.readServer(requestServer, new FrontEndObserverResult<>(this.debug, r));
            }
        }
        else{
            // multicast put request to all replicas
            for (int i = 0; i < this.numServers; i++){
                stubs[i].readServer(requestServer, new FrontEndObserverResult<>(this.debug, r));
            }
        }

        // wait to receive the first response
        int n = 0;
        while(n < 3) {
            try {
                if(this.debug) { System.err.println("Waiting to receive the first response\n"); }
                
                r.waitUntilAllReceived(n+1);

                // if response is an error, continue waiting
                if(isValidTuple(r.getResponses().get(n))) { break; }
                else { n++; }
            } catch(InterruptedException e) {
                // if wait was interrupted before it received the first response, continue waiting
                if(this.debug) { System.err.println("Waiting thread was interrupted...\n"); }
            }
        }

        // if all responses are errors, return error to client
        if(n == 3) {
            responseObserver.onError(Status.fromCode(Status.Code.valueOf(r.getResponses().get(0)))
                                            .withDescription("An error occured with the server. Replicas may be incoherent\n")
                                            .asRuntimeException());
            return;
        }

        //respond to client
        ReadResponse response = ReadResponse.newBuilder().setResult(r.getResponses().get(n)).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        if(this.debug) { System.err.println("Read response sent to the client: " + response.toString()); }
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        if(this.debug) { System.err.println("Received a take request from the client: " + request.toString()); }

        // check if argument is valid
        if(!isValidTuple(request.getSearchPattern())) {
            if(this.debug) { System.err.println("Invalid Argument\n"); }
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Argument").asRuntimeException());
            return;
        }

        int client_id = request.getId();

        // calculate quorum
        Integer[] quorum = {client_id % 3, (client_id + 1) % 3};
        int quorumSize = quorum.length;

        String tuple = null;

        // create response collector to collect lists of tuples that entered the Critical Section
        ResponseCollectorList enterCollector = new ResponseCollectorList(quorumSize);
        
        while(tuple == null) {
            // convert take request from client into enter CS request to server
            enterCriticalSectionRequest enterRequest = enterCriticalSectionRequest.newBuilder().setSearchPattern(request.getSearchPattern()).build();
            
            // multicast enter CS request to all replicas in the quorum
            for (int i = 0; i < quorumSize; i++) {
                stubs[quorum[i]].enterCriticalSection(enterRequest, new FrontEndObserverList<>(this.debug, enterCollector, i));
            }
            if(this.debug) { System.err.println("Sent enter Critical Section request to replicas " + quorum[0] + " and " + quorum[1] + "\n"); }
             
            // wait to receive response from all replicas of the quorum
            waitForResponses(enterCollector, quorumSize);
            
            // if at least one replica responded with an error, send an error back to the client
            for(String[] response : enterCollector.getAllResponses()) {
                if(!isValidTuple(response[0])){
                    responseObserver.onError(Status.fromCode(Status.Code.valueOf(response[0]))
                    .withDescription("An error occured with the server. Replicas may be incoherent\n")
                    .asRuntimeException());
                    return;
                }
            }
            
            // calculate intersection of all the responses from the quorum replicas
            Set<String> intersection = new HashSet<>(Arrays.asList(enterCollector.getResponses(0)));
            
            for (int i = 1; i < quorumSize; i++) {
                intersection.retainAll(Arrays.asList(enterCollector.getResponses(i)));
            }
            
            // get one of the tuples from the interception
            tuple = intersection.isEmpty() ? null : intersection.iterator().next();
            
            // if there is no tuple in the intersection, send exit CS request and repeat process
            if(tuple == null) {
                // create response collector to collect exits from critical section
                ResponseCollector exitCollector = new ResponseCollector();

                // multicast exit CS request to all replicas in the quorum
                for (int i = 0; i < quorumSize; i++){
                    // create exit CS request to server with the responses collected from the enter CS request
                    TakeRequestServer exitRequest = TakeRequestServer.newBuilder().addAllTuplesCS(Arrays.asList(enterCollector.getResponses(i))).setTupleTake("").build();
                    
                    stubs[quorum[i]].takeServer(exitRequest, new FrontEndObserverAck<>(this.debug, exitCollector));
                }
                if(this.debug) { System.err.println("There is no matching tuple between the replicas. Sending exit Critical Section request to replicas " + quorum[0] + " and " + quorum[1] + "\n"); }

                // wait to receive response from all replicas of the quorum
                waitForResponses(exitCollector, quorumSize);

                // if at least one replica responded with an error, send an error back to the client
                for(String response : exitCollector.getResponses()) {
                    if(!response.equals("ACK")){
                        responseObserver.onError(Status.fromCode(Status.Code.valueOf(response))
                                                    .withDescription("An error occured with the server. Replicas may be incoherent\n")
                                                    .asRuntimeException());
                        return;
                    }
                }

                // wait 500 ms to reduce number of messages sent
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}

                enterCollector = new ResponseCollectorList(quorumSize);
            }
        }

        // create response collector to collect take responses
        ResponseCollector takeCollector = new ResponseCollector();

        TakeRequestServer takeRequest;

        // check if request has a header
        String headerValue = HeaderFrontEndInterceptor.DELAY_CONTEXT_KEY.get();
        if (headerValue != null) {
            if(this.debug) { System.err.println("The take request from the client had a header with value: " + headerValue); }
            // split 3 delay values
            String[] delay = headerValue.split(" ");

            // multicast take request to all replicas with the respective delay in the metadata
            for (int i = 0; i < this.numServers; i++) {
                int index = Arrays.asList(quorum).indexOf(i);

                if(index == -1) {
                    // create take request to server for a replica that is not on the quorum (don't send tuples to exit CS)
                    takeRequest = TakeRequestServer.newBuilder().setTupleTake(tuple).addAllTuplesCS(Collections.emptyList()).build();
                }
                else {
                    // create take request to server for a replica that is on the quorum (send tuples to exit CS)
                    takeRequest = TakeRequestServer.newBuilder().setTupleTake(tuple).addAllTuplesCS(Arrays.asList(enterCollector.getResponses(index))).build();
                }
                
                TupleSpacesServerGrpc.TupleSpacesServerStub stubWithHeader = createStubWithHeader(delay, i);

                // send take request
                stubWithHeader.takeServer(takeRequest, new FrontEndObserverResult<>(this.debug, takeCollector));
            }
        }
        else {
            // multicast take request to all replicas
            for (int i = 0; i < this.numServers; i++){
                int index = Arrays.asList(quorum).indexOf(i);

                if(index == -1) {
                    // create take request to server for a replica that is not on the quorum (don't send tuples to exit CS)
                    takeRequest = TakeRequestServer.newBuilder().setTupleTake(tuple).addAllTuplesCS(Collections.emptyList()).build();
                }
                else {
                    // create take request to server for a replica that is on the quorum (send tuples to exit CS)
                    takeRequest = TakeRequestServer.newBuilder().setTupleTake(tuple).addAllTuplesCS(Arrays.asList(enterCollector.getResponses(index))).build();
                }

                stubs[i].takeServer(takeRequest, new FrontEndObserverResult<>(this.debug, takeCollector));
            }
        }
        if(this.debug) { System.err.println("Sent take request to all replicas for tuple " + tuple + "\n"); }

        // respond to client
        TakeResponse response = TakeResponse.newBuilder().setResult(tuple).build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        if(this.debug) { System.err.println("Take response sent to the client: " + response.toString()); }
        
        // wait to receive response from all replicas
        waitForResponses(takeCollector, this.numServers);

        // if at least one replica responded with an error, print the error (can't send it back to client)
        for(String r : takeCollector.getResponses()) {
            if(!isValidTuple(r)) {
                System.err.println("An error occurred with the server. Replicas may be incoherent.");
                return;
            }
        }
    }

    @Override
    public void getTupleSpacesState(getTupleSpacesStateRequest request, 
    StreamObserver<getTupleSpacesStateResponse> responseObserver) {
        if(this.debug) { System.err.println("Received a getTupleSpacesState request from the client. Sending it to the server\n"); }

        // convert getTupleSpacesState request from client into getTupleSpacesState request to server
        getTupleSpacesStateRequestServer requestServer = getTupleSpacesStateRequestServer.newBuilder().build();

        ResponseCollectorList r = new ResponseCollectorList(this.numServers);

        // multicast getTupleSpacesState request to all replicas
        for (int i = 0; i < this.numServers; i++){
            stubs[i].getTupleSpacesStateServer(requestServer, new FrontEndObserverList<>(this.debug, r, i));
        }
        
        // wait to receive response from all replicas
        waitForResponses(r, this.numServers);

        // combine all the responses of the response collector
        List<String> result = Arrays.stream(r.getAllResponses())
                                    .flatMap(Arrays::stream)
                                    .collect(Collectors.toList());
        
        // respond to client
        getTupleSpacesStateResponse response = getTupleSpacesStateResponse.newBuilder().addAllTuple(result).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        if(this.debug) { System.err.println("GetTupleSpacesState response sent to the client: " + response.toString()); }
    }

    public TupleSpacesServerGrpc.TupleSpacesServerStub createStubWithHeader(String[] delay, int i){
        //create metadata
        Metadata metadata = new Metadata();
        metadata.put(DELAY_KEY, delay[i]);

        //create stub with metadata
        return stubs[i].withInterceptors(MetadataUtils.newAttachHeadersInterceptor((metadata)));
    }

    public boolean isValidTuple(String tuple){
        return tuple.substring(0,1).equals("<") && tuple.endsWith(">");
    }
    
    public void waitForResponses (ResponseCollectorInterface r, int numServers) {
        while(true) {
            try {
                if(this.debug) { System.err.println("Waiting to receive the responses\n"); }
                r.waitUntilAllReceived(numServers);
                break;
            } catch(InterruptedException e) {
                // if wait was interrupted, wait for the remaining responses
                if(this.debug) { System.err.println("Waiting thread was interrupted...\n"); }
            }
        }
        return;
    }
}