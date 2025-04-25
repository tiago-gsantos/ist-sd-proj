package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class ClientObserver<R> implements StreamObserver<R>{
    
    private boolean debug;

    public ClientObserver(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void onNext(R response) {
        if(this.debug) { System.err.println("# OBSERVER: Received response\n"); }
    }
    
    @Override
    public void onError(Throwable throwable) {
        System.err.println("OBSERVER: ERROR: " + throwable.getMessage() + "\n");
    }

    @Override
    public void onCompleted() {
        if(this.debug) { System.out.println("# OBSERVER: Request completed\n"); }
    }
}
