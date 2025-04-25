import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')

import grpc
import TupleSpaces_pb2 as pb2
import TupleSpaces_pb2_grpc as pb2_grpc

class ClientService:
    def __init__(self, host_port: str, client_id: int, debug: bool):
        self.channel = grpc.insecure_channel(host_port)
        self.stub = pb2_grpc.TupleSpacesStub(self.channel)
        self.client_id = client_id
        self.debug = debug


    def close(self):
        if(self.debug): print("# Closing the channel", file=sys.stderr)
        self.channel.close()


    def put(self, tuple: str):
        request = pb2.PutRequest(newTuple = tuple)
        
        if(self.debug): print("# Sent a put request to the server: " + str(request), file=sys.stderr, end="")
        
        self.stub.put(request)

        if(self.debug): print("# Received a put response from the server\n", file=sys.stderr)


    def read(self, pattern: str):
        request = pb2.ReadRequest(searchPattern = pattern)
        
        if(self.debug): print("# Sent a read request to the server: " + str(request), file=sys.stderr, end="")

        response = self.stub.read(request)

        if(self.debug): print("# Received a read response from the server: " + str(response), file=sys.stderr)
        
        return response.result


    def take(self, pattern:str):
        request = pb2.TakeRequest(searchPattern = pattern, id = self.client_id)

        if(self.debug): print("# Sent a take request to the server: " + str(request), file=sys.stderr, end="")

        response = self.stub.take(request)

        if(self.debug): print("# Received a take response from the server: " + str(response), file=sys.stderr)

        return response.result


    def getTupleSpacesState(self):
        request = pb2.getTupleSpacesStateRequest()

        if(self.debug): print("# Sent a getTupleSpacesState request to the server\n", file=sys.stderr, end="")

        response = self.stub.getTupleSpacesState(request)

        if(self.debug): print("# Received a getTupleSpacesState response from the server:\n" + str(response) + "\n", file=sys.stderr, end="")

        return str(response.tuple)
