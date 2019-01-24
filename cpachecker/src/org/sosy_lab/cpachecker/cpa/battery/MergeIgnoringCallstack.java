package org.sosy_lab.cpachecker.cpa.battery;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class MergeIgnoringCallstack implements MergeOperator{

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision) throws CPAException {
    if (pState1 instanceof BatteryState && pState2 instanceof BatteryState) {
      BatteryState e1 = (BatteryState) pState1;
      BatteryState e2 = (BatteryState) pState2;
      return e1.join(e2);
    }
    return pState2;
  }

}
