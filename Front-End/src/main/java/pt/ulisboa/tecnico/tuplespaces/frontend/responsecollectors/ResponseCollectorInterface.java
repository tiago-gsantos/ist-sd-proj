package pt.ulisboa.tecnico.tuplespaces.frontend.responsecollectors;

public interface ResponseCollectorInterface {
    void waitUntilAllReceived(int numServers) throws InterruptedException;
}
