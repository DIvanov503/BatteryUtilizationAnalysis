package org.sosy_lab.cpachecker.cpa.battery;

import com.google.common.base.Objects;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

public class BatteryState implements AbstractState, Serializable,
                                     LatticeAbstractState<BatteryState>, Graphable {

  private static final long serialVersionUID = -7715698130795640052L;

  public static final BatteryState
      topElement = new BatteryState();

  public double deltaH = 0;
  public List<CFAEdge> path = new ArrayList<CFAEdge>();

  public BatteryState() {}

  public BatteryState(double deltaH, List<CFAEdge> path) {
    this.deltaH = deltaH;
    this.path = path;
  }

  public BatteryState(double deltaH) {
    this.deltaH = deltaH;
  }

  @Override
  public boolean isLessOrEqual(BatteryState higher) {
    return (higher.deltaH >= this.deltaH || higher == topElement);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(deltaH, path);
  }

  @Override
  public boolean equals(Object pO) {
    if (pO instanceof BatteryState) {
      BatteryState
          other = (BatteryState) pO;
      return deltaH == other.deltaH
          && Objects.equal(path, other.path);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return toDOTLabel();
  }

  @Override
  public BatteryState join(BatteryState toJoin) {
    if (toJoin == topElement || this == topElement) {
      return topElement;
    }
    if (toJoin == this || deltaH > toJoin.deltaH) {
      return this;
    } else {
      return toJoin;
    }
  }

  public BatteryState update(double deltaH, List<CFAEdge> path) {
    this.deltaH = deltaH;
    this.path = path;
    return this;
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();

    out.writeDouble(deltaH);

    out.writeInt(path.size());
    for (CFAEdge edge : path) {
      out.writeObject(edge);
    }
  }

  @SuppressWarnings("unchecked")
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    int size;
    deltaH = in.readDouble();
    size = in.readInt();
    for(int i = 0; i < size; i++) {
      path.add((CFAEdge) in.readObject());
    }
  }

  @Override
  public String toDOTLabel() {

    StringBuilder sb = new StringBuilder();

    sb.append("{");
    sb.append("\\n");

    sb.append(System.identityHashCode(this));
    sb.append("\\n");

    // create a string like: delta h:  value
    sb.append("delta h: ");
    sb.append(Double.toString(deltaH));
    sb.append("\\n");

    // create a string like: path:  [label1; label2; ... ; ...]
    sb.append("path: ");
    sb.append(createStringOfList(path));
    sb.append("\\n");

    sb.append("}");

    return sb.toString();
  }

  private String createStringOfList(List<CFAEdge> list) {
    StringBuilder sb = new StringBuilder();
    sb.append(" [");

    boolean first=true;

    for (CFAEdge entry : list) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }

      sb.append(" ");
      sb.append(entry);
    }
    sb.append("]");
    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

}
