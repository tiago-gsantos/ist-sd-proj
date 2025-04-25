package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Context;
import io.grpc.Contexts;

public class HeaderFrontEndInterceptor implements ServerInterceptor {

    // defining header key for delay
    static final Metadata.Key<String> DELAY_KEY = Metadata.Key.of("delay_key", Metadata.ASCII_STRING_MARSHALLER);

    // defining context key for delay
    static final Context.Key<String> DELAY_CONTEXT_KEY = Context.key("delay_context_key");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call,
        final Metadata requestHeaders,
        ServerCallHandler<ReqT, RespT> next) {
            String headerValue = requestHeaders.get(DELAY_KEY);

            if (headerValue != null) {
                Context context = Context.current().withValue(DELAY_CONTEXT_KEY, headerValue);
                return Contexts.interceptCall(context, call, requestHeaders, next);
            }

            return next.startCall(call, requestHeaders);
    }
}
