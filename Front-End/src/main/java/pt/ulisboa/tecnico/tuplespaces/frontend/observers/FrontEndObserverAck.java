package pt.ulisboa.tecnico.tuplespaces.frontend.observers;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.responsecollectors.ResponseCollector;

public class FrontEndObserverAck<R> implements StreamObserver<R>{
    
    private boolean debug;

    ResponseCollector collector;

    public FrontEndObserverAck(boolean debug, ResponseCollector collector) {
        this.debug = debug;
        this.collector = collector;
    }

    @Override
    public void onNext(R response) {
        // add acknowledgment to the response collector
        collector.addResponse("ACK");
        if(this.debug) { System.err.println("OBSERVER: Received response from the server\n"); }
    }
    
    @Override
    public void onError(Throwable throwable) {
        // add error code to the response collector
        collector.addResponse(Status.fromThrowable(throwable).getCode().name());
        
        if(this.debug) { System.err.println("OBSERVER: ERROR: " + throwable.getMessage() + "\n"); }
    }

    @Override
    public void onCompleted() {
        if(this.debug) { System.out.println("OBSERVER: Request completed\n"); }
    }
}
