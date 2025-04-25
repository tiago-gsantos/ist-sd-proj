package pt.ulisboa.tecnico.tuplespaces.frontend.observers;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.responsecollectors.ResponseCollectorList;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesServerOuterClass.*;
import java.util.Collections;

public class FrontEndObserverList<R> implements StreamObserver<R>{
    
    private boolean debug;
    private int replicaNum;
    
    ResponseCollectorList collector;


    public FrontEndObserverList(boolean debug, ResponseCollectorList collector, int i) {
        this.debug = debug;
        this.collector = collector;
        this.replicaNum = i;
    }

    @Override
    public void onNext(R response) {
        // add response to the ith position of the response collector
        if (response instanceof enterCriticalSectionResponse)
            collector.addResponse(replicaNum, ((enterCriticalSectionResponse) response).getTuplesList());
        else if(response instanceof getTupleSpacesStateResponseServer) {
            collector.addResponse(replicaNum, ((getTupleSpacesStateResponseServer) response).getTupleList());
        }
        
        if(this.debug) { System.err.println("OBSERVER: Received response from a server\n" + response.toString() + "\n"); }
    }

    @Override
    public void onError(Throwable throwable) {
        // add error code to the ith position of the response collector
        collector.addResponse(replicaNum, Collections.singletonList(Status.fromThrowable(throwable).getCode().name()));

        System.err.println("OBSERVER: ERROR: " + throwable.getMessage() + "\n");
    }

    @Override
    public void onCompleted() {
        if(this.debug) { System.out.println("OBSERVER: Request completed\n"); }
    }
}