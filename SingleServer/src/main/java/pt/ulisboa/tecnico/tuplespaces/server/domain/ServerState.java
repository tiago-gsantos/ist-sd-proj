package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.lang.InterruptedException;
import java.util.stream.Collectors;
import java.util.Arrays;

public class ServerState {

  private List<String> tuples;
  
  private List<String> criticalSection;

  public ServerState() {
    this.tuples = new ArrayList<String>();
    this.criticalSection = new ArrayList<String>();
  }

  public synchronized void put(String tuple, boolean debug) {
    // add tuple to TupleSpaces and notify all waiting threads
    tuples.add(tuple);
    notifyAll();

    if(debug) { System.err.println(tuple + " added to Tuple Spaces. Notifying all waiting threads\n"); }
  }

  private String getMatchingTuple(String pattern, boolean isCriticalSection) {
    // return first matching tuple
    for (String tuple : (isCriticalSection ? this.criticalSection : this.tuples)) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  public synchronized String read(String pattern, boolean debug) {
    String tuple = getMatchingTuple(pattern, false);

    // wait until there's a matching tuple in TupleSpaces
    while(tuple == null) {
      if(debug) { System.err.println(pattern + " not found in Tuple Spaces. Waiting...\n"); }
      try {
        wait();
      } catch (InterruptedException e) {
        if(debug) { System.err.println("ERROR: The waiting thread was interrupted\n"); }

        throw new RuntimeException(e);
      }

      tuple = getMatchingTuple(pattern, false);
    }

    if(debug) { System.err.println(pattern + " read from Tuple Spaces\n"); }
    return tuple;
  }

  public synchronized String take(String pattern, boolean debug) {
    // read tuple from TupleSpaces
    String tuple = read(pattern, debug);

    // remove tuple from TupleSpaces
    this.tuples.remove(tuple);

    if(debug) { System.err.println(pattern + " removed from Tuple Spaces\n"); }
    return tuple;
  }

  public synchronized List<String> getTupleSpacesState() {
    return this.tuples;
  }

  public synchronized List<String> enterCriticalSection(String searchPattern, boolean debug) {
    List<String> tuplesToLock = new ArrayList<>();
    
    while(true) {
      // get the tuples from tupleSpace that are not in Critical Section
      List<String> availableTuples = new ArrayList<>(this.tuples);
      for (String t : this.criticalSection) {
        availableTuples.remove(t);
      }

      // get all distinct tuples that match the searchPattern and are not on the Critical Section
      tuplesToLock =  availableTuples.stream()
                                     .filter(t -> t.matches(searchPattern))
                                     .distinct()
                                     .collect(Collectors.toList());

      // Wait while there is no matching tuple
      if(tuplesToLock.isEmpty()) {
        try {
          if(debug) { System.err.println("Can't enter Critical Section. Waiting...\n"); }
          wait();
        } catch (InterruptedException e) {
          if(debug) { System.err.println("ERROR: The waiting thread was interrupted\n"); }
          throw new RuntimeException(e);
        }
      } else {
        break;
      }
    }
    
    //add tuples to CriticalSectionList
    for(String tuple : tuplesToLock) {
      this.criticalSection.add(tuple);
    }
    
    if(debug) { System.err.println(tuplesToLock + " added to Critical Section list\n"); }
    return tuplesToLock;
  }

  public synchronized void exitCriticalSection(String[] tuples, boolean debug) {
    // remove tuples form CriticalSectionList
    for(String t : tuples) {
      this.criticalSection.remove(t);
      notifyAll();
    }

    if(debug) { System.err.println(Arrays.toString(tuples) + " removed from Critcal Section list. Notifying all waiting threads\n"); }
  }

  public void sleep(double delay, boolean debug) {
    try {
      if(debug) { System.err.println("Sleeping for " + delay + " seconds\n"); }
      Thread.sleep(Math.round(delay*1000));
    } catch (InterruptedException e) {
      System.err.println("ERROR: The sleeping thread was interrupted\n");
    }
  }
}
