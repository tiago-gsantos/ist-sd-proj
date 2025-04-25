package pt.ulisboa.tecnico.tuplespaces.frontend.observers;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.responsecollectors.ResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesServerOuterClass.*;

public class FrontEndObserverResult<R> implements StreamObserver<R>{
    
    private boolean debug;

    ResponseCollector collector;

    public FrontEndObserverResult(boolean debug, ResponseCollector collector) {
        this.debug = debug;
        this.collector = collector;
    }

    @Override
    public void onNext(R response) {
        // add response to the response collector
        if (response instanceof ReadResponseServer) {
            collector.addResponse(((ReadResponseServer) response).getResult());
        }
        else if(response instanceof TakeResponseServer) {
            collector.addResponse(((TakeResponseServer) response).getResultTake());
        }
        
        if(this.debug) { System.err.println("OBSERVER: Received response from a server\n" + response.toString() + "\n"); }
    }

    @Override
    public void onError(Throwable throwable) {
        // add error code to the response collector
        collector.addResponse(Status.fromThrowable(throwable).getCode().name());

        System.err.println("OBSERVER: ERROR: " + throwable.getMessage() + "\n");
    }

    @Override
    public void onCompleted() {
        if(this.debug) { System.out.println("OBSERVER: Request completed\n"); }
    }
}