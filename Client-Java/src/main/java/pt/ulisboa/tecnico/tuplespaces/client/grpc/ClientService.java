package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.client.ClientObserver;

public class ClientService {

    private final TupleSpacesGrpc.TupleSpacesBlockingStub stub;
    private final TupleSpacesGrpc.TupleSpacesStub asyncStub;
    private final ManagedChannel channel;

    private final int client_id;

    private final boolean debug;
    
    // defining header key for delay
    private static final Metadata.Key<String> DELAY_KEY = Metadata.Key.of("delay_key", Metadata.ASCII_STRING_MARSHALLER);
    
    public ClientService(String host_port, int client_id, boolean debug) {
        //create channel
        this.channel = ManagedChannelBuilder.forTarget(host_port).usePlaintext().build();
        
        //create stubs
        this.stub = TupleSpacesGrpc.newBlockingStub(channel);
        this.asyncStub = TupleSpacesGrpc.newStub(channel);

        this.client_id = client_id;
        this.debug = debug;
    }

    public void close() {
        if(this.debug) { System.err.println("# Closing the channel"); }
        
        channel.shutdownNow();
    }

    public void put(String tuple, String delay) {
        // create put request
        PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).build();

        if(this.debug) { System.err.print("# Sent a put request to the server: " + request.toString()); }

        if (delay != null) {
            TupleSpacesGrpc.TupleSpacesStub asyncStubWithHeader = createAsyncStubWithHeader(delay);
            // send put request with header
            asyncStubWithHeader.put(request, new ClientObserver<>(this.debug));
        }
        else {
            // send put request
            asyncStub.put(request, new ClientObserver<>(this.debug));
        }
    }

    public String read(String pattern, String delay) {
        // create read request
        ReadRequest request = ReadRequest.newBuilder().setSearchPattern(pattern).build();

        if(this.debug) { System.err.print("# Sent a read request to the server: " + request.toString()); }

        ReadResponse response;

        if (delay != null) {        
            TupleSpacesGrpc.TupleSpacesBlockingStub stubWithHeader = createStubWithHeader(delay);   
            // send read request with header
            response = stubWithHeader.read(request);
        }
        else {
            // send read request
            response = stub.read(request);
        }

        if(this.debug) { System.err.println("# Received a read response from the server: " + response.toString()); }

        return response.getResult();
    }

    public String take(String pattern, String delay) {
        // create take request
        TakeRequest request = TakeRequest.newBuilder().setSearchPattern(pattern).setId(this.client_id).build();

        if(this.debug) { System.err.print("# Sent a take request to the server: " + request.toString()); }

        TakeResponse response;

        if (delay != null) {
            TupleSpacesGrpc.TupleSpacesBlockingStub stubWithHeader = createStubWithHeader(delay);
            // send take request
            response = stubWithHeader.take(request);
        }
        else {
            // send take request
            response = stub.take(request);
        }

        if(this.debug) { System.err.println("# Received a take response from the server: " + response.toString()); }

        return response.getResult();
    }

    public String getTupleSpacesState() {
        // create getTupleSpacesState request
        getTupleSpacesStateRequest request = getTupleSpacesStateRequest.getDefaultInstance();

        if(this.debug) { System.err.print("# Sent a getTupleSpacesState request to the server\n"); }

        // send getTupleSpacesState request
        getTupleSpacesStateResponse response = stub.getTupleSpacesState(request);

        if(this.debug) { System.err.print("# Received a getTupleSpacesState response from the server:\n" + response.toString() + "\n"); }

        return response.getTupleList().toString();
    }

    public TupleSpacesGrpc.TupleSpacesBlockingStub createStubWithHeader(String delay) {
        // create metadata
        Metadata metadata = new Metadata();
        metadata.put(DELAY_KEY, delay);

        if(this.debug) { System.err.print("# Added header to the request: " + metadata.toString() + "\n"); }

        // create blocking stub with header
        return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor((metadata)));
    }

    public TupleSpacesGrpc.TupleSpacesStub createAsyncStubWithHeader(String delay) {
        // create metadata
        Metadata metadata = new Metadata();
        metadata.put(DELAY_KEY, delay);

        if(this.debug) { System.err.print("# Added header to the request: " + metadata.toString() + "\n"); }

        // create blocking stub with header
        return asyncStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor((metadata)));
    }
}
