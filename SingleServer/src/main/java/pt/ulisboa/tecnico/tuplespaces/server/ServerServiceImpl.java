package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesServerGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesServerOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

import static io.grpc.Status.INTERNAL;
import static io.grpc.Status.INVALID_ARGUMENT;

public class ServerServiceImpl extends TupleSpacesServerGrpc.TupleSpacesServerImplBase {

    private ServerState tupleSpaces = new ServerState();

    private boolean debug = false;

    public void setDebug() {
        this.debug = true;
    }

    @Override
    public void putServer(PutRequestServer request, StreamObserver<PutResponseServer> responseObserver) {
        if(this.debug) { System.err.println("Received a put request: " + request.toString()); }

        // check if request has header
        checkDelayInHeader();

        // execute put operation
        tupleSpaces.put(request.getNewTuple(), this.debug);

        // create and send response
        PutResponseServer response = PutResponseServer.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        if(this.debug) { System.err.println("Sent a put response\n"); }
    }

    @Override
    public void readServer(ReadRequestServer request, StreamObserver<ReadResponseServer> responseObserver) {
        if(this.debug) { System.err.println("Received a read request: " + request.toString()); }

        String tuple = request.getSearchPattern();

        // check if request has header
        checkDelayInHeader();

        try  {
            // execute read operation and send response
            ReadResponseServer response = ReadResponseServer.newBuilder().setResult(tupleSpaces.read(tuple, this.debug)).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            if(this.debug) { System.err.println("Sent a read response: " + response.toString()); }
        }
        catch (RuntimeException e) {
            responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void enterCriticalSection(enterCriticalSectionRequest request, StreamObserver<enterCriticalSectionResponse> responseObserver) {
        if(this.debug) { System.err.println("Received enter Critical Section request: " + request.toString()); }
        
        String searchPattern = request.getSearchPattern();

        try {
            // try to enter critical section and when successful, send response
            enterCriticalSectionResponse response = enterCriticalSectionResponse.newBuilder().addAllTuples(tupleSpaces.enterCriticalSection(searchPattern, this.debug)).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            if(this.debug) { System.err.println("Sent enter Critical Section response\n"); }
        } catch (RuntimeException e) {
            responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void takeServer(TakeRequestServer request, StreamObserver<TakeResponseServer> responseObserver) {

        String resultTake = "";
        String tuple = request.getTupleTake();

        // Take
        if (tuple != "") {
            if(this.debug) { System.err.println("Received a take request: " + request.toString()); }
            
            // check if request has header
            checkDelayInHeader();

            try {
                // execute take operation and send response
                resultTake = tupleSpaces.take(tuple, this.debug);
                
            } catch (RuntimeException e) {
                responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            }
        }

        String[] tuplesList = request.getTuplesCSList().toArray(new String[0]);

        // Exit Critical Section
        if(tuplesList.length != 0) {
            if(this.debug) { System.err.println("Received exit Critical Section request: " + request.toString()); }

            try {
                // exit critical section and send response
                tupleSpaces.exitCriticalSection(tuplesList, this.debug);
                
            } catch (RuntimeException e) {
                responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            }
        }

        TakeResponseServer response = TakeResponseServer.newBuilder().setResultTake(resultTake).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        if(this.debug) { System.err.println("Sent a take/exitCS response: " + response.toString()); }
    }
    
    @Override
    public void getTupleSpacesStateServer(getTupleSpacesStateRequestServer request, 
    StreamObserver<getTupleSpacesStateResponseServer> responseObserver) {
        if(this.debug) { System.err.println("Received a getTupleSpacesState request\n"); }

        // execute getTupleSpacesState operation and send response
        getTupleSpacesStateResponseServer response = getTupleSpacesStateResponseServer.newBuilder()
        .addAllTuple(tupleSpaces.getTupleSpacesState()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        if(this.debug) { System.err.print("Sent a getTupleSpacesState response:\n" + response.toString() + "\n"); }
    }

    public void checkDelayInHeader() {
        // check if request has header
        String headerValue = HeaderServerInterceptor.DELAY_CONTEXT_KEY.get();
        if (headerValue != null) {
            if(this.debug) { System.err.println("The request had a header with value: " + headerValue); }

            // sleep for number of seconds passed as metadata
            double delay = Double.parseDouble(headerValue);
            tupleSpaces.sleep(delay, this.debug);
        }
    }
}