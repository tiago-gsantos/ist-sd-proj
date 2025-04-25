package pt.ulisboa.tecnico.tuplespaces.frontend.responsecollectors;

import java.util.List;
import java.lang.InterruptedException;

public class ResponseCollectorList implements ResponseCollectorInterface{
    private final String[][] collectedResponses;

    public ResponseCollectorList(int size) {
        collectedResponses = new String[size][];
    }

    synchronized public void addResponse(int i, List<String> response) {
        if (i >= 0 && i < collectedResponses.length) {
            collectedResponses[i] = response.toArray(new String[0]);
            notifyAll();
        }
    }

    synchronized public String[][] getAllResponses() { 
        return collectedResponses; 
    }

    synchronized public String[] getResponses(int i) { 
        return collectedResponses[i]; 
    }

    @Override
    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (countReceived() < n) {
            wait();
        }
    }

    private int countReceived() {
        int count = 0;
        for (String[] response : collectedResponses) {
            if (response != null) {
                count++;
            }
        }
        return count;
    }
}