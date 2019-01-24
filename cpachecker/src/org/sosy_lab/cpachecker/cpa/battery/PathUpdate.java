package org.sosy_lab.cpachecker.cpa.battery;

import java.util.List;
import java.util.ListIterator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public interface PathUpdate {

  List<CFAEdge> update(List<CFAEdge> oldPath, ListIterator<CFAEdge> cycleStart, CFAEdge currentEdge);

}
