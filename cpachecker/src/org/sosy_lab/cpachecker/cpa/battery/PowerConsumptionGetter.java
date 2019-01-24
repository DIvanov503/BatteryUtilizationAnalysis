package org.sosy_lab.cpachecker.cpa.battery;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public interface PowerConsumptionGetter {

  PowerConsumption getPowerConsumption(CFAEdge edge);

}
