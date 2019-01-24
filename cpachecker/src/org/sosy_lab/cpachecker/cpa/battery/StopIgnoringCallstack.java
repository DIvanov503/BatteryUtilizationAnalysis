package org.sosy_lab.cpachecker.cpa.battery;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

public class StopIgnoringCallstack implements StopOperator{

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision) {
    try {
      BatteryState e1 = (BatteryState) pState;
      BatteryState e2;
      for (AbstractState p : pReached) {
        e2 = (BatteryState) p;
        if (e1.deltaH <= e2.deltaH) {
          return true;
        }
      }
    } catch (ClassCastException e) {
    }
    return false;
  }

}
