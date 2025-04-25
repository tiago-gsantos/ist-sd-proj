package pt.ulisboa.tecnico.tuplespaces.frontend.responsecollectors;

import java.util.ArrayList;
import java.lang.InterruptedException;

public class ResponseCollector implements ResponseCollectorInterface{
    ArrayList<String> collectedResponses;

    public ResponseCollector() {
        collectedResponses = new ArrayList<String>();
    }

    synchronized public void addResponse(String response) {
        collectedResponses.add(response);
        notifyAll();
    }

    synchronized public ArrayList<String> getResponses() { 
        return collectedResponses; 
    }

    @Override
    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (collectedResponses.size() < n)
            wait();
    }
}