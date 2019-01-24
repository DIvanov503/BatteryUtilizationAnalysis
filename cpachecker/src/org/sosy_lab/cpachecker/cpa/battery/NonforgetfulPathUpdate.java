package org.sosy_lab.cpachecker.cpa.battery;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class NonforgetfulPathUpdate implements PathUpdate {

  public static NonforgetfulPathUpdate create(Configuration pConfig){
    return new NonforgetfulPathUpdate();
  }

  @Override
  public List<CFAEdge> update(List<CFAEdge> oldPath, ListIterator<CFAEdge> cycleStart, CFAEdge currentEdge) {
    List<CFAEdge> newPath;
    newPath = new ArrayList<CFAEdge>(oldPath.subList(cycleStart.nextIndex() + 1, oldPath.size()));
    newPath.add(currentEdge);
    return newPath;
  }

}
