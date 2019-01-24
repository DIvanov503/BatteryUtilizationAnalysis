package org.sosy_lab.cpachecker.cpa.battery;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class ForgetfulPathUpdate implements PathUpdate {

  public static ForgetfulPathUpdate create(Configuration pConfig) {
    return new ForgetfulPathUpdate();
  }

  @Override
  public List<CFAEdge> update(List<CFAEdge> oldPath, ListIterator<CFAEdge> cycleStart, CFAEdge currentEdge) {
    return new ArrayList<CFAEdge>();
  }

}
